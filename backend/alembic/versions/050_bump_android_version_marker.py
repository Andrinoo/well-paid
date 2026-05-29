"""No-op revision to bump Android versionCode (derived from Alembic head).

Revision ID: 050
Revises: 049
Create Date: 2026-05-28
"""

from __future__ import annotations

from typing import Sequence, Union


revision: str = "050"
down_revision: Union[str, Sequence[str], None] = "049"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    pass


def downgrade() -> None:
    pass
