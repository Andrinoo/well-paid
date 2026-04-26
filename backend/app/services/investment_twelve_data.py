"""Integração Twelve Data para cotações (foco em cripto)."""

from __future__ import annotations

import logging
from typing import Any

import httpx

from app.core.config import get_settings

logger = logging.getLogger(__name__)

_TWELVE_DATA_URL = "https://api.twelvedata.com/price"
_USER_AGENT = "WellPaid/1.0 twelve-data-crypto"


def _twelve_data_key() -> str:
    return (get_settings().twelve_data_api_key or "").strip()


def fetch_crypto_quote_twelve_data(symbol: str, *, quote_currency: str = "USD") -> dict[str, Any] | None:
    """Retorna preço spot via Twelve Data no formato SYMBOL/QUOTE (ex.: BTC/USD)."""
    base = (symbol or "").strip().upper()
    quote = (quote_currency or "USD").strip().upper()
    if len(base) < 2 or len(base) > 10:
        return None
    key = _twelve_data_key()
    if not key:
        return None
    params = {
        "symbol": f"{base}/{quote}",
        "apikey": key,
    }
    try:
        with httpx.Client(timeout=10.0, headers={"User-Agent": _USER_AGENT}) as client:
            r = client.get(_TWELVE_DATA_URL, params=params)
    except Exception:
        logger.exception("Twelve Data crypto quote request failed for %s", base)
        return None
    if r.status_code != 200:
        return {"last_price": None, "as_of": None, "raw_error": f"http_{r.status_code}"}
    try:
        data = r.json()
    except Exception:
        return None
    if not isinstance(data, dict):
        return None
    if data.get("status") == "error":
        code = str(data.get("code") or "twelve_data_error").strip()
        return {"last_price": None, "as_of": None, "raw_error": code}
    raw_price = data.get("price")
    try:
        price = float(raw_price) if raw_price is not None else 0.0
    except (TypeError, ValueError):
        price = 0.0
    if price <= 0:
        return {"last_price": None, "as_of": None, "raw_error": "no_price"}
    return {"last_price": price, "as_of": None, "raw_error": None}
