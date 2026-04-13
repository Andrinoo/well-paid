import uuid
from datetime import date

import pytest
from pydantic import ValidationError

from app.schemas.expense import ExpenseCreate, ExpenseUpdate


def test_expense_create_rejects_non_positive_amount() -> None:
    with pytest.raises(ValidationError):
        ExpenseCreate(
            description="x",
            amount_cents=0,
            expense_date=date(2026, 4, 1),
            category_id=uuid.uuid4(),
        )


def test_expense_update_allows_partial() -> None:
    u = ExpenseUpdate(description="novo")
    assert u.description == "novo"
    assert u.amount_cents is None
