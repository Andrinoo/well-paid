"""emergency_reserves + emergency_reserve_accruals (reserva mensal agregada)

Revision ID: 011
Revises: 010
Create Date: 2026-04-08
"""

from __future__ import annotations

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects.postgresql import UUID

revision: str = "011"
down_revision: Union[str, Sequence[str], None] = "010"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        "emergency_reserves",
        sa.Column("id", UUID(as_uuid=True), nullable=False),
        sa.Column("family_id", UUID(as_uuid=True), nullable=True),
        sa.Column("solo_user_id", UUID(as_uuid=True), nullable=True),
        sa.Column("monthly_target_cents", sa.BigInteger(), nullable=False, server_default="0"),
        sa.Column("balance_cents", sa.BigInteger(), nullable=False, server_default="0"),
        sa.Column("tracking_start", sa.Date(), nullable=False),
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
            name="ck_emergency_reserves_scope_xor",
        ),
        sa.ForeignKeyConstraint(["family_id"], ["families.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["solo_user_id"], ["users.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("family_id"),
        sa.UniqueConstraint("solo_user_id"),
    )
    op.create_index(
        "ix_emergency_reserves_family_id",
        "emergency_reserves",
        ["family_id"],
        unique=False,
    )
    op.create_index(
        "ix_emergency_reserves_solo_user_id",
        "emergency_reserves",
        ["solo_user_id"],
        unique=False,
    )

    op.create_table(
        "emergency_reserve_accruals",
        sa.Column("id", UUID(as_uuid=True), nullable=False),
        sa.Column("reserve_id", UUID(as_uuid=True), nullable=False),
        sa.Column("year", sa.Integer(), nullable=False),
        sa.Column("month", sa.Integer(), nullable=False),
        sa.Column("amount_cents", sa.BigInteger(), nullable=False),
        sa.Column(
            "created_at",
            sa.DateTime(timezone=True),
            server_default=sa.text("now()"),
            nullable=False,
        ),
        sa.ForeignKeyConstraint(
            ["reserve_id"],
            ["emergency_reserves.id"],
            ondelete="CASCADE",
        ),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint(
            "reserve_id",
            "year",
            "month",
            name="uq_emergency_reserve_accruals_reserve_year_month",
        ),
    )
    op.create_index(
        "ix_emergency_reserve_accruals_reserve_id",
        "emergency_reserve_accruals",
        ["reserve_id"],
        unique=False,
    )


def downgrade() -> None:
    op.drop_index("ix_emergency_reserve_accruals_reserve_id", table_name="emergency_reserve_accruals")
    op.drop_table("emergency_reserve_accruals")
    op.drop_index("ix_emergency_reserves_solo_user_id", table_name="emergency_reserves")
    op.drop_index("ix_emergency_reserves_family_id", table_name="emergency_reserves")
    op.drop_table("emergency_reserves")
