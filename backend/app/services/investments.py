from __future__ import annotations

import logging
from datetime import date

from sqlalchemy import delete, select
from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.core.schema_introspection import session_has_table
from app.models.investment_position import InvestmentPosition
from app.schemas.investments import (
    InvestmentBucketOut,
    InvestmentEvolutionPointOut,
    InvestmentOverviewOut,
    InvestmentPositionCreate,
    InvestmentPositionOut,
)
from app.services.investment_market_rates import get_market_rates_snapshot
from app.services.emergency_reserve import list_plans_for_user
from app.services.recurrence import add_months

logger = logging.getLogger(__name__)


def _safe_positive(value: int) -> int:
    return value if value > 0 else 0


def _split_allocation(total_cents: int) -> tuple[int, int, int]:
    cdi = int(total_cents * 0.45)
    cdb = int(total_cents * 0.35)
    fixed_income = total_cents - cdi - cdb
    return cdi, cdb, fixed_income


def _load_positions_total(db: Session, user_id) -> int:
    if not session_has_table(db, "investment_positions"):
        return 0
    rows = db.scalars(
        select(InvestmentPosition.principal_cents).where(InvestmentPosition.owner_user_id == user_id)
    ).all()
    return sum(_safe_positive(int(v)) for v in rows)


def get_investment_overview_for_user(db: Session, user_id) -> InvestmentOverviewOut:
    total_allocated = _load_positions_total(db, user_id)
    if total_allocated == 0 and session_has_table(db, "emergency_reserve_plans"):
        plans = list_plans_for_user(db, user_id, active_only=False)
        total_allocated = sum(_safe_positive(int(p.balance_cents)) for p in plans)

    cdi_alloc, cdb_alloc, fixed_alloc = _split_allocation(total_allocated)

    rates = get_market_rates_snapshot()
    cdi_pct = float(rates["cdi_monthly"])
    cdb_pct = float(rates["cdb_monthly"])
    fixed_pct = float(rates["fixed_monthly"])
    rates_source = str(rates.get("source") or "fallback_default")
    rates_fallback_used = bool(rates.get("fallback_used", True))
    if rates_fallback_used:
        logger.warning("Investments rates fallback in use for user_id=%s", user_id)

    cdi_yield = int(cdi_alloc * cdi_pct)
    cdb_yield = int(cdb_alloc * cdb_pct)
    fixed_yield = int(fixed_alloc * fixed_pct)

    buckets = [
        InvestmentBucketOut(
            key="cdi",
            label="CDI",
            allocated_cents=cdi_alloc,
            yield_cents=cdi_yield,
            yield_pct_month=round(cdi_pct * 100.0, 2),
        ),
        InvestmentBucketOut(
            key="cdb",
            label="CDB",
            allocated_cents=cdb_alloc,
            yield_cents=cdb_yield,
            yield_pct_month=round(cdb_pct * 100.0, 2),
        ),
        InvestmentBucketOut(
            key="fixed_income",
            label="Renda fixa",
            allocated_cents=fixed_alloc,
            yield_cents=fixed_yield,
            yield_pct_month=round(fixed_pct * 100.0, 2),
        ),
    ]

    total_yield = cdi_yield + cdb_yield + fixed_yield
    return InvestmentOverviewOut(
        total_allocated_cents=total_allocated,
        total_yield_cents=total_yield,
        estimated_monthly_yield_cents=total_yield,
        rates_source=rates_source,
        rates_fallback_used=rates_fallback_used,
        buckets=buckets,
    )


def get_investment_evolution_for_user(
    db: Session,
    user_id,
    months: int = 6,
) -> list[InvestmentEvolutionPointOut]:
    settings = get_settings()
    horizon = max(1, min(int(months), 24))
    overview = get_investment_overview_for_user(db, user_id)
    start_total = overview.total_allocated_cents
    stable_window = min(max(0, int(settings.investments_fallback_stable_window_months)), horizon)

    if start_total > 0:
        monthly_rate = overview.estimated_monthly_yield_cents / float(start_total)
    else:
        # Base conservadora quando não há alocação ainda.
        rates = get_market_rates_snapshot()
        monthly_rate = (
            float(rates["cdi_monthly"]) * 0.45
            + float(rates["cdb_monthly"]) * 0.35
            + float(rates["fixed_monthly"]) * 0.20
        )

    points: list[InvestmentEvolutionPointOut] = []
    base_date = date.today().replace(day=1)
    projected = float(start_total)

    for i in range(horizon):
        if i > 0:
            projected = projected * (1.0 + monthly_rate)
        d = add_months(base_date, i)
        projected_cents = max(0, int(round(projected)))
        cumulative = max(0, projected_cents - start_total)
        points.append(
            InvestmentEvolutionPointOut(
                year=d.year,
                month=d.month,
                projected_total_cents=projected_cents,
                cumulative_yield_cents=cumulative,
                is_estimated=bool(overview.rates_fallback_used and i >= stable_window),
            )
        )
    return points


def list_positions_for_user(db: Session, user_id) -> list[InvestmentPositionOut]:
    if not session_has_table(db, "investment_positions"):
        return []
    rows = db.scalars(
        select(InvestmentPosition)
        .where(InvestmentPosition.owner_user_id == user_id)
        .order_by(InvestmentPosition.created_at.desc())
    ).all()
    return [
        InvestmentPositionOut(
            id=str(r.id),
            instrument_type=r.instrument_type,
            name=r.name,
            principal_cents=int(r.principal_cents),
            annual_rate_bps=int(r.annual_rate_bps),
            maturity_date=r.maturity_date,
            is_liquid=bool(r.is_liquid),
        )
        for r in rows
    ]


def create_position_for_user(db: Session, user_id, body: InvestmentPositionCreate) -> InvestmentPositionOut:
    if not session_has_table(db, "investment_positions"):
        raise ValueError("investments_positions_unavailable")
    row = InvestmentPosition(
        owner_user_id=user_id,
        instrument_type=body.instrument_type.strip().lower(),
        name=body.name.strip(),
        principal_cents=body.principal_cents,
        annual_rate_bps=body.annual_rate_bps,
        maturity_date=body.maturity_date,
        is_liquid=body.is_liquid,
    )
    db.add(row)
    db.commit()
    db.refresh(row)
    return InvestmentPositionOut(
        id=str(row.id),
        instrument_type=row.instrument_type,
        name=row.name,
        principal_cents=int(row.principal_cents),
        annual_rate_bps=int(row.annual_rate_bps),
        maturity_date=row.maturity_date,
        is_liquid=bool(row.is_liquid),
    )


def delete_position_for_user(db: Session, user_id, position_id: str) -> bool:
    if not session_has_table(db, "investment_positions"):
        return False
    try:
        import uuid

        pid = uuid.UUID(position_id)
    except Exception:
        return False
    result = db.execute(
        delete(InvestmentPosition).where(
            InvestmentPosition.id == pid,
            InvestmentPosition.owner_user_id == user_id,
        )
    )
    db.commit()
    return bool(result.rowcount and result.rowcount > 0)
