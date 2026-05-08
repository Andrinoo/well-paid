"""No-op revision to bump Android versionCode (derived from Alembic head).

Revision ID: 044
Revises: 043
Create Date: 2026-05-08
"""

from __future__ import annotations

from typing import Sequence, Union


revision: str = "044"
down_revision: Union[str, Sequence[str], None] = "043"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    pass


def downgrade() -> None:
    pass
