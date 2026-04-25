from __future__ import annotations

import uuid
from datetime import UTC, datetime, timedelta
from typing import Annotated, Any

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.api.deps import get_current_user
from app.core.config import get_settings
from app.core.database import get_db
from app.models.goal import Goal
from app.models.goal_contribution import GoalContribution
from app.models.goal_price_history import GoalPriceHistory
from app.models.user import User
from app.schemas.goal import (
    GoalContribute,
    GoalContributionResponse,
    GoalCreate,
    GoalPriceHistoryItem,
    GoalPriceHistoryResponse,
    GoalPreviewFromUrlBody,
    GoalPreviewFromUrlResponse,
    GoalProductHit,
    GoalProductSearchBody,
    GoalProductSearchResponse,
    GoalResponse,
    GoalUpdate,
)
from app.services.family_scope import family_visibility_scope
from app.services.goal_product_search import search_products_google_shopping
from app.services.goal_reference_price import fetch_product_hints

router = APIRouter(prefix="/goals", tags=["goals"])


def _default_due_at() -> datetime:
    return datetime.now(UTC) + timedelta(days=90)


def _record_goal_price_history(
    db: Session,
    row: Goal,
    *,
    capture_type: str,
    observed_price_cents: int | None = None,
    observed_title: str | None = None,
    observed_url: str | None = None,
    observed_source: str | None = None,
) -> None:
    price_cents = (
        int(observed_price_cents)
        if observed_price_cents is not None
        else int(row.reference_price_cents or 0)
    )
    if price_cents <= 0:
        return
    db.add(
        GoalPriceHistory(
            goal_id=row.id,
            price_cents=price_cents,
            currency=(row.reference_currency or "BRL")[:8],
            source=(observed_source or row.price_source or "unavailable")[:64],
            observed_url=(observed_url or row.target_url),
            observed_title=(observed_title or row.reference_product_name or row.title)[:500],
            capture_type=capture_type[:24],
        )
    )


def _owned_goal(db: Session, goal_id: uuid.UUID, owner_id: uuid.UUID) -> Goal | None:
    return db.query(Goal).filter(Goal.id == goal_id, Goal.owner_user_id == owner_id).first()


def _visible_goal(
    db: Session, goal_id: uuid.UUID, viewer_id: uuid.UUID
) -> Goal | None:
    viewer = db.get(User, viewer_id)
    if viewer is None:
        return None
    owner_ids, include_family = family_visibility_scope(db, viewer)
    visibility_clause = (
        Goal.owner_user_id.in_(owner_ids)
        if not include_family
        else (
            (Goal.owner_user_id == viewer_id)
            | ((Goal.owner_user_id.in_(owner_ids)) & (Goal.is_family.is_(True)))
        )
    )
    return (
        db.query(Goal)
        .filter(Goal.id == goal_id, visibility_clause)
        .first()
    )


def _to_response(row: Goal, viewer_id: uuid.UUID) -> GoalResponse:
    alts = row.price_alternatives if isinstance(row.price_alternatives, list) else []
    return GoalResponse(
        id=row.id,
        owner_user_id=row.owner_user_id,
        is_mine=row.owner_user_id == viewer_id,
        title=row.title,
        target_cents=int(row.target_cents),
        current_cents=int(row.current_cents),
        is_active=row.is_active,
        is_family=bool(row.is_family),
        created_at=row.created_at,
        updated_at=row.updated_at,
        target_url=row.target_url,
        reference_product_name=row.reference_product_name,
        reference_price_cents=int(row.reference_price_cents)
        if row.reference_price_cents is not None
        else None,
        reference_currency=row.reference_currency or "BRL",
        price_checked_at=row.price_checked_at,
        price_source=row.price_source,
        reference_thumbnail_url=row.reference_thumbnail_url,
        description=row.description,
        due_at=row.due_at,
        price_check_interval_hours=int(row.price_check_interval_hours or 12),
        last_price_track_at=row.last_price_track_at,
        price_alternatives=alts,
    )


@router.get("", response_model=list[GoalResponse])
def list_goals(
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> list[GoalResponse]:
    owner_ids, include_family = family_visibility_scope(db, user)
    visibility_clause = (
        Goal.owner_user_id.in_(owner_ids)
        if not include_family
        else (
            (Goal.owner_user_id == user.id)
            | ((Goal.owner_user_id.in_(owner_ids)) & (Goal.is_family.is_(True)))
        )
    )
    rows = (
        db.query(Goal)
        .filter(visibility_clause)
        .order_by(Goal.is_active.desc(), Goal.updated_at.desc())
        .all()
    )
    return [_to_response(r, user.id) for r in rows]


@router.post("", response_model=GoalResponse, status_code=status.HTTP_201_CREATED)
def create_goal(
    body: GoalCreate,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> GoalResponse:
    row = Goal(
        owner_user_id=user.id,
        title=body.title.strip(),
        target_cents=body.target_cents,
        current_cents=body.current_cents,
        is_active=body.is_active,
        is_family=bool(body.is_family),
        target_url=(body.target_url.strip() if body.target_url else None),
        reference_product_name=body.reference_product_name,
        reference_price_cents=body.reference_price_cents,
        reference_currency=body.reference_currency or "BRL",
        price_source=body.price_source,
        description=(body.description.strip() if body.description else None),
        due_at=(body.due_at or _default_due_at()),
        price_check_interval_hours=int(body.price_check_interval_hours or 12),
        reference_thumbnail_url=(
            body.reference_thumbnail_url.strip()
            if body.reference_thumbnail_url
            else None
        ),
    )
    db.add(row)
    db.flush()
    if int(body.current_cents) > 0:
        db.add(
            GoalContribution(
                goal_id=row.id,
                amount_cents=int(body.current_cents),
                note=None,
            )
        )
    _record_goal_price_history(db, row, capture_type="manual")
    db.commit()
    db.refresh(row)
    return _to_response(row, user.id)


@router.post("/preview-from-url", response_model=GoalPreviewFromUrlResponse)
def preview_goal_from_url(
    body: GoalPreviewFromUrlBody,
    user: Annotated[User, Depends(get_current_user)],
) -> GoalPreviewFromUrlResponse:
    """Pré-visualiza nome/preço a partir de um URL (sem gravar meta)."""
    _ = user
    hints = fetch_product_hints(body.url.strip())
    pc = hints.get("reference_price_cents")
    pc_i = int(pc) if pc is not None else None
    return GoalPreviewFromUrlResponse(
        reference_product_name=hints.get("reference_product_name"),
        reference_price_cents=pc_i,
        suggested_target_cents=pc_i,
        reference_currency=str(hints.get("reference_currency") or "BRL")[:8],
        price_source=str(hints.get("price_source") or "unavailable"),
    )


@router.post("/product-search", response_model=GoalProductSearchResponse)
def search_goal_products(
    body: GoalProductSearchBody,
    user: Annotated[User, Depends(get_current_user)],
) -> GoalProductSearchResponse:
    """Pesquisa por nome: Google Shopping via SerpAPI (SERPAPI_KEY obrigatória no servidor)."""
    _ = user
    q = body.query.strip()
    settings = get_settings()
    if not (settings.serpapi_key or "").strip():
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Pesquisa de preços indisponível: o servidor não tem SERPAPI_KEY configurada.",
        )
    rows = search_products_google_shopping(
        q,
        serpapi_key=settings.serpapi_key,
        serp_limit=12,
        serp_timeout_s=12.0,
        max_total=25,
    )
    return GoalProductSearchResponse(
        results=[GoalProductHit.model_validate(r) for r in rows],
    )


@router.get("/{goal_id}/contributions", response_model=list[GoalContributionResponse])
def list_goal_contributions(
    goal_id: uuid.UUID,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
    limit: int = 200,
) -> list[GoalContributionResponse]:
    row = _owned_goal(db, goal_id, user.id)
    if row is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Meta não encontrada")
    lim = min(max(limit, 1), 500)
    q = (
        db.query(GoalContribution)
        .filter(GoalContribution.goal_id == goal_id)
        .order_by(GoalContribution.recorded_at.desc())
        .limit(lim)
    )
    return [
        GoalContributionResponse(
            id=c.id,
            goal_id=c.goal_id,
            amount_cents=int(c.amount_cents),
            note=c.note,
            recorded_at=c.recorded_at,
        )
        for c in q.all()
    ]


@router.post("/{goal_id}/contribute", response_model=GoalResponse)
def contribute_goal(
    goal_id: uuid.UUID,
    body: GoalContribute,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> GoalResponse:
    row = _owned_goal(db, goal_id, user.id)
    if row is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Meta não encontrada")
    note = body.note.strip() if body.note else None
    if note == "":
        note = None
    db.add(
        GoalContribution(
            goal_id=row.id,
            amount_cents=int(body.amount_cents),
            note=note,
        )
    )
    row.current_cents = int(row.current_cents) + int(body.amount_cents)
    db.commit()
    db.refresh(row)
    return _to_response(row, user.id)


def _hints_from_title_search(title: str, settings) -> dict[str, Any]:
    """Quando não há URL: usa o título na pesquisa Google Shopping (SerpAPI)."""
    q = (title or "").strip()
    if len(q) < 2:
        return {}
    rows = search_products_google_shopping(
        q,
        serpapi_key=settings.serpapi_key,
        serp_limit=12,
        serp_timeout_s=12.0,
        max_total=25,
    )
    if not rows:
        return {}
    first = rows[0]
    pc = int(first.get("price_cents") or 0)
    if pc <= 0:
        return {}
    alts: list[dict[str, Any]] = []
    for r in rows[1:8]:
        try:
            ac = int(r.get("price_cents") or 0)
        except (TypeError, ValueError):
            ac = 0
        if ac <= 0:
            continue
        alts.append(
            {
                "label": str(r.get("title") or "")[:500],
                "price_cents": ac,
                "url": r.get("url"),
            }
        )
    thumb = first.get("thumbnail")
    thumb_s = str(thumb).strip()[:2048] if thumb else None
    return {
        "reference_product_name": str(first.get("title") or "")[:500] or None,
        "reference_price_cents": pc,
        "reference_currency": str(first.get("currency_id") or "BRL")[:8],
        "price_checked_at": datetime.now(UTC),
        "price_source": str(first.get("source") or "google_shopping"),
        "price_alternatives": alts,
        "_listing_url": str(first.get("url") or "").strip() or None,
        "reference_thumbnail_url": thumb_s or None,
    }


@router.post("/{goal_id}/refresh-reference-price", response_model=GoalResponse)
def refresh_goal_reference_price(
    goal_id: uuid.UUID,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> GoalResponse:
    row = _owned_goal(db, goal_id, user.id)
    if row is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Meta não encontrada")
    settings = get_settings()
    hints: dict[str, Any]
    if row.target_url and str(row.target_url).strip():
        hints = fetch_product_hints(row.target_url.strip())
    else:
        if not (settings.serpapi_key or "").strip():
            raise HTTPException(
                status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                detail=(
                    "Actualização por título indisponível: configure SERPAPI_KEY no servidor, "
                    "ou defina um link de produto na meta."
                ),
            )
        hints = _hints_from_title_search(row.title or "", settings)
        if not hints:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=(
                    "Defina um link de produto na meta ou use um título com pelo menos 2 caracteres "
                    "para pesquisar o preço de referência (Google Shopping)."
                ),
            )
        listing = hints.pop("_listing_url", None)
        if listing:
            row.target_url = listing
    previous_price = int(row.reference_price_cents or 0)
    if hints.get("reference_product_name"):
        row.reference_product_name = hints["reference_product_name"]
    if hints.get("reference_price_cents") is not None:
        previous_price = int(row.reference_price_cents or 0)
        pc = int(hints["reference_price_cents"])
        row.reference_price_cents = pc
        row.target_cents = pc
        if int(row.current_cents) > pc:
            row.current_cents = pc
    if hints.get("reference_currency"):
        row.reference_currency = str(hints["reference_currency"])[:8]
    if hints.get("price_checked_at"):
        row.price_checked_at = hints["price_checked_at"]
    src = hints.get("price_source")
    row.price_source = str(src) if src else "unavailable"
    row.price_alternatives = list(hints.get("price_alternatives") or [])
    if hints.get("reference_thumbnail_url"):
        row.reference_thumbnail_url = str(hints["reference_thumbnail_url"])[:2048]
    row.last_price_track_at = datetime.now(UTC)
    _record_goal_price_history(
        db,
        row,
        capture_type="manual",
        observed_price_cents=int(row.reference_price_cents or 0),
        observed_title=hints.get("reference_product_name"),
        observed_url=row.target_url,
        observed_source=row.price_source,
    )
    db.commit()
    db.refresh(row)
    return _to_response(row, user.id)


@router.get("/{goal_id}/price-history", response_model=GoalPriceHistoryResponse)
def list_goal_price_history(
    goal_id: uuid.UUID,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
    limit: int = 120,
) -> GoalPriceHistoryResponse:
    row = _visible_goal(db, goal_id, user.id)
    if row is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Meta não encontrada")
    lim = min(max(limit, 1), 365)
    history_rows = (
        db.query(GoalPriceHistory)
        .filter(GoalPriceHistory.goal_id == goal_id)
        .order_by(GoalPriceHistory.recorded_at.desc())
        .limit(lim)
        .all()
    )
    items = [
        GoalPriceHistoryItem.model_validate(h)
        for h in reversed(history_rows)
    ]
    return GoalPriceHistoryResponse(goal_id=goal_id, items=items)


@router.get("/{goal_id}", response_model=GoalResponse)
def get_goal(
    goal_id: uuid.UUID,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> GoalResponse:
    row = _visible_goal(db, goal_id, user.id)
    if row is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Meta não encontrada")
    return _to_response(row, user.id)


@router.put("/{goal_id}", response_model=GoalResponse)
def update_goal(
    goal_id: uuid.UUID,
    body: GoalUpdate,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> GoalResponse:
    row = _owned_goal(db, goal_id, user.id)
    if row is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Meta não encontrada")
    data = body.model_dump(exclude_unset=True)
    if "title" in data and data["title"] is not None:
        data["title"] = data["title"].strip()
    if "description" in data and isinstance(data["description"], str):
        data["description"] = data["description"].strip() or None
    if "price_check_interval_hours" in data and data["price_check_interval_hours"] is not None:
        data["price_check_interval_hours"] = int(data["price_check_interval_hours"])
    for k, v in data.items():
        setattr(row, k, v)
    if "reference_price_cents" in data and data.get("reference_price_cents") is not None:
        _record_goal_price_history(
            db,
            row,
            capture_type="manual",
            observed_price_cents=int(data["reference_price_cents"]),
            observed_title=row.reference_product_name,
            observed_url=row.target_url,
            observed_source=row.price_source,
        )
    db.commit()
    db.refresh(row)
    return _to_response(row, user.id)


@router.delete("/{goal_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_goal(
    goal_id: uuid.UUID,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> None:
    row = _owned_goal(db, goal_id, user.id)
    if row is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Meta não encontrada")
    if int(row.current_cents) > 0:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail=(
                "Não é possível apagar uma meta com saldo registado. "
                "Desative a meta (arquivar) em editar ou retire o valor antes de apagar."
            ),
        )
    db.delete(row)
    db.commit()
