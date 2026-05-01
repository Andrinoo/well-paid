"""add monthly interest bps to expenses

Revision ID: 042
Revises: 041
Create Date: 2026-05-01
"""

from __future__ import annotations

from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


revision: str = "042"
down_revision: Union[str, Sequence[str], None] = "041"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.add_column(
        "expenses",
        sa.Column("monthly_interest_bps", sa.Integer(), nullable=True),
    )


def downgrade() -> None:
    op.drop_column("expenses", "monthly_interest_bps")
