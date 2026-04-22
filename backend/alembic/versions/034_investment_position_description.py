"""add description to investment positions

Revision ID: 034
Revises: 030_investment_positions
Create Date: 2026-04-22 15:30:00
"""

from __future__ import annotations

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision = "034"
down_revision = "030"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.add_column(
        "investment_positions",
        sa.Column("description", sa.String(length=220), nullable=True),
    )


def downgrade() -> None:
    op.drop_column("investment_positions", "description")

