"""expense_shares, family_receivables, expenses.split_mode

Revision ID: 025
Revises: 024
Create Date: 2026-04-16
"""

from __future__ import annotations

import uuid
from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op
from sqlalchemy import text
from sqlalchemy.dialects.postgresql import UUID

revision: str = "025"
down_revision: Union[str, Sequence[str], None] = "024"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.add_column(
        "expenses",
        sa.Column("split_mode", sa.String(length=16), nullable=True),
    )

    op.create_table(
        "expense_shares",
        sa.Column("id", UUID(as_uuid=True), primary_key=True, nullable=False),
        sa.Column(
            "expense_id",
            UUID(as_uuid=True),
            sa.ForeignKey("expenses.id", ondelete="CASCADE"),
            nullable=False,
            index=True,
        ),
        sa.Column(
            "user_id",
            UUID(as_uuid=True),
            sa.ForeignKey("users.id", ondelete="CASCADE"),
            nullable=False,
            index=True,
        ),
        sa.Column("share_cents", sa.BigInteger(), nullable=False),
        sa.Column("share_percent_bps", sa.Integer(), nullable=True),
        sa.Column("paid_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("status", sa.String(length=24), nullable=False, server_default="pending"),
        sa.Column(
            "covered_by_user_id",
            UUID(as_uuid=True),
            sa.ForeignKey("users.id", ondelete="SET NULL"),
            nullable=True,
        ),
        sa.Column("covered_at", sa.DateTime(timezone=True), nullable=True),
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
        sa.UniqueConstraint("expense_id", "user_id", name="uq_expense_shares_expense_user"),
    )

    op.create_table(
        "family_receivables",
        sa.Column("id", UUID(as_uuid=True), primary_key=True, nullable=False),
        sa.Column(
            "creditor_user_id",
            UUID(as_uuid=True),
            sa.ForeignKey("users.id", ondelete="CASCADE"),
            nullable=False,
            index=True,
        ),
        sa.Column(
            "debtor_user_id",
            UUID(as_uuid=True),
            sa.ForeignKey("users.id", ondelete="CASCADE"),
            nullable=False,
            index=True,
        ),
        sa.Column("amount_cents", sa.BigInteger(), nullable=False),
        sa.Column("settle_by", sa.Date(), nullable=False),
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
        sa.Column("status", sa.String(length=16), nullable=False, server_default="open"),
        sa.Column("settled_at", sa.DateTime(timezone=True), nullable=True),
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
    )

    conn = op.get_bind()
    # Backfill: shared expenses → two rows 50/50 (remainder to owner)
    rows = conn.execute(
        text(
            """
            SELECT id, owner_user_id, shared_with_user_id, amount_cents
            FROM expenses
            WHERE is_shared = true
              AND shared_with_user_id IS NOT NULL
              AND deleted_at IS NULL
            """
        )
    ).fetchall()
    ins = text(
        """
        INSERT INTO expense_shares (
            id, expense_id, user_id, share_cents, share_percent_bps,
            paid_at, status, created_at, updated_at
        ) VALUES (
            :id, :eid, :uid, :sc, :bps, NULL, 'pending', now(), now()
        )
        """
    )
    for eid, owner_uid, peer_uid, total in rows:
        total = int(total)
        half = total // 2
        rem = total - half * 2
        owner_cents = half + rem
        peer_cents = half
        conn.execute(
            ins,
            {
                "id": uuid.uuid4(),
                "eid": eid,
                "uid": owner_uid,
                "sc": owner_cents,
                "bps": 5000,
            },
        )
        conn.execute(
            ins,
            {
                "id": uuid.uuid4(),
                "eid": eid,
                "uid": peer_uid,
                "sc": peer_cents,
                "bps": 5000,
            },
        )
    # Mark split_mode for backfilled
    conn.execute(
        text(
            """
            UPDATE expenses SET split_mode = 'amount'
            WHERE is_shared = true AND shared_with_user_id IS NOT NULL
            """
        )
    )


def downgrade() -> None:
    op.drop_table("family_receivables")
    op.drop_table("expense_shares")
    op.drop_column("expenses", "split_mode")
