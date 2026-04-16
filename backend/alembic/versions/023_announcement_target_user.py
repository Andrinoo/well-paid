"""announcements optional per-user targeting

Revision ID: 023
Revises: 022
Create Date: 2026-04-16
"""

from __future__ import annotations

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects.postgresql import UUID

revision: str = "023"
down_revision: Union[str, Sequence[str], None] = "022"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    conn = op.get_bind()
    insp = sa.inspect(conn)
    cols = {c["name"] for c in insp.get_columns("announcements")}
    if "target_user_id" in cols:
        return
    op.add_column(
        "announcements",
        sa.Column("target_user_id", UUID(as_uuid=True), nullable=True),
    )
    op.create_foreign_key(
        "fk_announcements_target_user_id_users",
        "announcements",
        "users",
        ["target_user_id"],
        ["id"],
        ondelete="SET NULL",
    )
    op.create_index(
        op.f("ix_announcements_target_user_id"),
        "announcements",
        ["target_user_id"],
        unique=False,
    )


def downgrade() -> None:
    op.drop_index(op.f("ix_announcements_target_user_id"), table_name="announcements")
    op.drop_constraint("fk_announcements_target_user_id_users", "announcements", type_="foreignkey")
    op.drop_column("announcements", "target_user_id")
