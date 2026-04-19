"""emergency_reserve_plans (multi-plano) + goals URL/preço; accruals por plano

Revision ID: 028
Revises: 027
Create Date: 2026-04-19
"""

from __future__ import annotations

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects.postgresql import JSONB, UUID

revision: str = "028"
down_revision: Union[str, Sequence[str], None] = "027"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        "emergency_reserve_plans",
        sa.Column("id", UUID(as_uuid=True), primary_key=True, nullable=False),
        sa.Column("family_id", UUID(as_uuid=True), nullable=True),
        sa.Column("solo_user_id", UUID(as_uuid=True), nullable=True),
        sa.Column("title", sa.String(length=200), nullable=False, server_default=""),
        sa.Column("monthly_target_cents", sa.BigInteger(), nullable=False, server_default="0"),
        sa.Column("balance_cents", sa.BigInteger(), nullable=False, server_default="0"),
        sa.Column("tracking_start", sa.Date(), nullable=False),
        sa.Column(
            "accrual_skip_months",
            JSONB(),
            nullable=False,
            server_default=sa.text("'[]'::jsonb"),
        ),
        sa.Column("plan_duration_months", sa.Integer(), nullable=True),
        sa.Column(
            "status",
            sa.String(length=20),
            nullable=False,
            server_default="active",
        ),
        sa.Column("completed_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("transfer_goal_id", UUID(as_uuid=True), nullable=True),
        sa.Column("transfer_to_plan_id", UUID(as_uuid=True), nullable=True),
        sa.Column("legacy_reserve_id", UUID(as_uuid=True), nullable=True),
        sa.Column(
            "created_at",
            sa.DateTime(timezone=True),
            server_default=sa.text("now()"),
            nullable=False,
        ),
        sa.Column(
            "updated_at",
            sa.DateTime(timezone=True),
            server_default=sa.text("now()"),
            nullable=False,
        ),
        sa.CheckConstraint(
            "(family_id IS NOT NULL AND solo_user_id IS NULL) OR "
            "(family_id IS NULL AND solo_user_id IS NOT NULL)",
            name="ck_emergency_reserve_plans_scope_xor",
        ),
        sa.ForeignKeyConstraint(["family_id"], ["families.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["solo_user_id"], ["users.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["transfer_goal_id"], ["goals.id"], ondelete="SET NULL"),
        sa.ForeignKeyConstraint(
            ["transfer_to_plan_id"],
            ["emergency_reserve_plans.id"],
            ondelete="SET NULL",
        ),
    )
    op.create_index(
        "ix_emergency_reserve_plans_family_id",
        "emergency_reserve_plans",
        ["family_id"],
        unique=False,
    )
    op.create_index(
        "ix_emergency_reserve_plans_solo_user_id",
        "emergency_reserve_plans",
        ["solo_user_id"],
        unique=False,
    )
    op.create_index(
        "ix_emergency_reserve_plans_legacy_reserve_id",
        "emergency_reserve_plans",
        ["legacy_reserve_id"],
        unique=True,
    )

    op.execute(
        """
        INSERT INTO emergency_reserve_plans (
            id, family_id, solo_user_id, title, monthly_target_cents, balance_cents,
            tracking_start, accrual_skip_months, plan_duration_months, status,
            legacy_reserve_id, created_at, updated_at
        )
        SELECT
            gen_random_uuid(),
            family_id,
            solo_user_id,
            'Legacy',
            monthly_target_cents,
            balance_cents,
            tracking_start,
            COALESCE(accrual_skip_months, '[]'::jsonb),
            NULL,
            'active',
            id,
            created_at,
            updated_at
        FROM emergency_reserves
        """
    )

    op.add_column(
        "emergency_reserve_accruals",
        sa.Column("plan_id", UUID(as_uuid=True), nullable=True),
    )

    op.execute(
        """
        UPDATE emergency_reserve_accruals AS a
        SET plan_id = p.id
        FROM emergency_reserve_plans AS p
        WHERE p.legacy_reserve_id = a.reserve_id
        """
    )

    op.drop_constraint(
        "emergency_reserve_accruals_reserve_id_fkey",
        "emergency_reserve_accruals",
        type_="foreignkey",
    )
    op.drop_constraint(
        "uq_emergency_reserve_accruals_reserve_year_month",
        "emergency_reserve_accruals",
        type_="unique",
    )
    op.drop_index("ix_emergency_reserve_accruals_reserve_id", table_name="emergency_reserve_accruals")
    op.drop_column("emergency_reserve_accruals", "reserve_id")

    op.alter_column("emergency_reserve_accruals", "plan_id", nullable=False)

    op.create_foreign_key(
        "emergency_reserve_accruals_plan_id_fkey",
        "emergency_reserve_accruals",
        "emergency_reserve_plans",
        ["plan_id"],
        ["id"],
        ondelete="CASCADE",
    )
    op.create_unique_constraint(
        "uq_emergency_reserve_accruals_plan_year_month",
        "emergency_reserve_accruals",
        ["plan_id", "year", "month"],
    )
    op.create_index(
        "ix_emergency_reserve_accruals_plan_id",
        "emergency_reserve_accruals",
        ["plan_id"],
        unique=False,
    )

    op.drop_index(
        "ix_emergency_reserve_plans_legacy_reserve_id",
        table_name="emergency_reserve_plans",
    )
    op.drop_column("emergency_reserve_plans", "legacy_reserve_id")

    op.drop_table("emergency_reserves")

    op.add_column("goals", sa.Column("target_url", sa.String(length=2048), nullable=True))
    op.add_column(
        "goals",
        sa.Column("reference_product_name", sa.String(length=500), nullable=True),
    )
    op.add_column(
        "goals",
        sa.Column("reference_price_cents", sa.BigInteger(), nullable=True),
    )
    op.add_column(
        "goals",
        sa.Column("reference_currency", sa.String(length=8), nullable=False, server_default="BRL"),
    )
    op.add_column(
        "goals",
        sa.Column("price_checked_at", sa.DateTime(timezone=True), nullable=True),
    )
    op.add_column(
        "goals",
        sa.Column("price_source", sa.String(length=32), nullable=True),
    )
    op.add_column(
        "goals",
        sa.Column(
            "price_alternatives",
            JSONB(),
            nullable=False,
            server_default=sa.text("'[]'::jsonb"),
        ),
    )


def downgrade() -> None:
    raise NotImplementedError(
        "028 is not safely reversible (multi-plan + accrual FK); restore from backup if needed."
    )
