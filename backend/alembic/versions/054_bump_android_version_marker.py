"""No-op revision to bump Android versionCode (Turnstile on signup).

Revision ID: 054
Revises: 053
Create Date: 2026-09-14
"""

from __future__ import annotations

from typing import Sequence, Union


revision: str = "054"
down_revision: Union[str, Sequence[str], None] = "053"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    pass


def downgrade() -> None:
    pass
