"""Integração Finnhub para cotações cripto."""

from __future__ import annotations

import logging
from typing import Any

import httpx

from app.core.config import get_settings

logger = logging.getLogger(__name__)

_FINNHUB_URL = "https://finnhub.io/api/v1/quote"
_USER_AGENT = "WellPaid/1.0 finnhub-crypto"


def _finnhub_key() -> str:
    return (get_settings().finnhub_api_key or "").strip()


def fetch_crypto_quote_finnhub(symbol: str, *, quote_currency: str = "USD") -> dict[str, Any] | None:
    """Retorna preço atual via Finnhub usando símbolo cripto (ex.: BINANCE:BTCUSDT)."""
    base = (symbol or "").strip().upper()
    quote = (quote_currency or "USD").strip().upper()
    if len(base) < 2 or len(base) > 10:
        return None
    key = _finnhub_key()
    if not key:
        return None
    pair = "USDT" if quote == "USD" else quote
    params = {
        "symbol": f"BINANCE:{base}{pair}",
        "token": key,
    }
    try:
        with httpx.Client(timeout=10.0, headers={"User-Agent": _USER_AGENT}) as client:
            r = client.get(_FINNHUB_URL, params=params)
    except Exception:
        logger.exception("Finnhub crypto quote request failed for %s", base)
        return None
    if r.status_code != 200:
        return {"last_price": None, "as_of": None, "raw_error": f"http_{r.status_code}"}
    try:
        data = r.json()
    except Exception:
        return None
    if not isinstance(data, dict):
        return None
    # Finnhub quote: c=current, d=change, dp=percent, h/l high/low, t=timestamp
    raw_price = data.get("c")
    try:
        price = float(raw_price) if raw_price is not None else 0.0
    except (TypeError, ValueError):
        price = 0.0
    if price <= 0:
        return {"last_price": None, "as_of": None, "raw_error": "no_price"}
    ts = data.get("t")
    as_of = str(ts) if ts is not None else None

    def _opt_float(key: str) -> float | None:
        v = data.get(key)
        if v is None:
            return None
        try:
            return float(v)
        except (TypeError, ValueError):
            return None

    return {
        "last_price": price,
        "as_of": as_of,
        "raw_error": None,
        "currency": "USD",
        "change_24h": _opt_float("d"),
        "change_24h_percent": _opt_float("dp"),
        "day_high": _opt_float("h"),
        "day_low": _opt_float("l"),
        "volume_24h": _opt_float("v"),
    }
