"""Despesas — Telas.txt §5.5–5.6, §6."""

from __future__ import annotations

import calendar
import uuid
from datetime import date, timedelta
from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy import select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session, joinedload

from app.api.deps import get_current_user
from app.core.database import get_db
from app.models.category import Category
from app.models.expense import Expense
from app.models.user import User
from app.schemas.dashboard import ExpenseStatus
from app.schemas.expense import ExpenseCreate, ExpenseResponse, ExpenseUpdate
from app.services.expense_share import ExpenseShareValidationError, normalize_expense_share
from app.services.family_scope import family_peer_user_ids
from app.services.recurrence import add_months, iter_occurrence_dates

router = APIRouter(prefix="/expenses", tags=["expenses"])


def _month_bounds(year: int, month: int) -> tuple[date, date]:
    last = calendar.monthrange(year, month)[1]
    return date(year, month, 1), date(year, month, last)


def _shared_with_label(e: Expense) -> str | None:
    if e.shared_with_user_id is None:
        return None
    su = e.shared_with_user
    if su is None:
        return None
    name = (su.full_name or "").strip()
    return name if name else su.email


def _to_response(e: Expense, viewer_id: uuid.UUID) -> ExpenseResponse:
    return ExpenseResponse(
        id=e.id,
        owner_user_id=e.owner_user_id,
        is_mine=e.owner_user_id == viewer_id,
        description=e.description,
        amount_cents=int(e.amount_cents),
        expense_date=e.expense_date,
        due_date=e.due_date,
        status=e.status,
        category_id=e.category_id,
        category_key=e.category.key,
        category_name=e.category.name,
        sync_status=e.sync_status,
        installment_total=e.installment_total,
        installment_number=e.installment_number,
        installment_group_id=e.installment_group_id,
        recurring_frequency=e.recurring_frequency,
        recurring_series_id=e.recurring_series_id,
        recurring_generated_until=e.recurring_generated_until,
        is_shared=e.is_shared,
        shared_with_user_id=e.shared_with_user_id,
        shared_with_label=_shared_with_label(e),
        created_at=e.created_at,
        updated_at=e.updated_at,
    )


def _get_owned(
    db: Session, expense_id: uuid.UUID, owner_id: uuid.UUID
) -> Expense | None:
    return db.scalar(
        select(Expense)
        .options(
            joinedload(Expense.category),
            joinedload(Expense.shared_with_user),
        )
        .where(Expense.id == expense_id, Expense.owner_user_id == owner_id)
    )


def _get_visible_in_family(
    db: Session, expense_id: uuid.UUID, viewer_id: uuid.UUID
) -> Expense | None:
    peer_ids = family_peer_user_ids(db, viewer_id)
    return db.scalar(
        select(Expense)
        .options(
            joinedload(Expense.category),
            joinedload(Expense.shared_with_user),
        )
        .where(
            Expense.id == expense_id,
            Expense.owner_user_id.in_(peer_ids),
        )
    )


def _ensure_category(db: Session, category_id: uuid.UUID) -> None:
    if db.get(Category, category_id) is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Categoria não encontrada",
        )


def _ensure_recurring_generated(db: Session, user: User, until: date) -> None:
    anchors = db.scalars(
        select(Expense).where(
            Expense.owner_user_id == user.id,
            Expense.installment_total == 1,
            Expense.installment_number == 1,
            Expense.recurring_frequency.isnot(None),
        )
    ).all()
    changed = False
    for a in anchors:
        freq = a.recurring_frequency
        if freq is None:
            continue
        series_id = a.recurring_series_id or uuid.uuid4()
        a.recurring_series_id = series_id
        seed = a.recurring_generated_until or a.expense_date
        due_delta = (
            (a.due_date - a.expense_date).days if a.due_date is not None else None
        )
        for next_day in iter_occurrence_dates(
            start_from=seed,
            frequency=freq,
            until=until,
        ):
            exists = db.scalar(
                select(Expense.id).where(
                    Expense.owner_user_id == user.id,
                    Expense.recurring_series_id == series_id,
                    Expense.expense_date == next_day,
                )
            )
            if exists is None:
                generated_due = (
                    next_day + timedelta(days=due_delta) if due_delta is not None else None
                )
                db.add(
                    Expense(
                        owner_user_id=a.owner_user_id,
                        description=a.description,
                        amount_cents=int(a.amount_cents),
                        expense_date=next_day,
                        due_date=generated_due,
                        status=ExpenseStatus.PENDING.value,
                        category_id=a.category_id,
                        sync_status=0,
                        installment_total=1,
                        installment_number=1,
                        installment_group_id=None,
                        recurring_frequency=None,
                        recurring_series_id=series_id,
                        recurring_generated_until=None,
                        is_shared=a.is_shared,
                        shared_with_user_id=a.shared_with_user_id,
                    )
                )
                changed = True
            a.recurring_generated_until = next_day
            changed = True
    if changed:
        db.commit()


@router.get("", response_model=list[ExpenseResponse])
def list_expenses(
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
    year: Annotated[int | None, Query(ge=2000, le=2100)] = None,
    month: Annotated[int | None, Query(ge=1, le=12)] = None,
    category_id: Annotated[uuid.UUID | None, Query()] = None,
    status_filter: Annotated[ExpenseStatus | None, Query(alias="status")] = None,
) -> list[ExpenseResponse]:
    if year is not None and month is not None:
        _, end = _month_bounds(year, month)
        _ensure_recurring_generated(db, user, end)

    peer_ids = family_peer_user_ids(db, user.id)
    stmt = (
        select(Expense)
        .options(
            joinedload(Expense.category),
            joinedload(Expense.shared_with_user),
        )
        .where(Expense.owner_user_id.in_(peer_ids))
        .order_by(Expense.expense_date.desc(), Expense.created_at.desc())
    )
    if year is not None and month is not None:
        start, end = _month_bounds(year, month)
        stmt = stmt.where(Expense.expense_date >= start, Expense.expense_date <= end)
    if category_id is not None:
        stmt = stmt.where(Expense.category_id == category_id)
    if status_filter is not None:
        stmt = stmt.where(Expense.status == status_filter.value)

    rows = db.scalars(stmt).unique().all()
    return [_to_response(e, user.id) for e in rows]


@router.post("", response_model=ExpenseResponse, status_code=status.HTTP_201_CREATED)
def create_expense(
    body: ExpenseCreate,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> ExpenseResponse:
    _ensure_category(db, body.category_id)
    try:
        is_s, sw = normalize_expense_share(
            db, user.id, body.is_shared, body.shared_with_user_id
        )
    except ExpenseShareValidationError as err:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(err),
        ) from err
    n = body.installment_total
    if n == 1:
        recurring_series_id = uuid.uuid4() if body.recurring_frequency else None
        e = Expense(
            owner_user_id=user.id,
            description=body.description.strip(),
            amount_cents=body.amount_cents,
            expense_date=body.expense_date,
            due_date=body.due_date,
            status=body.status.value,
            category_id=body.category_id,
            installment_total=1,
            installment_number=1,
            installment_group_id=None,
            recurring_frequency=body.recurring_frequency,
            recurring_series_id=recurring_series_id,
            recurring_generated_until=body.expense_date if body.recurring_frequency else None,
            is_shared=is_s,
            shared_with_user_id=sw,
        )
        db.add(e)
        try:
            db.commit()
        except IntegrityError:
            db.rollback()
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Dados inválidos (categoria ou referência)",
            ) from None
        db.refresh(e)
        e = _get_owned(db, e.id, user.id)
        assert e is not None
        return _to_response(e, user.id)

    group_id = uuid.uuid4()
    first_id: uuid.UUID | None = None
    for i in range(n):
        ed = add_months(body.expense_date, i)
        dd = add_months(body.due_date, i) if body.due_date else None
        row = Expense(
            owner_user_id=user.id,
            description=body.description.strip(),
            amount_cents=body.amount_cents,
            expense_date=ed,
            due_date=dd,
            status=body.status.value,
            category_id=body.category_id,
            installment_total=n,
            installment_number=i + 1,
            installment_group_id=group_id,
            recurring_frequency=None,
            recurring_series_id=None,
            recurring_generated_until=None,
            is_shared=is_s,
            shared_with_user_id=sw,
        )
        db.add(row)
        if i == 0:
            db.flush()
            first_id = row.id
    try:
        db.commit()
    except IntegrityError:
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Dados inválidos (categoria ou referência)",
        ) from None
    assert first_id is not None
    e = _get_owned(db, first_id, user.id)
    assert e is not None
    return _to_response(e, user.id)


@router.get("/{expense_id}", response_model=ExpenseResponse)
def get_expense(
    expense_id: uuid.UUID,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> ExpenseResponse:
    e = _get_visible_in_family(db, expense_id, user.id)
    if e is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Despesa não encontrada")
    return _to_response(e, user.id)


@router.put("/{expense_id}", response_model=ExpenseResponse)
def update_expense(
    expense_id: uuid.UUID,
    body: ExpenseUpdate,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> ExpenseResponse:
    e = _get_owned(db, expense_id, user.id)
    if e is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Despesa não encontrada")

    data = body.model_dump(exclude_unset=True, mode="python")
    is_upd = data.pop("is_shared", None)
    sw_upd = data.pop("shared_with_user_id", None)
    if is_upd is not None or sw_upd is not None:
        new_is = is_upd if is_upd is not None else e.is_shared
        new_sw = sw_upd if sw_upd is not None else e.shared_with_user_id
        if new_is is False:
            new_sw = None
        try:
            new_is, new_sw = normalize_expense_share(db, user.id, new_is, new_sw)
        except ExpenseShareValidationError as err:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=str(err),
            ) from err
        e.is_shared = new_is
        e.shared_with_user_id = new_sw
    st = data.get("status")
    if st is not None:
        data["status"] = st.value if hasattr(st, "value") else st
    rf = data.get("recurring_frequency")
    if rf is not None and rf == "":
        data["recurring_frequency"] = None
    if "category_id" in data and data["category_id"] is not None:
        _ensure_category(db, data["category_id"])

    for k, v in data.items():
        setattr(e, k, v)

    try:
        db.commit()
    except IntegrityError:
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Dados inválidos",
        ) from None
    db.refresh(e)
    e = _get_owned(db, expense_id, user.id)
    assert e is not None
    return _to_response(e, user.id)


@router.delete("/{expense_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_expense(
    expense_id: uuid.UUID,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> None:
    e = _get_owned(db, expense_id, user.id)
    if e is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Despesa não encontrada")
    db.delete(e)
    db.commit()


@router.post("/{expense_id}/pay", response_model=ExpenseResponse)
def pay_expense(
    expense_id: uuid.UUID,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> ExpenseResponse:
    e = _get_owned(db, expense_id, user.id)
    if e is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Despesa não encontrada")
    if e.status != ExpenseStatus.PENDING.value:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Só despesas pendentes podem ser quitadas",
        )
    e.status = ExpenseStatus.PAID.value
    db.commit()
    db.refresh(e)
    e = _get_owned(db, expense_id, user.id)
    assert e is not None
    return _to_response(e, user.id)


@router.post("/{expense_id}/recurrence/stop", response_model=ExpenseResponse)
def stop_recurrence(
    expense_id: uuid.UUID,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> ExpenseResponse:
    e = _get_owned(db, expense_id, user.id)
    if e is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Despesa não encontrada")
    if e.recurring_frequency is None:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Esta despesa não tem recorrência ativa",
        )
    e.recurring_frequency = None
    e.recurring_generated_until = None
    db.commit()
    db.refresh(e)
    e = _get_owned(db, expense_id, user.id)
    assert e is not None
    return _to_response(e, user.id)
