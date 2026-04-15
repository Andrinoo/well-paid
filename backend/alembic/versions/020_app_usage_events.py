"""app_usage_events table for admin metrics

Revision ID: 020
Revises: 019
Create Date: 2026-04-15
"""

from __future__ import annotations

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects.postgresql import UUID

revision: str = "020"
down_revision: Union[str, Sequence[str], None] = "019"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    conn = op.get_bind()
    insp = sa.inspect(conn)
    if "app_usage_events" in set(insp.get_table_names()):
        return

    op.create_table(
        "app_usage_events",
        sa.Column("id", UUID(as_uuid=True), nullable=False),
        sa.Column("user_id", UUID(as_uuid=True), nullable=False),
        sa.Column("event_type", sa.String(length=32), nullable=False),
        sa.Column("occurred_at", sa.DateTime(timezone=True), nullable=False),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(
        op.f("ix_app_usage_events_user_id"),
        "app_usage_events",
        ["user_id"],
        unique=False,
    )
    op.create_index(
        op.f("ix_app_usage_events_occurred_at"),
        "app_usage_events",
        ["occurred_at"],
        unique=False,
    )


def downgrade() -> None:
    op.drop_index(op.f("ix_app_usage_events_occurred_at"), table_name="app_usage_events")
    op.drop_index(op.f("ix_app_usage_events_user_id"), table_name="app_usage_events")
    op.drop_table("app_usage_events")
