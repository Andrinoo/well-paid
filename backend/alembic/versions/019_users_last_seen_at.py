"""users.last_seen_at — última actividade vía login/refresh (servidor)

Revision ID: 019
Revises: 018
Create Date: 2026-04-15
"""

from __future__ import annotations

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op

revision: str = "019"
down_revision: Union[str, Sequence[str], None] = "018"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    conn = op.get_bind()
    insp = sa.inspect(conn)
    existing = {c["name"] for c in insp.get_columns("users")}
    if "last_seen_at" in existing:
        return
    op.add_column(
        "users",
        sa.Column("last_seen_at", sa.DateTime(timezone=True), nullable=True),
    )
    op.create_index(
        op.f("ix_users_last_seen_at"),
        "users",
        ["last_seen_at"],
        unique=False,
    )


def downgrade() -> None:
    op.drop_index(op.f("ix_users_last_seen_at"), table_name="users")
    op.drop_column("users", "last_seen_at")
