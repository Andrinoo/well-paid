"""income_categories (seed) + incomes (proventos)

Revision ID: 009
Revises: 008
Create Date: 2026-04-08
"""

from __future__ import annotations

import uuid
from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects.postgresql import UUID

revision: str = "009"
down_revision: Union[str, Sequence[str], None] = "008"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None

_INCOME_CAT_NS = uuid.UUID("018ed000-0000-7000-8000-000000000002")


def _ic_uuid(key: str) -> uuid.UUID:
    return uuid.uuid5(_INCOME_CAT_NS, f"income_category:{key}")


_SEED: list[tuple[str, str, int]] = [
    ("salario", "Salário", 1),
    ("freelance", "Freelance / informal", 2),
    ("rendimentos_capital", "Juros, dividendos, aluguer", 3),
    ("presentes", "Presentes ou apoios familiares", 4),
    ("outros", "Outros proventos", 5),
]


def upgrade() -> None:
    op.create_table(
        "income_categories",
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
    op.create_index(
        op.f("ix_income_categories_key"), "income_categories", ["key"], unique=True
    )

    conn = op.get_bind()
    for key, name, sort in _SEED:
        conn.execute(
            sa.text(
                "INSERT INTO income_categories (id, key, name, sort_order) "
                "VALUES (:id, :k, :n, :s)"
            ),
            {"id": _ic_uuid(key), "k": key, "n": name, "s": sort},
        )

    op.create_table(
        "incomes",
        sa.Column("id", UUID(as_uuid=True), nullable=False),
        sa.Column("owner_user_id", UUID(as_uuid=True), nullable=False),
        sa.Column("description", sa.String(length=500), nullable=False),
        sa.Column("amount_cents", sa.BigInteger(), nullable=False),
        sa.Column("income_date", sa.Date(), nullable=False),
        sa.Column("income_category_id", UUID(as_uuid=True), nullable=False),
        sa.Column("notes", sa.String(length=500), nullable=True),
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
        sa.ForeignKeyConstraint(["owner_user_id"], ["users.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(
            ["income_category_id"],
            ["income_categories.id"],
            ondelete="RESTRICT",
        ),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(
        op.f("ix_incomes_owner_user_id"), "incomes", ["owner_user_id"], unique=False
    )
    op.create_index(
        op.f("ix_incomes_income_date"), "incomes", ["income_date"], unique=False
    )
    op.create_index(
        op.f("ix_incomes_income_category_id"),
        "incomes",
        ["income_category_id"],
        unique=False,
    )


def downgrade() -> None:
    op.drop_index(op.f("ix_incomes_income_category_id"), table_name="incomes")
    op.drop_index(op.f("ix_incomes_income_date"), table_name="incomes")
    op.drop_index(op.f("ix_incomes_owner_user_id"), table_name="incomes")
    op.drop_table("incomes")
    op.drop_index(op.f("ix_income_categories_key"), table_name="income_categories")
    op.drop_table("income_categories")
