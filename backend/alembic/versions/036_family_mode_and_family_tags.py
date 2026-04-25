"""family mode toggle, family tags, invite email and 24h invites

Revision ID: 036
Revises: 035
Create Date: 2026-04-25
"""

from __future__ import annotations

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op

revision: str = "036"
down_revision: Union[str, Sequence[str], None] = "035"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.add_column(
        "users",
        sa.Column(
            "family_mode_enabled",
            sa.Boolean(),
            nullable=False,
            server_default=sa.false(),
        ),
    )
    op.add_column(
        "expenses",
        sa.Column("is_family", sa.Boolean(), nullable=False, server_default=sa.false()),
    )
    op.add_column(
        "incomes",
        sa.Column("is_family", sa.Boolean(), nullable=False, server_default=sa.false()),
    )
    op.add_column(
        "goals",
        sa.Column("is_family", sa.Boolean(), nullable=False, server_default=sa.false()),
    )
    op.add_column(
        "shopping_lists",
        sa.Column("is_family", sa.Boolean(), nullable=False, server_default=sa.false()),
    )
    op.add_column(
        "family_invites",
        sa.Column("invite_email", sa.String(length=320), nullable=True),
    )

    op.create_index(op.f("ix_expenses_is_family"), "expenses", ["is_family"], unique=False)
    op.create_index(op.f("ix_incomes_is_family"), "incomes", ["is_family"], unique=False)
    op.create_index(op.f("ix_goals_is_family"), "goals", ["is_family"], unique=False)
    op.create_index(
        op.f("ix_shopping_lists_is_family"), "shopping_lists", ["is_family"], unique=False
    )
    op.create_index(
        op.f("ix_family_invites_invite_email"),
        "family_invites",
        ["invite_email"],
        unique=False,
    )


def downgrade() -> None:
    op.drop_index(op.f("ix_family_invites_invite_email"), table_name="family_invites")
    op.drop_index(op.f("ix_shopping_lists_is_family"), table_name="shopping_lists")
    op.drop_index(op.f("ix_goals_is_family"), table_name="goals")
    op.drop_index(op.f("ix_incomes_is_family"), table_name="incomes")
    op.drop_index(op.f("ix_expenses_is_family"), table_name="expenses")

    op.drop_column("family_invites", "invite_email")
    op.drop_column("shopping_lists", "is_family")
    op.drop_column("goals", "is_family")
    op.drop_column("incomes", "is_family")
    op.drop_column("expenses", "is_family")
    op.drop_column("users", "family_mode_enabled")
