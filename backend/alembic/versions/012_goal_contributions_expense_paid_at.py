"""goal_contributions ledger + expenses.paid_at

Revision ID: 012
Revises: 011
Create Date: 2026-04-09
"""

from __future__ import annotations

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects.postgresql import UUID

revision: str = "012"
down_revision: Union[str, Sequence[str], None] = "011"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        "goal_contributions",
        sa.Column("id", UUID(as_uuid=True), nullable=False),
        sa.Column("goal_id", UUID(as_uuid=True), nullable=False),
        sa.Column("amount_cents", sa.BigInteger(), nullable=False),
        sa.Column("note", sa.String(length=500), nullable=True),
        sa.Column(
            "recorded_at",
            sa.DateTime(timezone=True),
            server_default=sa.text("now()"),
            nullable=False,
        ),
        sa.ForeignKeyConstraint(["goal_id"], ["goals.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(
        op.f("ix_goal_contributions_goal_id"),
        "goal_contributions",
        ["goal_id"],
        unique=False,
    )
    op.create_index(
        op.f("ix_goal_contributions_recorded_at"),
        "goal_contributions",
        ["recorded_at"],
        unique=False,
    )

    op.add_column(
        "expenses",
        sa.Column("paid_at", sa.DateTime(timezone=True), nullable=True),
    )


def downgrade() -> None:
    op.drop_column("expenses", "paid_at")
    op.drop_index(op.f("ix_goal_contributions_recorded_at"), table_name="goal_contributions")
    op.drop_index(op.f("ix_goal_contributions_goal_id"), table_name="goal_contributions")
    op.drop_table("goal_contributions")
