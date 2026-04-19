"""Reserva de emergência: planos múltiplos + acréscimos mensais por plano."""

from __future__ import annotations

import uuid
from datetime import date, datetime, timezone
from typing import TYPE_CHECKING

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.schema_introspection import session_has_table
from app.models.emergency_reserve import EmergencyReserveAccrual, EmergencyReservePlan
from app.models.family import FamilyMember
from app.models.goal import Goal
from app.models.goal_contribution import GoalContribution
from app.services.family_scope import family_peer_user_ids

if TYPE_CHECKING:
    from app.models.user import User


def first_of_month(d: date) -> date:
    return date(d.year, d.month, 1)


def add_months(d: date, months_delta: int) -> date:
    """d deve ser primeiro dia do mês; devolve primeiro dia do mês destino."""
    y, m = d.year, d.month + months_delta
    while m > 12:
        y += 1
        m -= 12
    while m < 1:
        y -= 1
        m += 12
    return date(y, m, 1)


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


def last_included_month_first(tracking_start: date, plan_duration_months: int | None) -> date | None:
    """Último mês (1.º dia) incluído no plano; None = sem fim fixo."""
    if plan_duration_months is None or plan_duration_months <= 0:
        return None
    start = first_of_month(tracking_start)
    return add_months(start, plan_duration_months - 1)


def _skip_keys(plan: EmergencyReservePlan) -> set[str]:
    raw = plan.accrual_skip_months
    if raw is None:
        return set()
    if isinstance(raw, list):
        return {str(x) for x in raw}
    return set()


def _set_skip_keys(plan: EmergencyReservePlan, keys: set[str]) -> None:
    plan.accrual_skip_months = sorted(keys)


def add_skip_month(plan: EmergencyReservePlan, year: int, month: int) -> None:
    k = month_key(year, month)
    cur = _skip_keys(plan)
    cur.add(k)
    _set_skip_keys(plan, cur)


def remove_skip_month(plan: EmergencyReservePlan, year: int, month: int) -> None:
    k = month_key(year, month)
    cur = _skip_keys(plan)
    cur.discard(k)
    _set_skip_keys(plan, cur)


def is_month_skipped(plan: EmergencyReservePlan, year: int, month: int) -> bool:
    return month_key(year, month) in _skip_keys(plan)


def _resolve_scope(db: Session, user_id: uuid.UUID) -> tuple[uuid.UUID | None, uuid.UUID | None]:
    row = db.scalar(select(FamilyMember).where(FamilyMember.user_id == user_id))
    if row is not None:
        return (row.family_id, None)
    return (None, user_id)


def _scope_filter(q, family_id: uuid.UUID | None, solo_user_id: uuid.UUID | None):
    if family_id is not None:
        return q.where(EmergencyReservePlan.family_id == family_id)
    return q.where(EmergencyReservePlan.solo_user_id == solo_user_id)


def list_plans_for_user(
    db: Session,
    user_id: uuid.UUID,
    *,
    active_only: bool = False,
) -> list[EmergencyReservePlan]:
    family_id, solo_user_id = _resolve_scope(db, user_id)
    q = select(EmergencyReservePlan)
    q = _scope_filter(q, family_id, solo_user_id)
    if active_only:
        q = q.where(EmergencyReservePlan.status == "active")
    q = q.order_by(EmergencyReservePlan.created_at.asc())
    return list(db.scalars(q).all())


def get_plan_for_user(
    db: Session,
    user_id: uuid.UUID,
    plan_id: uuid.UUID,
) -> EmergencyReservePlan | None:
    family_id, solo_user_id = _resolve_scope(db, user_id)
    q = select(EmergencyReservePlan).where(EmergencyReservePlan.id == plan_id)
    q = _scope_filter(q, family_id, solo_user_id)
    return db.scalar(q)


def primary_active_plan(plans: list[EmergencyReservePlan]) -> EmergencyReservePlan | None:
    active = [p for p in plans if p.status == "active"]
    if not active:
        return None
    return sorted(active, key=lambda p: p.created_at)[0]


def get_primary_plan_for_user(db: Session, user_id: uuid.UUID) -> EmergencyReservePlan | None:
    return primary_active_plan(list_plans_for_user(db, user_id, active_only=True))


def upsert_monthly_target(
    db: Session,
    user_id: uuid.UUID,
    monthly_target_cents: int,
) -> EmergencyReservePlan:
    family_id, solo_user_id = _resolve_scope(db, user_id)
    r = get_primary_plan_for_user(db, user_id)
    if r is not None:
        r.monthly_target_cents = int(monthly_target_cents)
        db.commit()
        db.refresh(r)
        return r

    anchor = first_of_month(date.today())
    r = EmergencyReservePlan(
        id=uuid.uuid4(),
        family_id=family_id,
        solo_user_id=solo_user_id if family_id is None else None,
        title="",
        monthly_target_cents=int(monthly_target_cents),
        balance_cents=0,
        tracking_start=anchor,
        accrual_skip_months=[],
        plan_duration_months=None,
        status="active",
    )
    db.add(r)
    db.commit()
    db.refresh(r)
    return r


def ensure_accruals(db: Session, plan: EmergencyReservePlan, today: date) -> bool:
    """Cria créditos mensais em falta até ao mês civil (respeitando duração do plano)."""
    if plan.status != "active":
        return False
    tgt = int(plan.monthly_target_cents)
    if tgt <= 0:
        return False

    start = first_of_month(plan.tracking_start)
    end = first_of_month(today)
    cap = last_included_month_first(plan.tracking_start, plan.plan_duration_months)
    if cap is not None and end > cap:
        end = cap
    if start > end:
        return False

    changed = False
    for y, m in iter_months_inclusive(start, end):
        if is_month_skipped(plan, y, m):
            continue
        exists = db.scalar(
            select(EmergencyReserveAccrual.id).where(
                EmergencyReserveAccrual.plan_id == plan.id,
                EmergencyReserveAccrual.year == y,
                EmergencyReserveAccrual.month == m,
            )
        )
        if exists is not None:
            continue
        db.add(
            EmergencyReserveAccrual(
                id=uuid.uuid4(),
                plan_id=plan.id,
                year=y,
                month=m,
                amount_cents=tgt,
            )
        )
        plan.balance_cents = int(plan.balance_cents) + tgt
        changed = True

    if changed:
        db.commit()
        db.refresh(plan)
    return changed


def ensure_all_active_plans(db: Session, user_id: uuid.UUID, today: date | None = None) -> None:
    d = today or date.today()
    for p in list_plans_for_user(db, user_id, active_only=True):
        ensure_accruals(db, p, d)


def legacy_aggregate_read(
    db: Session,
    user_id: uuid.UUID,
    today: date | None = None,
) -> tuple[int, int, date, bool]:
    """(balance, monthly_target, tracking_start, configured) para GET /emergency-reserve legado."""
    d = today or date.today()
    if not session_has_table(db, "emergency_reserve_plans"):
        return (0, 0, first_of_month(d), False)
    plans = list_plans_for_user(db, user_id, active_only=False)
    if not plans:
        return (0, 0, first_of_month(d), False)
    active = [p for p in plans if p.status == "active"]
    ensure_all_active_plans(db, user_id, d)
    for p in active:
        db.refresh(p)
    if active:
        bal = sum(int(p.balance_cents) for p in active)
        tgt = sum(int(p.monthly_target_cents) for p in active)
        tr = min(p.tracking_start for p in active)
        return (bal, tgt, tr, True)
    return (0, 0, first_of_month(d), True)


def refresh_reserve_balances_for_user(db: Session, user: User, today: date | None = None) -> tuple[int, int]:
    """Soma saldos e metas mensais de todos os planos activos."""
    if not session_has_table(db, "emergency_reserve_plans"):
        return (0, 0)

    d = today or date.today()
    plans = list_plans_for_user(db, user.id, active_only=True)
    if not plans:
        return (0, 0)

    total_balance = 0
    total_target = 0
    for p in plans:
        ensure_accruals(db, p, d)
        db.refresh(p)
        total_balance += int(p.balance_cents)
        total_target += int(p.monthly_target_cents)
    return (total_balance, total_target)


def list_accruals_for_user(
    db: Session,
    user_id: uuid.UUID,
    *,
    limit: int = 12,
) -> list[EmergencyReserveAccrual]:
    plan = get_primary_plan_for_user(db, user_id)
    if plan is None:
        return []
    n = max(1, min(int(limit), 60))
    rows = db.scalars(
        select(EmergencyReserveAccrual)
        .where(EmergencyReserveAccrual.plan_id == plan.id)
        .order_by(
            EmergencyReserveAccrual.year.desc(),
            EmergencyReserveAccrual.month.desc(),
        )
        .limit(n)
    ).all()
    return list(rows)


def list_accruals_for_plan(
    db: Session,
    plan_id: uuid.UUID,
    *,
    limit: int = 60,
) -> list[EmergencyReserveAccrual]:
    n = max(1, min(int(limit), 120))
    rows = db.scalars(
        select(EmergencyReserveAccrual)
        .where(EmergencyReserveAccrual.plan_id == plan_id)
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
) -> tuple[EmergencyReservePlan | None, bool]:
    plan = get_primary_plan_for_user(db, user_id)
    if plan is None:
        return (None, False)
    row = db.scalar(
        select(EmergencyReserveAccrual).where(
            EmergencyReserveAccrual.plan_id == plan.id,
            EmergencyReserveAccrual.year == year,
            EmergencyReserveAccrual.month == month,
        )
    )
    if row is None:
        return (plan, False)
    amt = int(row.amount_cents)
    db.delete(row)
    plan.balance_cents = int(plan.balance_cents) - amt
    add_skip_month(plan, year, month)
    db.commit()
    db.refresh(plan)
    return (plan, True)


def patch_accrual_for_user(
    db: Session,
    user_id: uuid.UUID,
    year: int,
    month: int,
    amount_cents: int,
) -> EmergencyReservePlan | None:
    if amount_cents < 0:
        raise ValueError("amount_cents must be >= 0")
    plan = get_primary_plan_for_user(db, user_id)
    if plan is None:
        return None
    row = db.scalar(
        select(EmergencyReserveAccrual).where(
            EmergencyReserveAccrual.plan_id == plan.id,
            EmergencyReserveAccrual.year == year,
            EmergencyReserveAccrual.month == month,
        )
    )
    if row is None:
        if amount_cents == 0:
            add_skip_month(plan, year, month)
        else:
            remove_skip_month(plan, year, month)
            db.add(
                EmergencyReserveAccrual(
                    id=uuid.uuid4(),
                    plan_id=plan.id,
                    year=year,
                    month=month,
                    amount_cents=int(amount_cents),
                )
            )
            plan.balance_cents = int(plan.balance_cents) + int(amount_cents)
    else:
        old = int(row.amount_cents)
        if amount_cents == 0:
            db.delete(row)
            plan.balance_cents = int(plan.balance_cents) - old
            add_skip_month(plan, year, month)
        else:
            remove_skip_month(plan, year, month)
            row.amount_cents = int(amount_cents)
            plan.balance_cents = int(plan.balance_cents) + (int(amount_cents) - old)
    db.commit()
    db.refresh(plan)
    return plan


def delete_reserve_for_user(db: Session, user_id: uuid.UUID) -> bool:
    """Remove todos os planos de reserva do âmbito (histórico incluído)."""
    plans = list_plans_for_user(db, user_id, active_only=False)
    if not plans:
        return False
    for r in plans:
        db.delete(r)
    db.commit()
    return True


def create_plan(
    db: Session,
    user_id: uuid.UUID,
    *,
    title: str,
    monthly_target_cents: int,
    tracking_start: date | None = None,
    plan_duration_months: int | None = None,
) -> EmergencyReservePlan:
    family_id, solo_user_id = _resolve_scope(db, user_id)
    anchor = first_of_month(tracking_start or date.today())
    p = EmergencyReservePlan(
        id=uuid.uuid4(),
        family_id=family_id,
        solo_user_id=solo_user_id if family_id is None else None,
        title=title.strip()[:200],
        monthly_target_cents=int(monthly_target_cents),
        balance_cents=0,
        tracking_start=anchor,
        accrual_skip_months=[],
        plan_duration_months=plan_duration_months,
        status="active",
    )
    db.add(p)
    db.commit()
    db.refresh(p)
    ensure_accruals(db, p, date.today())
    db.refresh(p)
    return p


def complete_plan_transfer(
    db: Session,
    user_id: uuid.UUID,
    plan_id: uuid.UUID,
    *,
    goal_id: uuid.UUID | None = None,
    to_plan_id: uuid.UUID | None = None,
) -> EmergencyReservePlan:
    """Encerra o plano e transfere o saldo para uma meta ou outro plano activo."""
    if (goal_id is None) == (to_plan_id is None):
        raise ValueError("exactly one of goal_id or to_plan_id")
    plan = get_plan_for_user(db, user_id, plan_id)
    if plan is None:
        raise ValueError("plan_not_found")
    if plan.status != "active":
        raise ValueError("plan_not_active")
    amount = int(plan.balance_cents)
    peers = set(family_peer_user_ids(db, user_id))

    if goal_id is not None:
        g = db.get(Goal, goal_id)
        if g is None or g.owner_user_id not in peers:
            raise ValueError("goal_not_allowed")
        g.current_cents = int(g.current_cents) + amount
        db.add(
            GoalContribution(
                goal_id=g.id,
                amount_cents=amount,
                note="emergency_reserve_transfer",
            )
        )
        plan.transfer_goal_id = goal_id
        plan.transfer_to_plan_id = None
    else:
        assert to_plan_id is not None
        dest = get_plan_for_user(db, user_id, to_plan_id)
        if dest is None or dest.id == plan.id:
            raise ValueError("dest_plan_invalid")
        if dest.status != "active":
            raise ValueError("dest_not_active")
        dest.balance_cents = int(dest.balance_cents) + amount
        plan.transfer_goal_id = None
        plan.transfer_to_plan_id = to_plan_id

    plan.balance_cents = 0
    plan.status = "completed"
    plan.completed_at = datetime.now(timezone.utc)
    db.commit()
    db.refresh(plan)
    return plan


def month_breakdown_for_plan(
    db: Session,
    plan: EmergencyReservePlan,
    *,
    today: date | None = None,
) -> list[dict]:
    """Por mês civil: esperado, depositado (acréscimo), déficit."""
    d = today or date.today()
    start = first_of_month(plan.tracking_start)
    end = first_of_month(d)
    cap = last_included_month_first(plan.tracking_start, plan.plan_duration_months)
    if cap is not None and end > cap:
        end = cap
    if start > end:
        return []

    accrual_map: dict[tuple[int, int], int] = {}
    for row in db.scalars(
        select(EmergencyReserveAccrual).where(EmergencyReserveAccrual.plan_id == plan.id)
    ).all():
        accrual_map[(int(row.year), int(row.month))] = int(row.amount_cents)

    tgt = int(plan.monthly_target_cents)
    out: list[dict] = []
    for y, m in iter_months_inclusive(start, end):
        dep = accrual_map.get((y, m), 0)
        shortfall = max(0, tgt - dep) if tgt > 0 else 0
        out.append(
            {
                "year": y,
                "month": m,
                "expected_cents": tgt,
                "deposited_cents": dep,
                "shortfall_cents": shortfall,
            }
        )
    return out
