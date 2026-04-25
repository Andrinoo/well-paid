"""Agregações do dashboard (centavos, período mensal)."""

from __future__ import annotations

import calendar
from datetime import date

from sqlalchemy import func, nulls_last, select
from sqlalchemy.orm import Session

from app.core.schema_introspection import session_has_table
from app.models.category import Category
from app.models.expense import Expense
from app.models.goal import Goal
from app.models.income import Income
from app.models.user import User
from app.services.emergency_reserve import refresh_reserve_balances_for_user
from app.services.family_scope import family_visibility_scope
from app.schemas.dashboard import (
    CategorySpend,
    DashboardOverviewResponse,
    ExpenseStatus,
    GoalSummaryItem,
    PendingExpenseItem,
    PeriodMonth,
)


def month_bounds(year: int, month: int) -> tuple[date, date]:
    last = calendar.monthrange(year, month)[1]
    return date(year, month, 1), date(year, month, last)


def share_basis_points(part_cents: int, month_total_cents: int) -> int | None:
    if month_total_cents <= 0 or part_cents <= 0:
        return None
    return (part_cents * 10000) // month_total_cents


def category_spends_from_rows(
    rows: list[tuple[str, str, int]],
    month_total_cents: int,
) -> list[CategorySpend]:
    """rows: (category_key, display_name, sum_amount_cents) já ordenados."""
    out: list[CategorySpend] = []
    for key, name, amount in rows:
        if amount <= 0:
            continue
        out.append(
            CategorySpend(
                category_key=key,
                name=name,
                amount_cents=amount,
                share_bps=share_basis_points(amount, month_total_cents),
            )
        )
    return out


def get_dashboard_overview(
    db: Session,
    user: User,
    year: int,
    month: int,
    *,
    today: date | None = None,
) -> DashboardOverviewResponse:
    start, end = month_bounds(year, month)
    next_month_anchor = date(year, month, 1)
    nm_year = next_month_anchor.year + (1 if next_month_anchor.month == 12 else 0)
    nm_month = 1 if next_month_anchor.month == 12 else (next_month_anchor.month + 1)
    next_start, next_end = month_bounds(nm_year, nm_month)

    owner_ids, include_family = family_visibility_scope(db, user)
    visible_family_expense = (
        Expense.owner_user_id.in_(owner_ids)
        if not include_family
        else (
            (Expense.owner_user_id == user.id)
            | ((Expense.owner_user_id.in_(owner_ids)) & (Expense.is_family.is_(True)))
        )
    )
    visible_family_income = (
        Income.owner_user_id.in_(owner_ids)
        if not include_family
        else (
            (Income.owner_user_id == user.id)
            | ((Income.owner_user_id.in_(owner_ids)) & (Income.is_family.is_(True)))
        )
    )
    visible_family_goal = (
        Goal.owner_user_id.in_(owner_ids)
        if not include_family
        else (
            (Goal.owner_user_id == user.id)
            | ((Goal.owner_user_id.in_(owner_ids)) & (Goal.is_family.is_(True)))
        )
    )
    visible_expense = Expense.deleted_at.is_(None)

    er_balance, er_target = refresh_reserve_balances_for_user(
        db, user, today=today or date.today()
    )

    month_total = db.scalar(
        select(func.coalesce(func.sum(Expense.amount_cents), 0)).where(
            visible_family_expense,
            Expense.expense_date >= start,
            Expense.expense_date <= end,
            visible_expense,
        )
    )
    month_expense_total_cents = int(month_total or 0)

    cat_stmt = (
        select(
            Category.key,
            Category.name,
            func.sum(Expense.amount_cents).label("sum_cents"),
        )
        .join(Category, Expense.category_id == Category.id)
        .where(
            visible_family_expense,
            Expense.expense_date >= start,
            Expense.expense_date <= end,
            visible_expense,
        )
        .group_by(Category.id, Category.key, Category.name, Category.sort_order)
        .having(func.sum(Expense.amount_cents) > 0)
        .order_by(Category.sort_order.asc(), Category.name.asc())
    )
    cat_rows = db.execute(cat_stmt).all()
    spend_tuples = [(r.key, r.name, int(r.sum_cents)) for r in cat_rows]
    spending_by_category = category_spends_from_rows(
        spend_tuples, month_expense_total_cents
    )

    pending_total = db.scalar(
        select(func.coalesce(func.sum(Expense.amount_cents), 0)).where(
            visible_family_expense,
            Expense.status == ExpenseStatus.PENDING.value,
            Expense.expense_date >= next_start,
            Expense.expense_date <= next_end,
            visible_expense,
        )
    )
    pending_total_cents = int(pending_total or 0)

    preview_stmt = (
        select(Expense)
        .where(
            visible_family_expense,
            Expense.status == ExpenseStatus.PENDING.value,
            Expense.expense_date >= next_start,
            Expense.expense_date <= next_end,
            visible_expense,
        )
        .order_by(
            nulls_last(Expense.due_date.asc()),
            Expense.created_at.asc(),
        )
        .limit(5)
    )
    pending_expenses = db.scalars(preview_stmt).all()
    pending_preview = [
        PendingExpenseItem(
            id=e.id,
            description=e.description,
            amount_cents=int(e.amount_cents),
            due_date=e.due_date,
            is_mine=e.owner_user_id == user.id,
        )
        for e in pending_expenses
    ]

    upcoming_stmt = (
        select(Expense)
        .where(
            visible_family_expense,
            Expense.status == ExpenseStatus.PENDING.value,
            Expense.due_date.isnot(None),
            Expense.due_date >= next_start,
            Expense.due_date <= next_end,
            visible_expense,
        )
        .order_by(Expense.due_date.asc(), Expense.created_at.asc())
        .limit(20)
    )
    upcoming_rows = db.scalars(upcoming_stmt).all()
    upcoming_due = [
        PendingExpenseItem(
            id=e.id,
            description=e.description,
            amount_cents=int(e.amount_cents),
            due_date=e.due_date,
            is_mine=e.owner_user_id == user.id,
        )
        for e in upcoming_rows
    ]

    # Evita 500 se a migração de proventos (009) ainda não foi aplicada na BD.
    # Incomes e goals usam delete físico no estado atual do projeto.
    if session_has_table(db, "incomes"):
        income_sum = db.scalar(
            select(func.coalesce(func.sum(Income.amount_cents), 0)).where(
                visible_family_income,
                Income.income_date >= start,
                Income.income_date <= end,
            )
        )
        month_income_cents = int(income_sum or 0)
    else:
        month_income_cents = 0
    month_balance_cents = month_income_cents - month_expense_total_cents

    if session_has_table(db, "goals"):
        goals_rows = (
            db.query(Goal)
            .filter(visible_family_goal, Goal.is_active.is_(True))
            .order_by(Goal.updated_at.desc())
            .limit(3)
            .all()
        )
    else:
        goals_rows = []
    goals_preview = [
        GoalSummaryItem(
            id=g.id,
            title=g.title,
            current_cents=int(g.current_cents),
            target_cents=int(g.target_cents),
            is_mine=g.owner_user_id == user.id,
        )
        for g in goals_rows
    ]

    return DashboardOverviewResponse(
        period=PeriodMonth(year=year, month=month),
        month_income_cents=month_income_cents,
        month_expense_total_cents=month_expense_total_cents,
        month_balance_cents=month_balance_cents,
        spending_by_category=spending_by_category,
        pending_total_cents=pending_total_cents,
        pending_preview=pending_preview,
        upcoming_due=upcoming_due,
        goals_preview=goals_preview,
        emergency_reserve_balance_cents=er_balance,
        emergency_reserve_monthly_target_cents=er_target,
    )
