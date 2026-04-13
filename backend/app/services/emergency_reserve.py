"""Reserva de emergência: meta mensal + acréscimos idempotentes por mês civil."""

from __future__ import annotations

import uuid
from datetime import date
from typing import TYPE_CHECKING

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.schema_introspection import session_has_table
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


def month_key(year: int, month: int) -> str:
    return f"{year:04d}-{month:02d}"


def _skip_keys(reserve: EmergencyReserve) -> set[str]:
    raw = reserve.accrual_skip_months
    if raw is None:
        return set()
    if isinstance(raw, list):
        return {str(x) for x in raw}
    return set()


def _set_skip_keys(reserve: EmergencyReserve, keys: set[str]) -> None:
    reserve.accrual_skip_months = sorted(keys)


def add_skip_month(reserve: EmergencyReserve, year: int, month: int) -> None:
    k = month_key(year, month)
    cur = _skip_keys(reserve)
    cur.add(k)
    _set_skip_keys(reserve, cur)


def remove_skip_month(reserve: EmergencyReserve, year: int, month: int) -> None:
    k = month_key(year, month)
    cur = _skip_keys(reserve)
    cur.discard(k)
    _set_skip_keys(reserve, cur)


def is_month_skipped(reserve: EmergencyReserve, year: int, month: int) -> bool:
    return month_key(year, month) in _skip_keys(reserve)


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
        accrual_skip_months=[],
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
        if is_month_skipped(reserve, y, m):
            continue
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
    if not session_has_table(db, "emergency_reserves"):
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


def delete_accrual_for_user(
    db: Session,
    user_id: uuid.UUID,
    year: int,
    month: int,
) -> tuple[EmergencyReserve | None, bool]:
    """Remove o crédito do mês e regista o mês como ignorado para acréscimos automáticos.

    Devolve (reserva, True) se removeu uma linha; (reserva, False) se a reserva existe
    mas não havia crédito nesse mês; (None, False) se não há reserva configurada.
    """
    reserve = get_reserve_for_user(db, user_id)
    if reserve is None:
        return (None, False)
    row = db.scalar(
        select(EmergencyReserveAccrual).where(
            EmergencyReserveAccrual.reserve_id == reserve.id,
            EmergencyReserveAccrual.year == year,
            EmergencyReserveAccrual.month == month,
        )
    )
    if row is None:
        return (reserve, False)
    amt = int(row.amount_cents)
    db.delete(row)
    reserve.balance_cents = int(reserve.balance_cents) - amt
    add_skip_month(reserve, year, month)
    db.commit()
    db.refresh(reserve)
    return (reserve, True)


def patch_accrual_for_user(
    db: Session,
    user_id: uuid.UUID,
    year: int,
    month: int,
    amount_cents: int,
) -> EmergencyReserve | None:
    """Cria, actualiza ou remove (amount_cents=0) um crédito mensal; ajusta o saldo."""
    if amount_cents < 0:
        raise ValueError("amount_cents must be >= 0")
    reserve = get_reserve_for_user(db, user_id)
    if reserve is None:
        return None
    row = db.scalar(
        select(EmergencyReserveAccrual).where(
            EmergencyReserveAccrual.reserve_id == reserve.id,
            EmergencyReserveAccrual.year == year,
            EmergencyReserveAccrual.month == month,
        )
    )
    if row is None:
        if amount_cents == 0:
            add_skip_month(reserve, year, month)
        else:
            remove_skip_month(reserve, year, month)
            db.add(
                EmergencyReserveAccrual(
                    id=uuid.uuid4(),
                    reserve_id=reserve.id,
                    year=year,
                    month=month,
                    amount_cents=int(amount_cents),
                )
            )
            reserve.balance_cents = int(reserve.balance_cents) + int(amount_cents)
    else:
        old = int(row.amount_cents)
        if amount_cents == 0:
            db.delete(row)
            reserve.balance_cents = int(reserve.balance_cents) - old
            add_skip_month(reserve, year, month)
        else:
            remove_skip_month(reserve, year, month)
            row.amount_cents = int(amount_cents)
            reserve.balance_cents = int(reserve.balance_cents) + (int(amount_cents) - old)
    db.commit()
    db.refresh(reserve)
    return reserve


def delete_reserve_for_user(db: Session, user_id: uuid.UUID) -> bool:
    """Apaga a reserva e todo o histórico de créditos (CASCADE)."""
    r = get_reserve_for_user(db, user_id)
    if r is None:
        return False
    db.delete(r)
    db.commit()
    return True
