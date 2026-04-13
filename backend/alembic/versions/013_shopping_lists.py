"""shopping_lists + shopping_list_items

Revision ID: 013
Revises: 012
Create Date: 2026-04-09
"""

from __future__ import annotations

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects.postgresql import UUID

revision: str = "013"
down_revision: Union[str, Sequence[str], None] = "012"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        "shopping_lists",
        sa.Column("id", UUID(as_uuid=True), nullable=False),
        sa.Column("owner_user_id", UUID(as_uuid=True), nullable=False),
        sa.Column("title", sa.String(length=200), nullable=True),
        sa.Column("store_name", sa.String(length=200), nullable=True),
        sa.Column("status", sa.String(length=32), nullable=False),
        sa.Column("completed_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("expense_id", UUID(as_uuid=True), nullable=True),
        sa.Column("total_cents", sa.BigInteger(), nullable=True),
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
        sa.ForeignKeyConstraint(["owner_user_id"], ["users.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["expense_id"], ["expenses.id"], ondelete="SET NULL"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(
        op.f("ix_shopping_lists_owner_user_id"),
        "shopping_lists",
        ["owner_user_id"],
        unique=False,
    )
    op.create_index(
        op.f("ix_shopping_lists_status"),
        "shopping_lists",
        ["status"],
        unique=False,
    )

    op.create_table(
        "shopping_list_items",
        sa.Column("id", UUID(as_uuid=True), nullable=False),
        sa.Column("list_id", UUID(as_uuid=True), nullable=False),
        sa.Column("sort_order", sa.Integer(), nullable=False),
        sa.Column("label", sa.String(length=500), nullable=False),
        sa.Column("line_amount_cents", sa.BigInteger(), nullable=True),
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
        sa.ForeignKeyConstraint(["list_id"], ["shopping_lists.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(
        op.f("ix_shopping_list_items_list_id"),
        "shopping_list_items",
        ["list_id"],
        unique=False,
    )


def downgrade() -> None:
    op.drop_index(op.f("ix_shopping_list_items_list_id"), table_name="shopping_list_items")
    op.drop_table("shopping_list_items")
    op.drop_index(op.f("ix_shopping_lists_status"), table_name="shopping_lists")
    op.drop_index(op.f("ix_shopping_lists_owner_user_id"), table_name="shopping_lists")
    op.drop_table("shopping_lists")
