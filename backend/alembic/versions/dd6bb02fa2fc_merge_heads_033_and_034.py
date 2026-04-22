"""merge heads 033 and 034

Revision ID: dd6bb02fa2fc
Revises: 033, 034
Create Date: 2026-04-22 17:47:57.303707

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'dd6bb02fa2fc'
down_revision: Union[str, Sequence[str], None] = ('033', '034')
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    pass


def downgrade() -> None:
    pass
