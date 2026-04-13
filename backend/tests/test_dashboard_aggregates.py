"""Cálculos de agregação do dashboard (sem BD)."""

import calendar
from datetime import date

import pytest

from app.services.dashboard import (
    category_spends_from_rows,
    month_bounds,
    share_basis_points,
)


def test_month_bounds_january() -> None:
    start, end = month_bounds(2026, 1)
    assert start == date(2026, 1, 1)
    assert end == date(2026, 1, 31)


def test_month_bounds_february_leap() -> None:
    start, end = month_bounds(2024, 2)
    assert end == date(2024, 2, 29)
    assert calendar.monthrange(2024, 2)[1] == 29


def test_share_basis_points_total_zero() -> None:
    assert share_basis_points(100, 0) is None
    assert share_basis_points(0, 100) is None


def test_share_basis_points_integer_cents() -> None:
    assert share_basis_points(2500, 10000) == 2500
    assert share_basis_points(1, 3) == 3333  # truncagem intencional


def test_category_spends_skips_non_positive() -> None:
    rows = [("a", "A", 0), ("b", "B", 100)]
    out = category_spends_from_rows(rows, 100)
    assert len(out) == 1
    assert out[0].category_key == "b"
    assert out[0].amount_cents == 100
    assert out[0].share_bps == 10000


def test_category_spends_share_bps_sum_under_100_percent_when_truncation() -> None:
    rows = [("a", "A", 100), ("b", "B", 100), ("c", "C", 100)]
    out = category_spends_from_rows(rows, 300)
    bps = [x.share_bps for x in out if x.share_bps is not None]
    assert sum(bps) <= 10000
    assert all(b == 3333 for b in bps)
