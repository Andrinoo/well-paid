"""expense_shares declined_at, decline_reason

Revision ID: 026
Revises: 025
Create Date: 2026-04-16
"""

from __future__ import annotations

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op

revision: str = "026"
down_revision: Union[str, Sequence[str], None] = "025"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.add_column(
        "expense_shares",
        sa.Column("declined_at", sa.DateTime(timezone=True), nullable=True),
    )
    op.add_column(
        "expense_shares",
        sa.Column("decline_reason", sa.String(length=500), nullable=True),
    )


def downgrade() -> None:
    op.drop_column("expense_shares", "decline_reason")
    op.drop_column("expense_shares", "declined_at")
