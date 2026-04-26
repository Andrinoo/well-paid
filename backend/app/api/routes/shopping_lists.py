"""Listas de compras — Ordems §6.2.2."""

from __future__ import annotations

import uuid
from datetime import UTC, date, datetime
from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import func, select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session, selectinload

from app.api.deps import get_current_user
from app.core.config import get_settings
from app.core.database import get_db
from app.models.category import Category
from app.models.expense import Expense
from app.models.expense_share import ExpenseShare
from app.models.shopping_list import ShoppingList
from app.models.shopping_list_item import ShoppingListItem
from app.models.user import User
from app.schemas.dashboard import ExpenseStatus
from app.schemas.goal import GoalProductHit, GoalProductSearchResponse
from app.schemas.shopping_list import (
    ShoppingListComplete,
    ShoppingListCreate,
    ShoppingListDetailResponse,
    ShoppingListGroceryPriceBody,
    ShoppingListItemCreate,
    ShoppingListItemPatch,
    ShoppingListItemResponse,
    ShoppingListPatch,
    ShoppingListSummaryResponse,
)
from app.services.expense_share import ExpenseShareValidationError, normalize_expense_share
from app.services.expense_splits import (
    build_two_party_shares,
    mark_all_shares_paid_for_expense,
    replace_expense_shares,
    sync_expense_row_from_shares,
)
from app.services.family_scope import family_visibility_scope
from app.services.goal_product_search import (
    build_grocery_search_query,
    search_products_google_shopping,
)
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


def _expense_description_for_list(row: ShoppingList, expense_date: date) -> str:
    t = (row.title or "").strip()
    if t:
        return t[:500]
    return _default_expense_description(row.store_name, expense_date)


def _picked_items_for_totals(items: list[ShoppingListItem]) -> list[ShoppingListItem]:
    """Apenas itens marcados como pegos entram na soma e no fechamento."""
    return [i for i in items if bool(getattr(i, "is_picked", True))]


def _recompute_list_total_cents(
    items: list[ShoppingListItem],
    *,
    fallback_total_cents: int | None,
) -> int:
    picked = _picked_items_for_totals(items)
    line_extensions = [
        (i.line_amount_cents, int(i.quantity) if i.quantity is not None else 1)
        for i in picked
    ]
    try:
        return resolve_checkout_total_cents(
            line_extensions=line_extensions,
            total_cents_override=None,
        )
    except ValueError:
        if fallback_total_cents is not None and int(fallback_total_cents) > 0:
            return int(fallback_total_cents)
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Indique valores nas linhas para recalcular o total",
        ) from None


def _sync_completed_expense(db: Session, row: ShoppingList) -> None:
    if row.status != STATUS_COMPLETED or row.expense_id is None:
        return
    row = db.scalar(
        select(ShoppingList)
        .options(selectinload(ShoppingList.items))
        .where(ShoppingList.id == row.id)
    )
    if row is None or row.expense_id is None:
        return
    items = sorted(row.items, key=lambda x: x.sort_order)
    total = _recompute_list_total_cents(items, fallback_total_cents=row.total_cents)
    row.total_cents = total
    exp = db.get(Expense, row.expense_id)
    if exp is None:
        return
    exp.amount_cents = total
    exp.description = _expense_description_for_list(row, exp.expense_date)[:500]
    db.add(exp)
    db.add(row)


def _to_item(row: ShoppingListItem) -> ShoppingListItemResponse:
    return ShoppingListItemResponse(
        id=row.id,
        sort_order=row.sort_order,
        label=row.label,
        quantity=int(row.quantity) if getattr(row, "quantity", None) is not None else 1,
        line_amount_cents=int(row.line_amount_cents)
        if row.line_amount_cents is not None
        else None,
        is_picked=bool(getattr(row, "is_picked", True)),
    )


def _to_detail(row: ShoppingList, viewer_id: uuid.UUID) -> ShoppingListDetailResponse:
    items = sorted(row.items, key=lambda x: x.sort_order)
    return ShoppingListDetailResponse(
        id=row.id,
        owner_user_id=row.owner_user_id,
        is_mine=row.owner_user_id == viewer_id,
        is_family=bool(row.is_family),
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
        is_family=bool(row.is_family),
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
    owner_ids, include_family = family_visibility_scope(db, user)
    visibility_clause = (
        ShoppingList.owner_user_id.in_(owner_ids)
        if not include_family
        else (
            (ShoppingList.owner_user_id == user.id)
            | ((ShoppingList.owner_user_id.in_(owner_ids)) & (ShoppingList.is_family.is_(True)))
        )
    )
    rows = db.scalars(
        select(ShoppingList)
        .where(visibility_clause)
        .order_by(ShoppingList.updated_at.desc())
    ).all()
    counts = _item_counts(db, [r.id for r in rows])
    return [_to_summary(r, user.id, counts.get(r.id, 0)) for r in rows]


@router.post("/price-suggestions", response_model=GoalProductSearchResponse)
def grocery_price_suggestions(
    body: ShoppingListGroceryPriceBody,
    user: Annotated[User, Depends(get_current_user)],
) -> GoalProductSearchResponse:
    """Sugestões de preço para mercearia (Google Shopping via SerpAPI). Usado ao adicionar item à lista."""
    _ = user
    settings = get_settings()
    if not (settings.serpapi_key or "").strip():
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Sugestões de preço indisponíveis: configure SERPAPI_KEY no servidor.",
        )
    q = build_grocery_search_query(
        body.query.strip(),
        body.unit,
        settings.grocery_search_location_hint,
    )
    rows = search_products_google_shopping(
        q,
        serpapi_key=settings.serpapi_key,
        serp_limit=12,
        serp_timeout_s=10.0,
        max_total=24,
    )
    return GoalProductSearchResponse(
        results=[GoalProductHit.model_validate(r) for r in rows],
    )


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
        is_family=bool(body.is_family),
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
    owner_ids, include_family = family_visibility_scope(db, user)
    if row.owner_user_id == user.id:
        return _to_detail(row, user.id)
    if row.owner_user_id not in owner_ids or not include_family or not bool(row.is_family):
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
    if row.status == STATUS_DRAFT:
        data = body.model_dump(exclude_unset=True)
        data.pop("sync_total_from_line_items", None)
        for k, v in data.items():
            setattr(row, k, v)
        db.commit()
    elif row.status == STATUS_COMPLETED:
        data = body.model_dump(exclude_unset=True)
        sync_flag = data.pop("sync_total_from_line_items", None) is True
        if not sync_flag and not data:
            pass
        else:
            for k, v in data.items():
                setattr(row, k, v)
            if data:
                db.flush()
            _sync_completed_expense(db, row)
            db.commit()
    else:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Estado de lista inválido",
        )
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
    if row.status not in (STATUS_DRAFT, STATUS_COMPLETED):
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Lista não pode ser editada",
        )
    max_ord = db.scalar(
        select(func.max(ShoppingListItem.sort_order)).where(ShoppingListItem.list_id == list_id)
    )
    next_ord = (int(max_ord) + 1) if max_ord is not None else 0
    item = ShoppingListItem(
        list_id=list_id,
        sort_order=next_ord,
        label=body.label,
        quantity=body.quantity,
        line_amount_cents=body.line_amount_cents,
        is_picked=body.is_picked,
    )
    db.add(item)
    db.flush()
    if row.status == STATUS_COMPLETED:
        _sync_completed_expense(db, row)
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
    if row.status not in (STATUS_DRAFT, STATUS_COMPLETED):
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Lista não pode ser editada",
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
    db.flush()
    if row.status == STATUS_COMPLETED:
        _sync_completed_expense(db, row)
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
    if row.status not in (STATUS_DRAFT, STATUS_COMPLETED):
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Lista não pode ser editada",
        )
    row = db.scalar(
        select(ShoppingList)
        .options(selectinload(ShoppingList.items))
        .where(ShoppingList.id == list_id)
    )
    if row is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Lista não encontrada")
    if len(row.items) <= 1:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="A lista deve manter pelo menos um item",
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
    db.flush()
    if row.status == STATUS_COMPLETED:
        _sync_completed_expense(db, row)
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
    picked_items = _picked_items_for_totals(list(row.items))
    if not picked_items:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Marque pelo menos um item como pego antes de fechar a compra.",
        )
    line_extensions = [
        (i.line_amount_cents, int(i.quantity) if i.quantity is not None else 1)
        for i in picked_items
    ]
    try:
        total = resolve_checkout_total_cents(
            line_extensions=line_extensions,
            total_cents_override=body.total_cents,
            discount_cents=body.discount_cents,
        )
    except ValueError as e:
        code = str(e)
        if code == "shopping_list_total_empty":
            msg = "Indique valores nas linhas, um total manual ou um desconto compatível com a soma"
        elif code == "shopping_list_discount_invalid":
            msg = "Desconto inválido"
        elif code == "shopping_list_total_after_discount_invalid":
            msg = "Com este desconto o total seria zero ou negativo; ajusta o desconto ou as linhas"
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
    if is_s and sw is None:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Indica o membro da família com quem partilhas a despesa.",
        )

    desc = _expense_description_for_list(row, body.expense_date)
    disc = int(body.discount_cents or 0)
    if disc > 0:
        brl = f"{disc // 100},{disc % 100:02d}"
        extra = f" · Desconto R$ {brl}"
        desc = f"{desc}{extra}"[:500]

    now = datetime.now(UTC)
    # Listas de compras: despesa sempre paga, sem parcelas/recorrência (Ordems §6.2.2).
    paid_at = now

    expense = Expense(
        owner_user_id=user.id,
        description=desc[:500],
        amount_cents=total,
        expense_date=body.expense_date,
        due_date=None,
        status=ExpenseStatus.PAID.value,
        category_id=body.category_id,
        installment_total=1,
        installment_number=1,
        installment_group_id=None,
        recurring_frequency=None,
        recurring_series_id=None,
        recurring_generated_until=None,
        is_shared=is_s,
        is_family=bool(body.is_family),
        shared_with_user_id=sw,
        paid_at=paid_at,
    )
    db.add(expense)
    db.flush()
    if is_s and sw is not None:
        share_rows = build_two_party_shares(
            owner_id=user.id,
            peer_id=sw,
            amount_cents=total,
            split_mode="percent",
            owner_share_cents=None,
            peer_share_cents=None,
            owner_percent_bps=5000,
            peer_percent_bps=5000,
        )
        replace_expense_shares(db, expense.id, share_rows)
        shs = mark_all_shares_paid_for_expense(db, expense.id, now=now)
        sync_expense_row_from_shares(expense, shs, now=now)

    row.status = STATUS_COMPLETED
    row.is_family = bool(body.is_family)
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
