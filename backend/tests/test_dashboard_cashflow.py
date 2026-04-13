"""Série cashflow mensal (sem BD)."""

from datetime import date

import pytest

from app.services.dashboard_cashflow import cashflow_month_keys
from app.services.recurrence import add_months


def test_cashflow_month_keys_includes_forecast_extension() -> None:
    keys = cashflow_month_keys(date(2026, 1, 1), date(2026, 2, 1), forecast_months=1)
    assert keys == [(2026, 1), (2026, 2), (2026, 3)]


def test_cashflow_month_keys_year_rollover() -> None:
    keys = cashflow_month_keys(date(2025, 11, 1), date(2025, 12, 1), forecast_months=2)
    assert keys[-1] == (2026, 2)


def test_dynamic_window_eight_months_span() -> None:
    """Alinhado a Ordems §6.2.1: últimos 8 meses civis inclusive."""
    today_first = date(2026, 4, 1)
    hist_end = today_first
    hist_start = add_months(hist_end, -7)
    assert hist_start == date(2025, 9, 1)
    keys = cashflow_month_keys(hist_start, hist_end, forecast_months=0)
    assert len(keys) == 8
    assert keys[0] == (2025, 9)
    assert keys[-1] == (2026, 4)


def test_get_dashboard_cashflow_invalid_range_raises() -> None:
    from unittest.mock import MagicMock

    from app.services.dashboard_cashflow import get_dashboard_cashflow

    db = MagicMock()
    user = MagicMock()
    with pytest.raises(ValueError, match="cashflow_invalid_range"):
        get_dashboard_cashflow(
            db,
            user,
            dynamic=False,
            start_year=2026,
            start_month=3,
            end_year=2026,
            end_month=1,
            forecast_months=1,
        )
