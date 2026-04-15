from datetime import UTC, datetime, timedelta
import uuid
from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, Query, Request, status
from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.api.deps import get_current_admin_user
from app.core.database import get_db
from app.core.limiter import limiter
from app.models.app_usage_event import AppUsageEvent
from app.models.user import User
from app.schemas.admin import (
    AdminMeResponse,
    AdminUsagePoint,
    AdminUsageSummaryResponse,
    AdminUserListResponse,
    AdminUserPatch,
    AdminUserPatchResponse,
    AdminUserRow,
)

router = APIRouter(prefix="/admin", tags=["admin"])

_MAX_LIMIT = 100


@router.get("/usage/summary", response_model=AdminUsageSummaryResponse)
@limiter.limit("60/minute")
def usage_summary(
    request: Request,
    _admin: Annotated[User, Depends(get_current_admin_user)],
    db: Annotated[Session, Depends(get_db)],
    days: Annotated[int, Query(ge=7, le=90)] = 14,
) -> AdminUsageSummaryResponse:
    now = datetime.now(UTC)
    from_24h = now - timedelta(hours=24)
    from_7d = now - timedelta(days=7)
    from_30d = now - timedelta(days=30)
    from_series = now - timedelta(days=days - 1)

    events_24h = int(
        db.scalar(
            select(func.count()).where(AppUsageEvent.occurred_at >= from_24h)
        )
        or 0
    )
    dau_7d = int(
        db.scalar(
            select(func.count(func.distinct(AppUsageEvent.user_id))).where(
                AppUsageEvent.occurred_at >= from_7d
            )
        )
        or 0
    )
    mau_30d = int(
        db.scalar(
            select(func.count(func.distinct(AppUsageEvent.user_id))).where(
                AppUsageEvent.occurred_at >= from_30d
            )
        )
        or 0
    )

    day_col = func.date_trunc("day", AppUsageEvent.occurred_at)
    points = list(
        db.execute(
            select(
                day_col.label("day"),
                func.count().label("events"),
                func.count(func.distinct(AppUsageEvent.user_id)).label("active_users"),
            )
            .where(AppUsageEvent.occurred_at >= from_series)
            .group_by(day_col)
            .order_by(day_col.asc())
        ).all()
    )
    series = [
        AdminUsagePoint(
            day=str(p.day.date()),
            events=int(p.events),
            active_users=int(p.active_users),
        )
        for p in points
    ]
    return AdminUsageSummaryResponse(
        events_24h=events_24h,
        dau_7d=dau_7d,
        mau_30d=mau_30d,
        series_days=days,
        series=series,
    )


@router.get("/me", response_model=AdminMeResponse)
@limiter.limit("60/minute")
def admin_me(
    request: Request,
    admin_user: Annotated[User, Depends(get_current_admin_user)],
) -> AdminMeResponse:
    return AdminMeResponse(email=admin_user.email, is_admin=True)


@router.get("/users", response_model=AdminUserListResponse)
@limiter.limit("120/minute")
def list_users(
    request: Request,
    _admin: Annotated[User, Depends(get_current_admin_user)],
    db: Annotated[Session, Depends(get_db)],
    q: Annotated[str | None, Query(description="Pesquisa por e-mail (contém)")] = None,
    skip: Annotated[int, Query(ge=0)] = 0,
    limit: Annotated[int, Query(ge=1, le=_MAX_LIMIT)] = 50,
) -> AdminUserListResponse:
    stmt = select(User)
    count_stmt = select(func.count()).select_from(User)
    if q and q.strip():
        term = f"%{q.strip().lower()}%"
        flt = User.email.ilike(term)
        stmt = stmt.where(flt)
        count_stmt = select(func.count()).select_from(User).where(flt)
    total = int(db.scalar(count_stmt) or 0)
    rows = list(
        db.scalars(
            stmt.order_by(User.created_at.desc()).offset(skip).limit(limit)
        ).all()
    )
    return AdminUserListResponse(
        items=[AdminUserRow.model_validate(r) for r in rows],
        total=total,
        skip=skip,
        limit=limit,
    )


@router.patch(
    "/users/{user_id}",
    response_model=AdminUserPatchResponse,
)
@limiter.limit("60/minute")
def patch_user(
    request: Request,
    user_id: uuid.UUID,
    body: AdminUserPatch,
    _admin: Annotated[User, Depends(get_current_admin_user)],
    db: Annotated[Session, Depends(get_db)],
) -> AdminUserPatchResponse:
    if not body.has_updates():
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail="Nenhum campo para atualizar",
        )
    target = db.get(User, user_id)
    if target is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Utilizador não encontrado",
        )
    if body.is_active is not None:
        target.is_active = body.is_active
    db.add(target)
    db.commit()
    db.refresh(target)
    return AdminUserPatchResponse(
        id=target.id,
        email=target.email,
        is_active=target.is_active,
    )
