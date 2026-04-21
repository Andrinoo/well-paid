"""Reserva de emergência: planos múltiplos + acréscimos mensais por plano."""

from __future__ import annotations

import uuid
from datetime import date, datetime, timezone
from math import ceil
from typing import TYPE_CHECKING

from sqlalchemy import extract, func, select
from sqlalchemy.orm import Session

from app.core.schema_introspection import session_has_table
from app.models.emergency_reserve import (
    EmergencyReserveAccrual,
    EmergencyReserveContribution,
    EmergencyReserveContributionItem,
    EmergencyReservePlan,
)
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


def effective_plan_end_first_of_month(plan: EmergencyReservePlan) -> date | None:
    """Fim do plano para cronograma e ritmo: data-alvo fim tem prioridade; senão duração em meses."""
    if plan.target_end_date is not None:
        return first_of_month(plan.target_end_date)
    return last_included_month_first(plan.tracking_start, plan.plan_duration_months)


def normalize_plan_end_fields(plan: EmergencyReservePlan) -> None:
    """Evita conflito entre data fim e duração: se ambos vierem, a data fim prevalece e a duração é recalculada."""
    start = first_of_month(plan.tracking_start)
    te = first_of_month(plan.target_end_date) if plan.target_end_date is not None else None
    dur = (
        int(plan.plan_duration_months)
        if plan.plan_duration_months is not None and int(plan.plan_duration_months) > 0
        else None
    )
    if te is not None:
        plan.target_end_date = te
        plan.plan_duration_months = months_between_inclusive(start, te)
    elif dur is not None:
        end_cap = last_included_month_first(plan.tracking_start, dur)
        plan.target_end_date = end_cap
        plan.plan_duration_months = dur
    else:
        plan.target_end_date = None
        plan.plan_duration_months = None


def months_between_inclusive(start: date, end: date) -> int:
    if end < start:
        return 0
    return (end.year - start.year) * 12 + (end.month - start.month) + 1


def pace_status_from_delta(delta_cents: int, tolerance_cents: int = 100) -> str:
    if delta_cents > tolerance_cents:
        return "above"
    if delta_cents < -tolerance_cents:
        return "below"
    return "on_track"


def plan_timeline_metrics(
    plan: EmergencyReservePlan,
    *,
    today: date | None = None,
) -> dict:
    d = today or date.today()
    start = first_of_month(plan.tracking_start)
    end_target = effective_plan_end_first_of_month(plan)

    if end_target is not None:
        months_total = months_between_inclusive(start, end_target)
        months_passed = months_between_inclusive(start, min(first_of_month(d), end_target))
        months_remaining = max(0, months_total - months_passed)
    else:
        months_total = None
        months_passed = max(0, months_between_inclusive(start, first_of_month(d)))
        months_remaining = None

    expected_so_far = int(plan.monthly_target_cents) * months_passed
    pace_delta_cents = int(plan.balance_cents) - expected_so_far

    monthly_needed_cents: int | None = None
    if plan.target_cents is not None and end_target is not None:
        remaining_amount = max(0, int(plan.target_cents) - int(plan.balance_cents))
        remaining_months = max(1, months_remaining or 0)
        monthly_needed_cents = ceil(remaining_amount / remaining_months) if remaining_amount > 0 else 0

    return {
        "months_total": months_total,
        "months_passed": months_passed,
        "months_remaining": months_remaining,
        "monthly_needed_cents": monthly_needed_cents,
        "pace_delta_cents": pace_delta_cents,
        "pace_status": pace_status_from_delta(pace_delta_cents),
    }


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


def contribution_sum_for_plan(db: Session, plan_id: uuid.UUID) -> int:
    v = db.scalar(
        select(func.coalesce(func.sum(EmergencyReserveContributionItem.amount_cents), 0)).where(
            EmergencyReserveContributionItem.plan_id == plan_id
        )
    )
    return int(v or 0)


def refresh_plan_cash_balance(db: Session, plan: EmergencyReservePlan) -> None:
    plan.balance_cents = int(plan.opening_balance_cents) + contribution_sum_for_plan(db, plan.id)


def _persist_plan_cash_balance(db: Session, plan: EmergencyReservePlan) -> None:
    refresh_plan_cash_balance(db, plan)
    if db.is_modified(plan, include_collections=False):
        db.commit()
        db.refresh(plan)


def contribution_deposits_by_year_month(db: Session, plan_id: uuid.UUID) -> dict[tuple[int, int], int]:
    y = extract("year", EmergencyReserveContribution.contribution_date)
    m = extract("month", EmergencyReserveContribution.contribution_date)
    rows = db.execute(
        select(y, m, func.sum(EmergencyReserveContributionItem.amount_cents))
        .join(
            EmergencyReserveContributionItem,
            EmergencyReserveContributionItem.contribution_id == EmergencyReserveContribution.id,
        )
        .where(EmergencyReserveContributionItem.plan_id == plan_id)
        .group_by(y, m)
    ).all()
    out: dict[tuple[int, int], int] = {}
    for row in rows:
        yy, mm, total = int(row[0]), int(row[1]), int(row[2] or 0)
        out[(yy, mm)] = total
    return out


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
        details=None,
        monthly_target_cents=int(monthly_target_cents),
        target_cents=None,
        balance_cents=0,
        opening_balance_cents=0,
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
        _persist_plan_cash_balance(db, plan)
        return False
    tgt = int(plan.monthly_target_cents)
    if tgt <= 0:
        _persist_plan_cash_balance(db, plan)
        return False

    start = first_of_month(plan.tracking_start)
    end = first_of_month(today)
    cap = effective_plan_end_first_of_month(plan)
    if cap is not None and end > cap:
        end = cap
    if start > end:
        _persist_plan_cash_balance(db, plan)
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
        changed = True

    if changed:
        db.commit()
        db.refresh(plan)
    _persist_plan_cash_balance(db, plan)
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
    else:
        old = int(row.amount_cents)
        if amount_cents == 0:
            db.delete(row)
            add_skip_month(plan, year, month)
        else:
            remove_skip_month(plan, year, month)
            row.amount_cents = int(amount_cents)
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
    details: str | None,
    monthly_target_cents: int,
    target_cents: int | None = None,
    tracking_start: date | None = None,
    target_end_date: date | None = None,
    plan_duration_months: int | None = None,
    opening_balance_cents: int | None = None,
) -> EmergencyReservePlan:
    family_id, solo_user_id = _resolve_scope(db, user_id)
    anchor = first_of_month(tracking_start or date.today())
    opening = int(opening_balance_cents or 0)
    p = EmergencyReservePlan(
        id=uuid.uuid4(),
        family_id=family_id,
        solo_user_id=solo_user_id if family_id is None else None,
        title=title.strip()[:200],
        details=(details or "").strip()[:1200] or None,
        monthly_target_cents=int(monthly_target_cents),
        target_cents=int(target_cents) if target_cents is not None else None,
        balance_cents=0,
        opening_balance_cents=opening,
        tracking_start=anchor,
        target_end_date=first_of_month(target_end_date) if target_end_date is not None else None,
        accrual_skip_months=[],
        plan_duration_months=plan_duration_months,
        status="active",
    )
    normalize_plan_end_fields(p)
    db.add(p)
    db.commit()
    db.refresh(p)
    ensure_accruals(db, p, date.today())
    _persist_plan_cash_balance(db, p)
    return p


def update_plan_for_user(
    db: Session,
    user_id: uuid.UUID,
    plan_id: uuid.UUID,
    *,
    title: str,
    details: str | None,
    monthly_target_cents: int,
    target_cents: int | None = None,
    tracking_start: date | None = None,
    target_end_date: date | None = None,
    plan_duration_months: int | None = None,
    opening_balance_cents: int | None = None,
) -> EmergencyReservePlan | None:
    plan = get_plan_for_user(db, user_id, plan_id)
    if plan is None:
        return None
    if plan.status != "active":
        raise ValueError("plan_not_active")
    plan.title = title.strip()[:200]
    plan.details = (details or "").strip()[:1200] or None
    plan.monthly_target_cents = int(monthly_target_cents)
    plan.target_cents = int(target_cents) if target_cents is not None else None
    if tracking_start is not None:
        plan.tracking_start = first_of_month(tracking_start)
    plan.target_end_date = (
        first_of_month(target_end_date) if target_end_date is not None else None
    )
    plan.plan_duration_months = plan_duration_months
    if opening_balance_cents is not None:
        plan.opening_balance_cents = int(opening_balance_cents)
    normalize_plan_end_fields(plan)
    db.commit()
    db.refresh(plan)
    ensure_accruals(db, plan, date.today())
    _persist_plan_cash_balance(db, plan)
    return plan


def delete_plan_for_user(
    db: Session,
    user_id: uuid.UUID,
    plan_id: uuid.UUID,
) -> bool:
    plan = get_plan_for_user(db, user_id, plan_id)
    if plan is None:
        return False
    db.delete(plan)
    db.commit()
    return True


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
    ensure_accruals(db, plan, date.today())
    _persist_plan_cash_balance(db, plan)
    amount = max(0, int(plan.balance_cents))
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
        dest.opening_balance_cents = int(dest.opening_balance_cents) + amount
        _persist_plan_cash_balance(db, dest)
        plan.transfer_goal_id = None
        plan.transfer_to_plan_id = to_plan_id

    src_contrib_total = contribution_sum_for_plan(db, plan.id)
    plan.opening_balance_cents = -src_contrib_total
    _persist_plan_cash_balance(db, plan)
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
    """Por mês civil: esperado, depositado (aportes reais naquele mês), déficit."""
    d = today or date.today()
    start = first_of_month(plan.tracking_start)
    end = first_of_month(d)
    cap = effective_plan_end_first_of_month(plan)
    if cap is not None and end > cap:
        end = cap
    if start > end:
        return []

    dep_by_month = contribution_deposits_by_year_month(db, plan.id)

    tgt = int(plan.monthly_target_cents)
    out: list[dict] = []
    cumulative_expected = 0
    cumulative_deposited = 0
    for y, m in iter_months_inclusive(start, end):
        dep = int(dep_by_month.get((y, m), 0))
        shortfall = max(0, tgt - dep) if tgt > 0 else 0
        cumulative_expected += tgt
        cumulative_deposited += dep
        cumulative_delta = cumulative_deposited - cumulative_expected
        out.append(
            {
                "year": y,
                "month": m,
                "expected_cents": tgt,
                "deposited_cents": dep,
                "shortfall_cents": shortfall,
                "cumulative_expected_cents": cumulative_expected,
                "cumulative_deposited_cents": cumulative_deposited,
                "cumulative_delta_cents": cumulative_delta,
                "pace_status": pace_status_from_delta(cumulative_delta),
            }
        )
    return out


def create_contribution(
    db: Session,
    user_id: uuid.UUID,
    *,
    contribution_date: date | None,
    total_amount_cents: int,
    allocations: list[dict],
    note: str | None = None,
) -> EmergencyReserveContribution:
    family_id, solo_user_id = _resolve_scope(db, user_id)
    contrib = EmergencyReserveContribution(
        id=uuid.uuid4(),
        family_id=family_id,
        solo_user_id=solo_user_id if family_id is None else None,
        contribution_date=contribution_date or date.today(),
        total_amount_cents=int(total_amount_cents),
        created_by_user_id=user_id,
        note=(note or "").strip()[:500] or None,
    )
    db.add(contrib)

    touched_plan_ids: dict[uuid.UUID, None] = {}
    for alloc in allocations:
        plan_id = alloc["plan_id"]
        amount_cents = int(alloc["amount_cents"])
        plan = get_plan_for_user(db, user_id, plan_id)
        if plan is None:
            raise ValueError("plan_not_found_or_not_allowed")
        if plan.status != "active":
            raise ValueError("plan_not_active")
        touched_plan_ids[plan.id] = None
        db.add(
            EmergencyReserveContributionItem(
                id=uuid.uuid4(),
                contribution_id=contrib.id,
                plan_id=plan.id,
                amount_cents=amount_cents,
            )
        )

    db.commit()
    db.refresh(contrib)
    for pid in touched_plan_ids:
        p2 = db.get(EmergencyReservePlan, pid)
        if p2 is not None:
            _persist_plan_cash_balance(db, p2)
    return contrib
