"""goals.reference_thumbnail_url — URL de miniatura (externo; não armazena binários)

Revision ID: 029
Revises: 028
Create Date: 2026-04-19
"""

from __future__ import annotations

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op

revision: str = "029"
down_revision: Union[str, Sequence[str], None] = "028"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.add_column(
        "goals",
        sa.Column("reference_thumbnail_url", sa.String(length=2048), nullable=True),
    )


def downgrade() -> None:
    op.drop_column("goals", "reference_thumbnail_url")
