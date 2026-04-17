"""Emissão de eventos append-only para histórico financeiro entre membros da família."""

from __future__ import annotations

import uuid
from typing import Any

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models.family import FamilyMember
from app.models.family_financial_event import FamilyFinancialEvent

EVENT_PEER_DECLINED_SHARE = "peer_declined_share"
EVENT_OWNER_ASSUMED_EXPENSE_LINE = "owner_assumed_expense_line"
EVENT_COVER_REQUESTED = "cover_requested"
EVENT_RECEIVABLE_SETTLED = "receivable_settled"
EVENT_RECEIVABLE_CANCELLED = "receivable_cancelled"


def family_id_for_user(db: Session, user_id: uuid.UUID) -> uuid.UUID | None:
    return db.scalar(select(FamilyMember.family_id).where(FamilyMember.user_id == user_id))


def _emit(
    db: Session,
    *,
    family_id: uuid.UUID,
    event_type: str,
    actor_user_id: uuid.UUID,
    counterparty_user_id: uuid.UUID | None,
    amount_cents: int | None,
    source_expense_id: uuid.UUID | None,
    source_expense_share_id: uuid.UUID | None,
    source_receivable_id: uuid.UUID | None,
    payload_json: dict[str, Any] | None,
) -> None:
    db.add(
        FamilyFinancialEvent(
            family_id=family_id,
            event_type=event_type,
            actor_user_id=actor_user_id,
            counterparty_user_id=counterparty_user_id,
            amount_cents=amount_cents,
            source_expense_id=source_expense_id,
            source_expense_share_id=source_expense_share_id,
            source_receivable_id=source_receivable_id,
            payload_json=payload_json,
        )
    )


def record_peer_declined_share(
    db: Session,
    *,
    peer_user_id: uuid.UUID,
    owner_user_id: uuid.UUID,
    expense_id: uuid.UUID,
    expense_share_id: uuid.UUID,
    share_amount_cents: int,
    decline_reason: str | None,
) -> None:
    fid = family_id_for_user(db, peer_user_id)
    if fid is None:
        return
    payload: dict[str, Any] = {}
    if decline_reason:
        payload["decline_reason"] = decline_reason
    _emit(
        db,
        family_id=fid,
        event_type=EVENT_PEER_DECLINED_SHARE,
        actor_user_id=peer_user_id,
        counterparty_user_id=owner_user_id,
        amount_cents=share_amount_cents,
        source_expense_id=expense_id,
        source_expense_share_id=expense_share_id,
        source_receivable_id=None,
        payload_json=payload or None,
    )


def record_receivable_cancelled(
    db: Session,
    *,
    debtor_user_id: uuid.UUID,
    creditor_user_id: uuid.UUID,
    receivable_id: uuid.UUID,
    source_expense_id: uuid.UUID | None,
    amount_cents: int,
    reason: str | None = None,
) -> None:
    fid = family_id_for_user(db, debtor_user_id)
    if fid is None:
        return
    payload: dict[str, Any] = {}
    if reason:
        payload["cancel_reason"] = reason
    _emit(
        db,
        family_id=fid,
        event_type=EVENT_RECEIVABLE_CANCELLED,
        actor_user_id=debtor_user_id,
        counterparty_user_id=creditor_user_id,
        amount_cents=amount_cents,
        source_expense_id=source_expense_id,
        source_expense_share_id=None,
        source_receivable_id=receivable_id,
        payload_json=payload or None,
    )


def record_owner_assumed_expense_line(
    db: Session,
    *,
    owner_user_id: uuid.UUID,
    peer_user_id: uuid.UUID,
    expense_id: uuid.UUID,
    amount_cents: int,
    installment_number: int | None,
    installment_group_id: uuid.UUID | None,
) -> None:
    fid = family_id_for_user(db, owner_user_id)
    if fid is None:
        return
    payload: dict[str, Any] = {}
    if installment_number is not None:
        payload["installment_number"] = installment_number
    if installment_group_id is not None:
        payload["installment_group_id"] = str(installment_group_id)
    _emit(
        db,
        family_id=fid,
        event_type=EVENT_OWNER_ASSUMED_EXPENSE_LINE,
        actor_user_id=owner_user_id,
        counterparty_user_id=peer_user_id,
        amount_cents=amount_cents,
        source_expense_id=expense_id,
        source_expense_share_id=None,
        source_receivable_id=None,
        payload_json=payload or None,
    )


def record_cover_requested(
    db: Session,
    *,
    debtor_user_id: uuid.UUID,
    creditor_user_id: uuid.UUID,
    expense_id: uuid.UUID,
    expense_share_id: uuid.UUID,
    receivable_id: uuid.UUID,
    amount_cents: int,
) -> None:
    fid = family_id_for_user(db, debtor_user_id)
    if fid is None:
        return
    _emit(
        db,
        family_id=fid,
        event_type=EVENT_COVER_REQUESTED,
        actor_user_id=debtor_user_id,
        counterparty_user_id=creditor_user_id,
        amount_cents=amount_cents,
        source_expense_id=expense_id,
        source_expense_share_id=expense_share_id,
        source_receivable_id=receivable_id,
        payload_json=None,
    )


def record_receivable_settled(
    db: Session,
    *,
    creditor_user_id: uuid.UUID,
    debtor_user_id: uuid.UUID,
    receivable_id: uuid.UUID,
    source_expense_id: uuid.UUID | None,
    amount_cents: int,
) -> None:
    fid = family_id_for_user(db, creditor_user_id)
    if fid is None:
        return
    _emit(
        db,
        family_id=fid,
        event_type=EVENT_RECEIVABLE_SETTLED,
        actor_user_id=creditor_user_id,
        counterparty_user_id=debtor_user_id,
        amount_cents=amount_cents,
        source_expense_id=source_expense_id,
        source_expense_share_id=None,
        source_receivable_id=receivable_id,
        payload_json=None,
    )
