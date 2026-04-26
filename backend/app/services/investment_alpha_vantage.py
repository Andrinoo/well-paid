"""Integração Alpha Vantage para cotações de criptoativos."""

from __future__ import annotations

import logging
from typing import Any

import httpx

from app.core.config import get_settings

logger = logging.getLogger(__name__)

_ALPHA_VANTAGE_URL = "https://www.alphavantage.co/query"
_USER_AGENT = "WellPaid/1.0 alpha-vantage-crypto"


def _alpha_vantage_key() -> str:
    return (get_settings().alpha_vantage_api_key or "").strip()


def fetch_crypto_quote_alpha_vantage(symbol: str, *, market: str = "USD") -> dict[str, Any] | None:
    """Retorna cotação cripto em tempo real (BTC/ETH/etc) via CURRENCY_EXCHANGE_RATE."""
    base = (symbol or "").strip().upper()
    to = (market or "USD").strip().upper()
    if len(base) < 2 or len(base) > 10:
        return None
    key = _alpha_vantage_key()
    if not key:
        return None
    params = {
        "function": "CURRENCY_EXCHANGE_RATE",
        "from_currency": base,
        "to_currency": to,
        "apikey": key,
    }
    try:
        with httpx.Client(timeout=12.0, headers={"User-Agent": _USER_AGENT}) as client:
            r = client.get(_ALPHA_VANTAGE_URL, params=params)
    except Exception:
        logger.exception("Alpha Vantage crypto quote request failed for %s", base)
        return None
    if r.status_code != 200:
        logger.warning("Alpha Vantage crypto quote HTTP %s for %s", r.status_code, base)
        return {"last_price": None, "as_of": None, "raw_error": f"http_{r.status_code}"}
    try:
        data = r.json()
    except Exception:
        return None
    if not isinstance(data, dict):
        return None
    if data.get("Error Message"):
        return {"last_price": None, "as_of": None, "raw_error": "alpha_vantage_error"}
    if data.get("Note"):
        # Normalmente rate limit.
        return {"last_price": None, "as_of": None, "raw_error": "alpha_vantage_rate_limit"}
    block = data.get("Realtime Currency Exchange Rate") or {}
    if not isinstance(block, dict):
        return {"last_price": None, "as_of": None, "raw_error": "empty_results"}
    raw_price = block.get("5. Exchange Rate")
    as_of = block.get("6. Last Refreshed")
    try:
        price = float(raw_price) if raw_price is not None else 0.0
    except (TypeError, ValueError):
        price = 0.0
    if price <= 0:
        return {"last_price": None, "as_of": str(as_of) if as_of else None, "raw_error": "no_price"}
    return {
        "last_price": price,
        "as_of": str(as_of) if as_of else None,
        "raw_error": None,
    }
