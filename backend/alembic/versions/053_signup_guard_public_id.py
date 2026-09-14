"""Signup throttle by hashed IP and unique public user id.

Revision ID: 053
Revises: 052
Create Date: 2026-09-14
"""

from __future__ import annotations

import secrets
from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op

revision: str = "053"
down_revision: Union[str, Sequence[str], None] = "052"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None

_ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"


def _new_public_id() -> str:
    body = "".join(secrets.choice(_ALPHABET) for _ in range(8))
    return f"WP{body}"


def upgrade() -> None:
    op.add_column("users", sa.Column("public_id", sa.String(length=16), nullable=True))
    op.add_column(
        "users", sa.Column("signup_ip_hash", sa.String(length=64), nullable=True)
    )
    bind = op.get_bind()
    rows = bind.execute(sa.text("SELECT id FROM users WHERE public_id IS NULL")).fetchall()
    used: set[str] = set()
    for row in rows:
        pid = _new_public_id()
        while pid in used:
            pid = _new_public_id()
        used.add(pid)
        bind.execute(
            sa.text("UPDATE users SET public_id = :pid WHERE id = :id"),
            {"pid": pid, "id": row[0]},
        )
    op.alter_column("users", "public_id", existing_type=sa.String(length=16), nullable=False)
    op.create_index("ix_users_public_id", "users", ["public_id"], unique=True)
    op.create_index("ix_users_signup_ip_hash", "users", ["signup_ip_hash"], unique=False)

    op.create_table(
        "signup_attempts",
        sa.Column("ip_hash", sa.String(length=64), primary_key=True),
        sa.Column("success_count", sa.Integer(), nullable=False, server_default="0"),
        sa.Column("window_started_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("locked_until", sa.DateTime(timezone=True), nullable=True),
        sa.Column("last_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column(
            "created_at",
            sa.DateTime(timezone=True),
            server_default=sa.func.now(),
            nullable=False,
        ),
        sa.Column(
            "updated_at",
            sa.DateTime(timezone=True),
            server_default=sa.func.now(),
            nullable=False,
        ),
    )


def downgrade() -> None:
    op.drop_table("signup_attempts")
    op.drop_index("ix_users_signup_ip_hash", table_name="users")
    op.drop_index("ix_users_public_id", table_name="users")
    op.drop_column("users", "signup_ip_hash")
    op.drop_column("users", "public_id")
