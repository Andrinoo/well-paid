"""remove family scope from emergency reserve

Revision ID: 041
Revises: 040
Create Date: 2026-04-27
"""

from __future__ import annotations

from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects.postgresql import UUID


revision: str = "041"
down_revision: Union[str, Sequence[str], None] = "040"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # Backfill de planos familiares: associa ao criador da família
    # e, como fallback, ao membro owner da família.
    op.execute(
        """
        WITH family_owner AS (
            SELECT fm.family_id, fm.user_id
            FROM family_members fm
            WHERE fm.role = 'owner'
        )
        UPDATE emergency_reserve_plans p
        SET solo_user_id = COALESCE(p.solo_user_id, f.created_by_user_id, fo.user_id)
        FROM families f
        LEFT JOIN family_owner fo ON fo.family_id = f.id
        WHERE p.family_id = f.id
          AND p.solo_user_id IS NULL
        """
    )

    # Backfill de contribuições familiares para escopo solo.
    op.execute(
        """
        WITH family_owner AS (
            SELECT fm.family_id, fm.user_id
            FROM family_members fm
            WHERE fm.role = 'owner'
        )
        UPDATE emergency_reserve_contributions c
        SET solo_user_id = COALESCE(c.solo_user_id, c.created_by_user_id, f.created_by_user_id, fo.user_id)
        FROM families f
        LEFT JOIN family_owner fo ON fo.family_id = f.id
        WHERE c.family_id = f.id
          AND c.solo_user_id IS NULL
        """
    )

    # Fallback extra: deriva utilizador de um plano ligado ao item.
    op.execute(
        """
        WITH contribution_plan_user AS (
            SELECT ci.contribution_id, MIN(p.solo_user_id::text)::uuid AS inferred_user_id
            FROM emergency_reserve_contribution_items ci
            JOIN emergency_reserve_plans p ON p.id = ci.plan_id
            WHERE p.solo_user_id IS NOT NULL
            GROUP BY ci.contribution_id
        )
        UPDATE emergency_reserve_contributions c
        SET solo_user_id = cpu.inferred_user_id
        FROM contribution_plan_user cpu
        WHERE c.id = cpu.contribution_id
          AND c.solo_user_id IS NULL
        """
    )

    op.drop_constraint(
        "ck_emergency_reserve_plans_scope_xor",
        "emergency_reserve_plans",
        type_="check",
    )
    op.drop_constraint(
        "ck_emergency_reserve_contributions_scope_xor",
        "emergency_reserve_contributions",
        type_="check",
    )

    op.drop_index(
        "ix_emergency_reserve_plans_family_id",
        table_name="emergency_reserve_plans",
    )
    op.drop_index(
        "ix_emergency_reserve_contributions_family_id",
        table_name="emergency_reserve_contributions",
    )

    op.drop_constraint(
        "emergency_reserve_plans_family_id_fkey",
        "emergency_reserve_plans",
        type_="foreignkey",
    )
    op.drop_constraint(
        "emergency_reserve_contributions_family_id_fkey",
        "emergency_reserve_contributions",
        type_="foreignkey",
    )

    op.drop_column("emergency_reserve_plans", "family_id")
    op.drop_column("emergency_reserve_contributions", "family_id")


def downgrade() -> None:
    op.add_column(
        "emergency_reserve_plans",
        sa.Column("family_id", UUID(as_uuid=True), nullable=True),
    )
    op.add_column(
        "emergency_reserve_contributions",
        sa.Column("family_id", UUID(as_uuid=True), nullable=True),
    )

    op.create_foreign_key(
        "emergency_reserve_plans_family_id_fkey",
        "emergency_reserve_plans",
        "families",
        ["family_id"],
        ["id"],
        ondelete="CASCADE",
    )
    op.create_foreign_key(
        "emergency_reserve_contributions_family_id_fkey",
        "emergency_reserve_contributions",
        "families",
        ["family_id"],
        ["id"],
        ondelete="CASCADE",
    )

    op.create_index(
        "ix_emergency_reserve_plans_family_id",
        "emergency_reserve_plans",
        ["family_id"],
        unique=False,
    )
    op.create_index(
        "ix_emergency_reserve_contributions_family_id",
        "emergency_reserve_contributions",
        ["family_id"],
        unique=False,
    )

    op.create_check_constraint(
        "ck_emergency_reserve_plans_scope_xor",
        "emergency_reserve_plans",
        "(family_id IS NOT NULL AND solo_user_id IS NULL) OR "
        "(family_id IS NULL AND solo_user_id IS NOT NULL)",
    )
    op.create_check_constraint(
        "ck_emergency_reserve_contributions_scope_xor",
        "emergency_reserve_contributions",
        "(family_id IS NOT NULL AND solo_user_id IS NULL) OR "
        "(family_id IS NULL AND solo_user_id IS NOT NULL)",
    )
