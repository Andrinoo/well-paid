"""Reflexão de esquema na ligação já associada à sessão.

Evita ``inspect(engine)`` / ``inspect(bind)`` com [Engine], que no SQLAlchemy 2
pode fazer ``connect()`` só para o Inspector e esgota o QueuePool sob carga
paralela (ex.: vários GET em simultâneo no warmup do cliente).
"""

from __future__ import annotations

from sqlalchemy import inspect
from sqlalchemy.orm import Session


def session_has_table(session: Session, table_name: str) -> bool:
    return inspect(session.connection()).has_table(table_name)
