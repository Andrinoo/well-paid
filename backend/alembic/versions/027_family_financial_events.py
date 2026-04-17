"""family_financial_events append-only log

Revision ID: 027
Revises: 026
Create Date: 2026-04-16
"""

from __future__ import annotations

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects.postgresql import JSONB, UUID

revision: str = "027"
down_revision: Union[str, Sequence[str], None] = "026"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        "family_financial_events",
        sa.Column("id", UUID(as_uuid=True), primary_key=True, nullable=False),
        sa.Column(
            "family_id",
            UUID(as_uuid=True),
            sa.ForeignKey("families.id", ondelete="CASCADE"),
            nullable=False,
            index=True,
        ),
        sa.Column("event_type", sa.String(length=48), nullable=False),
        sa.Column(
            "actor_user_id",
            UUID(as_uuid=True),
            sa.ForeignKey("users.id", ondelete="CASCADE"),
            nullable=False,
            index=True,
        ),
        sa.Column(
            "counterparty_user_id",
            UUID(as_uuid=True),
            sa.ForeignKey("users.id", ondelete="SET NULL"),
            nullable=True,
            index=True,
        ),
        sa.Column("amount_cents", sa.BigInteger(), nullable=True),
        sa.Column(
            "source_expense_id",
            UUID(as_uuid=True),
            sa.ForeignKey("expenses.id", ondelete="SET NULL"),
            nullable=True,
        ),
        sa.Column(
            "source_expense_share_id",
            UUID(as_uuid=True),
            sa.ForeignKey("expense_shares.id", ondelete="SET NULL"),
            nullable=True,
        ),
        sa.Column(
            "source_receivable_id",
            UUID(as_uuid=True),
            sa.ForeignKey("family_receivables.id", ondelete="SET NULL"),
            nullable=True,
        ),
        sa.Column("payload_json", JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column(
            "created_at",
            sa.DateTime(timezone=True),
            server_default=sa.text("now()"),
            nullable=False,
        ),
    )
    op.create_index(
        "ix_family_financial_events_family_created",
        "family_financial_events",
        ["family_id", "created_at"],
    )


def downgrade() -> None:
    op.drop_index("ix_family_financial_events_family_created", table_name="family_financial_events")
    op.drop_table("family_financial_events")
