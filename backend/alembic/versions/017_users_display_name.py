"""users.display_name for greeting / profile

Revision ID: 017
Revises: 016
Create Date: 2026-04-14
"""

from __future__ import annotations

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op

revision: str = "017"
down_revision: Union[str, Sequence[str], None] = "016"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """Idempotente: a coluna pode já existir (BD alinhada à mão ou retry após falha parcial)."""
    conn = op.get_bind()
    insp = sa.inspect(conn)
    existing = {c["name"] for c in insp.get_columns("users")}
    if "display_name" in existing:
        return
    op.add_column(
        "users",
        sa.Column("display_name", sa.String(length=200), nullable=True),
    )


def downgrade() -> None:
    conn = op.get_bind()
    insp = sa.inspect(conn)
    existing = {c["name"] for c in insp.get_columns("users")}
    if "display_name" not in existing:
        return
    op.drop_column("users", "display_name")
