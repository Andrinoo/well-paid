"""Entitlements: superuser, free plan, trial, payments, modules.

Revision ID: 052
Revises: 051
Create Date: 2026-09-14
"""

from __future__ import annotations

import os
import uuid
from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects import postgresql

revision: str = "052"
down_revision: Union[str, Sequence[str], None] = "051"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.add_column(
        "users",
        sa.Column(
            "is_superuser",
            sa.Boolean(),
            nullable=False,
            server_default=sa.false(),
        ),
    )
    op.add_column(
        "users",
        sa.Column(
            "is_free_plan",
            sa.Boolean(),
            nullable=False,
            server_default=sa.false(),
        ),
    )
    op.add_column(
        "users",
        sa.Column("trial_ends_at", sa.DateTime(timezone=True), nullable=True),
    )
    op.execute(sa.text("UPDATE users SET is_free_plan = true"))
    email = os.environ.get("SUPERUSER_EMAIL", "").strip().lower()
    if email:
        op.execute(
            sa.text(
                "UPDATE users SET is_superuser = true WHERE lower(email) = :email"
            ).bindparams(email=email)
        )

    op.create_table(
        "payments",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column(
            "user_id",
            postgresql.UUID(as_uuid=True),
            sa.ForeignKey("users.id", ondelete="CASCADE"),
            nullable=False,
        ),
        sa.Column("amount_cents", sa.Integer(), nullable=False),
        sa.Column("currency", sa.String(length=8), nullable=False, server_default="BRL"),
        sa.Column("method", sa.String(length=32), nullable=False, server_default="pix"),
        sa.Column("status", sa.String(length=32), nullable=False, server_default="pending"),
        sa.Column("payer_name", sa.String(length=200), nullable=True),
        sa.Column("pix_id", sa.String(length=128), nullable=True),
        sa.Column("pix_copy", sa.Text(), nullable=True),
        sa.Column("pix_qr", sa.Text(), nullable=True),
        sa.Column("paid_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("due_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("pix_received_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("notes", sa.String(length=500), nullable=True),
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
    op.create_index("ix_payments_user_id", "payments", ["user_id"])
    op.create_index("ix_payments_due_at", "payments", ["due_at"])

    op.create_table(
        "user_modules",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column(
            "user_id",
            postgresql.UUID(as_uuid=True),
            sa.ForeignKey("users.id", ondelete="CASCADE"),
            nullable=False,
        ),
        sa.Column("module_id", sa.String(length=64), nullable=False),
        sa.Column("enabled", sa.Boolean(), nullable=False, server_default=sa.true()),
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
        sa.UniqueConstraint("user_id", "module_id", name="uq_user_modules_user_module"),
    )
    op.create_index("ix_user_modules_user_id", "user_modules", ["user_id"])

    conn = op.get_bind()
    users = conn.execute(sa.text("SELECT id FROM users")).fetchall()
    for row in users:
        for module_id in ("dashboard", "payables", "incomes"):
            conn.execute(
                sa.text(
                    "INSERT INTO user_modules (id, user_id, module_id, enabled) "
                    "VALUES (:id, :uid, :mid, true)"
                ),
                {"id": uuid.uuid4(), "uid": row[0], "mid": module_id},
            )


def downgrade() -> None:
    op.drop_table("user_modules")
    op.drop_table("payments")
    op.drop_column("users", "trial_ends_at")
    op.drop_column("users", "is_free_plan")
    op.drop_column("users", "is_superuser")
