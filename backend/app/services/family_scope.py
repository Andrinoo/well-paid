"""Utilitários de escopo familiar: membros e visibilidade por utilizador."""

from __future__ import annotations

import uuid

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.core.schema_introspection import session_has_table
from app.models.family import FamilyMember
from app.models.user import User


def family_peer_user_ids(db: Session, user_id: uuid.UUID) -> list[uuid.UUID]:
    """Membros da família do utilizador; se não tiver família, só ele próprio.

    Este helper devolve o agregado familiar independentemente do toggle individual
    de modo família, pois também é usado em fluxos de convite/partilha.
    """
    settings = get_settings()
    if not settings.family_data_sharing_enabled:
        return [user_id]
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


def family_visibility_scope(db: Session, user: User) -> tuple[list[uuid.UUID], bool]:
    """Retorna (owner_ids_visiveis, include_family_tagged_from_peers)."""
    peer_ids = family_peer_user_ids(db, user.id)
    if not bool(user.family_mode_enabled):
        return [user.id], False
    if len(peer_ids) <= 1:
        return [user.id], False
    return peer_ids, True
