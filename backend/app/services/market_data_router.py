from __future__ import annotations

import re
from dataclasses import dataclass
from typing import Any

from app.services.providers.b3_provider import B3Provider
from app.services.providers.brapi_provider import BrapiProvider
from app.services.providers.fundamentus_provider import FundamentusProvider
from app.services.providers.sgs_provider import SgsProvider

_TICKER_RX = re.compile(r"^[A-Z]{4}\d{1,2}$")
_ALLOWED_RANGES = {"5m", "30m", "60m", "3h", "12h", "1d", "1w", "1m", "3m", "6m", "1y"}


@dataclass
class MarketDataRouterService:
    b3: B3Provider = B3Provider()
    brapi: BrapiProvider = BrapiProvider()
    sgs: SgsProvider = SgsProvider()
    fundamentus: FundamentusProvider = FundamentusProvider()

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
        rows = self.b3.search_tickers(query=query, limit=limit)
        if rows:
            return rows
        return self.brapi.search_tickers(query=query, limit=limit)

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


market_data_router = MarketDataRouterService()
