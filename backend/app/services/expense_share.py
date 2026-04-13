"""Regras de partilha de despesas na família (Ordems §4.5)."""

from __future__ import annotations

import uuid

from sqlalchemy.orm import Session

from app.services.family_scope import family_peer_user_ids


class ExpenseShareValidationError(ValueError):
    pass


def normalize_expense_share(
    db: Session,
    owner_id: uuid.UUID,
    is_shared: bool,
    shared_with_user_id: uuid.UUID | None,
) -> tuple[bool, uuid.UUID | None]:
    if not is_shared:
        if shared_with_user_id is not None:
            raise ExpenseShareValidationError(
                "shared_with_user_id só é permitido quando is_shared é true"
            )
        return False, None
    peers = family_peer_user_ids(db, owner_id)
    if len(peers) < 2:
        raise ExpenseShareValidationError(
            "Partilha requer família com pelo menos dois membros"
        )
    if shared_with_user_id is None:
        return True, None
    if shared_with_user_id == owner_id:
        raise ExpenseShareValidationError(
            "shared_with_user_id deve ser outro membro da família"
        )
    if shared_with_user_id not in peers:
        raise ExpenseShareValidationError(
            "shared_with_user_id deve pertencer à tua família"
        )
    return True, shared_with_user_id
