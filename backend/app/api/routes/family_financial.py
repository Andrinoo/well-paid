"""Leitura de eventos financeiros da família (histórico append-only)."""

from __future__ import annotations

import uuid
from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy import and_, or_, select
from sqlalchemy.orm import Session, joinedload

from app.api.deps import get_current_user
from app.core.database import get_db
from app.models.family_financial_event import FamilyFinancialEvent
from app.models.user import User
from app.schemas.family_financial import FamilyFinancialEventOut
from app.services.family_financial_events import family_id_for_user

router = APIRouter(prefix="/family", tags=["family"])


def _user_label(u: User | None) -> str | None:
    if u is None:
        return None
    name = (u.full_name or "").strip()
    return name if name else u.email


@router.get("/financial-events", response_model=list[FamilyFinancialEventOut])
def list_family_financial_events(
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
    limit: Annotated[int, Query(ge=1, le=100)] = 50,
    before: Annotated[uuid.UUID | None, Query(description="ID do evento anterior (paginação)")] = None,
    event_type: Annotated[str | None, Query(max_length=48)] = None,
) -> list[FamilyFinancialEventOut]:
    fid = family_id_for_user(db, user.id)
    if fid is None:
        return []

    stmt = (
        select(FamilyFinancialEvent)
        .where(FamilyFinancialEvent.family_id == fid)
        .options(
            joinedload(FamilyFinancialEvent.actor),
            joinedload(FamilyFinancialEvent.counterparty),
        )
    )
    if event_type is not None:
        stmt = stmt.where(FamilyFinancialEvent.event_type == event_type)

    if before is not None:
        ref = db.get(FamilyFinancialEvent, before)
        if ref is None or ref.family_id != fid:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Cursor inválido (before)",
            )
        stmt = stmt.where(
            or_(
                FamilyFinancialEvent.created_at < ref.created_at,
                and_(
                    FamilyFinancialEvent.created_at == ref.created_at,
                    FamilyFinancialEvent.id < ref.id,
                ),
            )
        )

    stmt = stmt.order_by(
        FamilyFinancialEvent.created_at.desc(),
        FamilyFinancialEvent.id.desc(),
    ).limit(limit)
    rows = list(db.scalars(stmt).unique().all())
    return [
        FamilyFinancialEventOut(
            id=r.id,
            event_type=r.event_type,
            created_at=r.created_at,
            amount_cents=r.amount_cents,
            source_expense_id=r.source_expense_id,
            source_expense_share_id=r.source_expense_share_id,
            source_receivable_id=r.source_receivable_id,
            payload_json=r.payload_json,
            actor_user_id=r.actor_user_id,
            counterparty_user_id=r.counterparty_user_id,
            actor_label=_user_label(r.actor),
            counterparty_label=_user_label(r.counterparty),
        )
        for r in rows
    ]
