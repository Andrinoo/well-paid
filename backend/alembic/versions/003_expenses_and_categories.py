"""expenses and categories (dashboard MVP)

Revision ID: 003
Revises: 002
Create Date: 2026-04-06

"""

from __future__ import annotations

import uuid
from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects.postgresql import UUID

revision: str = "003"
down_revision: Union[str, Sequence[str], None] = "002"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None

# UUID estáveis para categorias MVP (Telas.txt §5.4) — mesmo conjunto em todos os ambientes.
_CATEGORY_NS = uuid.UUID("018ed000-0000-7000-8000-000000000001")


def _category_uuid(key: str) -> uuid.UUID:
    return uuid.uuid5(_CATEGORY_NS, f"category:{key}")


_SEED_CATEGORIES: list[tuple[str, str, int]] = [
    ("alimentacao", "Alimentação", 1),
    ("transporte", "Transporte", 2),
    ("moradia", "Moradia", 3),
    ("saude", "Saúde", 4),
    ("educacao", "Educação", 5),
    ("lazer", "Lazer", 6),
    ("pessoais", "Pessoais", 7),
    ("emprestimos", "Empréstimos", 8),
    ("outros", "Outros", 9),
]


def upgrade() -> None:
    op.create_table(
        "categories",
        sa.Column("id", UUID(as_uuid=True), nullable=False),
        sa.Column("key", sa.String(length=64), nullable=False),
        sa.Column("name", sa.String(length=100), nullable=False),
        sa.Column("sort_order", sa.Integer(), nullable=False, server_default="0"),
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
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(op.f("ix_categories_key"), "categories", ["key"], unique=True)

    op.create_table(
        "expenses",
        sa.Column("id", UUID(as_uuid=True), nullable=False),
        sa.Column("owner_user_id", UUID(as_uuid=True), nullable=False),
        sa.Column("description", sa.String(length=500), nullable=False),
        sa.Column("amount_cents", sa.BigInteger(), nullable=False),
        sa.Column("expense_date", sa.Date(), nullable=False),
        sa.Column("due_date", sa.Date(), nullable=True),
        sa.Column("status", sa.String(length=16), nullable=False),
        sa.Column("category_id", UUID(as_uuid=True), nullable=False),
        sa.Column(
            "sync_status",
            sa.Integer(),
            nullable=False,
            server_default="0",
        ),
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
        sa.ForeignKeyConstraint(["category_id"], ["categories.id"], ondelete="RESTRICT"),
        sa.ForeignKeyConstraint(["owner_user_id"], ["users.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(
        op.f("ix_expenses_category_id"), "expenses", ["category_id"], unique=False
    )
    op.create_index(
        op.f("ix_expenses_owner_user_id"), "expenses", ["owner_user_id"], unique=False
    )
    op.create_index(
        "ix_expenses_owner_expense_date",
        "expenses",
        ["owner_user_id", "expense_date"],
        unique=False,
    )
    op.create_index(
        "ix_expenses_owner_status_due_date",
        "expenses",
        ["owner_user_id", "status", "due_date"],
        unique=False,
    )

    bind = op.get_bind()
    for key, name, sort_order in _SEED_CATEGORIES:
        bind.execute(
            sa.text(
                "INSERT INTO categories (id, key, name, sort_order) "
                "VALUES (:id, :key, :name, :sort_order)"
            ),
            {
                "id": _category_uuid(key),
                "key": key,
                "name": name,
                "sort_order": sort_order,
            },
        )


def downgrade() -> None:
    op.drop_index("ix_expenses_owner_status_due_date", table_name="expenses")
    op.drop_index("ix_expenses_owner_expense_date", table_name="expenses")
    op.drop_index(op.f("ix_expenses_owner_user_id"), table_name="expenses")
    op.drop_index(op.f("ix_expenses_category_id"), table_name="expenses")
    op.drop_table("expenses")
    op.drop_index(op.f("ix_categories_key"), table_name="categories")
    op.drop_table("categories")
