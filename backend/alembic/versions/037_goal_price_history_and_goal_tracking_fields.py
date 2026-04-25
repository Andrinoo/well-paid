"""goal price history and tracking fields

Revision ID: 037
Revises: 036
Create Date: 2026-04-25
"""

from __future__ import annotations

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects import postgresql

revision: str = "037"
down_revision: Union[str, Sequence[str], None] = "036"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.add_column("goals", sa.Column("description", sa.String(length=1000), nullable=True))
    op.add_column("goals", sa.Column("due_at", sa.DateTime(timezone=True), nullable=True))
    op.add_column(
        "goals",
        sa.Column(
            "price_check_interval_hours",
            sa.BigInteger(),
            nullable=False,
            server_default="12",
        ),
    )
    op.add_column(
        "goals",
        sa.Column("last_price_track_at", sa.DateTime(timezone=True), nullable=True),
    )

    op.create_table(
        "goal_price_history",
        sa.Column("id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("goal_id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("price_cents", sa.BigInteger(), nullable=False),
        sa.Column("currency", sa.String(length=8), nullable=False),
        sa.Column("source", sa.String(length=64), nullable=True),
        sa.Column("observed_url", sa.String(length=2048), nullable=True),
        sa.Column("observed_title", sa.String(length=500), nullable=True),
        sa.Column("capture_type", sa.String(length=24), nullable=False, server_default="manual"),
        sa.Column(
            "recorded_at",
            sa.DateTime(timezone=True),
            nullable=False,
            server_default=sa.text("now()"),
        ),
        sa.ForeignKeyConstraint(["goal_id"], ["goals.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(
        op.f("ix_goal_price_history_goal_id"),
        "goal_price_history",
        ["goal_id"],
        unique=False,
    )
    op.create_index(
        op.f("ix_goal_price_history_recorded_at"),
        "goal_price_history",
        ["recorded_at"],
        unique=False,
    )
    op.create_index(
        "ix_goal_price_history_goal_id_recorded_at",
        "goal_price_history",
        ["goal_id", "recorded_at"],
        unique=False,
    )


def downgrade() -> None:
    op.drop_index("ix_goal_price_history_goal_id_recorded_at", table_name="goal_price_history")
    op.drop_index(op.f("ix_goal_price_history_recorded_at"), table_name="goal_price_history")
    op.drop_index(op.f("ix_goal_price_history_goal_id"), table_name="goal_price_history")
    op.drop_table("goal_price_history")

    op.drop_column("goals", "last_price_track_at")
    op.drop_column("goals", "price_check_interval_hours")
    op.drop_column("goals", "due_at")
    op.drop_column("goals", "description")
