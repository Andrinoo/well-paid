"""emergency_reserve_plans.opening_balance_cents — saldo = aporte inicial + aportes reais

Revision ID: 033
Revises: 032
Create Date: 2026-04-21
"""

from __future__ import annotations

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op

revision: str = "033"
down_revision: Union[str, Sequence[str], None] = "032"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.add_column(
        "emergency_reserve_plans",
        sa.Column(
            "opening_balance_cents",
            sa.BigInteger(),
            nullable=False,
            server_default="0",
        ),
    )
    # Saldo passava a incluir créditos de acréscimo automático; opening = saldo antigo − aportes reais.
    op.execute(
        """
        UPDATE emergency_reserve_plans p
        SET opening_balance_cents = p.balance_cents - COALESCE(
            (
                SELECT SUM(i.amount_cents)::bigint
                FROM emergency_reserve_contribution_items i
                WHERE i.plan_id = p.id
            ),
            0
        )
        """
    )
    op.alter_column(
        "emergency_reserve_plans",
        "opening_balance_cents",
        server_default=None,
    )


def downgrade() -> None:
    op.drop_column("emergency_reserve_plans", "opening_balance_cents")
