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


def _format_pt_decimal2(value: Any) -> str | None:
    """Número API → string com vírgula (ex. EV/EBITDA), ou None se inválido."""
    if value is None:
        return None
    try:
        v = float(value)
    except (TypeError, ValueError):
        return None
    s = f"{v:.2f}".replace(".", ",")
    return s


def _net_debt_to_ebitda_from_financial_data(fin: Any) -> str | None:
    """
    Dívida líquida / EBITDA ≈ (totalDebt - totalCash) / ebitda (Yahoo financialData).
    O Fundamentus muitas vezes não publica a linha «Dív. Líquida/EBITDA» no HTML.
    """
    if not isinstance(fin, dict):
        return None
    try:
        total_debt = float(fin.get("totalDebt") or 0)
        total_cash = float(fin.get("totalCash") or 0)
        ebitda = float(fin.get("ebitda") or 0)
    except (TypeError, ValueError):
        return None
    if ebitda <= 0:
        return None
    net_debt = total_debt - total_cash
    ratio = net_debt / ebitda
    return _format_pt_decimal2(ratio)


def fetch_brapi_key_statistics_enrichment(symbol: str) -> dict[str, Any] | None:
    """
    Uma chamada: GET /quote/{SYM}?modules=defaultKeyStatistics,financialData

    - defaultKeyStatistics.enterpriseToEbitda → EV/EBITDA
    - financialData (totalDebt, totalCash, ebitda) → dív. líq. / EBITDA
    - Nome: longName / shortName

    Retorno: ev_ebitda, net_debt_ebitda, company_name; None se a resposta for inútil.
    """
    s = (symbol or "").strip().upper()
    if not s or len(s) > 12:
        return None
    token = (get_settings().brapi_api_key or "").strip()
    params: dict[str, str] = {
        "modules": "defaultKeyStatistics,financialData,summaryDetail",
    }
    if token:
        params["token"] = token
    try:
        with httpx.Client(timeout=16.0, headers={"User-Agent": _USER_AGENT}) as client:
            r = client.get(f"{_BRAPI_BASE}/quote/{s}", params=params)
    except Exception:
        logger.exception("BRAPI fundamental modules request failed for %s", s)
        return None
    if r.status_code != 200:
        logger.warning("BRAPI fundamental modules HTTP %s for %s", r.status_code, s)
        return None
    try:
        data = r.json()
    except Exception:
        return None
    results = data.get("results") or []
    if not results or not isinstance(results[0], dict):
        return None
    row = results[0]
    name = str(
        row.get("longName") or row.get("shortName") or row.get("name") or ""
    ).strip()
    dks = row.get("defaultKeyStatistics")
    ev: str | None = None
    if isinstance(dks, dict):
        ev = _format_pt_decimal2(dks.get("enterpriseToEbitda"))
    fin = row.get("financialData")
    net_debt_e: str | None = _net_debt_to_ebitda_from_financial_data(fin)
    sd = row.get("summaryDetail")
    pvp: str | None = None
    dy: str | None = None
    if isinstance(sd, dict):
        pvp = _format_pt_decimal2(sd.get("priceToBook"))
        # Convert ratio to percentage when BRAPI returns 0.x.
        raw_dy = sd.get("dividendYield")
        try:
            if raw_dy is not None:
                dy_val = float(raw_dy)
                dy = _format_pt_decimal2(dy_val * 100.0 if dy_val <= 1 else dy_val)
        except (TypeError, ValueError):
            dy = None
    # Some instruments (especially FIIs/ETFs/BDRs) expose these fields at root level.
    if pvp is None:
        pvp = _format_pt_decimal2(row.get("priceToBook"))
    if dy is None:
        try:
            root_dy = row.get("dividendYield")
            if root_dy is not None:
                root_dy_val = float(root_dy)
                dy = _format_pt_decimal2(root_dy_val * 100.0 if root_dy_val <= 1 else root_dy_val)
        except (TypeError, ValueError):
            dy = None

    if not name and not ev and not net_debt_e and not pvp and not dy:
        return None
    return {
        "ev_ebitda": ev,
        "net_debt_ebitda": net_debt_e,
        "pvp": pvp,
        "dividend_yield": dy,
        "company_name": name or None,
    }


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


def fetch_stock_quote_snapshot_brapi(symbol: str) -> dict[str, Any] | None:
    """
    Retorna snapshot mais completo para rankings:
    { symbol, name, last_price, change_percent, volume, as_of, raw_error }
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
        logger.exception("BRAPI snapshot request failed for %s", s)
        return None
    if r.status_code != 200:
        return None
    try:
        data = r.json()
    except Exception:
        return None
    results = data.get("results") or []
    if not results:
        return None
    row = results[0] if isinstance(results[0], dict) else {}
    raw_price = row.get("regularMarketPrice") or row.get("lastPrice") or row.get("close")
    try:
        price = float(raw_price) if raw_price is not None else 0.0
    except (TypeError, ValueError):
        price = 0.0
    raw_cp = row.get("regularMarketChangePercent") or row.get("changePercent")
    try:
        change_percent = float(raw_cp) if raw_cp is not None else None
    except (TypeError, ValueError):
        change_percent = None
    raw_vol = row.get("regularMarketVolume") or row.get("volume")
    try:
        volume = float(raw_vol) if raw_vol is not None else None
    except (TypeError, ValueError):
        volume = None
    dref = row.get("regularMarketTime") or row.get("updatedAt") or row.get("date")
    as_of = str(dref) if dref is not None else None
    name = str(row.get("shortName") or row.get("longName") or row.get("name") or s).strip()
    return {
        "symbol": s,
        "name": name,
        "last_price": price,
        "change_percent": change_percent,
        "volume": volume,
        "as_of": as_of,
        "raw_error": None if price > 0 else "no_price",
    }


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
