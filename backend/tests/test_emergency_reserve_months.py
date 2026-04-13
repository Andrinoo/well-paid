"""Funções puras da reserva de emergência (sem BD)."""

from datetime import date

from app.services.emergency_reserve import first_of_month, iter_months_inclusive


def test_first_of_month() -> None:
    assert first_of_month(date(2026, 4, 15)) == date(2026, 4, 1)


def test_iter_months_inclusive_single_month() -> None:
    s = date(2026, 3, 1)
    e = date(2026, 3, 1)
    assert list(iter_months_inclusive(s, e)) == [(2026, 3)]


def test_iter_months_inclusive_year_boundary() -> None:
    s = date(2025, 11, 1)
    e = date(2026, 2, 1)
    assert list(iter_months_inclusive(s, e)) == [
        (2025, 11),
        (2025, 12),
        (2026, 1),
        (2026, 2),
    ]
