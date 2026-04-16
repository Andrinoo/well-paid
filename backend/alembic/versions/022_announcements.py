"""announcements content table

Revision ID: 022
Revises: 021
Create Date: 2026-04-16
"""

from __future__ import annotations

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects.postgresql import UUID

revision: str = "022"
down_revision: Union[str, Sequence[str], None] = "021"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    conn = op.get_bind()
    insp = sa.inspect(conn)
    if "announcements" in set(insp.get_table_names()):
        return

    op.create_table(
        "announcements",
        sa.Column("id", UUID(as_uuid=True), nullable=False),
        sa.Column("title", sa.String(length=140), nullable=False),
        sa.Column("body", sa.Text(), nullable=False),
        sa.Column("kind", sa.String(length=24), nullable=False, server_default="info"),
        sa.Column(
            "placement", sa.String(length=32), nullable=False, server_default="home_banner"
        ),
        sa.Column("priority", sa.Integer(), nullable=False, server_default="0"),
        sa.Column("cta_label", sa.String(length=80), nullable=True),
        sa.Column("cta_url", sa.String(length=500), nullable=True),
        sa.Column("is_active", sa.Boolean(), nullable=False, server_default=sa.text("false")),
        sa.Column("starts_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("ends_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("created_by_user_id", UUID(as_uuid=True), nullable=True),
        sa.Column(
            "created_at",
            sa.DateTime(timezone=True),
            nullable=False,
            server_default=sa.text("now()"),
        ),
        sa.Column(
            "updated_at",
            sa.DateTime(timezone=True),
            nullable=False,
            server_default=sa.text("now()"),
        ),
        sa.ForeignKeyConstraint(["created_by_user_id"], ["users.id"], ondelete="SET NULL"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(
        op.f("ix_announcements_created_at"), "announcements", ["created_at"], unique=False
    )
    op.create_index(
        op.f("ix_announcements_created_by_user_id"),
        "announcements",
        ["created_by_user_id"],
        unique=False,
    )
    op.create_index(
        op.f("ix_announcements_placement"), "announcements", ["placement"], unique=False
    )


def downgrade() -> None:
    op.drop_index(op.f("ix_announcements_placement"), table_name="announcements")
    op.drop_index(op.f("ix_announcements_created_by_user_id"), table_name="announcements")
    op.drop_index(op.f("ix_announcements_created_at"), table_name="announcements")
    op.drop_table("announcements")
