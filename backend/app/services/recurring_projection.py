"""UUID determinístico, materialização e resolução de ocorrências recorrentes projetadas."""

from __future__ import annotations

import uuid
from datetime import date, timedelta
from types import SimpleNamespace
from typing import TYPE_CHECKING

from sqlalchemy import select
from sqlalchemy.orm import Session, joinedload

from app.models.expense import Expense
from app.schemas.dashboard import ExpenseStatus
from app.schemas.expense import ExpenseResponse
from app.services.expense_splits import (
    clone_share_rows_from_expense,
    compute_share_extras,
    replace_expense_shares,
)
from app.services.recurrence import add_months, iter_occurrence_dates

if TYPE_CHECKING:
    pass

_PROJECTED_NS = uuid.NAMESPACE_URL


def projected_recurring_uuid(series_id: uuid.UUID, occ_date: date) -> uuid.UUID:
    return uuid.uuid5(
        _PROJECTED_NS, f"wellpaid:recurring:{series_id}:{occ_date.isoformat()}"
    )


def monthly_occurrence_on_calendar_month(
    anchor_expense_date: date, year: int, month: int
) -> date | None:
    for k in range(0, 240):
        d = add_months(anchor_expense_date, k)
        if d.year == year and d.month == month:
            return d
        if d.year > year or (d.year == year and d.month > month):
            return None
    return None


def find_anchor_and_occurrence_for_projected_id(
    db: Session,
    owner_user_id: uuid.UUID,
    expense_id: uuid.UUID,
) -> tuple[Expense, date] | None:
    anchors = db.scalars(
        select(Expense)
        .options(
            joinedload(Expense.expense_shares),
            joinedload(Expense.owner),
        )
        .where(
            Expense.owner_user_id == owner_user_id,
            Expense.installment_total == 1,
            Expense.installment_number == 1,
            Expense.recurring_frequency == "monthly",
            Expense.recurring_series_id.isnot(None),
            Expense.deleted_at.is_(None),
        )
    ).unique().all()

    horizon_end = date.today() + timedelta(days=800)
    for a in anchors:
        sid = a.recurring_series_id
        assert sid is not None
        freq = a.recurring_frequency
        if freq is None:
            continue
        seed = a.recurring_generated_until or a.expense_date
        for next_day in iter_occurrence_dates(
            start_from=seed,
            frequency=freq,
            until=horizon_end,
        ):
            if projected_recurring_uuid(sid, next_day) == expense_id:
                return (a, next_day)
    return None


def materialize_recurring_occurrence(
    db: Session,
    anchor: Expense,
    occ_date: date,
) -> Expense:
    sid = anchor.recurring_series_id
    assert sid is not None
    pid = projected_recurring_uuid(sid, occ_date)
    existing_id = db.scalar(
        select(Expense.id).where(
            Expense.owner_user_id == anchor.owner_user_id,
            Expense.recurring_series_id == sid,
            Expense.expense_date == occ_date,
            Expense.deleted_at.is_(None),
        )
    )
    if existing_id is not None:
        row = db.scalar(
            select(Expense)
            .options(
                joinedload(Expense.category),
                joinedload(Expense.shared_with_user),
            )
            .where(Expense.id == existing_id)
        )
        assert row is not None
        return row

    due_delta = (
        (anchor.due_date - anchor.expense_date).days
        if anchor.due_date is not None
        else None
    )
    gen_due = (
        occ_date + timedelta(days=due_delta) if due_delta is not None else None
    )
    row = Expense(
        id=pid,
        owner_user_id=anchor.owner_user_id,
        description=anchor.description,
        amount_cents=int(anchor.amount_cents),
        expense_date=occ_date,
        due_date=gen_due,
        status=ExpenseStatus.PENDING.value,
        category_id=anchor.category_id,
        sync_status=0,
        installment_total=1,
        installment_number=1,
        installment_group_id=None,
        recurring_frequency=None,
        recurring_series_id=sid,
        recurring_generated_until=None,
        split_mode=anchor.split_mode,
        is_shared=anchor.is_shared,
        shared_with_user_id=anchor.shared_with_user_id,
    )
    db.add(row)
    db.flush()
    sr = clone_share_rows_from_expense(anchor)
    if sr:
        replace_expense_shares(db, pid, sr)
    if anchor.recurring_generated_until is None or occ_date > anchor.recurring_generated_until:
        anchor.recurring_generated_until = occ_date
    db.flush()
    db.refresh(row)
    row = db.scalars(
        select(Expense)
        .options(
            joinedload(Expense.category),
            joinedload(Expense.shared_with_user),
            joinedload(Expense.owner),
            joinedload(Expense.expense_shares),
        )
        .where(Expense.id == row.id)
    ).unique().one()
    return row


def build_projected_expense_response(
    anchor: Expense,
    occ_date: date,
    viewer_id: uuid.UUID,
    *,
    shared_with_label: str | None,
) -> ExpenseResponse:
    due_delta = (
        (anchor.due_date - anchor.expense_date).days
        if anchor.due_date is not None
        else None
    )
    gen_due = (
        occ_date + timedelta(days=due_delta) if due_delta is not None else None
    )
    sid = anchor.recurring_series_id
    assert sid is not None
    pid = projected_recurring_uuid(sid, occ_date)
    shares = list(anchor.expense_shares) if getattr(anchor, "expense_shares", None) else []
    faux = SimpleNamespace(
        due_date=gen_due,
        is_shared=anchor.is_shared,
        shared_with_user_id=anchor.shared_with_user_id,
        owner_user_id=anchor.owner_user_id,
        split_mode=getattr(anchor, "split_mode", None),
    )
    sx = compute_share_extras(
        viewer_id=viewer_id,
        expense=faux,
        shares=shares,
        today=date.today(),
    )
    def _cp_label() -> str | None:
        if not anchor.is_shared or anchor.shared_with_user_id is None:
            return None
        if viewer_id == anchor.owner_user_id:
            return shared_with_label
        ow = anchor.owner
        if ow is None:
            return None
        name = (ow.full_name or "").strip()
        return name if name else ow.email

    return ExpenseResponse(
        id=pid,
        owner_user_id=anchor.owner_user_id,
        is_mine=anchor.owner_user_id == viewer_id,
        description=anchor.description,
        amount_cents=int(anchor.amount_cents),
        expense_date=occ_date,
        due_date=gen_due,
        status=ExpenseStatus.PENDING.value,
        category_id=anchor.category_id,
        category_key=anchor.category.key,
        category_name=anchor.category.name,
        sync_status=0,
        installment_total=1,
        installment_number=1,
        installment_group_id=None,
        recurring_frequency=None,
        recurring_series_id=sid,
        recurring_generated_until=None,
        is_shared=anchor.is_shared,
        shared_with_user_id=anchor.shared_with_user_id,
        shared_with_label=shared_with_label,
        created_at=anchor.created_at,
        updated_at=anchor.updated_at,
        paid_at=None,
        installment_plan_has_paid=None,
        is_projected=True,
        is_advanced_payment=False,
        split_mode=sx["split_mode"],
        counterparty_label=_cp_label(),
        my_share_cents=sx["my_share_cents"],
        other_user_share_cents=sx["other_user_share_cents"],
        my_share_paid=sx["my_share_paid"],
        other_share_paid=sx["other_share_paid"],
        shared_expense_payment_alert=sx["shared_expense_payment_alert"],
        shared_expense_peer_declined_alert=sx["shared_expense_peer_declined_alert"],
        my_share_declined=sx["my_share_declined"],
    )
