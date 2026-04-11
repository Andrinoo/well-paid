"""IDs de utilizadores no mesmo agregado familiar (para leitura partilhada)."""

from __future__ import annotations

import uuid

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.schema_introspection import session_has_table
from app.models.family import FamilyMember


def family_peer_user_ids(db: Session, user_id: uuid.UUID) -> list[uuid.UUID]:
    """Membros da família do utilizador; se não tiver família, só ele próprio."""
    if not session_has_table(db, "family_members"):
        # Migração de família ainda não aplicada na BD — mesmo efeito que “sem família”.
        return [user_id]
    row = db.scalar(select(FamilyMember).where(FamilyMember.user_id == user_id))
    if row is None:
        return [user_id]
    rows = db.scalars(
        select(FamilyMember.user_id).where(FamilyMember.family_id == row.family_id)
    ).all()
    return list(rows)
