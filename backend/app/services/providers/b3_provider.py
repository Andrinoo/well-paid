from __future__ import annotations

import logging
from dataclasses import dataclass
from typing import Any

import httpx

from app.core.config import get_settings

logger = logging.getLogger(__name__)


@dataclass
class B3Provider:
    source: str = "b3"

    def _headers(self) -> dict[str, str]:
        s = get_settings()
        token = (getattr(s, "b3_api_key", None) or "").strip()
        if not token:
            return {}
        return {"Authorization": f"Bearer {token}"}

    def _base_url(self) -> str:
        s = get_settings()
        return (getattr(s, "b3_api_base_url", None) or "").strip()

    def _enabled(self) -> bool:
        return bool(self._base_url() and self._headers())

    def search_tickers(self, query: str, limit: int = 12) -> list[dict[str, str]]:
        if not self._enabled():
            return []
        url = f"{self._base_url().rstrip('/')}/assets/search"
        params = {"q": query, "limit": max(1, min(limit, 50))}
        try:
            with httpx.Client(timeout=8.0, headers=self._headers()) as client:
                r = client.get(url, params=params)
        except Exception:
            logger.exception("B3 search request failed")
            return []
        if r.status_code != 200:
            return []
        try:
            data = r.json()
        except Exception:
            return []
        rows = data.get("results") or data.get("items") or []
        out: list[dict[str, str]] = []
        for row in rows:
            if not isinstance(row, dict):
                continue
            symbol = str(row.get("symbol") or row.get("ticker") or "").upper().strip()
            if not symbol:
                continue
            name = str(row.get("name") or row.get("companyName") or symbol).strip()
            out.append(
                {
                    "symbol": symbol,
                    "name": name,
                    "instrument_type": "stocks",
                    "source": self.source,
                    "confidence": 0.96,
                }
            )
        return out

    def quote(self, symbol: str) -> dict[str, Any] | None:
        if not self._enabled():
            return None
        url = f"{self._base_url().rstrip('/')}/assets/{symbol.upper()}/quote"
        try:
            with httpx.Client(timeout=8.0, headers=self._headers()) as client:
                r = client.get(url)
        except Exception:
            logger.exception("B3 quote request failed")
            return None
        if r.status_code != 200:
            return None
        try:
            row = r.json()
        except Exception:
            return None
        price = row.get("last") or row.get("close") or row.get("price")
        try:
            last_price = float(price)
        except (TypeError, ValueError):
            return None
        return {
            "symbol": symbol.upper(),
            "last_price": last_price,
            "as_of": row.get("asOf") or row.get("updatedAt"),
            "error": None,
            "source": self.source,
            "confidence": 0.97,
        }

    def history(self, symbol: str, range_key: str) -> dict[str, Any] | None:
        if not self._enabled():
            return None
        url = f"{self._base_url().rstrip('/')}/assets/{symbol.upper()}/history"
        params = {"range": range_key}
        try:
            with httpx.Client(timeout=10.0, headers=self._headers()) as client:
                r = client.get(url, params=params)
        except Exception:
            logger.exception("B3 history request failed")
            return None
        if r.status_code != 200:
            return None
        try:
            data = r.json()
        except Exception:
            return None
        items = data.get("points") or data.get("results") or []
        points: list[dict[str, Any]] = []
        for item in items:
            if not isinstance(item, dict):
                continue
            close_raw = item.get("close") or item.get("price")
            try:
                close = float(close_raw)
            except (TypeError, ValueError):
                continue
            points.append({"close": close, "as_of": item.get("asOf") or item.get("date")})
        return {
            "symbol": symbol.upper(),
            "range": range_key,
            "points": points,
            "error": None,
            "source": self.source,
            "confidence": 0.95,
        }

    def top_movers(self, window: str, limit: int = 10) -> list[dict[str, Any]]:
        # Placeholder: endpoint/cobertura B3 varia por contrato.
        # Mantemos interface para priorização futura sem quebrar roteador.
        return []
