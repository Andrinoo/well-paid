from datetime import date

from app.services.expense_finance import (
    financed_principal_from_pmt,
    present_value_of_single_installment,
)


def test_financed_principal_from_pmt_with_interest() -> None:
    principal = financed_principal_from_pmt(
        pmt_cents=140000,
        monthly_interest_bps=450,
        months=12,
    )
    assert int(round(float(principal))) == 1334875


def test_present_value_discount_for_future_due_date() -> None:
    pv = present_value_of_single_installment(
        installment_cents=140000,
        monthly_interest_bps=450,
        settlement_date=date(2026, 5, 1),
        due_date=date(2026, 6, 1),
    )
    assert pv < 140000
    assert pv == 133951
