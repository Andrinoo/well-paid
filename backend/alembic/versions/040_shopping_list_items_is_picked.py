"""shopping_list_items.is_picked

Revision ID: 040
Revises: 039
Create Date: 2026-04-25
"""

from __future__ import annotations

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op

revision: str = "040"
down_revision: Union[str, Sequence[str], None] = "039"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.add_column(
        "shopping_list_items",
        sa.Column("is_picked", sa.Boolean(), nullable=False, server_default=sa.true()),
    )
    op.alter_column("shopping_list_items", "is_picked", server_default=None)


def downgrade() -> None:
    op.drop_column("shopping_list_items", "is_picked")
