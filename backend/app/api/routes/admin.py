from datetime import UTC, datetime, timedelta
import uuid
from typing import Annotated, Literal

from fastapi import APIRouter, Depends, HTTPException, Query, Request, status
from sqlalchemy import func, select, update
from sqlalchemy.orm import Session, joinedload

from app.api.deps import get_current_admin_user
from app.core.database import get_db
from app.core.limiter import limiter
from app.models.admin_audit_event import AdminAuditEvent
from app.models.app_usage_event import AppUsageEvent
from app.models.category import Category
from app.models.emergency_reserve import EmergencyReserve, EmergencyReserveAccrual
from app.models.expense import Expense
from app.models.family import Family, FamilyMember
from app.models.goal import Goal
from app.models.goal_contribution import GoalContribution
from app.models.income import Income
from app.models.income_category import IncomeCategory
from app.models.refresh_token import RefreshToken
from app.models.shopping_list import ShoppingList
from app.models.shopping_list_item import ShoppingListItem
from app.models.user import User
from app.schemas.admin import (
    AdminFamilyDetailResponse,
    AdminFamilyInviteRow,
    AdminFamilyListResponse,
    AdminFamilyMemberRow,
    AdminFamilyRow,
    AdminAuditEventOut,
    AdminAuditListResponse,
    AdminFinanceSummaryResponse,
    AdminProductFunnelResponse,
    AdminUserDetailResponse,
    AdminUserRecentEvent,
    AdminMeResponse,
    AdminUsagePoint,
    AdminUsageSummaryResponse,
    AdminUserListResponse,
    AdminUserPatch,
    AdminUserPatchResponse,
    AdminUserRow,
)
from app.services.family_limits import MAX_FAMILY_MEMBERS

router = APIRouter(prefix="/admin", tags=["admin"])

_MAX_LIMIT = 100
_RECENT_EVENTS_LIMIT = 30


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


@router.get("/finance/summary", response_model=AdminFinanceSummaryResponse)
@limiter.limit("60/minute")
def finance_summary(
    request: Request,
    _admin: Annotated[User, Depends(get_current_admin_user)],
    db: Annotated[Session, Depends(get_db)],
) -> AdminFinanceSummaryResponse:
    today = datetime.now(UTC).date()
    from_30d = today - timedelta(days=30)

    expenses_total = int(db.scalar(select(func.count()).select_from(Expense)) or 0)
    expenses_active = int(
        db.scalar(
            select(func.count()).select_from(Expense).where(Expense.deleted_at.is_(None))
        )
        or 0
    )
    expenses_deleted = int(
        db.scalar(
            select(func.count())
            .select_from(Expense)
            .where(Expense.deleted_at.is_not(None))
        )
        or 0
    )
    expenses_shared = int(
        db.scalar(
            select(func.count())
            .select_from(Expense)
            .where(Expense.deleted_at.is_(None), Expense.is_shared.is_(True))
        )
        or 0
    )
    incomes_total = int(db.scalar(select(func.count()).select_from(Income)) or 0)
    goals_total = int(db.scalar(select(func.count()).select_from(Goal)) or 0)
    goal_contributions_total = int(
        db.scalar(select(func.count()).select_from(GoalContribution)) or 0
    )
    shopping_lists_total = int(
        db.scalar(select(func.count()).select_from(ShoppingList)) or 0
    )
    shopping_list_items_total = int(
        db.scalar(select(func.count()).select_from(ShoppingListItem)) or 0
    )
    emergency_reserves_total = int(
        db.scalar(select(func.count()).select_from(EmergencyReserve)) or 0
    )
    emergency_reserve_accruals_total = int(
        db.scalar(select(func.count()).select_from(EmergencyReserveAccrual)) or 0
    )
    categories_total = int(db.scalar(select(func.count()).select_from(Category)) or 0)
    income_categories_total = int(
        db.scalar(select(func.count()).select_from(IncomeCategory)) or 0
    )
    expenses_sum_cents_30d = int(
        db.scalar(
            select(func.coalesce(func.sum(Expense.amount_cents), 0)).where(
                Expense.deleted_at.is_(None),
                Expense.expense_date >= from_30d,
            )
        )
        or 0
    )
    incomes_sum_cents_30d = int(
        db.scalar(
            select(func.coalesce(func.sum(Income.amount_cents), 0)).where(
                Income.income_date >= from_30d,
            )
        )
        or 0
    )
    return AdminFinanceSummaryResponse(
        expenses_total=expenses_total,
        expenses_active=expenses_active,
        expenses_deleted=expenses_deleted,
        expenses_shared=expenses_shared,
        incomes_total=incomes_total,
        goals_total=goals_total,
        goal_contributions_total=goal_contributions_total,
        shopping_lists_total=shopping_lists_total,
        shopping_list_items_total=shopping_list_items_total,
        emergency_reserves_total=emergency_reserves_total,
        emergency_reserve_accruals_total=emergency_reserve_accruals_total,
        categories_total=categories_total,
        income_categories_total=income_categories_total,
        expenses_sum_cents_30d=expenses_sum_cents_30d,
        incomes_sum_cents_30d=incomes_sum_cents_30d,
    )


@router.get("/metrics/funnel", response_model=AdminProductFunnelResponse)
@limiter.limit("60/minute")
def product_funnel(
    request: Request,
    _admin: Annotated[User, Depends(get_current_admin_user)],
    db: Annotated[Session, Depends(get_db)],
) -> AdminProductFunnelResponse:
    now = datetime.now(UTC)
    from_7d = now - timedelta(days=7)
    from_30d = now - timedelta(days=30)

    users_total = int(db.scalar(select(func.count()).select_from(User)) or 0)
    email_verified_total = int(
        db.scalar(
            select(func.count())
            .select_from(User)
            .where(User.email_verified_at.is_not(None))
        )
        or 0
    )
    users_with_family_total = int(
        db.scalar(select(func.count()).select_from(FamilyMember)) or 0
    )
    users_with_expense_total = int(
        db.scalar(
            select(func.count(func.distinct(Expense.owner_user_id))).where(
                Expense.deleted_at.is_(None)
            )
        )
        or 0
    )
    users_with_income_total = int(
        db.scalar(select(func.count(func.distinct(Income.owner_user_id)))) or 0
    )
    users_app_open_7d = int(
        db.scalar(
            select(func.count(func.distinct(AppUsageEvent.user_id))).where(
                AppUsageEvent.event_type == "app_open",
                AppUsageEvent.occurred_at >= from_7d,
            )
        )
        or 0
    )
    signups_7d = int(
        db.scalar(select(func.count()).select_from(User).where(User.created_at >= from_7d))
        or 0
    )
    signups_30d = int(
        db.scalar(select(func.count()).select_from(User).where(User.created_at >= from_30d))
        or 0
    )
    return AdminProductFunnelResponse(
        users_total=users_total,
        email_verified_total=email_verified_total,
        users_with_family_total=users_with_family_total,
        users_with_expense_total=users_with_expense_total,
        users_with_income_total=users_with_income_total,
        users_app_open_7d=users_app_open_7d,
        signups_7d=signups_7d,
        signups_30d=signups_30d,
    )


@router.get("/audit/events", response_model=AdminAuditListResponse)
@limiter.limit("120/minute")
def list_audit_events(
    request: Request,
    _admin: Annotated[User, Depends(get_current_admin_user)],
    db: Annotated[Session, Depends(get_db)],
    skip: Annotated[int, Query(ge=0)] = 0,
    limit: Annotated[int, Query(ge=1, le=_MAX_LIMIT)] = 50,
) -> AdminAuditListResponse:
    total = int(db.scalar(select(func.count()).select_from(AdminAuditEvent)) or 0)
    rows = list(
        db.scalars(
            select(AdminAuditEvent)
            .order_by(AdminAuditEvent.created_at.desc())
            .offset(skip)
            .limit(limit)
        ).all()
    )
    return AdminAuditListResponse(
        items=[
            AdminAuditEventOut(
                id=r.id,
                created_at=r.created_at,
                actor_email=r.actor_email,
                action=r.action,
                target_email=r.target_email,
                details=r.details,
            )
            for r in rows
        ],
        total=total,
        skip=skip,
        limit=limit,
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
    is_active: Annotated[bool | None, Query(description="Filtrar contas ativas/inativas")] = None,
    is_admin: Annotated[bool | None, Query(description="Filtrar contas admin")] = None,
    email_verified: Annotated[
        bool | None, Query(description="Filtrar por e-mail verificado")
    ] = None,
    created_from: Annotated[
        datetime | None, Query(description="Criado a partir desta data/hora ISO")
    ] = None,
    created_to: Annotated[
        datetime | None, Query(description="Criado até esta data/hora ISO")
    ] = None,
    order_by: Annotated[
        Literal["created_at", "last_seen_at", "email"],
        Query(description="Campo de ordenação"),
    ] = "created_at",
    order_dir: Annotated[
        Literal["asc", "desc"], Query(description="Direção de ordenação")
    ] = "desc",
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
    if is_active is not None:
        flt = User.is_active.is_(is_active)
        stmt = stmt.where(flt)
        count_stmt = count_stmt.where(flt)
    if is_admin is not None:
        flt = User.is_admin.is_(is_admin)
        stmt = stmt.where(flt)
        count_stmt = count_stmt.where(flt)
    if email_verified is not None:
        if email_verified:
            flt = User.email_verified_at.is_not(None)
        else:
            flt = User.email_verified_at.is_(None)
        stmt = stmt.where(flt)
        count_stmt = count_stmt.where(flt)
    if created_from is not None:
        flt = User.created_at >= created_from
        stmt = stmt.where(flt)
        count_stmt = count_stmt.where(flt)
    if created_to is not None:
        flt = User.created_at <= created_to
        stmt = stmt.where(flt)
        count_stmt = count_stmt.where(flt)

    order_field = {
        "created_at": User.created_at,
        "last_seen_at": User.last_seen_at,
        "email": User.email,
    }[order_by]
    order_expr = order_field.asc() if order_dir == "asc" else order_field.desc()
    total = int(db.scalar(count_stmt) or 0)
    rows = list(
        db.scalars(
            stmt.order_by(order_expr).offset(skip).limit(limit)
        ).all()
    )
    return AdminUserListResponse(
        items=[AdminUserRow.model_validate(r) for r in rows],
        total=total,
        skip=skip,
        limit=limit,
    )


@router.get("/users/{user_id}", response_model=AdminUserDetailResponse)
@limiter.limit("120/minute")
def get_user_detail(
    request: Request,
    user_id: uuid.UUID,
    _admin: Annotated[User, Depends(get_current_admin_user)],
    db: Annotated[Session, Depends(get_db)],
) -> AdminUserDetailResponse:
    target = db.get(User, user_id)
    if target is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Utilizador não encontrado",
        )

    now = datetime.now(UTC)
    from_7d = now - timedelta(days=7)
    from_30d = now - timedelta(days=30)
    events_7d = int(
        db.scalar(
            select(func.count())
            .select_from(AppUsageEvent)
            .where(
                AppUsageEvent.user_id == target.id,
                AppUsageEvent.occurred_at >= from_7d,
            )
        )
        or 0
    )
    events_30d = int(
        db.scalar(
            select(func.count())
            .select_from(AppUsageEvent)
            .where(
                AppUsageEvent.user_id == target.id,
                AppUsageEvent.occurred_at >= from_30d,
            )
        )
        or 0
    )
    rows = list(
        db.execute(
            select(
                AppUsageEvent.event_type,
                func.count().label("total"),
            )
            .where(
                AppUsageEvent.user_id == target.id,
                AppUsageEvent.occurred_at >= from_30d,
            )
            .group_by(AppUsageEvent.event_type)
            .order_by(func.count().desc(), AppUsageEvent.event_type.asc())
        ).all()
    )
    breakdown = {str(r.event_type): int(r.total) for r in rows}
    recent_rows = list(
        db.scalars(
            select(AppUsageEvent)
            .where(AppUsageEvent.user_id == target.id)
            .order_by(AppUsageEvent.occurred_at.desc())
            .limit(_RECENT_EVENTS_LIMIT)
        ).all()
    )
    recent_events = [
        AdminUserRecentEvent(
            occurred_at=e.occurred_at,
            event_type=e.event_type,
        )
        for e in recent_rows
    ]
    return AdminUserDetailResponse(
        user=AdminUserRow.model_validate(target),
        events_7d=events_7d,
        events_30d=events_30d,
        event_types_30d=breakdown,
        recent_events=recent_events,
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
    admin_user: Annotated[User, Depends(get_current_admin_user)],
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
    if body.is_admin is not None:
        # Evita que um admin remova o próprio acesso por engano.
        if target.id == admin_user.id and body.is_admin is False:
            raise HTTPException(
                status_code=status.HTTP_409_CONFLICT,
                detail="Não é permitido remover o próprio acesso admin.",
            )
        target.is_admin = body.is_admin

    revoked_sessions = 0
    if body.revoke_sessions:
        revoked_sessions = int(
            db.scalar(
                select(func.count())
                .select_from(RefreshToken)
                .where(
                    RefreshToken.user_id == target.id,
                    RefreshToken.revoked.is_(False),
                )
            )
            or 0
        )
        db.execute(
            update(RefreshToken)
            .where(
                RefreshToken.user_id == target.id,
                RefreshToken.revoked.is_(False),
            )
            .values(revoked=True)
        )
    details: dict[str, object] = {}
    if body.is_active is not None:
        details["is_active"] = body.is_active
    if body.is_admin is not None:
        details["is_admin"] = body.is_admin
    if body.revoke_sessions:
        details["revoke_sessions"] = True
        details["revoked_sessions"] = revoked_sessions
    db.add(
        AdminAuditEvent(
            actor_user_id=admin_user.id,
            actor_email=admin_user.email,
            action="user.patch",
            target_user_id=target.id,
            target_email=target.email,
            details=details or None,
        )
    )
    db.add(target)
    db.commit()
    db.refresh(target)
    return AdminUserPatchResponse(
        id=target.id,
        email=target.email,
        is_active=target.is_active,
        is_admin=target.is_admin,
        revoked_sessions=revoked_sessions,
    )


@router.get("/families", response_model=AdminFamilyListResponse)
@limiter.limit("120/minute")
def list_families(
    request: Request,
    _admin: Annotated[User, Depends(get_current_admin_user)],
    db: Annotated[Session, Depends(get_db)],
    q: Annotated[str | None, Query(description="Pesquisa por nome (contém)")] = None,
    order_by: Annotated[
        Literal["created_at", "name"],
        Query(description="Campo de ordenação"),
    ] = "created_at",
    order_dir: Annotated[
        Literal["asc", "desc"], Query(description="Direção de ordenação")
    ] = "desc",
    skip: Annotated[int, Query(ge=0)] = 0,
    limit: Annotated[int, Query(ge=1, le=_MAX_LIMIT)] = 50,
) -> AdminFamilyListResponse:
    member_counts = (
        select(
            FamilyMember.family_id.label("fid"),
            func.count().label("mc"),
        )
        .group_by(FamilyMember.family_id)
    ).subquery()

    stmt = (
        select(Family, func.coalesce(member_counts.c.mc, 0).label("member_count"))
        .outerjoin(member_counts, Family.id == member_counts.c.fid)
    )
    count_stmt = select(func.count()).select_from(Family)
    if q and q.strip():
        term = f"%{q.strip()}%"
        flt = Family.name.ilike(term)
        stmt = stmt.where(flt)
        count_stmt = select(func.count()).select_from(Family).where(flt)

    order_field = Family.name if order_by == "name" else Family.created_at
    order_expr = order_field.asc() if order_dir == "asc" else order_field.desc()
    total = int(db.scalar(count_stmt) or 0)
    result_rows = db.execute(
        stmt.order_by(order_expr).offset(skip).limit(limit)
    ).all()
    items: list[AdminFamilyRow] = []
    for row in result_rows:
        fam = row[0]
        mc = int(row[1] or 0)
        items.append(
            AdminFamilyRow(
                id=fam.id,
                name=fam.name,
                member_count=mc,
                created_at=fam.created_at,
                updated_at=fam.updated_at,
                created_by_user_id=fam.created_by_user_id,
            )
        )
    return AdminFamilyListResponse(
        items=items,
        total=total,
        skip=skip,
        limit=limit,
    )


@router.get("/families/{family_id}", response_model=AdminFamilyDetailResponse)
@limiter.limit("120/minute")
def get_family_detail(
    request: Request,
    family_id: uuid.UUID,
    _admin: Annotated[User, Depends(get_current_admin_user)],
    db: Annotated[Session, Depends(get_db)],
) -> AdminFamilyDetailResponse:
    fam = db.scalars(
        select(Family)
        .where(Family.id == family_id)
        .options(
            joinedload(Family.members).joinedload(FamilyMember.user),
            joinedload(Family.invites),
        )
    ).unique().one_or_none()
    if fam is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Família não encontrada",
        )
    members_sorted = sorted(
        fam.members,
        key=lambda m: (
            0 if m.role == "owner" else 1,
            (m.user.email.lower() if m.user else ""),
        ),
    )
    member_rows: list[AdminFamilyMemberRow] = []
    for m in members_sorted:
        u = m.user
        if u is None:
            continue
        member_rows.append(
            AdminFamilyMemberRow(
                user_id=m.user_id,
                email=u.email,
                full_name=u.full_name,
                display_name=u.display_name,
                role=m.role,
                is_active=u.is_active,
            )
        )
    invites_sorted = sorted(
        fam.invites,
        key=lambda inv: inv.expires_at,
        reverse=True,
    )
    invite_rows = [
        AdminFamilyInviteRow(
            id=inv.id,
            expires_at=inv.expires_at,
            used=inv.used,
        )
        for inv in invites_sorted
    ]
    return AdminFamilyDetailResponse(
        id=fam.id,
        name=fam.name,
        member_count=len(fam.members),
        max_members=MAX_FAMILY_MEMBERS,
        created_at=fam.created_at,
        updated_at=fam.updated_at,
        created_by_user_id=fam.created_by_user_id,
        members=member_rows,
        invites=invite_rows,
    )
