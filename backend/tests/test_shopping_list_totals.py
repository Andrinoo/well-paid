import pytest

from app.services.shopping_list_totals import resolve_checkout_total_cents, sum_line_amounts_cents


def test_sum_line_amounts_skips_null() -> None:
    assert sum_line_amounts_cents([100, None, 200]) == 300
    assert sum_line_amounts_cents([None, None]) == 0


def test_resolve_uses_override() -> None:
    assert (
        resolve_checkout_total_cents(line_amounts=[None, 100], total_cents_override=5000)
        == 5000
    )


def test_resolve_sums_when_no_override() -> None:
    assert resolve_checkout_total_cents(line_amounts=[100, 200], total_cents_override=None) == 300


def test_resolve_empty_raises() -> None:
    with pytest.raises(ValueError, match="shopping_list_total_empty"):
        resolve_checkout_total_cents(line_amounts=[None, None], total_cents_override=None)


def test_resolve_invalid_override_raises() -> None:
    with pytest.raises(ValueError, match="shopping_list_total_invalid"):
        resolve_checkout_total_cents(line_amounts=[100], total_cents_override=0)
