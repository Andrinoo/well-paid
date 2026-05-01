from __future__ import annotations

from dataclasses import dataclass
from datetime import date
from decimal import Decimal, ROUND_HALF_UP


MONEY_QUANT = Decimal("1")
RATE_SCALE = Decimal("10000")


def bps_to_rate_decimal(bps: int) -> Decimal:
    return Decimal(bps) / RATE_SCALE


def financed_principal_from_pmt(pmt_cents: int, monthly_interest_bps: int, months: int) -> Decimal:
    if months <= 0:
        raise ValueError("months must be > 0")
    pmt = Decimal(pmt_cents)
    i = bps_to_rate_decimal(monthly_interest_bps)
    if i == 0:
        return pmt * Decimal(months)
    factor = (Decimal(1) + i) ** Decimal(months)
    return pmt * (factor - Decimal(1)) / (i * factor)


def present_value_of_single_installment(
    installment_cents: int,
    monthly_interest_bps: int,
    settlement_date: date,
    due_date: date,
) -> int:
    nominal = Decimal(installment_cents)
    if settlement_date >= due_date:
        return int(nominal.quantize(MONEY_QUANT, rounding=ROUND_HALF_UP))
    i = bps_to_rate_decimal(monthly_interest_bps)
    if i == 0:
        return int(nominal)
    days = Decimal((due_date - settlement_date).days)
    months_fraction = days / Decimal(30)
    denominator = (Decimal(1) + i) ** months_fraction
    pv = nominal / denominator
    return int(pv.quantize(MONEY_QUANT, rounding=ROUND_HALF_UP))


@dataclass(frozen=True)
class AdvanceQuote:
    nominal_amount_cents: int
    settlement_amount_cents: int
    discount_cents: int
