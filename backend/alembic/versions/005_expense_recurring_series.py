"""expense recurring series support

Revision ID: 005
Revises: 004
Create Date: 2026-04-08
"""

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects.postgresql import UUID

revision: str = "005"
down_revision: Union[str, Sequence[str], None] = "004"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.add_column(
        "expenses",
        sa.Column("recurring_series_id", UUID(as_uuid=True), nullable=True),
    )
    op.add_column(
        "expenses",
        sa.Column("recurring_generated_until", sa.Date(), nullable=True),
    )
    op.create_index(
        "ix_expenses_recurring_series_id",
        "expenses",
        ["recurring_series_id"],
        unique=False,
    )


def downgrade() -> None:
    op.drop_index("ix_expenses_recurring_series_id", table_name="expenses")
    op.drop_column("expenses", "recurring_generated_until")
    op.drop_column("expenses", "recurring_series_id")
