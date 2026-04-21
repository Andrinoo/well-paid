"""emergency reserve plan details and target ceiling

Revision ID: 031
Revises: 030
Create Date: 2026-04-21
"""

from __future__ import annotations

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op

revision: str = "031"
down_revision: Union[str, Sequence[str], None] = "030"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.add_column(
        "emergency_reserve_plans",
        sa.Column("details", sa.String(length=1200), nullable=True),
    )
    op.add_column(
        "emergency_reserve_plans",
        sa.Column("target_cents", sa.BigInteger(), nullable=True),
    )


def downgrade() -> None:
    op.drop_column("emergency_reserve_plans", "target_cents")
    op.drop_column("emergency_reserve_plans", "details")
