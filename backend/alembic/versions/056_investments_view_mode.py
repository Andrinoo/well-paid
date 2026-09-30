"""Persist the investments cards/list preference.

Revision ID: 056
Revises: 055
"""

from alembic import op
import sqlalchemy as sa

revision = "056"
down_revision = "055"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.add_column(
        "users",
        sa.Column("investments_view_mode", sa.String(length=8), nullable=False, server_default="cards"),
    )


def downgrade() -> None:
    op.drop_column("users", "investments_view_mode")
