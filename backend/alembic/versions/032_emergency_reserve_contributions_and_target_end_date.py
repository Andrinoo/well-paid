"""emergency reserve contributions ledger + plan target end date

Revision ID: 032
Revises: 031
Create Date: 2026-04-21
"""

from __future__ import annotations

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects.postgresql import UUID

revision: str = "032"
down_revision: Union[str, Sequence[str], None] = "031"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.add_column(
        "emergency_reserve_plans",
        sa.Column("target_end_date", sa.Date(), nullable=True),
    )

    op.create_table(
        "emergency_reserve_contributions",
        sa.Column("id", UUID(as_uuid=True), primary_key=True, nullable=False),
        sa.Column("family_id", UUID(as_uuid=True), nullable=True),
        sa.Column("solo_user_id", UUID(as_uuid=True), nullable=True),
        sa.Column("contribution_date", sa.Date(), nullable=False),
        sa.Column("total_amount_cents", sa.BigInteger(), nullable=False, server_default="0"),
        sa.Column("created_by_user_id", UUID(as_uuid=True), nullable=True),
        sa.Column("note", sa.String(length=500), nullable=True),
        sa.Column(
            "created_at",
            sa.DateTime(timezone=True),
            server_default=sa.text("now()"),
            nullable=False,
        ),
        sa.Column(
            "updated_at",
            sa.DateTime(timezone=True),
            server_default=sa.text("now()"),
            nullable=False,
        ),
        sa.CheckConstraint(
            "(family_id IS NOT NULL AND solo_user_id IS NULL) OR "
            "(family_id IS NULL AND solo_user_id IS NOT NULL)",
            name="ck_emergency_reserve_contributions_scope_xor",
        ),
        sa.ForeignKeyConstraint(["family_id"], ["families.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["solo_user_id"], ["users.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(
            ["created_by_user_id"],
            ["users.id"],
            ondelete="SET NULL",
        ),
    )
    op.create_index(
        "ix_emergency_reserve_contributions_family_id",
        "emergency_reserve_contributions",
        ["family_id"],
        unique=False,
    )
    op.create_index(
        "ix_emergency_reserve_contributions_solo_user_id",
        "emergency_reserve_contributions",
        ["solo_user_id"],
        unique=False,
    )
    op.create_index(
        "ix_emergency_reserve_contributions_created_by_user_id",
        "emergency_reserve_contributions",
        ["created_by_user_id"],
        unique=False,
    )

    op.create_table(
        "emergency_reserve_contribution_items",
        sa.Column("id", UUID(as_uuid=True), primary_key=True, nullable=False),
        sa.Column("contribution_id", UUID(as_uuid=True), nullable=False),
        sa.Column("plan_id", UUID(as_uuid=True), nullable=False),
        sa.Column("amount_cents", sa.BigInteger(), nullable=False, server_default="0"),
        sa.ForeignKeyConstraint(
            ["contribution_id"],
            ["emergency_reserve_contributions.id"],
            ondelete="CASCADE",
        ),
        sa.ForeignKeyConstraint(
            ["plan_id"],
            ["emergency_reserve_plans.id"],
            ondelete="CASCADE",
        ),
        sa.UniqueConstraint(
            "contribution_id",
            "plan_id",
            name="uq_emergency_reserve_contribution_items_contribution_plan",
        ),
    )
    op.create_index(
        "ix_emergency_reserve_contribution_items_contribution_id",
        "emergency_reserve_contribution_items",
        ["contribution_id"],
        unique=False,
    )
    op.create_index(
        "ix_emergency_reserve_contribution_items_plan_id",
        "emergency_reserve_contribution_items",
        ["plan_id"],
        unique=False,
    )


def downgrade() -> None:
    op.drop_index(
        "ix_emergency_reserve_contribution_items_plan_id",
        table_name="emergency_reserve_contribution_items",
    )
    op.drop_index(
        "ix_emergency_reserve_contribution_items_contribution_id",
        table_name="emergency_reserve_contribution_items",
    )
    op.drop_table("emergency_reserve_contribution_items")

    op.drop_index(
        "ix_emergency_reserve_contributions_created_by_user_id",
        table_name="emergency_reserve_contributions",
    )
    op.drop_index(
        "ix_emergency_reserve_contributions_solo_user_id",
        table_name="emergency_reserve_contributions",
    )
    op.drop_index(
        "ix_emergency_reserve_contributions_family_id",
        table_name="emergency_reserve_contributions",
    )
    op.drop_table("emergency_reserve_contributions")

    op.drop_column("emergency_reserve_plans", "target_end_date")
