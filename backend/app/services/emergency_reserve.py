"""Reserva de emergência: meta mensal + acréscimos idempotentes por mês civil."""

from __future__ import annotations

import uuid
from datetime import date
from typing import TYPE_CHECKING

from sqlalchemy import inspect, select
from sqlalchemy.orm import Session

from app.models.emergency_reserve import EmergencyReserve, EmergencyReserveAccrual
from app.models.family import FamilyMember

if TYPE_CHECKING:
    from app.models.user import User


def first_of_month(d: date) -> date:
    return date(d.year, d.month, 1)


def iter_months_inclusive(start: date, end: date):
    """start, end: primeiros dias do mês; end inclusivo."""
    y, m = start.year, start.month
    ey, em = end.year, end.month
    while (y, m) <= (ey, em):
        yield y, m
        if m == 12:
            y += 1
            m = 1
        else:
            m += 1


def _resolve_scope(db: Session, user_id: uuid.UUID) -> tuple[uuid.UUID | None, uuid.UUID | None]:
    row = db.scalar(select(FamilyMember).where(FamilyMember.user_id == user_id))
    if row is not None:
        return (row.family_id, None)
    return (None, user_id)


def get_reserve_for_user(db: Session, user_id: uuid.UUID) -> EmergencyReserve | None:
    family_id, solo_user_id = _resolve_scope(db, user_id)
    if family_id is not None:
        return db.scalar(
            select(EmergencyReserve).where(EmergencyReserve.family_id == family_id)
        )
    return db.scalar(
        select(EmergencyReserve).where(EmergencyReserve.solo_user_id == solo_user_id)
    )


def upsert_monthly_target(
    db: Session,
    user_id: uuid.UUID,
    monthly_target_cents: int,
) -> EmergencyReserve:
    family_id, solo_user_id = _resolve_scope(db, user_id)
    r = get_reserve_for_user(db, user_id)
    if r is not None:
        r.monthly_target_cents = int(monthly_target_cents)
        db.commit()
        db.refresh(r)
        return r

    anchor = first_of_month(date.today())
    r = EmergencyReserve(
        id=uuid.uuid4(),
        family_id=family_id,
        solo_user_id=solo_user_id if family_id is None else None,
        monthly_target_cents=int(monthly_target_cents),
        balance_cents=0,
        tracking_start=anchor,
    )
    db.add(r)
    db.commit()
    db.refresh(r)
    return r


def ensure_accruals(db: Session, reserve: EmergencyReserve, today: date) -> bool:
    """Cria linhas de acréscimo em falta de tracking_start até ao mês de `today`.
    Usa um único valor `monthly_target_cents` (estado actual) para todos os meses
    em falta nesta execução. Devolve True se alterou a BD."""
    tgt = int(reserve.monthly_target_cents)
    if tgt <= 0:
        return False

    start = first_of_month(reserve.tracking_start)
    end = first_of_month(today)
    if start > end:
        return False

    changed = False
    for y, m in iter_months_inclusive(start, end):
        exists = db.scalar(
            select(EmergencyReserveAccrual.id).where(
                EmergencyReserveAccrual.reserve_id == reserve.id,
                EmergencyReserveAccrual.year == y,
                EmergencyReserveAccrual.month == m,
            )
        )
        if exists is not None:
            continue
        db.add(
            EmergencyReserveAccrual(
                id=uuid.uuid4(),
                reserve_id=reserve.id,
                year=y,
                month=m,
                amount_cents=tgt,
            )
        )
        reserve.balance_cents = int(reserve.balance_cents) + tgt
        changed = True

    if changed:
        db.commit()
        db.refresh(reserve)
    return changed


def refresh_reserve_balances_for_user(db: Session, user: User, today: date | None = None) -> tuple[int, int]:
    """Garante acréscimos até ao mês corrente; devolve (balance, monthly_target)."""
    bind = db.get_bind()
    if bind is None or not inspect(bind).has_table("emergency_reserves"):
        return (0, 0)

    d = today or date.today()
    r = get_reserve_for_user(db, user.id)
    if r is None:
        return (0, 0)

    ensure_accruals(db, r, d)
    db.refresh(r)
    return (int(r.balance_cents), int(r.monthly_target_cents))


def list_accruals_for_user(
    db: Session,
    user_id: uuid.UUID,
    *,
    limit: int = 12,
) -> list[EmergencyReserveAccrual]:
    reserve = get_reserve_for_user(db, user_id)
    if reserve is None:
        return []
    n = max(1, min(int(limit), 60))
    rows = db.scalars(
        select(EmergencyReserveAccrual)
        .where(EmergencyReserveAccrual.reserve_id == reserve.id)
        .order_by(
            EmergencyReserveAccrual.year.desc(),
            EmergencyReserveAccrual.month.desc(),
        )
        .limit(n)
    ).all()
    return list(rows)
