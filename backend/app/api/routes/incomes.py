"""Proventos — entradas de dinheiro (centavos, competência mensal, família)."""

from __future__ import annotations

import calendar
import uuid
from datetime import date
from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy import select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session, joinedload

from app.api.deps import get_current_user
from app.core.database import get_db
from app.models.income import Income
from app.models.income_category import IncomeCategory
from app.models.user import User
from app.schemas.income import IncomeCreate, IncomeResponse, IncomeUpdate
from app.services.family_scope import family_visibility_scope

router = APIRouter(prefix="/incomes", tags=["incomes"])


def _month_bounds(year: int, month: int) -> tuple[date, date]:
    last = calendar.monthrange(year, month)[1]
    return date(year, month, 1), date(year, month, last)


def _to_response(row: Income, viewer_id: uuid.UUID) -> IncomeResponse:
    return IncomeResponse(
        id=row.id,
        owner_user_id=row.owner_user_id,
        is_mine=row.owner_user_id == viewer_id,
        description=row.description,
        amount_cents=int(row.amount_cents),
        income_date=row.income_date,
        income_category_id=row.income_category_id,
        category_key=row.income_category.key,
        category_name=row.income_category.name,
        notes=row.notes,
        sync_status=row.sync_status,
        is_family=bool(row.is_family),
        created_at=row.created_at,
        updated_at=row.updated_at,
    )


def _get_owned(
    db: Session, income_id: uuid.UUID, owner_id: uuid.UUID
) -> Income | None:
    return db.scalar(
        select(Income)
        .options(joinedload(Income.income_category))
        .where(Income.id == income_id, Income.owner_user_id == owner_id)
    )


def _get_visible_for_viewer(
    db: Session, income_id: uuid.UUID, viewer_id: uuid.UUID
) -> Income | None:
    viewer = db.get(User, viewer_id)
    if viewer is None:
        return None
    owner_ids, include_family = family_visibility_scope(db, viewer)
    visibility_clause = (
        Income.owner_user_id.in_(owner_ids)
        if not include_family
        else (
            (Income.owner_user_id == viewer_id)
            | ((Income.owner_user_id.in_(owner_ids)) & (Income.is_family.is_(True)))
        )
    )
    return db.scalar(
        select(Income)
        .options(joinedload(Income.income_category))
        .where(
            Income.id == income_id,
            visibility_clause,
        )
    )


def _ensure_income_category(db: Session, category_id: uuid.UUID) -> None:
    if db.get(IncomeCategory, category_id) is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Categoria de provento não encontrada",
        )


@router.get("", response_model=list[IncomeResponse])
def list_incomes(
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
    year: Annotated[int | None, Query(ge=2000, le=2100)] = None,
    month: Annotated[int | None, Query(ge=1, le=12)] = None,
) -> list[IncomeResponse]:
    owner_ids, include_family = family_visibility_scope(db, user)
    stmt = (
        select(Income)
        .options(joinedload(Income.income_category))
        .where(
            Income.owner_user_id.in_(owner_ids)
            if not include_family
            else (
                (Income.owner_user_id == user.id)
                | ((Income.owner_user_id.in_(owner_ids)) & (Income.is_family.is_(True)))
            )
        )
        .order_by(Income.income_date.desc(), Income.created_at.desc())
    )
    if year is not None and month is not None:
        start, end = _month_bounds(year, month)
        stmt = stmt.where(Income.income_date >= start, Income.income_date <= end)
    rows = db.scalars(stmt).unique().all()
    return [_to_response(r, user.id) for r in rows]


@router.post("", response_model=IncomeResponse, status_code=status.HTTP_201_CREATED)
def create_income(
    body: IncomeCreate,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> IncomeResponse:
    _ensure_income_category(db, body.income_category_id)
    notes = body.notes.strip() if body.notes and body.notes.strip() else None
    row = Income(
        owner_user_id=user.id,
        description=body.description.strip(),
        amount_cents=body.amount_cents,
        income_date=body.income_date,
        income_category_id=body.income_category_id,
        notes=notes,
        sync_status=0,
        is_family=bool(body.is_family),
    )
    db.add(row)
    try:
        db.commit()
    except IntegrityError:
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Dados inválidos",
        ) from None
    db.refresh(row)
    row = _get_owned(db, row.id, user.id)
    assert row is not None
    return _to_response(row, user.id)


@router.get("/{income_id}", response_model=IncomeResponse)
def get_income(
    income_id: uuid.UUID,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> IncomeResponse:
    row = _get_visible_for_viewer(db, income_id, user.id)
    if row is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Provento não encontrado")
    return _to_response(row, user.id)


@router.put("/{income_id}", response_model=IncomeResponse)
def update_income(
    income_id: uuid.UUID,
    body: IncomeUpdate,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> IncomeResponse:
    row = _get_owned(db, income_id, user.id)
    if row is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Provento não encontrado")

    data = body.model_dump(exclude_unset=True, mode="python")
    if "income_category_id" in data and data["income_category_id"] is not None:
        _ensure_income_category(db, data["income_category_id"])
    if "notes" in data:
        n = data["notes"]
        if n is not None and isinstance(n, str):
            n = n.strip() or None
        data["notes"] = n
    if "description" in data and data["description"] is not None:
        data["description"] = data["description"].strip()

    for k, v in data.items():
        setattr(row, k, v)

    try:
        db.commit()
    except IntegrityError:
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Dados inválidos",
        ) from None
    db.refresh(row)
    row = _get_owned(db, income_id, user.id)
    assert row is not None
    return _to_response(row, user.id)


@router.delete("/{income_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_income(
    income_id: uuid.UUID,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> None:
    row = _get_owned(db, income_id, user.id)
    if row is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Provento não encontrado")
    db.delete(row)
    db.commit()
