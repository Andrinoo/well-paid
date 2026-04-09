"""Série mensal cashflow para GET /dashboard/cashflow (Ordems §6.2.1)."""

from __future__ import annotations

import calendar
from datetime import date, datetime

from sqlalchemy import Date, cast, func, inspect, select
from sqlalchemy.orm import Session

from app.models.expense import Expense
from app.models.income import Income
from app.models.user import User
from app.schemas.dashboard import DashboardCashflowResponse, ExpenseStatus, PeriodMonth
from app.services.emergency_reserve import iter_months_inclusive
from app.services.family_scope import family_peer_user_ids
from app.services.recurrence import add_months


def _last_day_of_month(y: int, m: int) -> date:
    return date(y, m, calendar.monthrange(y, m)[1])


def _as_date(bucket: object) -> date:
    if isinstance(bucket, datetime):
        return bucket.date()
    if isinstance(bucket, date):
        return bucket
    raise TypeError(f"expected date-like bucket, got {type(bucket)!r}")


def cashflow_month_keys(
    hist_start: date, hist_end: date, forecast_months: int
) -> list[tuple[int, int]]:
    """hist_start e hist_end são primeiros dias do mês (inclusivos)."""
    series_end = add_months(hist_end, forecast_months)
    return list(iter_months_inclusive(hist_start, series_end))


def get_dashboard_cashflow(
    db: Session,
    user: User,
    *,
    dynamic: bool,
    start_year: int | None,
    start_month: int | None,
    end_year: int | None,
    end_month: int | None,
    forecast_months: int,
    today: date | None = None,
) -> DashboardCashflowResponse:
    t = today or date.today()
    today_first = date(t.year, t.month, 1)

    if dynamic:
        hist_end = today_first
        hist_start = add_months(hist_end, -7)
    else:
        if not all(
            x is not None
            for x in (start_year, start_month, end_year, end_month)
        ):
            raise ValueError("cashflow_manual_range_incomplete")
        hist_start = date(int(start_year), int(start_month), 1)
        hist_end = date(int(end_year), int(end_month), 1)
        if hist_start > hist_end:
            raise ValueError("cashflow_invalid_range")

    peer_ids = family_peer_user_ids(db, user.id)
    visible = Expense.deleted_at.is_(None)

    month_keys = cashflow_month_keys(hist_start, hist_end, forecast_months)
    if not month_keys:
        return DashboardCashflowResponse(
            dynamic=dynamic,
            forecast_months=forecast_months,
            months=[],
            income_cents=[],
            expense_paid_cents=[],
            expense_forecast_cents=[],
        )

    range_start = date(month_keys[0][0], month_keys[0][1], 1)
    y_end, m_end = month_keys[-1]
    range_end = _last_day_of_month(y_end, m_end)

    income_map: dict[tuple[int, int], int] = {}
    bind = db.get_bind()
    if bind is not None and inspect(bind).has_table("incomes"):
        inc_bucket = cast(func.date_trunc("month", Income.income_date), Date)
        inc_rows = db.execute(
            select(inc_bucket, func.coalesce(func.sum(Income.amount_cents), 0)).where(
                Income.owner_user_id.in_(peer_ids),
                Income.income_date >= range_start,
                Income.income_date <= range_end,
            ).group_by(inc_bucket)
        ).all()
        for bucket, total in inc_rows:
            if bucket is None:
                continue
            d = _as_date(bucket)
            income_map[(d.year, d.month)] = int(total or 0)

    attribution = cast(
        func.coalesce(cast(Expense.paid_at, Date), Expense.expense_date),
        Date,
    )
    paid_bucket = cast(func.date_trunc("month", attribution), Date)
    paid_rows = db.execute(
        select(paid_bucket, func.coalesce(func.sum(Expense.amount_cents), 0)).where(
            Expense.owner_user_id.in_(peer_ids),
            Expense.status == ExpenseStatus.PAID.value,
            visible,
            attribution >= range_start,
            attribution <= range_end,
        ).group_by(paid_bucket)
    ).all()
    paid_map: dict[tuple[int, int], int] = {}
    for bucket, total in paid_rows:
        if bucket is None:
            continue
        d = _as_date(bucket)
        paid_map[(d.year, d.month)] = int(total or 0)

    pend_bucket = cast(func.date_trunc("month", Expense.expense_date), Date)
    pend_rows = db.execute(
        select(pend_bucket, func.coalesce(func.sum(Expense.amount_cents), 0)).where(
            Expense.owner_user_id.in_(peer_ids),
            Expense.status == ExpenseStatus.PENDING.value,
            visible,
            Expense.expense_date >= range_start,
            Expense.expense_date <= range_end,
        ).group_by(pend_bucket)
    ).all()
    pend_map: dict[tuple[int, int], int] = {}
    for bucket, total in pend_rows:
        if bucket is None:
            continue
        d = _as_date(bucket)
        pend_map[(d.year, d.month)] = int(total or 0)

    months: list[PeriodMonth] = []
    income_cents: list[int] = []
    expense_paid_cents: list[int] = []
    expense_forecast_cents: list[int] = []

    for y, m in month_keys:
        months.append(PeriodMonth(year=y, month=m))
        key = (y, m)
        month_first = date(y, m, 1)
        income_cents.append(income_map.get(key, 0))
        expense_paid_cents.append(paid_map.get(key, 0))
        if month_first >= today_first:
            expense_forecast_cents.append(pend_map.get(key, 0))
        else:
            expense_forecast_cents.append(0)

    return DashboardCashflowResponse(
        dynamic=dynamic,
        forecast_months=forecast_months,
        months=months,
        income_cents=income_cents,
        expense_paid_cents=expense_paid_cents,
        expense_forecast_cents=expense_forecast_cents,
    )
