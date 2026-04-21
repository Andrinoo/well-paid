from __future__ import annotations

from datetime import datetime, timedelta, timezone
import logging

import httpx

from app.core.config import get_settings

logger = logging.getLogger(__name__)

_BACEN_SGS_BASE = "https://api.bcb.gov.br/dados/serie/bcdata.sgs"
_USER_AGENT = "WellPaid/1.0 investments-market-rates"

_cache_expires_at: datetime | None = None
_cache_payload: dict[str, float | bool | str] | None = None


def _safe_float(value: object) -> float | None:
    if isinstance(value, (int, float)):
        return float(value)
    if isinstance(value, str):
        v = value.strip().replace(",", ".")
        try:
            return float(v)
        except ValueError:
            return None
    return None


def _fetch_cdi_daily_pct(series_id: int, timeout_s: float = 8.0) -> float | None:
    url = f"{_BACEN_SGS_BASE}/{series_id}/dados/ultimos/1"
    try:
        with httpx.Client(timeout=timeout_s, headers={"User-Agent": _USER_AGENT}) as client:
            response = client.get(url, params={"formato": "json"})
            if response.status_code != 200:
                logger.warning("CDI source HTTP status=%s", response.status_code)
                return None
            payload = response.json()
    except Exception:
        logger.exception("CDI source request failed")
        return None

    if not isinstance(payload, list) or not payload:
        return None
    row = payload[-1]
    if not isinstance(row, dict):
        return None
    raw = row.get("valor")
    val = _safe_float(raw)
    if val is None or val <= 0:
        return None
    return val


def get_market_rates_snapshot() -> dict[str, float | bool | str]:
    """
    Returns monthly rates in decimal form.
    - cdi_monthly: e.g. 0.0091 means 0.91%/month
    - cdb_monthly: cdi_monthly adjusted by settings factor
    - fixed_monthly: conservative fixed-income baseline
    """
    global _cache_expires_at, _cache_payload

    now = datetime.now(timezone.utc)
    if _cache_expires_at and _cache_payload and now < _cache_expires_at:
        return _cache_payload

    settings = get_settings()
    cdi_daily_pct = _fetch_cdi_daily_pct(settings.investments_cdi_sgs_series)

    # Fallback defensivo para manter o endpoint estável mesmo sem fonte externa.
    cdi_daily_decimal = ((cdi_daily_pct or 0.043) / 100.0)
    cdi_monthly = (1.0 + cdi_daily_decimal) ** 21 - 1.0
    cdb_monthly = cdi_monthly * float(settings.investments_cdb_pct_of_cdi)
    fixed_monthly = max(cdi_monthly * 0.9, 0.0045)

    fallback_used = cdi_daily_pct is None
    payload = {
        "cdi_monthly": cdi_monthly,
        "cdb_monthly": cdb_monthly,
        "fixed_monthly": fixed_monthly,
        "source": "bacen_sgs" if not fallback_used else "fallback_default",
        "fallback_used": fallback_used,
    }
    ttl = max(60, int(settings.investments_rates_cache_ttl_seconds))
    _cache_payload = payload
    _cache_expires_at = now + timedelta(seconds=ttl)
    return payload
