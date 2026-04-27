"""Reserva de emergência (meta mensal + saldo acumulado)."""

from datetime import date
from typing import Annotated
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, Path, Query, Response, status
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.api.deps import get_current_user
from app.core.database import get_db
from app.core.schema_introspection import session_has_table
from app.models.family import FamilyMember
from app.models.user import User
from app.schemas.emergency_reserve import (
    EmergencyReserveAccrualItem,
    EmergencyReserveAccrualPatch,
    EmergencyReserveCompleteBody,
    EmergencyReserveContributionCreate,
    EmergencyReserveContributionItem,
    EmergencyReserveContributionResponse,
    EmergencyReserveMonthRow,
    EmergencyReservePlanCreate,
    EmergencyReservePlanItem,
    EmergencyReservePlanUpdate,
    EmergencyReserveResponse,
    EmergencyReserveUpdate,
)
from app.services.emergency_reserve import (
    complete_plan_transfer,
    create_contribution,
    create_plan,
    delete_plan_for_user,
    delete_accrual_for_user,
    delete_reserve_for_user,
    ensure_accruals,
    get_plan_for_user,
    legacy_aggregate_read,
    list_accruals_for_user,
    list_plans_for_user,
    month_breakdown_for_plan,
    plan_timeline_metrics,
    patch_accrual_for_user,
    update_plan_for_user,
    upsert_monthly_target,
)

router = APIRouter(prefix="/emergency-reserve", tags=["emergency-reserve"])


def _is_owner_role(role: str | None) -> bool:
    normalized = (role or "").strip().lower()
    return normalized in {"owner", "titular", "admin"}


def _tables_ready(db: Session) -> bool:
    return session_has_table(db, "emergency_reserve_plans") and session_has_table(
        db, "emergency_reserve_accruals"
    )


def _require_owner_if_family_scope(db: Session, user_id) -> None:
    row = db.scalar(select(FamilyMember).where(FamilyMember.user_id == user_id))
    if row is None:
        return
    members = db.scalars(
        select(FamilyMember.user_id).where(FamilyMember.family_id == row.family_id)
    ).all()
    # Ambiente de teste/edge-case: com família unitária, não bloquear operações de manutenção.
    if len(members) <= 1:
        return
    role = row.role
    if not _is_owner_role(role):
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Apenas o titular da família pode alterar a reserva de emergência.",
        )


def _has_family_peers(db: Session, user_id) -> bool:
    row = db.scalar(select(FamilyMember).where(FamilyMember.user_id == user_id))
    if row is None:
        return False
    members = db.scalars(
        select(FamilyMember.user_id).where(FamilyMember.family_id == row.family_id)
    ).all()
    return len(members) >= 2


@router.get("/plans", response_model=list[EmergencyReservePlanItem])
def list_reserve_plans(
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> list[EmergencyReservePlanItem]:
    if not _tables_ready(db):
        return []
    rows = list_plans_for_user(db, user.id, active_only=False)
    d = date.today()
    for r in rows:
        ensure_accruals(db, r, d)
    return [_to_plan_item(r) for r in rows]


def _to_plan_item(r) -> EmergencyReservePlanItem:
    metrics = plan_timeline_metrics(r)
    return EmergencyReservePlanItem(
        id=r.id,
        title=r.title or "",
        details=r.details,
        is_family=bool(r.family_id is not None),
        monthly_target_cents=int(r.monthly_target_cents),
        target_cents=int(r.target_cents) if r.target_cents is not None else None,
        balance_cents=int(r.balance_cents),
        opening_balance_cents=int(r.opening_balance_cents),
        tracking_start=r.tracking_start,
        target_end_date=r.target_end_date,
        plan_duration_months=r.plan_duration_months,
        months_total=metrics["months_total"],
        months_passed=metrics["months_passed"],
        months_remaining=metrics["months_remaining"],
        monthly_needed_cents=metrics["monthly_needed_cents"],
        pace_status=metrics["pace_status"],
        pace_delta_cents=metrics["pace_delta_cents"],
        status=r.status,
        completed_at=r.completed_at.date() if r.completed_at else None,
    )


@router.post("/plans", response_model=EmergencyReservePlanItem, status_code=status.HTTP_201_CREATED)
def create_reserve_plan(
    body: EmergencyReservePlanCreate,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> EmergencyReservePlanItem:
    if not _tables_ready(db):
        raise _reserve_unavailable()
    use_family_scope = bool(body.is_family) and bool(user.family_mode_enabled) and _has_family_peers(db, user.id)
    if use_family_scope:
        _require_owner_if_family_scope(db, user.id)
    p = create_plan(
        db,
        user.id,
        title=body.title,
        details=body.details,
        monthly_target_cents=body.monthly_target_cents,
        target_cents=body.target_cents,
        tracking_start=body.tracking_start,
        target_end_date=body.target_end_date,
        plan_duration_months=body.plan_duration_months,
        opening_balance_cents=body.opening_balance_cents,
        is_family=use_family_scope,
    )
    return _to_plan_item(p)


@router.post("/contributions", response_model=EmergencyReserveContributionResponse, status_code=status.HTTP_201_CREATED)
def create_manual_contribution(
    body: EmergencyReserveContributionCreate,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> EmergencyReserveContributionResponse:
    if not _tables_ready(db):
        raise _reserve_unavailable()
    _require_owner_if_family_scope(db, user.id)
    total_alloc = sum(int(a.amount_cents) for a in body.allocations)
    if total_alloc != int(body.total_amount_cents):
        raise HTTPException(
            status.HTTP_400_BAD_REQUEST,
            detail="A soma das alocações deve ser igual ao total do aporte",
        )
    try:
        c = create_contribution(
            db,
            user.id,
            contribution_date=body.contribution_date,
            total_amount_cents=body.total_amount_cents,
            allocations=[{"plan_id": a.plan_id, "amount_cents": a.amount_cents} for a in body.allocations],
            note=body.note,
        )
    except ValueError as e:
        raise HTTPException(status.HTTP_400_BAD_REQUEST, detail=str(e)) from e
    return EmergencyReserveContributionResponse(
        id=c.id,
        contribution_date=c.contribution_date,
        total_amount_cents=int(c.total_amount_cents),
        note=c.note,
        created_at=c.created_at.date() if c.created_at else None,
        items=[
            EmergencyReserveContributionItem(
                plan_id=i.plan_id,
                amount_cents=int(i.amount_cents),
            )
            for i in c.items
        ],
    )


@router.put("/plans/{plan_id}", response_model=EmergencyReservePlanItem)
def update_reserve_plan(
    plan_id: UUID,
    body: EmergencyReservePlanUpdate,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> EmergencyReservePlanItem:
    if not _tables_ready(db):
        raise _reserve_unavailable()
    use_family_scope = (
        bool(body.is_family)
        and bool(user.family_mode_enabled)
        and _has_family_peers(db, user.id)
    ) if body.is_family is not None else None
    if use_family_scope:
        _require_owner_if_family_scope(db, user.id)
    try:
        p = update_plan_for_user(
            db,
            user.id,
            plan_id,
            title=body.title,
            details=body.details,
            monthly_target_cents=body.monthly_target_cents,
            target_cents=body.target_cents,
            tracking_start=body.tracking_start,
            target_end_date=body.target_end_date,
            plan_duration_months=body.plan_duration_months,
            opening_balance_cents=body.opening_balance_cents,
            is_family=use_family_scope,
        )
    except ValueError as e:
        raise HTTPException(status.HTTP_400_BAD_REQUEST, detail=str(e)) from e
    if p is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, detail="Plano não encontrado")
    return _to_plan_item(p)


@router.delete("/plans/{plan_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_reserve_plan(
    plan_id: UUID,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> Response:
    if not _tables_ready(db):
        raise _reserve_unavailable()
    _require_owner_if_family_scope(db, user.id)
    if not delete_plan_for_user(db, user.id, plan_id):
        raise HTTPException(status.HTTP_404_NOT_FOUND, detail="Plano não encontrado")
    return Response(status_code=status.HTTP_204_NO_CONTENT)


@router.get("/plans/{plan_id}/months", response_model=list[EmergencyReserveMonthRow])
def list_plan_month_breakdown(
    plan_id: UUID,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> list[EmergencyReserveMonthRow]:
    if not _tables_ready(db):
        return []
    plan = get_plan_for_user(db, user.id, plan_id)
    if plan is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, detail="Plano não encontrado")
    ensure_accruals(db, plan, date.today())
    db.refresh(plan)
    rows = month_breakdown_for_plan(db, plan, today=date.today())
    return [EmergencyReserveMonthRow(**r) for r in rows]


@router.post("/plans/{plan_id}/complete", response_model=EmergencyReservePlanItem)
def complete_reserve_plan(
    plan_id: UUID,
    body: EmergencyReserveCompleteBody,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> EmergencyReservePlanItem:
    if not _tables_ready(db):
        raise _reserve_unavailable()
    _require_owner_if_family_scope(db, user.id)
    if body.goal_id is not None and body.to_plan_id is not None:
        raise HTTPException(
            status.HTTP_400_BAD_REQUEST,
            detail="Indique apenas um destino: goal_id ou to_plan_id",
        )
    try:
        if body.goal_id is not None:
            p = complete_plan_transfer(db, user.id, plan_id, goal_id=body.goal_id)
        elif body.to_plan_id is not None:
            p = complete_plan_transfer(db, user.id, plan_id, to_plan_id=body.to_plan_id)
        else:
            raise HTTPException(
                status.HTTP_400_BAD_REQUEST,
                detail="Indique goal_id ou to_plan_id",
            )
    except ValueError as e:
        raise HTTPException(status.HTTP_400_BAD_REQUEST, detail=str(e)) from e
    return _to_plan_item(p)


@router.get("", response_model=EmergencyReserveResponse)
def read_emergency_reserve(
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> EmergencyReserveResponse:
    anchor = date.today().replace(day=1)
    if not _tables_ready(db):
        return EmergencyReserveResponse(
            monthly_target_cents=0,
            balance_cents=0,
            tracking_start=anchor,
            configured=False,
        )

    bal, tgt, tr, cfg = legacy_aggregate_read(db, user.id, date.today())
    if not cfg:
        return EmergencyReserveResponse(
            monthly_target_cents=0,
            balance_cents=0,
            tracking_start=anchor,
            configured=False,
        )
    return EmergencyReserveResponse(
        monthly_target_cents=tgt,
        balance_cents=bal,
        tracking_start=tr,
        configured=True,
    )


@router.put("", response_model=EmergencyReserveResponse)
def update_emergency_reserve(
    body: EmergencyReserveUpdate,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> EmergencyReserveResponse:
    if not _tables_ready(db):
        raise _reserve_unavailable()

    _require_owner_if_family_scope(db, user.id)
    r = upsert_monthly_target(db, user.id, body.monthly_target_cents)
    ensure_accruals(db, r, date.today())
    db.refresh(r)
    return _to_response_single_plan(r)


def _to_response_single_plan(r) -> EmergencyReserveResponse:
    return EmergencyReserveResponse(
        monthly_target_cents=int(r.monthly_target_cents),
        balance_cents=int(r.balance_cents),
        tracking_start=r.tracking_start,
        configured=True,
    )


@router.get("/accruals", response_model=list[EmergencyReserveAccrualItem])
def list_emergency_reserve_accruals(
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
    limit: Annotated[int, Query(ge=1, le=60)] = 12,
) -> list[EmergencyReserveAccrualItem]:
    if not _tables_ready(db):
        return []
    rows = list_accruals_for_user(db, user.id, limit=limit)
    return [
        EmergencyReserveAccrualItem(
            year=int(r.year),
            month=int(r.month),
            amount_cents=int(r.amount_cents),
            created_at=r.created_at.date() if r.created_at is not None else None,
        )
        for r in rows
    ]


def _reserve_unavailable() -> HTTPException:
    return HTTPException(
        status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
        detail=(
            "Reserva de emergência indisponível: base de dados sem migração "
            "necessária. Execute: python -m alembic upgrade head"
        ),
    )


@router.patch("/accruals/{year}/{month}", response_model=EmergencyReserveResponse)
def patch_emergency_reserve_accrual(
    year: Annotated[int, Path(ge=2000, le=2100)],
    month: Annotated[int, Path(ge=1, le=12)],
    body: EmergencyReserveAccrualPatch,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> EmergencyReserveResponse:
    if not _tables_ready(db):
        raise _reserve_unavailable()
    _require_owner_if_family_scope(db, user.id)
    try:
        r = patch_accrual_for_user(db, user.id, year, month, body.amount_cents)
    except ValueError as e:
        raise HTTPException(status.HTTP_400_BAD_REQUEST, detail=str(e)) from e
    if r is None:
        raise HTTPException(
            status.HTTP_404_NOT_FOUND,
            detail="Reserva de emergência não configurada",
        )
    ensure_accruals(db, r, date.today())
    db.refresh(r)
    return _to_response_single_plan(r)


@router.delete("/accruals/{year}/{month}", response_model=EmergencyReserveResponse)
def delete_emergency_reserve_accrual(
    year: Annotated[int, Path(ge=2000, le=2100)],
    month: Annotated[int, Path(ge=1, le=12)],
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> EmergencyReserveResponse:
    if not _tables_ready(db):
        raise _reserve_unavailable()
    _require_owner_if_family_scope(db, user.id)
    reserve, deleted = delete_accrual_for_user(db, user.id, year, month)
    if reserve is None:
        raise HTTPException(
            status.HTTP_404_NOT_FOUND,
            detail="Reserva de emergência não configurada",
        )
    if not deleted:
        raise HTTPException(
            status.HTTP_404_NOT_FOUND,
            detail="Não existe crédito mensal para este período",
        )
    ensure_accruals(db, reserve, date.today())
    db.refresh(reserve)
    return _to_response_single_plan(reserve)


@router.delete("", status_code=status.HTTP_204_NO_CONTENT)
def delete_emergency_reserve(
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> Response:
    """Remove todos os planos de reserva e o histórico (recomeçar do zero)."""
    if not _tables_ready(db):
        raise _reserve_unavailable()
    _require_owner_if_family_scope(db, user.id)
    if not delete_reserve_for_user(db, user.id):
        raise HTTPException(
            status.HTTP_404_NOT_FOUND,
            detail="Reserva de emergência não configurada",
        )
    return Response(status_code=status.HTTP_204_NO_CONTENT)
