"""Valores a receber entre membros da família (coberturas de despesas partilhadas)."""

from __future__ import annotations

import uuid
from datetime import UTC, date, datetime
from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.orm import Session, joinedload

from app.api.deps import get_current_user
from app.core.database import get_db
from app.models.expense import Expense
from app.models.family_receivable import FamilyReceivable
from app.models.income import Income
from app.models.income_category import IncomeCategory
from app.models.user import User
from app.schemas.receivable import ReceivableOut, SettleReceivableRequest
from app.services.family_scope import family_visibility_scope
from app.services.family_financial_events import record_receivable_settled

router = APIRouter(prefix="/receivables", tags=["receivables"])


def _user_label(u: User | None) -> str | None:
    if u is None:
        return None
    name = (u.full_name or "").strip()
    return name if name else u.email


def _to_receivable_out(row: FamilyReceivable, viewer_id: uuid.UUID) -> ReceivableOut:
    creditor = row.creditor
    debtor = row.debtor
    return ReceivableOut(
        id=row.id,
        creditor_user_id=row.creditor_user_id,
        debtor_user_id=row.debtor_user_id,
        amount_cents=int(row.amount_cents),
        settle_by=row.settle_by,
        source_expense_id=row.source_expense_id,
        status=row.status,
        settled_at=row.settled_at,
        created_at=row.created_at,
        updated_at=row.updated_at,
        debtor_display_name=_user_label(debtor) if viewer_id == row.creditor_user_id else None,
        creditor_display_name=_user_label(creditor) if viewer_id == row.debtor_user_id else None,
    )


@router.get("", response_model=dict)
def list_receivables(
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> dict:
    owner_ids, include_family = family_visibility_scope(db, user)
    if len(owner_ids) <= 1 and not include_family:
        return {"as_creditor": [], "as_debtor": []}
    visibility_clause = (
        FamilyReceivable.creditor_user_id.in_(owner_ids)
        & FamilyReceivable.debtor_user_id.in_(owner_ids)
        if not include_family
        else (
            (
                (
                    FamilyReceivable.creditor_user_id == user.id
                ) | (FamilyReceivable.debtor_user_id == user.id)
            )
            & (
                (Expense.owner_user_id == user.id)
                | ((Expense.owner_user_id.in_(owner_ids)) & (Expense.is_family.is_(True)))
            )
        )
    )
    rows = db.scalars(
        select(FamilyReceivable)
        .options(
            joinedload(FamilyReceivable.creditor),
            joinedload(FamilyReceivable.debtor),
        )
        .join(Expense, Expense.id == FamilyReceivable.source_expense_id, isouter=True)
        .where(
            FamilyReceivable.status == "open",
            visibility_clause,
        )
        .order_by(FamilyReceivable.settle_by.asc(), FamilyReceivable.created_at.desc())
    ).unique().all()
    as_creditor = [
        _to_receivable_out(r, user.id)
        for r in rows
        if r.creditor_user_id == user.id
    ]
    as_debtor = [
        _to_receivable_out(r, user.id)
        for r in rows
        if r.debtor_user_id == user.id
    ]
    return {"as_creditor": as_creditor, "as_debtor": as_debtor}


@router.post("/{receivable_id}/settle", response_model=ReceivableOut)
def settle_receivable(
    receivable_id: uuid.UUID,
    body: SettleReceivableRequest,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> ReceivableOut:
    row = db.scalar(
        select(FamilyReceivable)
        .options(
            joinedload(FamilyReceivable.creditor),
            joinedload(FamilyReceivable.debtor),
        )
        .where(FamilyReceivable.id == receivable_id)
    )
    if row is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Registo não encontrado")
    if row.status != "open":
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Já liquidado")
    if row.creditor_user_id != user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Só o credor pode marcar como recebido",
        )
    now = datetime.now(UTC)
    row.status = "settled"
    row.settled_at = now
    record_receivable_settled(
        db,
        creditor_user_id=row.creditor_user_id,
        debtor_user_id=row.debtor_user_id,
        receivable_id=row.id,
        source_expense_id=row.source_expense_id,
        amount_cents=int(row.amount_cents),
    )
    if body.create_income:
        cat_id = body.income_category_id
        if cat_id is None:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="create_income exige income_category_id",
            )
        if db.get(IncomeCategory, cat_id) is None:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Categoria de provento não encontrada",
            )
        inc_date = body.income_date or date.today()
        db.add(
            Income(
                owner_user_id=user.id,
                description=f"Reembolso familiar (receber)",
                amount_cents=int(row.amount_cents),
                income_date=inc_date,
                income_category_id=cat_id,
                notes=f"family_receivable_id={row.id}",
                sync_status=0,
            )
        )
    db.commit()
    row = db.scalar(
        select(FamilyReceivable)
        .options(
            joinedload(FamilyReceivable.creditor),
            joinedload(FamilyReceivable.debtor),
        )
        .where(FamilyReceivable.id == receivable_id)
    )
    assert row is not None
    return _to_receivable_out(row, user.id)
