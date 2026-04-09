"""expenses.deleted_at — soft delete (dashboard mantém histórico do mês)

Revision ID: 010
Revises: 009
Create Date: 2026-04-08
"""

from __future__ import annotations

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op

revision: str = "010"
down_revision: Union[str, Sequence[str], None] = "009"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.add_column(
        "expenses",
        sa.Column("deleted_at", sa.DateTime(timezone=True), nullable=True),
    )
    op.create_index("ix_expenses_deleted_at", "expenses", ["deleted_at"])


def downgrade() -> None:
    op.drop_index("ix_expenses_deleted_at", table_name="expenses")
    op.drop_column("expenses", "deleted_at")
