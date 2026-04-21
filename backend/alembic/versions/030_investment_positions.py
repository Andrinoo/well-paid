"""investment_positions table for real user holdings

Revision ID: 030
Revises: 029
Create Date: 2026-04-21
"""

from __future__ import annotations

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects import postgresql

revision: str = "030"
down_revision: Union[str, Sequence[str], None] = "029"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        "investment_positions",
        sa.Column("id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("owner_user_id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("instrument_type", sa.String(length=32), nullable=False),
        sa.Column("name", sa.String(length=180), nullable=False),
        sa.Column("principal_cents", sa.BigInteger(), nullable=False),
        sa.Column("annual_rate_bps", sa.Integer(), nullable=False, server_default="0"),
        sa.Column("maturity_date", sa.Date(), nullable=True),
        sa.Column("is_liquid", sa.Boolean(), nullable=False, server_default=sa.text("true")),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.ForeignKeyConstraint(["owner_user_id"], ["users.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(
        op.f("ix_investment_positions_owner_user_id"),
        "investment_positions",
        ["owner_user_id"],
        unique=False,
    )
    op.create_index(
        op.f("ix_investment_positions_instrument_type"),
        "investment_positions",
        ["instrument_type"],
        unique=False,
    )


def downgrade() -> None:
    op.drop_index(op.f("ix_investment_positions_instrument_type"), table_name="investment_positions")
    op.drop_index(op.f("ix_investment_positions_owner_user_id"), table_name="investment_positions")
    op.drop_table("investment_positions")
