"""expenses.is_shared + shared_with_user_id (família)

Revision ID: 008
Revises: 007
Create Date: 2026-04-08
"""

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects.postgresql import UUID

revision: str = "008"
down_revision: Union[str, Sequence[str], None] = "007"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.add_column(
        "expenses",
        sa.Column(
            "is_shared",
            sa.Boolean(),
            nullable=False,
            server_default=sa.false(),
        ),
    )
    op.add_column(
        "expenses",
        sa.Column("shared_with_user_id", UUID(as_uuid=True), nullable=True),
    )
    op.create_foreign_key(
        "fk_expenses_shared_with_user_id_users",
        "expenses",
        "users",
        ["shared_with_user_id"],
        ["id"],
        ondelete="SET NULL",
    )
    op.create_index(
        op.f("ix_expenses_shared_with_user_id"),
        "expenses",
        ["shared_with_user_id"],
        unique=False,
    )


def downgrade() -> None:
    op.drop_index(op.f("ix_expenses_shared_with_user_id"), table_name="expenses")
    op.drop_constraint(
        "fk_expenses_shared_with_user_id_users", "expenses", type_="foreignkey"
    )
    op.drop_column("expenses", "shared_with_user_id")
    op.drop_column("expenses", "is_shared")
