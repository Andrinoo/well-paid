"""Despesas — Telas.txt §5.5–5.6, §6."""

from __future__ import annotations

import calendar
import uuid
from datetime import UTC, date, datetime, timedelta
from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy import delete, select, update
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session, joinedload

from app.api.deps import get_current_user
from app.core.database import get_db
from app.models.category import Category
from app.models.expense import Expense
from app.models.expense_share import ExpenseShare
from app.models.family_receivable import FamilyReceivable
from app.models.user import User
from app.schemas.dashboard import ExpenseStatus
from app.schemas.expense import (
    ExpenseCreate,
    ExpenseCreateOutcome,
    ExpensePayRequest,
    ExpenseResponse,
    ExpenseShareDeclineRequest,
    ExpenseUpdate,
)
from app.schemas.receivable import ExpenseCoverRequest
from app.services.expense_share import ExpenseShareValidationError, normalize_expense_share
from app.services.expense_splits import (
    ExpenseSplitValidationError,
    build_two_party_shares,
    clone_share_rows_from_expense,
    compute_share_extras,
    mark_all_shares_paid_for_expense,
    replace_expense_shares,
    scale_pending_share_amounts,
    share_resolved,
    sync_expense_row_from_shares,
)
from app.services.family_scope import family_peer_user_ids
from app.services.family_financial_events import (
    record_cover_requested,
    record_owner_assumed_expense_line,
    record_peer_declined_share,
    record_receivable_cancelled,
)
from app.services.expense_delete import (
    ExpenseDeleteScope,
    ExpenseDeleteTarget,
    apply_expense_delete,
    installment_plan_has_paid,
    propagate_recurring_template_amount,
)
from app.services.recurrence import add_months, iter_occurrence_dates
from app.services.recurring_projection import (
    build_projected_expense_response,
    find_anchor_and_occurrence_for_projected_id,
    materialize_recurring_occurrence,
    monthly_occurrence_on_calendar_month,
    projected_recurring_uuid,
)

router = APIRouter(prefix="/expenses", tags=["expenses"])


def _expense_detail_loads():
    return (
        joinedload(Expense.category),
        joinedload(Expense.shared_with_user),
        joinedload(Expense.owner),
        joinedload(Expense.expense_shares),
    )


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


def _counterparty_label(e: Expense, viewer_id: uuid.UUID) -> str | None:
    if not e.is_shared or e.shared_with_user_id is None:
        return None
    if viewer_id == e.owner_user_id:
        return _shared_with_label(e)
    ow = e.owner
    if ow is None:
        return None
    name = (ow.full_name or "").strip()
    return name if name else ow.email


def _to_response(
    e: Expense,
    viewer_id: uuid.UUID,
    *,
    installment_plan_has_paid: bool | None = None,
    is_projected: bool = False,
) -> ExpenseResponse:
    is_advanced_payment = False
    if e.status == ExpenseStatus.PAID.value and e.paid_at is not None and e.due_date is not None:
        is_advanced_payment = e.paid_at.date() < e.due_date
    shares = list(e.expense_shares) if e.expense_shares else []
    sx = compute_share_extras(
        viewer_id=viewer_id,
        expense=e,
        shares=shares,
        today=date.today(),
    )
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
        paid_at=e.paid_at,
        installment_plan_has_paid=installment_plan_has_paid,
        is_projected=is_projected,
        is_advanced_payment=is_advanced_payment,
        split_mode=sx["split_mode"],
        counterparty_label=_counterparty_label(e, viewer_id),
        my_share_cents=sx["my_share_cents"],
        other_user_share_cents=sx["other_user_share_cents"],
        my_share_paid=sx["my_share_paid"],
        other_share_paid=sx["other_share_paid"],
        shared_expense_payment_alert=sx["shared_expense_payment_alert"],
        shared_expense_peer_declined_alert=sx["shared_expense_peer_declined_alert"],
        my_share_declined=sx["my_share_declined"],
    )


def _get_owned(
    db: Session, expense_id: uuid.UUID, owner_id: uuid.UUID
) -> Expense | None:
    return db.scalar(
        select(Expense)
        .options(*_expense_detail_loads())
        .where(
            Expense.id == expense_id,
            Expense.owner_user_id == owner_id,
            Expense.deleted_at.is_(None),
        )
    )


def _get_visible_in_family(
    db: Session, expense_id: uuid.UUID, viewer_id: uuid.UUID
) -> Expense | None:
    peer_ids = family_peer_user_ids(db, viewer_id)
    return db.scalar(
        select(Expense)
        .options(*_expense_detail_loads())
        .where(
            Expense.id == expense_id,
            Expense.owner_user_id.in_(peer_ids),
            Expense.deleted_at.is_(None),
        )
    )


def _ensure_category(db: Session, category_id: uuid.UUID) -> None:
    if db.get(Category, category_id) is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Categoria não encontrada",
        )


def _ensure_recurring_generated(db: Session, owner_user_id: uuid.UUID, until: date) -> None:
    anchors = db.scalars(
        select(Expense)
        .options(joinedload(Expense.expense_shares))
        .where(
            Expense.owner_user_id == owner_user_id,
            Expense.installment_total == 1,
            Expense.installment_number == 1,
            Expense.recurring_frequency.isnot(None),
            Expense.deleted_at.is_(None),
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
            if next_day > until:
                break
            exists = db.scalar(
                select(Expense.id).where(
                    Expense.owner_user_id == owner_user_id,
                    Expense.recurring_series_id == series_id,
                    Expense.expense_date == next_day,
                    Expense.deleted_at.is_(None),
                )
            )
            if exists is not None:
                if a.recurring_generated_until is None or next_day > a.recurring_generated_until:
                    a.recurring_generated_until = next_day
                changed = True
                continue

            generated_due = (
                next_day + timedelta(days=due_delta) if due_delta is not None else None
            )
            ref = generated_due if generated_due is not None else next_day
            if date.today() < ref - timedelta(days=3):
                break

            row_id = projected_recurring_uuid(series_id, next_day)
            db.add(
                Expense(
                    id=row_id,
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
                    split_mode=a.split_mode,
                    is_shared=a.is_shared,
                    shared_with_user_id=a.shared_with_user_id,
                )
            )
            db.flush()
            sr = clone_share_rows_from_expense(a)
            if sr:
                replace_expense_shares(db, row_id, sr)
            a.recurring_generated_until = next_day
            changed = True
    if changed:
        db.commit()


def _merge_projected_recurring_month(
    db: Session,
    *,
    viewer_id: uuid.UUID,
    peer_ids: list[uuid.UUID],
    year: int,
    month: int,
    db_rows: list[Expense],
) -> list[ExpenseResponse]:
    keys_existing = {
        (e.recurring_series_id, e.expense_date)
        for e in db_rows
        if e.recurring_series_id is not None
    }
    seen_ids: set[uuid.UUID] = {e.id for e in db_rows}

    anchors = db.scalars(
        select(Expense)
        .options(*_expense_detail_loads())
        .where(
            Expense.owner_user_id.in_(peer_ids),
            Expense.installment_total == 1,
            Expense.installment_number == 1,
            Expense.recurring_frequency == "monthly",
            Expense.recurring_series_id.isnot(None),
            Expense.deleted_at.is_(None),
        )
    ).unique().all()

    projected: list[ExpenseResponse] = []
    for a in anchors:
        occ = monthly_occurrence_on_calendar_month(a.expense_date, year, month)
        if occ is None or occ < a.expense_date:
            continue
        sid = a.recurring_series_id
        assert sid is not None
        if (sid, occ) in keys_existing:
            continue
        exists = db.scalar(
            select(Expense.id).where(
                Expense.owner_user_id == a.owner_user_id,
                Expense.recurring_series_id == sid,
                Expense.expense_date == occ,
                Expense.deleted_at.is_(None),
            )
        )
        if exists is not None:
            continue
        due_delta = (
            (a.due_date - a.expense_date).days if a.due_date is not None else None
        )
        gen_due = occ + timedelta(days=due_delta) if due_delta is not None else None
        ref = gen_due if gen_due is not None else occ
        if date.today() >= ref - timedelta(days=3):
            continue
        pr = build_projected_expense_response(
            a,
            occ,
            viewer_id,
            shared_with_label=_shared_with_label(a),
        )
        if pr.id not in seen_ids:
            projected.append(pr)
            seen_ids.add(pr.id)

    out = [_to_response(e, viewer_id) for e in db_rows]
    out.extend(projected)
    out.sort(key=lambda r: (r.expense_date, r.created_at), reverse=True)
    return out


@router.get("", response_model=list[ExpenseResponse])
def list_expenses(
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
    year: Annotated[int | None, Query(ge=2000, le=2100)] = None,
    month: Annotated[int | None, Query(ge=1, le=12)] = None,
    category_id: Annotated[uuid.UUID | None, Query()] = None,
    status_filter: Annotated[ExpenseStatus | None, Query(alias="status")] = None,
    installment_group_id: Annotated[uuid.UUID | None, Query()] = None,
) -> list[ExpenseResponse]:
    today = date.today()
    until_h = today + timedelta(days=450)
    if year is not None and month is not None:
        _, end_m = _month_bounds(year, month)
        until_h = max(until_h, end_m)
    peer_ids = family_peer_user_ids(db, user.id)
    for uid in peer_ids:
        _ensure_recurring_generated(db, uid, until_h)
    stmt = (
        select(Expense)
        .options(*_expense_detail_loads())
        .where(
            Expense.owner_user_id.in_(peer_ids),
            Expense.deleted_at.is_(None),
        )
        .order_by(Expense.expense_date.desc(), Expense.created_at.desc())
    )
    if year is not None and month is not None:
        start, end = _month_bounds(year, month)
        stmt = stmt.where(Expense.expense_date >= start, Expense.expense_date <= end)
    if category_id is not None:
        stmt = stmt.where(Expense.category_id == category_id)
    if status_filter is not None:
        stmt = stmt.where(Expense.status == status_filter.value)
    if installment_group_id is not None:
        stmt = stmt.where(Expense.installment_group_id == installment_group_id)

    rows = db.scalars(stmt).unique().all()
    if (
        year is not None
        and month is not None
        and installment_group_id is None
        and status_filter != ExpenseStatus.PAID
    ):
        return _merge_projected_recurring_month(
            db,
            viewer_id=user.id,
            peer_ids=peer_ids,
            year=year,
            month=month,
            db_rows=list(rows),
        )
    if (
        status_filter == ExpenseStatus.PENDING
        and year is None
        and month is None
        and installment_group_id is None
    ):
        base = [_to_response(e, user.id) for e in rows]
        seen: set[uuid.UUID] = {r.id for r in base}
        extra: list[ExpenseResponse] = []
        cy, cm = today.year, today.month
        for _ in range(7):
            for r in _merge_projected_recurring_month(
                db,
                viewer_id=user.id,
                peer_ids=peer_ids,
                year=cy,
                month=cm,
                db_rows=[],
            ):
                if r.id not in seen:
                    seen.add(r.id)
                    extra.append(r)
            if cm == 12:
                cy += 1
                cm = 1
            else:
                cm += 1
        return base + extra
    return [_to_response(e, user.id) for e in rows]


@router.post("", response_model=ExpenseCreateOutcome, status_code=status.HTTP_201_CREATED)
def create_expense(
    body: ExpenseCreate,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> ExpenseCreateOutcome:
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
    if is_s and sw is None:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Indica o membro da família com quem partilhas a despesa.",
        )
    share_rows: list[dict] | None = None
    if is_s and sw is not None:
        assert body.split_mode is not None
        try:
            share_rows = build_two_party_shares(
                owner_id=user.id,
                peer_id=sw,
                amount_cents=body.amount_cents,
                split_mode=body.split_mode,
                owner_share_cents=body.owner_share_cents,
                peer_share_cents=body.peer_share_cents,
                owner_percent_bps=body.owner_percent_bps,
                peer_percent_bps=body.peer_percent_bps,
            )
        except ExpenseSplitValidationError as err:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=str(err),
            ) from err
    n = body.installment_total
    today = date.today()
    now_utc = datetime.now(UTC)
    if n == 1:
        # Para recorrência, a primeira competência é sempre a data de vencimento informada.
        first_occurrence_date = (
            body.due_date if body.recurring_frequency is not None and body.due_date is not None else body.expense_date
        )
        recurring_series_id = uuid.uuid4() if body.recurring_frequency else None
        auto_paid = (
            body.recurring_frequency is not None
            and body.due_date is not None
            and first_occurrence_date <= today
        )
        row_status = ExpenseStatus.PAID.value if auto_paid else body.status.value
        e = Expense(
            owner_user_id=user.id,
            description=body.description.strip(),
            amount_cents=body.amount_cents,
            expense_date=first_occurrence_date,
            due_date=body.due_date,
            status=row_status,
            category_id=body.category_id,
            installment_total=1,
            installment_number=1,
            installment_group_id=None,
            recurring_frequency=body.recurring_frequency,
            recurring_series_id=recurring_series_id,
            recurring_generated_until=first_occurrence_date if body.recurring_frequency else None,
            split_mode=body.split_mode if is_s and sw else None,
            is_shared=is_s,
            shared_with_user_id=sw,
            paid_at=now_utc if row_status == ExpenseStatus.PAID.value else None,
        )
        db.add(e)
        try:
            db.flush()
            if share_rows is not None:
                replace_expense_shares(db, e.id, share_rows)
                if row_status == ExpenseStatus.PAID.value:
                    shs = mark_all_shares_paid_for_expense(db, e.id, now=now_utc)
                else:
                    shs = list(
                        db.scalars(
                            select(ExpenseShare).where(ExpenseShare.expense_id == e.id)
                        ).all()
                    )
                sync_expense_row_from_shares(e, shs, now=now_utc)
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
        return ExpenseCreateOutcome(
            installment_group_id=None,
            expenses=[_to_response(e, user.id)],
        )

    group_id = uuid.uuid4()
    first_id: uuid.UUID | None = None
    assert body.due_date is not None
    first_due_date = body.due_date
    for i in range(n):
        # Parcelas mensais a partir do primeiro vencimento; competência = data de vencimento.
        dd = add_months(first_due_date, i)
        ed = dd
        # Parcelas com vencimento/competência já passados ficam como pagas ao criar o plano
        # (ex.: primeira competência dia 10, registo dia 14 — a parcela do dia 10 não fica pendente).
        auto_paid = dd <= today
        row_status = ExpenseStatus.PAID.value if auto_paid else ExpenseStatus.PENDING.value
        row = Expense(
            owner_user_id=user.id,
            description=body.description.strip(),
            amount_cents=body.amount_cents,
            expense_date=ed,
            due_date=dd,
            status=row_status,
            category_id=body.category_id,
            installment_total=n,
            installment_number=i + 1,
            installment_group_id=group_id,
            recurring_frequency=None,
            recurring_series_id=None,
            recurring_generated_until=None,
            split_mode=body.split_mode if is_s and sw else None,
            is_shared=is_s,
            shared_with_user_id=sw,
            paid_at=now_utc if row_status == ExpenseStatus.PAID.value else None,
        )
        db.add(row)
        if i == 0:
            db.flush()
            first_id = row.id
    try:
        db.flush()
        if share_rows is not None:
            grp_rows = db.scalars(
                select(Expense)
                .where(
                    Expense.owner_user_id == user.id,
                    Expense.installment_group_id == group_id,
                    Expense.deleted_at.is_(None),
                )
                .order_by(Expense.installment_number.asc())
            ).all()
            for row in grp_rows:
                replace_expense_shares(db, row.id, share_rows)
                if row.status == ExpenseStatus.PAID.value:
                    shs = mark_all_shares_paid_for_expense(db, row.id, now=now_utc)
                else:
                    shs = list(
                        db.scalars(
                            select(ExpenseShare).where(ExpenseShare.expense_id == row.id)
                        ).all()
                    )
                sync_expense_row_from_shares(row, shs, now=now_utc)
        db.commit()
    except IntegrityError:
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Dados inválidos (categoria ou referência)",
        ) from None
    assert first_id is not None
    siblings = db.scalars(
        select(Expense)
        .options(*_expense_detail_loads())
        .where(
            Expense.owner_user_id == user.id,
            Expense.installment_group_id == group_id,
            Expense.deleted_at.is_(None),
        )
        .order_by(Expense.installment_number.asc())
    ).unique().all()
    return ExpenseCreateOutcome(
        installment_group_id=group_id,
        expenses=[_to_response(x, user.id) for x in siblings],
    )


@router.get("/{expense_id}", response_model=ExpenseResponse)
def get_expense(
    expense_id: uuid.UUID,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> ExpenseResponse:
    e = _get_visible_in_family(db, expense_id, user.id)
    if e is None:
        peer_ids = family_peer_user_ids(db, user.id)
        for uid in peer_ids:
            hit = find_anchor_and_occurrence_for_projected_id(db, uid, expense_id)
            if hit is not None:
                anchor, occ = hit
                return build_projected_expense_response(
                    anchor,
                    occ,
                    user.id,
                    shared_with_label=_shared_with_label(anchor),
                )
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Despesa não encontrada")
    has_paid: bool | None = None
    if e.installment_group_id is not None:
        has_paid = installment_plan_has_paid(
            db, e.owner_user_id, e.installment_group_id
        )
    return _to_response(e, user.id, installment_plan_has_paid=has_paid)


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

    prior_shares = list(
        db.scalars(select(ExpenseShare).where(ExpenseShare.expense_id == expense_id)).all()
    )
    has_declined_share = any(s.status == "declined" for s in prior_shares)

    data = body.model_dump(exclude_unset=True, mode="python")
    is_upd = data.pop("is_shared", None)
    sw_upd = data.pop("shared_with_user_id", None)
    if has_declined_share and (is_upd is not None or sw_upd is not None):
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Parte recusada: usa Assumir despesa para resolver antes de alterar a partilha.",
        )
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
        if new_is is False:
            e.split_mode = None
            db.execute(delete(ExpenseShare).where(ExpenseShare.expense_id == expense_id))
    st = data.get("status")
    if st is not None:
        data["status"] = st.value if hasattr(st, "value") else st
    rf = data.get("recurring_frequency")
    if rf is not None and rf == "":
        data["recurring_frequency"] = None
    if "category_id" in data and data["category_id"] is not None:
        _ensure_category(db, data["category_id"])

    old_amount = int(e.amount_cents)
    split_mode_in = data.pop("split_mode", None)
    owner_share_cents_in = data.pop("owner_share_cents", None)
    peer_share_cents_in = data.pop("peer_share_cents", None)
    owner_percent_bps_in = data.pop("owner_percent_bps", None)
    peer_percent_bps_in = data.pop("peer_percent_bps", None)
    series_for_propagate: uuid.UUID | None = None
    if (
        e.recurring_frequency is not None
        and e.recurring_series_id is not None
        and "amount_cents" in data
    ):
        series_for_propagate = e.recurring_series_id

    for k, v in data.items():
        setattr(e, k, v)

    try:
        if series_for_propagate is not None and int(e.amount_cents) != old_amount:
            propagate_recurring_template_amount(
                db,
                user_id=user.id,
                series_id=series_for_propagate,
                amount_cents=int(e.amount_cents),
                today=date.today(),
            )
        new_total = int(e.amount_cents)
        if e.is_shared and e.shared_with_user_id is not None:
            if has_declined_share:
                split_touch = (
                    split_mode_in is not None
                    or owner_share_cents_in is not None
                    or peer_share_cents_in is not None
                    or owner_percent_bps_in is not None
                    or peer_percent_bps_in is not None
                )
                if split_touch or new_total != old_amount:
                    raise HTTPException(
                        status_code=status.HTTP_409_CONFLICT,
                        detail="Parte recusada: usa Assumir despesa para resolver antes de alterar valores ou divisão.",
                    )
            if split_mode_in is not None:
                e.split_mode = split_mode_in
            should_rebuild = False
            if split_mode_in == "amount" and owner_share_cents_in is not None and peer_share_cents_in is not None:
                should_rebuild = True
            if split_mode_in == "percent" and owner_percent_bps_in is not None and peer_percent_bps_in is not None:
                should_rebuild = True
            if should_rebuild and split_mode_in is not None:
                try:
                    rows = build_two_party_shares(
                        owner_id=e.owner_user_id,
                        peer_id=e.shared_with_user_id,
                        amount_cents=new_total,
                        split_mode=split_mode_in,
                        owner_share_cents=owner_share_cents_in,
                        peer_share_cents=peer_share_cents_in,
                        owner_percent_bps=owner_percent_bps_in,
                        peer_percent_bps=peer_percent_bps_in,
                    )
                    replace_expense_shares(db, e.id, rows)
                except ExpenseSplitValidationError as err:
                    raise HTTPException(
                        status_code=status.HTTP_400_BAD_REQUEST,
                        detail=str(err),
                    ) from err
            elif new_total != old_amount:
                try:
                    scale_pending_share_amounts(
                        db,
                        e.id,
                        new_total=new_total,
                        old_total=old_amount,
                    )
                except ExpenseSplitValidationError as err:
                    raise HTTPException(
                        status_code=status.HTTP_400_BAD_REQUEST,
                        detail=str(err),
                    ) from err
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
    delete_target: Annotated[ExpenseDeleteTarget, Query()] = ExpenseDeleteTarget.series,
    delete_scope: Annotated[ExpenseDeleteScope, Query()] = ExpenseDeleteScope.all,
    confirm_delete_paid: Annotated[bool, Query()] = False,
) -> None:
    e = _get_owned(db, expense_id, user.id)
    if e is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Despesa não encontrada")
    if e.installment_group_id is not None and delete_target != ExpenseDeleteTarget.series:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Parcelamento: use delete_target=series",
        )
    if (
        e.installment_group_id is not None
        and delete_scope == ExpenseDeleteScope.all
        and installment_plan_has_paid(db, user.id, e.installment_group_id)
        and not confirm_delete_paid
    ):
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail=(
                "Este plano tem parcelas já pagas. Use delete_scope=future_unpaid para remover "
                "apenas as parcelas pendentes futuras (não apaga histórico pago), ou envie "
                "confirm_delete_paid=true para apagar o plano inteiro (incluindo pagas) após "
                "confirmação explícita no cliente."
            ),
        )
    if (
        e.recurring_series_id is not None
        and e.recurring_frequency is not None
        and delete_target == ExpenseDeleteTarget.occurrence
    ):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=(
                "Esta linha é a âncora da recorrência; não pode ser eliminada como "
                "ocorrência isolada. Pare a recorrência na edição ou elimine a série."
            ),
        )
    now = datetime.now(UTC)
    today = date.today()
    apply_expense_delete(
        db,
        user_id=user.id,
        e=e,
        target=delete_target,
        scope=delete_scope,
        now=now,
        today=today,
    )
    db.commit()


def _get_expense_for_family_action(
    db: Session, expense_id: uuid.UUID, viewer_id: uuid.UUID
) -> Expense | None:
    e = _get_owned(db, expense_id, viewer_id)
    if e is not None:
        return e
    return _get_visible_in_family(db, expense_id, viewer_id)


def _cancel_open_receivables_for_share(db: Session, share_id: uuid.UUID) -> list[FamilyReceivable]:
    open_rows = list(
        db.scalars(
            select(FamilyReceivable).where(
                FamilyReceivable.source_expense_share_id == share_id,
                FamilyReceivable.status == "open",
            )
        ).all()
    )
    now = datetime.now(UTC)
    for r in open_rows:
        r.status = "cancelled"
        r.settled_at = now
    return open_rows


@router.post("/{expense_id}/pay", response_model=ExpenseResponse)
def pay_expense(
    expense_id: uuid.UUID,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
    body: ExpensePayRequest | None = None,
) -> ExpenseResponse:
    req = body or ExpensePayRequest()
    peer_ids = family_peer_user_ids(db, user.id)
    e = _get_owned(db, expense_id, user.id)
    if e is None:
        e = _get_visible_in_family(db, expense_id, user.id)
    if e is None:
        for uid in peer_ids:
            hit = find_anchor_and_occurrence_for_projected_id(db, uid, expense_id)
            if hit is not None:
                e = materialize_recurring_occurrence(db, hit[0], hit[1])
                db.commit()
                e = _get_visible_in_family(db, expense_id, user.id)
                break
    if e is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Despesa não encontrada")

    shs = list(db.scalars(select(ExpenseShare).where(ExpenseShare.expense_id == e.id)).all())
    if e.is_shared and shs:
        if e.status != ExpenseStatus.PENDING.value:
            raise HTTPException(
                status_code=status.HTTP_409_CONFLICT,
                detail="Só despesas pendentes podem ser quitadas",
            )
        mine = next((x for x in shs if x.user_id == user.id), None)
        other = next((x for x in shs if x.user_id != user.id), None)
        if mine is None:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="Sem parte atribuída a ti nesta despesa partilhada",
            )
        if mine.status == "declined":
            raise HTTPException(
                status_code=status.HTTP_409_CONFLICT,
                detail="Recusaste a tua parte; só o criador pode assumir a despesa nesta linha.",
            )
        if (
            other is not None
            and other.status == "declined"
            and user.id == e.owner_user_id
        ):
            raise HTTPException(
                status_code=status.HTTP_409_CONFLICT,
                detail="A outra parte recusou; usa Assumir despesa para continuar.",
            )
        if share_resolved(mine.status):
            raise HTTPException(
                status_code=status.HTTP_409_CONFLICT,
                detail="A tua parte já está quitada",
            )
        today = date.today()
        if (
            e.due_date is not None
            and e.due_date > today
            and (e.due_date - today).days > 5
            and not req.allow_advance
        ):
            raise HTTPException(
                status_code=status.HTTP_409_CONFLICT,
                detail="Pagamento antecipado só é permitido a até 5 dias do vencimento ou com antecipação ativa.",
            )
        now = datetime.now(UTC)
        mine.status = "paid"
        mine.paid_at = now
        sync_expense_row_from_shares(e, shs, now=now)
        paid_date_tag = now.date().strftime("%d/%m/%Y")
        if e.status == ExpenseStatus.PAID.value and f"[Pago em {paid_date_tag}]" not in e.description:
            base_desc = e.description.strip()
            tagged = f"{base_desc} [Pago em {paid_date_tag}]"
            e.description = tagged[:500]
        db.commit()
        e2 = _get_expense_for_family_action(db, expense_id, user.id)
        assert e2 is not None
        return _to_response(e2, user.id)

    if e.status != ExpenseStatus.PENDING.value:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Só despesas pendentes podem ser quitadas",
        )
    today = date.today()
    if (
        e.due_date is not None
        and e.due_date > today
        and (e.due_date - today).days > 5
        and not req.allow_advance
    ):
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Pagamento antecipado só é permitido a até 5 dias do vencimento ou com antecipação ativa.",
        )
    now = datetime.now(UTC)
    if req.amount_cents is not None:
        e.amount_cents = int(req.amount_cents)
    e.status = ExpenseStatus.PAID.value
    e.paid_at = now
    paid_date_tag = now.date().strftime("%d/%m/%Y")
    if f"[Pago em {paid_date_tag}]" not in e.description:
        base_desc = e.description.strip()
        tagged = f"{base_desc} [Pago em {paid_date_tag}]"
        e.description = tagged[:500]
    db.commit()
    db.refresh(e)
    e = _get_owned(db, expense_id, user.id)
    assert e is not None
    return _to_response(e, user.id)


@router.post("/{expense_id}/share/cover-request", response_model=ExpenseResponse)
def request_share_cover(
    expense_id: uuid.UUID,
    body: ExpenseCoverRequest,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> ExpenseResponse:
    e = _get_expense_for_family_action(db, expense_id, user.id)
    if e is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Despesa não encontrada")
    if not e.is_shared:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Só despesas partilhadas",
        )
    shs = list(db.scalars(select(ExpenseShare).where(ExpenseShare.expense_id == e.id)).all())
    if len(shs) < 2:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Partes em falta; edita a despesa para definir a divisão",
        )
    mine = next((x for x in shs if x.user_id == user.id), None)
    other = next((x for x in shs if x.user_id != user.id), None)
    if mine is None or other is None:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Sem parte associada a ti nesta despesa",
        )
    if mine.status == "declined":
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Recusaste a tua parte; não podes pedir cobertura para a mesma linha.",
        )
    if share_resolved(mine.status):
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="A tua parte já está tratada",
        )
    dup = db.scalar(
        select(FamilyReceivable.id).where(
            FamilyReceivable.source_expense_share_id == mine.id,
            FamilyReceivable.status == "open",
        )
    )
    if dup is not None:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Já existe um pedido de cobertura pendente para esta parte",
        )
    now = datetime.now(UTC)
    creditor_id = other.user_id
    mine.status = "covered_by_peer"
    mine.covered_by_user_id = creditor_id
    mine.covered_at = now
    sync_expense_row_from_shares(e, shs, now=now)
    fr = FamilyReceivable(
        creditor_user_id=creditor_id,
        debtor_user_id=user.id,
        amount_cents=int(mine.share_cents),
        settle_by=body.settle_by,
        source_expense_id=e.id,
        source_expense_share_id=mine.id,
        status="open",
    )
    db.add(fr)
    db.flush()
    record_cover_requested(
        db,
        debtor_user_id=user.id,
        creditor_user_id=creditor_id,
        expense_id=e.id,
        expense_share_id=mine.id,
        receivable_id=fr.id,
        amount_cents=int(mine.share_cents),
    )
    db.commit()
    e2 = _get_expense_for_family_action(db, expense_id, user.id)
    assert e2 is not None
    return _to_response(e2, user.id)


@router.post("/{expense_id}/share/decline", response_model=ExpenseResponse)
def decline_expense_share(
    expense_id: uuid.UUID,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
    body: ExpenseShareDeclineRequest | None = None,
) -> ExpenseResponse:
    req = body or ExpenseShareDeclineRequest()
    e = _get_visible_in_family(db, expense_id, user.id)
    if e is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Despesa não encontrada")
    if not e.is_shared or e.shared_with_user_id is None:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Só despesas partilhadas",
        )
    if user.id != e.shared_with_user_id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Só o outro membro pode recusar a sua parte",
        )
    shs = list(db.scalars(select(ExpenseShare).where(ExpenseShare.expense_id == e.id)).all())
    if len(shs) < 2:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Partes em falta; edita a despesa para definir a divisão",
        )
    mine = next((x for x in shs if x.user_id == user.id), None)
    if mine is None:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Sem parte associada a ti nesta despesa",
        )
    if mine.status == "declined":
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Já recusaste esta parte",
        )
    if mine.status != "pending":
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="A tua parte já não pode ser recusada neste estado",
        )
    reason = (req.reason or "").strip()
    now = datetime.now(UTC)
    mine.status = "declined"
    mine.declined_at = now
    mine.decline_reason = reason[:500] if reason else None
    cancelled_receivables = _cancel_open_receivables_for_share(db, mine.id)
    sync_expense_row_from_shares(e, shs, now=now)
    record_peer_declined_share(
        db,
        peer_user_id=user.id,
        owner_user_id=e.owner_user_id,
        expense_id=e.id,
        expense_share_id=mine.id,
        share_amount_cents=int(mine.share_cents),
        decline_reason=mine.decline_reason,
    )
    for r in cancelled_receivables:
        record_receivable_cancelled(
            db,
            debtor_user_id=r.debtor_user_id,
            creditor_user_id=r.creditor_user_id,
            receivable_id=r.id,
            source_expense_id=r.source_expense_id,
            amount_cents=int(r.amount_cents),
            reason="peer_declined_share",
        )
    db.commit()
    e2 = _get_expense_for_family_action(db, expense_id, user.id)
    assert e2 is not None
    return _to_response(e2, user.id)


@router.post("/{expense_id}/share/assume-full", response_model=ExpenseResponse)
def assume_full_expense_share(
    expense_id: uuid.UUID,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> ExpenseResponse:
    e = _get_owned(db, expense_id, user.id)
    if e is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Despesa não encontrada")
    if not e.is_shared or e.shared_with_user_id is None:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Só despesas partilhadas",
        )
    if user.id != e.owner_user_id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Só o criador pode assumir a despesa nesta linha",
        )
    shs = list(db.scalars(select(ExpenseShare).where(ExpenseShare.expense_id == e.id)).all())
    peer = next((x for x in shs if x.user_id == e.shared_with_user_id), None)
    if peer is None or peer.status != "declined":
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="A outra parte ainda não recusou ou a situação não exige assumir.",
        )
    peer_uid = e.shared_with_user_id
    assert peer_uid is not None
    snap_amount = int(e.amount_cents)
    snap_installment_number = e.installment_number
    snap_installment_group_id = e.installment_group_id
    snap_expense_id = e.id
    db.execute(delete(ExpenseShare).where(ExpenseShare.expense_id == e.id))
    e.is_shared = False
    e.shared_with_user_id = None
    e.split_mode = None
    record_owner_assumed_expense_line(
        db,
        owner_user_id=user.id,
        peer_user_id=peer_uid,
        expense_id=snap_expense_id,
        amount_cents=snap_amount,
        installment_number=snap_installment_number,
        installment_group_id=snap_installment_group_id,
    )
    db.commit()
    db.refresh(e)
    e2 = _get_owned(db, expense_id, user.id)
    assert e2 is not None
    return _to_response(e2, user.id)


@router.post("/{expense_id}/share/mark-paid", response_model=ExpenseResponse)
def mark_share_paid_alias(
    expense_id: uuid.UUID,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
    body: ExpensePayRequest | None = None,
) -> ExpenseResponse:
    return pay_expense(expense_id, user, db, body)


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
