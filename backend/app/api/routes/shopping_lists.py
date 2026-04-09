"""Listas de compras — Ordems §6.2.2."""

from __future__ import annotations

import uuid
from datetime import UTC, datetime
from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import func, select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session, selectinload

from app.api.deps import get_current_user
from app.core.database import get_db
from app.models.category import Category
from app.models.expense import Expense
from app.models.shopping_list import ShoppingList
from app.models.shopping_list_item import ShoppingListItem
from app.models.user import User
from app.schemas.dashboard import ExpenseStatus
from app.schemas.shopping_list import (
    ShoppingListComplete,
    ShoppingListCreate,
    ShoppingListDetailResponse,
    ShoppingListItemCreate,
    ShoppingListItemPatch,
    ShoppingListItemResponse,
    ShoppingListPatch,
    ShoppingListSummaryResponse,
)
from app.services.expense_share import ExpenseShareValidationError, normalize_expense_share
from app.services.family_scope import family_peer_user_ids
from app.services.shopping_list_totals import resolve_checkout_total_cents

router = APIRouter(prefix="/shopping-lists", tags=["shopping-lists"])

STATUS_DRAFT = "draft"
STATUS_COMPLETED = "completed"


def _owned_list(db: Session, list_id: uuid.UUID, owner_id: uuid.UUID) -> ShoppingList | None:
    return db.scalar(
        select(ShoppingList).where(
            ShoppingList.id == list_id,
            ShoppingList.owner_user_id == owner_id,
        )
    )


def _visible_list(db: Session, list_id: uuid.UUID, viewer_id: uuid.UUID) -> ShoppingList | None:
    peer_ids = family_peer_user_ids(db, viewer_id)
    return db.scalar(
        select(ShoppingList).where(
            ShoppingList.id == list_id,
            ShoppingList.owner_user_id.in_(peer_ids),
        )
    )


def _item_counts(db: Session, list_ids: list[uuid.UUID]) -> dict[uuid.UUID, int]:
    if not list_ids:
        return {}
    rows = db.execute(
        select(ShoppingListItem.list_id, func.count(ShoppingListItem.id)).where(
            ShoppingListItem.list_id.in_(list_ids)
        ).group_by(ShoppingListItem.list_id)
    ).all()
    return {lid: int(c) for lid, c in rows}


def _ensure_category(db: Session, category_id: uuid.UUID) -> None:
    if db.get(Category, category_id) is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Categoria não encontrada",
        )


def _default_expense_description(store_name: str | None, expense_date: object) -> str:
    ds = expense_date.strftime("%d/%m/%Y")
    if store_name and str(store_name).strip():
        return f"Compras ({store_name.strip()}) — {ds}"
    return f"Compras — {ds}"


def _to_item(row: ShoppingListItem) -> ShoppingListItemResponse:
    return ShoppingListItemResponse(
        id=row.id,
        sort_order=row.sort_order,
        label=row.label,
        line_amount_cents=int(row.line_amount_cents)
        if row.line_amount_cents is not None
        else None,
    )


def _to_detail(row: ShoppingList, viewer_id: uuid.UUID) -> ShoppingListDetailResponse:
    items = sorted(row.items, key=lambda x: x.sort_order)
    return ShoppingListDetailResponse(
        id=row.id,
        owner_user_id=row.owner_user_id,
        is_mine=row.owner_user_id == viewer_id,
        title=row.title,
        store_name=row.store_name,
        status=row.status,
        completed_at=row.completed_at,
        expense_id=row.expense_id,
        total_cents=int(row.total_cents) if row.total_cents is not None else None,
        items=[_to_item(i) for i in items],
        created_at=row.created_at,
        updated_at=row.updated_at,
    )


def _to_summary(
    row: ShoppingList,
    viewer_id: uuid.UUID,
    items_count: int,
) -> ShoppingListSummaryResponse:
    return ShoppingListSummaryResponse(
        id=row.id,
        owner_user_id=row.owner_user_id,
        is_mine=row.owner_user_id == viewer_id,
        title=row.title,
        store_name=row.store_name,
        status=row.status,
        completed_at=row.completed_at,
        expense_id=row.expense_id,
        total_cents=int(row.total_cents) if row.total_cents is not None else None,
        items_count=items_count,
        created_at=row.created_at,
        updated_at=row.updated_at,
    )


@router.get("", response_model=list[ShoppingListSummaryResponse])
def list_shopping_lists(
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> list[ShoppingListSummaryResponse]:
    peer_ids = family_peer_user_ids(db, user.id)
    rows = db.scalars(
        select(ShoppingList)
        .where(ShoppingList.owner_user_id.in_(peer_ids))
        .order_by(ShoppingList.updated_at.desc())
    ).all()
    counts = _item_counts(db, [r.id for r in rows])
    return [_to_summary(r, user.id, counts.get(r.id, 0)) for r in rows]


@router.post("", response_model=ShoppingListDetailResponse, status_code=status.HTTP_201_CREATED)
def create_shopping_list(
    body: ShoppingListCreate,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> ShoppingListDetailResponse:
    row = ShoppingList(
        owner_user_id=user.id,
        title=body.title,
        store_name=body.store_name,
        status=STATUS_DRAFT,
        completed_at=None,
        expense_id=None,
        total_cents=None,
    )
    db.add(row)
    db.commit()
    db.refresh(row)
    row = db.scalar(
        select(ShoppingList)
        .options(selectinload(ShoppingList.items))
        .where(ShoppingList.id == row.id)
    )
    assert row is not None
    return _to_detail(row, user.id)


@router.get("/{list_id}", response_model=ShoppingListDetailResponse)
def get_shopping_list(
    list_id: uuid.UUID,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> ShoppingListDetailResponse:
    row = db.scalar(
        select(ShoppingList)
        .options(selectinload(ShoppingList.items))
        .where(ShoppingList.id == list_id)
    )
    if row is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Lista não encontrada")
    peer_ids = family_peer_user_ids(db, user.id)
    if row.owner_user_id not in peer_ids:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Lista não encontrada")
    return _to_detail(row, user.id)


@router.patch("/{list_id}", response_model=ShoppingListDetailResponse)
def patch_shopping_list(
    list_id: uuid.UUID,
    body: ShoppingListPatch,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> ShoppingListDetailResponse:
    row = _owned_list(db, list_id, user.id)
    if row is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Lista não encontrada")
    if row.status != STATUS_DRAFT:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Só é possível editar listas em rascunho",
        )
    data = body.model_dump(exclude_unset=True)
    for k, v in data.items():
        setattr(row, k, v)
    db.commit()
    row = db.scalar(
        select(ShoppingList)
        .options(selectinload(ShoppingList.items))
        .where(ShoppingList.id == list_id)
    )
    assert row is not None
    return _to_detail(row, user.id)


@router.delete("/{list_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_shopping_list(
    list_id: uuid.UUID,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> None:
    row = _owned_list(db, list_id, user.id)
    if row is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Lista não encontrada")
    if row.status != STATUS_DRAFT:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Só é possível apagar listas em rascunho",
        )
    db.delete(row)
    db.commit()


@router.post(
    "/{list_id}/items",
    response_model=ShoppingListDetailResponse,
    status_code=status.HTTP_201_CREATED,
)
def add_shopping_list_item(
    list_id: uuid.UUID,
    body: ShoppingListItemCreate,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> ShoppingListDetailResponse:
    row = _owned_list(db, list_id, user.id)
    if row is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Lista não encontrada")
    if row.status != STATUS_DRAFT:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Só é possível editar listas em rascunho",
        )
    max_ord = db.scalar(
        select(func.max(ShoppingListItem.sort_order)).where(ShoppingListItem.list_id == list_id)
    )
    next_ord = (int(max_ord) + 1) if max_ord is not None else 0
    item = ShoppingListItem(
        list_id=list_id,
        sort_order=next_ord,
        label=body.label,
        line_amount_cents=body.line_amount_cents,
    )
    db.add(item)
    db.commit()
    row = db.scalar(
        select(ShoppingList)
        .options(selectinload(ShoppingList.items))
        .where(ShoppingList.id == list_id)
    )
    assert row is not None
    return _to_detail(row, user.id)


@router.patch("/{list_id}/items/{item_id}", response_model=ShoppingListDetailResponse)
def patch_shopping_list_item(
    list_id: uuid.UUID,
    item_id: uuid.UUID,
    body: ShoppingListItemPatch,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> ShoppingListDetailResponse:
    row = _owned_list(db, list_id, user.id)
    if row is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Lista não encontrada")
    if row.status != STATUS_DRAFT:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Só é possível editar listas em rascunho",
        )
    item = db.scalar(
        select(ShoppingListItem).where(
            ShoppingListItem.id == item_id,
            ShoppingListItem.list_id == list_id,
        )
    )
    if item is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Item não encontrado")
    data = body.model_dump(exclude_unset=True)
    for k, v in data.items():
        setattr(item, k, v)
    db.commit()
    row = db.scalar(
        select(ShoppingList)
        .options(selectinload(ShoppingList.items))
        .where(ShoppingList.id == list_id)
    )
    assert row is not None
    return _to_detail(row, user.id)


@router.delete("/{list_id}/items/{item_id}", response_model=ShoppingListDetailResponse)
def delete_shopping_list_item(
    list_id: uuid.UUID,
    item_id: uuid.UUID,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> ShoppingListDetailResponse:
    row = _owned_list(db, list_id, user.id)
    if row is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Lista não encontrada")
    if row.status != STATUS_DRAFT:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Só é possível editar listas em rascunho",
        )
    item = db.scalar(
        select(ShoppingListItem).where(
            ShoppingListItem.id == item_id,
            ShoppingListItem.list_id == list_id,
        )
    )
    if item is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Item não encontrado")
    db.delete(item)
    db.commit()
    row = db.scalar(
        select(ShoppingList)
        .options(selectinload(ShoppingList.items))
        .where(ShoppingList.id == list_id)
    )
    assert row is not None
    return _to_detail(row, user.id)


@router.post("/{list_id}/complete", response_model=ShoppingListDetailResponse)
def complete_shopping_list(
    list_id: uuid.UUID,
    body: ShoppingListComplete,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> ShoppingListDetailResponse:
    row = _owned_list(db, list_id, user.id)
    if row is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Lista não encontrada")
    if row.status != STATUS_DRAFT:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Esta lista já foi concluída",
        )
    row = db.scalar(
        select(ShoppingList)
        .options(selectinload(ShoppingList.items))
        .where(ShoppingList.id == list_id)
    )
    assert row is not None
    if not row.items:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Adicione pelo menos um item antes de fechar a compra",
        )
    line_amounts = [i.line_amount_cents for i in row.items]
    try:
        total = resolve_checkout_total_cents(
            line_amounts=line_amounts,
            total_cents_override=body.total_cents,
        )
    except ValueError as e:
        code = str(e)
        if code == "shopping_list_total_empty":
            msg = "Indique valores nas linhas ou um total manual"
        else:
            msg = "Total inválido"
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=msg) from e

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

    desc = body.description
    if not desc:
        desc = _default_expense_description(row.store_name, body.expense_date)

    now = datetime.now(UTC)
    paid_at = now if body.status == ExpenseStatus.PAID else None

    expense = Expense(
        owner_user_id=user.id,
        description=desc[:500],
        amount_cents=total,
        expense_date=body.expense_date,
        due_date=None,
        status=body.status.value,
        category_id=body.category_id,
        installment_total=1,
        installment_number=1,
        installment_group_id=None,
        recurring_frequency=None,
        recurring_series_id=None,
        recurring_generated_until=None,
        is_shared=is_s,
        shared_with_user_id=sw,
        paid_at=paid_at,
    )
    db.add(expense)
    db.flush()

    row.status = STATUS_COMPLETED
    row.completed_at = now
    row.expense_id = expense.id
    row.total_cents = total

    try:
        db.commit()
    except IntegrityError:
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Dados inválidos (categoria ou referência)",
        ) from None

    row = db.scalar(
        select(ShoppingList)
        .options(selectinload(ShoppingList.items))
        .where(ShoppingList.id == list_id)
    )
    assert row is not None
    return _to_detail(row, user.id)
