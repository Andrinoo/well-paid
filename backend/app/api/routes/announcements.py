from datetime import UTC, datetime
import uuid
from typing import Annotated, Any

from fastapi import APIRouter, Depends, HTTPException, Query, Request, status
from sqlalchemy import and_, case, func, or_, select
from sqlalchemy.orm import Session, joinedload

from app.api.deps import get_current_admin_user, get_current_user
from app.core.database import get_db
from app.core.limiter import limiter
from app.models.admin_audit_event import AdminAuditEvent
from app.models.announcement import Announcement
from app.models.announcement_user_state import AnnouncementUserState
from app.models.user import User
from app.schemas.announcement import (
    AnnouncementListResponse,
    AnnouncementPatch,
    AnnouncementRow,
    AnnouncementWrite,
    AnnouncementPlacement,
)

router = APIRouter(tags=["announcements"])

_MAX_LIMIT = 100
_UNSET = object()


def _resolve_target_user_id(db: Session, email: str | None) -> uuid.UUID | None:
    if not email or not email.strip():
        return None
    s = email.strip().lower()
    user = db.scalar(select(User).where(User.email == s))
    if user is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Utilizador não encontrado para este e-mail.",
        )
    return user.id


def _announcement_to_row(
    db: Session,
    row: Announcement,
    *,
    user_read_at: datetime | None = None,
    engagement_read_count: int = 0,
    engagement_hidden_count: int = 0,
) -> AnnouncementRow:
    base = AnnouncementRow.model_validate(row)
    email: str | None = None
    if row.target_user_id:
        u = row.target_user
        if u is None:
            u = db.get(User, row.target_user_id)
        email = u.email if u else None
    return base.model_copy(
        update={
            "target_user_email": email,
            "user_read_at": user_read_at,
            "engagement_read_count": engagement_read_count,
            "engagement_hidden_count": engagement_hidden_count,
        }
    )


def _announcement_visible_to_user(_db: Session, user: User, ann: Announcement) -> bool:
    now = datetime.now(UTC)
    if not ann.is_active:
        return False
    if ann.starts_at is not None and ann.starts_at > now:
        return False
    if ann.ends_at is not None and ann.ends_at < now:
        return False
    if ann.target_user_id is not None and ann.target_user_id != user.id:
        return False
    return True


def _hidden_announcement_ids_for_user(db: Session, user_id: uuid.UUID) -> list[uuid.UUID]:
    return list(
        db.scalars(
            select(AnnouncementUserState.announcement_id).where(
                AnnouncementUserState.user_id == user_id,
                AnnouncementUserState.hidden_at.isnot(None),
            )
        ).all()
    )


def _engagement_stats_for_announcements(
    db: Session, ann_ids: list[uuid.UUID]
) -> dict[uuid.UUID, tuple[int, int]]:
    if not ann_ids:
        return {}
    read_sum = func.coalesce(
        func.sum(case((AnnouncementUserState.read_at.isnot(None), 1), else_=0)),
        0,
    )
    hide_sum = func.coalesce(
        func.sum(case((AnnouncementUserState.hidden_at.isnot(None), 1), else_=0)),
        0,
    )
    rows = db.execute(
        select(AnnouncementUserState.announcement_id, read_sum, hide_sum)
        .where(AnnouncementUserState.announcement_id.in_(ann_ids))
        .group_by(AnnouncementUserState.announcement_id)
    ).all()
    return {r[0]: (int(r[1]), int(r[2])) for r in rows}


@router.get("/announcements/active", response_model=AnnouncementListResponse)
@limiter.limit("120/minute")
def list_active_announcements(
    request: Request,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
    placement: AnnouncementPlacement = "home_banner",
    limit: Annotated[int, Query(ge=1, le=_MAX_LIMIT)] = 20,
) -> AnnouncementListResponse:
    now = datetime.now(UTC)
    active_window = and_(
        Announcement.is_active.is_(True),
        or_(Announcement.starts_at.is_(None), Announcement.starts_at <= now),
        or_(Announcement.ends_at.is_(None), Announcement.ends_at >= now),
    )
    audience = or_(
        Announcement.target_user_id.is_(None),
        Announcement.target_user_id == user.id,
    )
    hidden_ids = _hidden_announcement_ids_for_user(db, user.id)
    base_where = and_(active_window, Announcement.placement == placement, audience)
    if hidden_ids:
        base_where = and_(base_where, Announcement.id.not_in(hidden_ids))

    total = int(db.scalar(select(func.count()).select_from(Announcement).where(base_where)) or 0)
    rows = list(
        db.scalars(
            select(Announcement)
            .where(base_where)
            .order_by(Announcement.priority.desc(), Announcement.created_at.desc())
            .limit(limit)
        ).all()
    )
    ids = [r.id for r in rows]
    state_map: dict[uuid.UUID, AnnouncementUserState] = {}
    if ids:
        for st in db.scalars(
            select(AnnouncementUserState).where(
                AnnouncementUserState.user_id == user.id,
                AnnouncementUserState.announcement_id.in_(ids),
            )
        ).all():
            state_map[st.announcement_id] = st

    items = []
    for r in rows:
        st = state_map.get(r.id)
        user_read_at = st.read_at if st else None
        items.append(_announcement_to_row(db, r, user_read_at=user_read_at))

    return AnnouncementListResponse(
        items=items,
        total=total,
        skip=0,
        limit=limit,
    )


@router.post("/announcements/{announcement_id}/read")
@limiter.limit("120/minute")
def mark_announcement_read(
    request: Request,
    announcement_id: uuid.UUID,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> dict[str, Any]:
    ann = db.get(Announcement, announcement_id)
    if ann is None or not _announcement_visible_to_user(db, user, ann):
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Aviso não encontrado")
    now = datetime.now(UTC)
    state = db.scalar(
        select(AnnouncementUserState).where(
            AnnouncementUserState.user_id == user.id,
            AnnouncementUserState.announcement_id == announcement_id,
        )
    )
    if state is None:
        state = AnnouncementUserState(
            user_id=user.id,
            announcement_id=announcement_id,
            read_at=now,
            updated_at=now,
        )
        db.add(state)
    else:
        state.read_at = now
        state.updated_at = now
    db.commit()
    return {"ok": True}


@router.post("/announcements/{announcement_id}/hide")
@limiter.limit("120/minute")
def hide_announcement_for_user(
    request: Request,
    announcement_id: uuid.UUID,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> dict[str, Any]:
    ann = db.get(Announcement, announcement_id)
    if ann is None or not _announcement_visible_to_user(db, user, ann):
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Aviso não encontrado")
    now = datetime.now(UTC)
    state = db.scalar(
        select(AnnouncementUserState).where(
            AnnouncementUserState.user_id == user.id,
            AnnouncementUserState.announcement_id == announcement_id,
        )
    )
    if state is None:
        state = AnnouncementUserState(
            user_id=user.id,
            announcement_id=announcement_id,
            hidden_at=now,
            updated_at=now,
        )
        db.add(state)
    else:
        state.hidden_at = now
        state.updated_at = now
    db.commit()
    return {"ok": True}


@router.get("/admin/announcements", response_model=AnnouncementListResponse)
@limiter.limit("120/minute")
def list_admin_announcements(
    request: Request,
    _admin: Annotated[User, Depends(get_current_admin_user)],
    db: Annotated[Session, Depends(get_db)],
    placement: AnnouncementPlacement | None = None,
    is_active: bool | None = None,
    skip: Annotated[int, Query(ge=0)] = 0,
    limit: Annotated[int, Query(ge=1, le=_MAX_LIMIT)] = 50,
) -> AnnouncementListResponse:
    stmt = select(Announcement).options(joinedload(Announcement.target_user))
    count_stmt = select(func.count()).select_from(Announcement)
    if placement is not None:
        flt = Announcement.placement == placement
        stmt = stmt.where(flt)
        count_stmt = count_stmt.where(flt)
    if is_active is not None:
        flt = Announcement.is_active.is_(is_active)
        stmt = stmt.where(flt)
        count_stmt = count_stmt.where(flt)
    total = int(db.scalar(count_stmt) or 0)
    rows = list(
        db.scalars(
            stmt.order_by(Announcement.created_at.desc()).offset(skip).limit(limit)
        ).unique().all()
    )
    stats = _engagement_stats_for_announcements(db, [r.id for r in rows])
    items = [
        _announcement_to_row(
            db,
            r,
            engagement_read_count=stats.get(r.id, (0, 0))[0],
            engagement_hidden_count=stats.get(r.id, (0, 0))[1],
        )
        for r in rows
    ]
    return AnnouncementListResponse(
        items=items,
        total=total,
        skip=skip,
        limit=limit,
    )


@router.post(
    "/admin/announcements",
    response_model=AnnouncementRow,
    status_code=status.HTTP_201_CREATED,
)
@limiter.limit("60/minute")
def create_announcement(
    request: Request,
    body: AnnouncementWrite,
    admin_user: Annotated[User, Depends(get_current_admin_user)],
    db: Annotated[Session, Depends(get_db)],
) -> AnnouncementRow:
    tid = _resolve_target_user_id(db, body.target_user_email)
    row = Announcement(
        title=body.title,
        body=body.body,
        kind=body.kind,
        placement=body.placement,
        priority=body.priority,
        cta_label=body.cta_label,
        cta_url=str(body.cta_url) if body.cta_url else None,
        is_active=body.is_active,
        starts_at=body.starts_at,
        ends_at=body.ends_at,
        created_by_user_id=admin_user.id,
        target_user_id=tid,
    )
    db.add(row)
    db.add(
        AdminAuditEvent(
            actor_user_id=admin_user.id,
            actor_email=admin_user.email,
            action="announcement.create",
            details={
                "placement": row.placement,
                "kind": row.kind,
                "is_active": row.is_active,
                "target_user_id": str(tid) if tid else None,
            },
        )
    )
    db.commit()
    db.refresh(row)
    row = db.scalars(
        select(Announcement)
        .where(Announcement.id == row.id)
        .options(joinedload(Announcement.target_user))
    ).one()
    return _announcement_to_row(db, row)


@router.patch("/admin/announcements/{announcement_id}", response_model=AnnouncementRow)
@limiter.limit("60/minute")
def patch_announcement(
    request: Request,
    announcement_id: uuid.UUID,
    body: AnnouncementPatch,
    admin_user: Annotated[User, Depends(get_current_admin_user)],
    db: Annotated[Session, Depends(get_db)],
) -> AnnouncementRow:
    row = db.get(Announcement, announcement_id)
    if row is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Aviso não encontrado",
        )
    changes = body.model_dump(exclude_unset=True)
    if not changes:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail="Nenhum campo para atualizar",
        )
    target_email_update: str | None | object = _UNSET
    if "target_user_email" in changes:
        raw = changes.pop("target_user_email")
        if raw is None:
            target_email_update = None  # explicit null → todos (sem destinatário)
        elif isinstance(raw, str) and raw == "":
            target_email_update = ""
        else:
            target_email_update = raw if isinstance(raw, str) else None

    if "title" in changes and isinstance(changes["title"], str):
        changes["title"] = changes["title"].strip()
    if "body" in changes and isinstance(changes["body"], str):
        changes["body"] = changes["body"].strip()
    if "cta_label" in changes and isinstance(changes["cta_label"], str):
        changes["cta_label"] = changes["cta_label"].strip() or None
    if "cta_url" in changes and changes["cta_url"] is not None:
        changes["cta_url"] = str(changes["cta_url"])
    next_starts_at = changes.get("starts_at", row.starts_at)
    next_ends_at = changes.get("ends_at", row.ends_at)
    if next_starts_at is not None and next_ends_at is not None and next_ends_at < next_starts_at:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail="ends_at deve ser maior ou igual a starts_at",
        )
    for key, value in changes.items():
        setattr(row, key, value)

    if target_email_update is not _UNSET:
        if target_email_update is None or target_email_update == "":
            row.target_user_id = None
        else:
            row.target_user_id = _resolve_target_user_id(db, str(target_email_update))

    db.add(
        AdminAuditEvent(
            actor_user_id=admin_user.id,
            actor_email=admin_user.email,
            action="announcement.patch",
            details={"announcement_id": str(row.id), "fields": sorted(body.model_dump(exclude_unset=True).keys())},
        )
    )
    db.add(row)
    db.commit()
    db.refresh(row)
    row = db.scalars(
        select(Announcement)
        .where(Announcement.id == row.id)
        .options(joinedload(Announcement.target_user))
    ).one()
    reads, hides = _engagement_stats_for_announcements(db, [row.id]).get(row.id, (0, 0))
    return _announcement_to_row(
        db,
        row,
        engagement_read_count=reads,
        engagement_hidden_count=hides,
    )
