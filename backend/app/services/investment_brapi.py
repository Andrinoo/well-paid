"""Integrações BRAPI para investimentos (cotação, busca e histórico)."""

from __future__ import annotations

import logging
from datetime import datetime, timezone
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


def search_tickers_brapi(query: str, *, limit: int = 12) -> list[dict[str, str]]:
    q = (query or "").strip()
    if len(q) < 2:
        return []
    token = (get_settings().brapi_api_key or "").strip()
    params: dict[str, str] = {"search": q, "sortBy": "symbol", "sortOrder": "asc"}
    if token:
        params["token"] = token
    try:
        with httpx.Client(timeout=12.0, headers={"User-Agent": _USER_AGENT}) as client:
            r = client.get(f"{_BRAPI_BASE}/quote/list", params=params)
    except Exception:
        logger.exception("BRAPI ticker search failed for %s", q)
        return []
    if r.status_code != 200:
        logger.warning("BRAPI ticker search HTTP %s for %s", r.status_code, q)
        return []
    try:
        data = r.json()
    except Exception:
        return []
    rows = data.get("stocks") or data.get("results") or []
    out: list[dict[str, str]] = []
    for raw in rows:
        if not isinstance(raw, dict):
            continue
        symbol = str(raw.get("stock") or raw.get("symbol") or "").strip().upper()
        if not symbol:
            continue
        name = str(raw.get("name") or raw.get("longName") or raw.get("shortName") or symbol).strip()
        if len(symbol) < 5:
            continue
        out.append({"symbol": symbol, "name": name})
        if len(out) >= max(1, min(limit, 50)):
            break
    return out


def _map_range_to_brapi(range_key: str) -> tuple[str, str]:
    key = (range_key or "").strip().lower()
    mapping = {
        "5m": ("1d", "5m"),
        "30m": ("5d", "30m"),
        "60m": ("5d", "60m"),
        "3h": ("1mo", "1h"),
        "12h": ("1mo", "1h"),
        "1d": ("5d", "1d"),
        "1w": ("1mo", "1d"),
        "1m": ("1mo", "1d"),
        "3m": ("3mo", "1d"),
        "6m": ("6mo", "1d"),
        "1y": ("1y", "1d"),
    }
    return mapping.get(key, ("3mo", "1d"))


def fetch_stock_history_brapi(symbol: str, range_key: str) -> dict[str, Any] | None:
    s = (symbol or "").strip().upper()
    if not s:
        return None
    brapi_range, interval = _map_range_to_brapi(range_key)
    token = (get_settings().brapi_api_key or "").strip()
    params: dict[str, str] = {"range": brapi_range, "interval": interval}
    if token:
        params["token"] = token
    try:
        with httpx.Client(timeout=14.0, headers={"User-Agent": _USER_AGENT}) as client:
            r = client.get(f"{_BRAPI_BASE}/quote/{s}", params=params)
    except Exception:
        logger.exception("BRAPI history failed for %s", s)
        return None
    if r.status_code != 200:
        logger.warning("BRAPI history HTTP %s for %s", r.status_code, s)
        return {"symbol": s, "range": range_key, "points": [], "raw_error": f"http_{r.status_code}"}
    try:
        data = r.json()
    except Exception:
        return None
    rows = data.get("results") or []
    if not rows:
        return {"symbol": s, "range": range_key, "points": [], "raw_error": "empty_results"}
    row = rows[0] if isinstance(rows[0], dict) else {}
    history = row.get("historicalDataPrice") or []
    points: list[dict[str, Any]] = []
    for item in history:
        if not isinstance(item, dict):
            continue
        close_raw = item.get("close")
        ts_raw = item.get("date")
        try:
            close = float(close_raw)
            if close <= 0:
                continue
        except (TypeError, ValueError):
            continue
        as_of = None
        if isinstance(ts_raw, (int, float)):
            as_of = datetime.fromtimestamp(float(ts_raw), tz=timezone.utc).isoformat()
        elif ts_raw is not None:
            as_of = str(ts_raw)
        points.append({"close": close, "as_of": as_of})
    return {"symbol": s, "range": range_key, "points": points, "raw_error": None}
