"""per-user announcement read/hidden state for history

Revision ID: 024
Revises: 023
Create Date: 2026-04-16
"""

from __future__ import annotations

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects.postgresql import UUID

revision: str = "024"
down_revision: Union[str, Sequence[str], None] = "023"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    conn = op.get_bind()
    insp = sa.inspect(conn)
    if "announcement_user_states" in insp.get_table_names():
        return
    op.create_table(
        "announcement_user_states",
        sa.Column("id", UUID(as_uuid=True), primary_key=True, nullable=False),
        sa.Column("user_id", UUID(as_uuid=True), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column(
            "announcement_id",
            UUID(as_uuid=True),
            sa.ForeignKey("announcements.id", ondelete="CASCADE"),
            nullable=False,
        ),
        sa.Column("read_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("hidden_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False),
        sa.UniqueConstraint(
            "user_id",
            "announcement_id",
            name="uq_announcement_user_states_user_announcement",
        ),
    )
    op.create_index(
        op.f("ix_announcement_user_states_user_id"),
        "announcement_user_states",
        ["user_id"],
        unique=False,
    )
    op.create_index(
        op.f("ix_announcement_user_states_announcement_id"),
        "announcement_user_states",
        ["announcement_id"],
        unique=False,
    )


def downgrade() -> None:
    op.drop_index(op.f("ix_announcement_user_states_announcement_id"), table_name="announcement_user_states")
    op.drop_index(op.f("ix_announcement_user_states_user_id"), table_name="announcement_user_states")
    op.drop_table("announcement_user_states")
