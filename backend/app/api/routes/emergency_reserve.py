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
from app.models.family import Family
from app.models.family import FamilyMember
from app.models.user import User
from app.schemas.emergency_reserve import (
    EmergencyReserveAccrualItem,
    EmergencyReserveAccrualPatch,
    EmergencyReserveCompleteBody,
    EmergencyReserveMonthRow,
    EmergencyReservePlanCreate,
    EmergencyReservePlanItem,
    EmergencyReservePlanUpdate,
    EmergencyReserveResponse,
    EmergencyReserveUpdate,
)
from app.services.emergency_reserve import (
    complete_plan_transfer,
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
    member = db.execute(
        select(FamilyMember.family_id, FamilyMember.role).where(FamilyMember.user_id == user_id)
    ).one_or_none()
    if member is None:
        return
    family_id, role = member
    # Regra de edição restrita só quando há agregado familiar de facto (2+ membros).
    has_shared_scope = db.scalar(
        select(FamilyMember.id).where(FamilyMember.family_id == family_id).offset(1).limit(1)
    ) is not None
    # Fallback de segurança para evitar falso negativo: em alguns casos de dados legados
    # o criador da família pode estar sem role "owner" apesar de ser o titular efetivo.
    is_family_creator = db.scalar(
        select(Family.created_by_user_id).where(Family.id == family_id)
    ) == user_id
    if has_shared_scope and not (_is_owner_role(role) or is_family_creator):
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail=(
                "Apenas o titular da família pode alterar ou remover a reserva de emergência"
            ),
        )


@router.get("/plans", response_model=list[EmergencyReservePlanItem])
def list_reserve_plans(
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> list[EmergencyReservePlanItem]:
    if not _tables_ready(db):
        return []
    rows = list_plans_for_user(db, user.id, active_only=False)
    return [
        EmergencyReservePlanItem(
            id=r.id,
            title=r.title or "",
            details=r.details,
            monthly_target_cents=int(r.monthly_target_cents),
            target_cents=int(r.target_cents) if r.target_cents is not None else None,
            balance_cents=int(r.balance_cents),
            tracking_start=r.tracking_start,
            plan_duration_months=r.plan_duration_months,
            status=r.status,
            completed_at=r.completed_at.date() if r.completed_at else None,
        )
        for r in rows
    ]


@router.post("/plans", response_model=EmergencyReservePlanItem, status_code=status.HTTP_201_CREATED)
def create_reserve_plan(
    body: EmergencyReservePlanCreate,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> EmergencyReservePlanItem:
    if not _tables_ready(db):
        raise _reserve_unavailable()
    _require_owner_if_family_scope(db, user.id)
    p = create_plan(
        db,
        user.id,
        title=body.title,
        details=body.details,
        monthly_target_cents=body.monthly_target_cents,
        target_cents=body.target_cents,
        tracking_start=body.tracking_start,
        plan_duration_months=body.plan_duration_months,
    )
    return EmergencyReservePlanItem(
        id=p.id,
        title=p.title or "",
        details=p.details,
        monthly_target_cents=int(p.monthly_target_cents),
        target_cents=int(p.target_cents) if p.target_cents is not None else None,
        balance_cents=int(p.balance_cents),
        tracking_start=p.tracking_start,
        plan_duration_months=p.plan_duration_months,
        status=p.status,
        completed_at=p.completed_at.date() if p.completed_at else None,
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
            plan_duration_months=body.plan_duration_months,
        )
    except ValueError as e:
        raise HTTPException(status.HTTP_400_BAD_REQUEST, detail=str(e)) from e
    if p is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, detail="Plano não encontrado")
    return EmergencyReservePlanItem(
        id=p.id,
        title=p.title or "",
        details=p.details,
        monthly_target_cents=int(p.monthly_target_cents),
        target_cents=int(p.target_cents) if p.target_cents is not None else None,
        balance_cents=int(p.balance_cents),
        tracking_start=p.tracking_start,
        plan_duration_months=p.plan_duration_months,
        status=p.status,
        completed_at=p.completed_at.date() if p.completed_at else None,
    )


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
    return EmergencyReservePlanItem(
        id=p.id,
        title=p.title or "",
        details=p.details,
        monthly_target_cents=int(p.monthly_target_cents),
        target_cents=int(p.target_cents) if p.target_cents is not None else None,
        balance_cents=int(p.balance_cents),
        tracking_start=p.tracking_start,
        plan_duration_months=p.plan_duration_months,
        status=p.status,
        completed_at=p.completed_at.date() if p.completed_at else None,
    )


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
