"""Cotação de ações B3 via brapi.dev (alinhado a docs/stock.py)."""

from __future__ import annotations

import logging
from typing import Any

import httpx

from app.core.config import get_settings

logger = logging.getLogger(__name__)

_BRAPI_BASE = "https://brapi.dev/api"
_USER_AGENT = "WellPaid/1.0 brapi-quote"


def fetch_stock_quote_brapi(symbol: str) -> dict[str, Any] | None:
    """
    Retorna { last_price, as_of, raw_error } ou None se falha de rede/HTTP.
    """
    s = (symbol or "").strip().upper()
    if not s or len(s) > 12:
        return None
    token = (get_settings().brapi_api_key or "").strip()
    url = f"{_BRAPI_BASE}/quote/{s}"
    params: dict[str, str] = {}
    if token:
        params["token"] = token
    try:
        with httpx.Client(timeout=12.0, headers={"User-Agent": _USER_AGENT}) as client:
            r = client.get(url, params=params or None)
    except Exception:
        logger.exception("BRAPI request failed for %s", s)
        return None
    if r.status_code != 200:
        logger.warning("BRAPI HTTP %s for %s", r.status_code, s)
        return {"last_price": None, "as_of": None, "raw_error": f"http_{r.status_code}"}
    try:
        data = r.json()
    except Exception:
        return None
    results = data.get("results") or []
    if not results:
        return {"last_price": None, "as_of": None, "raw_error": "empty_results"}
    row = results[0] if isinstance(results[0], dict) else {}
    raw = (
        row.get("regularMarketPrice")
        or row.get("lastPrice")
        or row.get("close")
    )
    try:
        last = float(raw) if raw is not None else 0.0
    except (TypeError, ValueError):
        last = 0.0
    if last <= 0:
        return {"last_price": None, "as_of": None, "raw_error": "no_price"}
    # Datas variam; não bloquear se ausente
    dref = (
        row.get("regularMarketTime")
        or row.get("updatedAt")
        or row.get("date")
    )
    as_of = str(dref) if dref is not None else None
    return {"last_price": last, "as_of": as_of, "raw_error": None}
