from datetime import date

import pytest

from app.services.recurrence import add_months, iter_occurrence_dates, next_occurrence_date


def test_add_months_clamps_end_of_month() -> None:
    assert add_months(date(2026, 1, 31), 1) == date(2026, 2, 28)


def test_next_occurrence_date_weekly() -> None:
    assert next_occurrence_date(date(2026, 4, 8), "weekly") == date(2026, 4, 15)


def test_iter_occurrence_dates_monthly_until_limit() -> None:
    out = iter_occurrence_dates(
        start_from=date(2026, 4, 8),
        frequency="monthly",
        until=date(2026, 7, 31),
    )
    assert out == [date(2026, 5, 8), date(2026, 6, 8), date(2026, 7, 8)]


def test_next_occurrence_date_rejects_invalid_frequency() -> None:
    with pytest.raises(ValueError):
        next_occurrence_date(date(2026, 4, 8), "daily")
