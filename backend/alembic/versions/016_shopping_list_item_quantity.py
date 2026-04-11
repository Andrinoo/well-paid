"""shopping_list_items.quantity

Revision ID: 016
Revises: 015
Create Date: 2026-04-10
"""

from __future__ import annotations

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op

revision: str = "016"
down_revision: Union[str, Sequence[str], None] = "015"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.add_column(
        "shopping_list_items",
        sa.Column("quantity", sa.Integer(), nullable=False, server_default="1"),
    )
    op.alter_column("shopping_list_items", "quantity", server_default=None)


def downgrade() -> None:
    op.drop_column("shopping_list_items", "quantity")
