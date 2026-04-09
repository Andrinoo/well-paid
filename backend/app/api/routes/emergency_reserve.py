"""Reserva de emergência (meta mensal + saldo acumulado)."""

from datetime import date
from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy import inspect
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.api.deps import get_current_user
from app.core.database import get_db
from app.models.family import FamilyMember
from app.models.user import User
from app.schemas.emergency_reserve import (
    EmergencyReserveAccrualItem,
    EmergencyReserveResponse,
    EmergencyReserveUpdate,
)
from app.services.emergency_reserve import (
    ensure_accruals,
    get_reserve_for_user,
    list_accruals_for_user,
    upsert_monthly_target,
)

router = APIRouter(prefix="/emergency-reserve", tags=["emergency-reserve"])


def _tables_ready(db: Session) -> bool:
    bind = db.get_bind()
    if bind is None:
        return False
    insp = inspect(bind)
    return insp.has_table("emergency_reserves") and insp.has_table(
        "emergency_reserve_accruals"
    )


def _require_owner_if_family_scope(db: Session, user_id) -> None:
    role = db.scalar(
        select(FamilyMember.role).where(FamilyMember.user_id == user_id)
    )
    if role is not None and role != "owner":
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Apenas o titular da família pode alterar a meta da reserva",
        )


@router.get("", response_model=EmergencyReserveResponse)
def read_emergency_reserve(
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> EmergencyReserveResponse:
    if not _tables_ready(db):
        anchor = date.today().replace(day=1)
        return EmergencyReserveResponse(
            monthly_target_cents=0,
            balance_cents=0,
            tracking_start=anchor,
            configured=False,
        )

    r = get_reserve_for_user(db, user.id)
    if r is None:
        anchor = date.today().replace(day=1)
        return EmergencyReserveResponse(
            monthly_target_cents=0,
            balance_cents=0,
            tracking_start=anchor,
            configured=False,
        )
    ensure_accruals(db, r, date.today())
    db.refresh(r)
    return EmergencyReserveResponse(
        monthly_target_cents=int(r.monthly_target_cents),
        balance_cents=int(r.balance_cents),
        tracking_start=r.tracking_start,
        configured=True,
    )


@router.put("", response_model=EmergencyReserveResponse)
def update_emergency_reserve(
    body: EmergencyReserveUpdate,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> EmergencyReserveResponse:
    if not _tables_ready(db):
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail=(
                "Reserva de emergência indisponível: base de dados sem migração "
                "necessária. Execute: python -m alembic upgrade head"
            ),
        )

    _require_owner_if_family_scope(db, user.id)
    r = upsert_monthly_target(db, user.id, body.monthly_target_cents)
    ensure_accruals(db, r, date.today())
    db.refresh(r)
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
