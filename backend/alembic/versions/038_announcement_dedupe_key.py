"""announcement dedupe key

Revision ID: 038
Revises: 037
Create Date: 2026-04-25
"""

from __future__ import annotations

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op

revision: str = "038"
down_revision: Union[str, Sequence[str], None] = "037"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.add_column(
        "announcements",
        sa.Column("dedupe_key", sa.String(length=120), nullable=True),
    )
    op.create_index(
        op.f("ix_announcements_dedupe_key"),
        "announcements",
        ["dedupe_key"],
        unique=True,
    )


def downgrade() -> None:
    op.drop_index(op.f("ix_announcements_dedupe_key"), table_name="announcements")
    op.drop_column("announcements", "dedupe_key")
