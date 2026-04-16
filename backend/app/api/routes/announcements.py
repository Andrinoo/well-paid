from datetime import UTC, datetime
import uuid
from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, Query, Request, status
from sqlalchemy import and_, func, or_, select
from sqlalchemy.orm import Session

from app.api.deps import get_current_admin_user, get_current_user
from app.core.database import get_db
from app.core.limiter import limiter
from app.models.admin_audit_event import AdminAuditEvent
from app.models.announcement import Announcement
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


@router.get("/announcements/active", response_model=AnnouncementListResponse)
@limiter.limit("120/minute")
def list_active_announcements(
    request: Request,
    _user: Annotated[User, Depends(get_current_user)],
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
    total = int(
        db.scalar(
            select(func.count())
            .select_from(Announcement)
            .where(active_window, Announcement.placement == placement)
        )
        or 0
    )
    rows = list(
        db.scalars(
            select(Announcement)
            .where(active_window, Announcement.placement == placement)
            .order_by(Announcement.priority.desc(), Announcement.created_at.desc())
            .limit(limit)
        ).all()
    )
    return AnnouncementListResponse(
        items=[AnnouncementRow.model_validate(row) for row in rows],
        total=total,
        skip=0,
        limit=limit,
    )


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
    stmt = select(Announcement)
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
        ).all()
    )
    return AnnouncementListResponse(
        items=[AnnouncementRow.model_validate(row) for row in rows],
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
    )
    db.add(row)
    db.add(
        AdminAuditEvent(
            actor_user_id=admin_user.id,
            actor_email=admin_user.email,
            action="announcement.create",
            details={"placement": row.placement, "kind": row.kind, "is_active": row.is_active},
        )
    )
    db.commit()
    db.refresh(row)
    return AnnouncementRow.model_validate(row)


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
    db.add(
        AdminAuditEvent(
            actor_user_id=admin_user.id,
            actor_email=admin_user.email,
            action="announcement.patch",
            details={"announcement_id": str(row.id), "fields": sorted(changes.keys())},
        )
    )
    db.add(row)
    db.commit()
    db.refresh(row)
    return AnnouncementRow.model_validate(row)
