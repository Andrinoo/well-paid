import uuid
from datetime import date

import pytest
from pydantic import ValidationError

from app.schemas.income import IncomeCreate


def test_income_create_rejects_non_positive_amount() -> None:
    with pytest.raises(ValidationError):
        IncomeCreate(
            description="x",
            amount_cents=0,
            income_date=date(2026, 4, 1),
            income_category_id=uuid.uuid4(),
        )


def test_income_create_without_notes() -> None:
    row = IncomeCreate(
        description="Salário abril",
        amount_cents=500_000,
        income_date=date(2026, 4, 1),
        income_category_id=uuid.uuid4(),
    )
    assert row.notes is None
