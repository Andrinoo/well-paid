"""Regras de exclusão: parcelamento, recorrência, âncora vs ocorrência."""

from __future__ import annotations

import uuid
from datetime import date, datetime
from enum import Enum

from sqlalchemy import func, select, update
from sqlalchemy.orm import Session

from app.models.expense import Expense
from app.schemas.dashboard import ExpenseStatus


class ExpenseDeleteTarget(str, Enum):
    series = "series"
    occurrence = "occurrence"


class ExpenseDeleteScope(str, Enum):
    all = "all"
    future_unpaid = "future_unpaid"


def apply_expense_delete(
    db: Session,
    *,
    user_id: uuid.UUID,
    e: Expense,
    target: ExpenseDeleteTarget,
    scope: ExpenseDeleteScope,
    now: datetime,
    today: date,
) -> None:
    """Aplica soft-delete ou desvinculação conforme alvo e âmbito."""

    if e.installment_group_id is not None:
        _delete_installment_plan(
            db,
            user_id=user_id,
            group_id=e.installment_group_id,
            scope=scope,
            now=now,
            today=today,
        )
        return

    if e.recurring_series_id is not None:
        if target == ExpenseDeleteTarget.occurrence:
            _delete_recurring_occurrence(db, e=e, now=now)
            return
        _delete_recurring_series(
            db,
            user_id=user_id,
            series_id=e.recurring_series_id,
            scope=scope,
            now=now,
            today=today,
        )
        return

    e.deleted_at = now


def _delete_installment_plan(
    db: Session,
    *,
    user_id: uuid.UUID,
    group_id: uuid.UUID,
    scope: ExpenseDeleteScope,
    now: datetime,
    today: date,
) -> None:
    if scope == ExpenseDeleteScope.all:
        db.execute(
            update(Expense)
            .where(
                Expense.owner_user_id == user_id,
                Expense.installment_group_id == group_id,
                Expense.deleted_at.is_(None),
            )
            .values(deleted_at=now)
        )
        return

    db.execute(
        update(Expense)
        .where(
            Expense.owner_user_id == user_id,
            Expense.installment_group_id == group_id,
            Expense.status == ExpenseStatus.PENDING.value,
            Expense.expense_date > today,
            Expense.deleted_at.is_(None),
        )
        .values(deleted_at=now)
    )


def _delete_recurring_series(
    db: Session,
    *,
    user_id: uuid.UUID,
    series_id: uuid.UUID,
    scope: ExpenseDeleteScope,
    now: datetime,
    today: date,
) -> None:
    if scope == ExpenseDeleteScope.all:
        db.execute(
            update(Expense)
            .where(
                Expense.owner_user_id == user_id,
                Expense.recurring_series_id == series_id,
                Expense.status == ExpenseStatus.PENDING.value,
                Expense.deleted_at.is_(None),
            )
            .values(deleted_at=now)
        )
        db.execute(
            update(Expense)
            .where(
                Expense.owner_user_id == user_id,
                Expense.recurring_series_id == series_id,
                Expense.status == ExpenseStatus.PAID.value,
                Expense.deleted_at.is_(None),
            )
            .values(
                recurring_series_id=None,
                recurring_frequency=None,
                recurring_generated_until=None,
            )
        )
        return

    db.execute(
        update(Expense)
        .where(
            Expense.owner_user_id == user_id,
            Expense.recurring_series_id == series_id,
            Expense.status == ExpenseStatus.PENDING.value,
            Expense.expense_date > today,
            Expense.deleted_at.is_(None),
        )
        .values(deleted_at=now)
    )


def installment_plan_has_paid(
    db: Session, owner_id: uuid.UUID, group_id: uuid.UUID
) -> bool:
    n = db.scalar(
        select(func.count())
        .select_from(Expense)
        .where(
            Expense.owner_user_id == owner_id,
            Expense.installment_group_id == group_id,
            Expense.status == ExpenseStatus.PAID.value,
            Expense.deleted_at.is_(None),
        )
    )
    return (n or 0) > 0


def propagate_recurring_template_amount(
    db: Session,
    *,
    user_id: uuid.UUID,
    series_id: uuid.UUID,
    amount_cents: int,
    today: date,
) -> None:
    """Ocorrências geradas pendentes com data > hoje passam a usar o novo valor."""
    db.execute(
        update(Expense)
        .where(
            Expense.owner_user_id == user_id,
            Expense.recurring_series_id == series_id,
            Expense.recurring_frequency.is_(None),
            Expense.status == ExpenseStatus.PENDING.value,
            Expense.expense_date > today,
            Expense.deleted_at.is_(None),
        )
        .values(amount_cents=amount_cents)
    )


def _delete_recurring_occurrence(db: Session, *, e: Expense, now: datetime) -> None:
    if e.status == ExpenseStatus.PAID.value:
        e.recurring_series_id = None
        e.recurring_frequency = None
        e.recurring_generated_until = None
        return
    e.deleted_at = now
