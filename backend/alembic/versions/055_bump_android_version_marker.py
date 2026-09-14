"""No-op revision to bump Android versionCode (goal thumbnails via media proxy).

Revision ID: 055
Revises: 054
Create Date: 2026-09-14
"""

from __future__ import annotations

from typing import Sequence, Union


revision: str = "055"
down_revision: Union[str, Sequence[str], None] = "054"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    pass


def downgrade() -> None:
    pass
