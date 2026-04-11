import pytest

from app.services.shopping_list_totals import (
    resolve_checkout_total_cents,
    sum_line_extensions_cents,
)


def test_sum_line_extensions_skips_null_unit() -> None:
    assert sum_line_extensions_cents([(100, 1), (None, 5), (200, 1)]) == 300
    assert sum_line_extensions_cents([(None, 3), (None, 1)]) == 0


def test_sum_line_extensions_multiplies_quantity() -> None:
    assert sum_line_extensions_cents([(50, 3)]) == 150
    assert sum_line_extensions_cents([(100, 2), (25, 4)]) == 300


def test_resolve_uses_override() -> None:
    assert (
        resolve_checkout_total_cents(
            line_extensions=[(None, 1), (100, 1)],
            total_cents_override=5000,
        )
        == 5000
    )


def test_resolve_sums_when_no_override() -> None:
    assert (
        resolve_checkout_total_cents(
            line_extensions=[(100, 1), (200, 1)],
            total_cents_override=None,
        )
        == 300
    )


def test_resolve_empty_raises() -> None:
    with pytest.raises(ValueError, match="shopping_list_total_empty"):
        resolve_checkout_total_cents(
            line_extensions=[(None, 2), (None, 1)],
            total_cents_override=None,
        )


def test_resolve_invalid_override_raises() -> None:
    with pytest.raises(ValueError, match="shopping_list_total_invalid"):
        resolve_checkout_total_cents(
            line_extensions=[(100, 1)],
            total_cents_override=0,
        )
