"""Helpers de recorrência para despesas."""

from __future__ import annotations

import calendar
from datetime import date, timedelta


def add_months(d: date, delta: int) -> date:
    month = d.month - 1 + delta
    year = d.year + month // 12
    month = month % 12 + 1
    day = min(d.day, calendar.monthrange(year, month)[1])
    return date(year, month, day)


def next_occurrence_date(current: date, frequency: str) -> date:
    if frequency == "weekly":
        return current + timedelta(days=7)
    if frequency == "monthly":
        return add_months(current, 1)
    if frequency == "yearly":
        return add_months(current, 12)
    raise ValueError("Frequência inválida")


def iter_occurrence_dates(
    *,
    start_from: date,
    frequency: str,
    until: date,
) -> list[date]:
    out: list[date] = []
    cur = start_from
    while True:
        cur = next_occurrence_date(cur, frequency)
        if cur > until:
            break
        out.append(cur)
    return out
