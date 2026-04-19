from __future__ import annotations

import uuid
from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.api.deps import get_current_user
from app.core.database import get_db
from app.models.goal import Goal
from app.models.goal_contribution import GoalContribution
from app.models.user import User
from app.schemas.goal import (
    GoalContribute,
    GoalContributionResponse,
    GoalCreate,
    GoalPreviewFromUrlBody,
    GoalPreviewFromUrlResponse,
    GoalProductHit,
    GoalProductSearchBody,
    GoalProductSearchResponse,
    GoalResponse,
    GoalUpdate,
)
from app.services.family_scope import family_peer_user_ids
from app.services.goal_product_search import search_mercadolibre
from app.services.goal_reference_price import fetch_product_hints

router = APIRouter(prefix="/goals", tags=["goals"])


def _owned_goal(db: Session, goal_id: uuid.UUID, owner_id: uuid.UUID) -> Goal | None:
    return db.query(Goal).filter(Goal.id == goal_id, Goal.owner_user_id == owner_id).first()


def _visible_goal(
    db: Session, goal_id: uuid.UUID, viewer_id: uuid.UUID
) -> Goal | None:
    peer_ids = family_peer_user_ids(db, viewer_id)
    return (
        db.query(Goal)
        .filter(Goal.id == goal_id, Goal.owner_user_id.in_(peer_ids))
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
        price_alternatives=alts,
    )


@router.get("", response_model=list[GoalResponse])
def list_goals(
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> list[GoalResponse]:
    peer_ids = family_peer_user_ids(db, user.id)
    rows = (
        db.query(Goal)
        .filter(Goal.owner_user_id.in_(peer_ids))
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
        target_url=(body.target_url.strip() if body.target_url else None),
        reference_product_name=body.reference_product_name,
        reference_price_cents=body.reference_price_cents,
        reference_currency=body.reference_currency or "BRL",
        price_source=body.price_source,
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
    """Pesquisa por nome (Mercado Livre API pública)."""
    _ = user
    site = (body.site_id or "MLB").strip() or "MLB"
    rows = search_mercadolibre(body.query.strip(), site_id=site)
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


@router.post("/{goal_id}/refresh-reference-price", response_model=GoalResponse)
def refresh_goal_reference_price(
    goal_id: uuid.UUID,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> GoalResponse:
    row = _owned_goal(db, goal_id, user.id)
    if row is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Meta não encontrada")
    if not row.target_url:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Defina um link (URL) na meta para actualizar o preço de referência.",
        )
    hints = fetch_product_hints(row.target_url.strip())
    if hints.get("reference_product_name"):
        row.reference_product_name = hints["reference_product_name"]
    if hints.get("reference_price_cents") is not None:
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
    db.commit()
    db.refresh(row)
    return _to_response(row, user.id)


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
    for k, v in data.items():
        setattr(row, k, v)
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
