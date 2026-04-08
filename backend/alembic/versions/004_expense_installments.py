"""expense installments and recurring metadata

Revision ID: 004
Revises: 003
Create Date: 2026-04-07

"""

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects.postgresql import UUID

revision: str = "004"
down_revision: Union[str, Sequence[str], None] = "003"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.add_column(
        "expenses",
        sa.Column(
            "installment_total",
            sa.Integer(),
            nullable=False,
            server_default="1",
        ),
    )
    op.add_column(
        "expenses",
        sa.Column(
            "installment_number",
            sa.Integer(),
            nullable=False,
            server_default="1",
        ),
    )
    op.add_column(
        "expenses",
        sa.Column("installment_group_id", UUID(as_uuid=True), nullable=True),
    )
    op.add_column(
        "expenses",
        sa.Column("recurring_frequency", sa.String(length=32), nullable=True),
    )
    op.create_index(
        "ix_expenses_installment_group_id",
        "expenses",
        ["installment_group_id"],
        unique=False,
    )


def downgrade() -> None:
    op.drop_index("ix_expenses_installment_group_id", table_name="expenses")
    op.drop_column("expenses", "recurring_frequency")
    op.drop_column("expenses", "installment_group_id")
    op.drop_column("expenses", "installment_number")
    op.drop_column("expenses", "installment_total")
