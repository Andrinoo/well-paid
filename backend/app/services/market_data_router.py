from __future__ import annotations

import re
from dataclasses import dataclass, field
from typing import Any

from app.services.providers.b3_provider import B3Provider
from app.services.providers.brapi_provider import BrapiProvider
from app.services.providers.fundamentus_provider import FundamentusProvider
from app.services.providers.sgs_provider import SgsProvider

_TICKER_RX = re.compile(r"^[A-Z]{4}\d{1,2}$")
_TICKER_FAMILY_RX = re.compile(r"^([A-Z]{4})(\d{1,2})?$")
_ALLOWED_RANGES = {"5m", "30m", "60m", "3h", "12h", "1d", "1w", "1m", "3m", "6m", "1y"}
_ALLOWED_MOVER_WINDOWS = {"hour", "day", "week"}


@dataclass
class MarketDataRouterService:
    b3: B3Provider = field(default_factory=B3Provider)
    brapi: BrapiProvider = field(default_factory=BrapiProvider)
    sgs: SgsProvider = field(default_factory=SgsProvider)
    fundamentus: FundamentusProvider = field(default_factory=FundamentusProvider)

    def _normalize_ticker(self, symbol: str) -> str:
        s = (symbol or "").strip().upper()
        if not _TICKER_RX.match(s):
            raise ValueError("ticker_invalid")
        return s

    def _normalize_range(self, range_key: str) -> str:
        key = (range_key or "").strip().lower()
        if key not in _ALLOWED_RANGES:
            raise ValueError("range_invalid")
        return key

    def search_tickers(self, query: str, limit: int = 12) -> list[dict[str, str]]:
        q = (query or "").strip().upper()
        if len(q) < 2:
            return []
        raw_rows = self.b3.search_tickers(query=q, limit=max(40, limit))
        if not raw_rows:
            raw_rows = self.brapi.search_tickers(query=q, limit=max(40, limit))
        return self._rank_and_trim_search_rows(raw_rows, query=q, limit=limit)

    def quote(self, symbol: str) -> dict[str, Any] | None:
        ticker = self._normalize_ticker(symbol)
        quote = self.b3.quote(ticker)
        if quote and not quote.get("error"):
            return quote
        return self.brapi.quote(ticker)

    def history(self, symbol: str, range_key: str) -> dict[str, Any] | None:
        ticker = self._normalize_ticker(symbol)
        rk = self._normalize_range(range_key)
        data = self.b3.history(ticker, rk)
        if data and data.get("points"):
            return data
        return self.brapi.history(ticker, rk)

    def macro_snapshot(self) -> dict[str, Any]:
        return self.sgs.macro_snapshot()

    def fundamentals(self, symbol: str) -> dict[str, Any] | None:
        ticker = self._normalize_ticker(symbol)
        return self.fundamentus.fundamentals(ticker)

    def top_movers(self, window: str, limit: int = 10) -> list[dict[str, Any]]:
        w = (window or "").strip().lower()
        if w not in _ALLOWED_MOVER_WINDOWS:
            raise ValueError("window_invalid")
        top = self.b3.top_movers(window=w, limit=limit)
        if top:
            return top
        return self.brapi.top_movers(window=w, limit=limit)

    def _rank_and_trim_search_rows(
        self,
        rows: list[dict[str, Any]],
        *,
        query: str,
        limit: int,
    ) -> list[dict[str, Any]]:
        seen: set[str] = set()
        normalized: list[dict[str, Any]] = []
        q = query.upper().strip()
        family_query = self._ticker_family(q)

        for row in rows:
            symbol = str(row.get("symbol") or "").upper().strip()
            if not symbol or symbol in seen:
                continue
            seen.add(symbol)
            normalized.append(
                {
                    "symbol": symbol,
                    "name": str(row.get("name") or symbol).strip(),
                    "instrument_type": str(row.get("instrument_type") or "stocks"),
                    "source": str(row.get("source") or "unknown"),
                    "confidence": float(row.get("confidence") or 0.7),
                }
            )

        def score(item: dict[str, Any]) -> tuple[int, int, int, str]:
            symbol = str(item["symbol"])
            family = self._ticker_family(symbol)
            same_family = 1 if family_query and family == family_query else 0
            starts = 1 if symbol.startswith(q) else 0
            contains = 1 if q in symbol else 0
            return (-same_family, -starts, -contains, symbol)

        normalized.sort(key=score)
        return normalized[: max(1, min(limit, 50))]

    def _ticker_family(self, value: str) -> str | None:
        m = _TICKER_FAMILY_RX.match((value or "").strip().upper())
        if not m:
            return None
        return m.group(1)


market_data_router = MarketDataRouterService()
