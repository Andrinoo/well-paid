"""equity fundamentals history table

Revision ID: 035
Revises: dd6bb02fa2fc
Create Date: 2026-04-22
"""

from __future__ import annotations

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects import postgresql

revision: str = "035"
down_revision: Union[str, Sequence[str], None] = "dd6bb02fa2fc"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        "equity_fundamentals_history",
        sa.Column("id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("symbol", sa.String(length=12), nullable=False),
        sa.Column("pl", sa.String(length=64), nullable=True),
        sa.Column("pvp", sa.String(length=64), nullable=True),
        sa.Column("dividend_yield", sa.String(length=64), nullable=True),
        sa.Column("roe", sa.String(length=64), nullable=True),
        sa.Column("ev_ebitda", sa.String(length=64), nullable=True),
        sa.Column("net_margin", sa.String(length=64), nullable=True),
        sa.Column("net_debt_ebitda", sa.String(length=64), nullable=True),
        sa.Column("eps", sa.String(length=64), nullable=True),
        sa.Column("source", sa.String(length=32), nullable=False, server_default="fundamentus"),
        sa.Column("confidence", sa.Float(), nullable=True),
        sa.Column("collected_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.text("now()")),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(
        op.f("ix_equity_fundamentals_history_symbol"),
        "equity_fundamentals_history",
        ["symbol"],
        unique=False,
    )
    op.create_index(
        op.f("ix_equity_fundamentals_history_collected_at"),
        "equity_fundamentals_history",
        ["collected_at"],
        unique=False,
    )
    op.create_index(
        "ix_equity_fundamentals_history_symbol_collected_at",
        "equity_fundamentals_history",
        ["symbol", "collected_at"],
        unique=False,
    )


def downgrade() -> None:
    op.drop_index(
        "ix_equity_fundamentals_history_symbol_collected_at",
        table_name="equity_fundamentals_history",
    )
    op.drop_index(
        op.f("ix_equity_fundamentals_history_collected_at"),
        table_name="equity_fundamentals_history",
    )
    op.drop_index(
        op.f("ix_equity_fundamentals_history_symbol"),
        table_name="equity_fundamentals_history",
    )
    op.drop_table("equity_fundamentals_history")
