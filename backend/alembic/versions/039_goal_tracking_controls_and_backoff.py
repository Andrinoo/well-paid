"""goal tracking controls and backoff fields

Revision ID: 039
Revises: 038
Create Date: 2026-04-25
"""

from __future__ import annotations

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op

revision: str = "039"
down_revision: Union[str, Sequence[str], None] = "038"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.add_column(
        "goals",
        sa.Column("tracking_enabled", sa.Boolean(), nullable=False, server_default=sa.true()),
    )
    op.add_column(
        "goals",
        sa.Column("tracking_failures", sa.BigInteger(), nullable=False, server_default="0"),
    )
    op.add_column(
        "goals",
        sa.Column("next_track_after", sa.DateTime(timezone=True), nullable=True),
    )


def downgrade() -> None:
    op.drop_column("goals", "next_track_after")
    op.drop_column("goals", "tracking_failures")
    op.drop_column("goals", "tracking_enabled")
