"""admin_audit_events for admin action audit trail

Revision ID: 021
Revises: 020
Create Date: 2026-04-15
"""

from __future__ import annotations

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects.postgresql import JSONB, UUID

revision: str = "021"
down_revision: Union[str, Sequence[str], None] = "020"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    conn = op.get_bind()
    insp = sa.inspect(conn)
    if "admin_audit_events" in set(insp.get_table_names()):
        return

    op.create_table(
        "admin_audit_events",
        sa.Column("id", UUID(as_uuid=True), nullable=False),
        sa.Column("actor_user_id", UUID(as_uuid=True), nullable=True),
        sa.Column("actor_email", sa.String(length=320), nullable=False),
        sa.Column("action", sa.String(length=64), nullable=False),
        sa.Column("target_user_id", UUID(as_uuid=True), nullable=True),
        sa.Column("target_email", sa.String(length=320), nullable=True),
        sa.Column("details", JSONB(), nullable=True),
        sa.Column(
            "created_at",
            sa.DateTime(timezone=True),
            nullable=False,
            server_default=sa.text("now()"),
        ),
        sa.ForeignKeyConstraint(["actor_user_id"], ["users.id"], ondelete="SET NULL"),
        sa.ForeignKeyConstraint(["target_user_id"], ["users.id"], ondelete="SET NULL"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(
        op.f("ix_admin_audit_events_actor_user_id"),
        "admin_audit_events",
        ["actor_user_id"],
        unique=False,
    )
    op.create_index(
        op.f("ix_admin_audit_events_action"),
        "admin_audit_events",
        ["action"],
        unique=False,
    )
    op.create_index(
        op.f("ix_admin_audit_events_target_user_id"),
        "admin_audit_events",
        ["target_user_id"],
        unique=False,
    )
    op.create_index(
        op.f("ix_admin_audit_events_created_at"),
        "admin_audit_events",
        ["created_at"],
        unique=False,
    )


def downgrade() -> None:
    op.drop_index(op.f("ix_admin_audit_events_created_at"), table_name="admin_audit_events")
    op.drop_index(op.f("ix_admin_audit_events_target_user_id"), table_name="admin_audit_events")
    op.drop_index(op.f("ix_admin_audit_events_action"), table_name="admin_audit_events")
    op.drop_index(op.f("ix_admin_audit_events_actor_user_id"), table_name="admin_audit_events")
    op.drop_table("admin_audit_events")
