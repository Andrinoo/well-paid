from __future__ import annotations

from dataclasses import dataclass

from app.services.investment_brapi import (
    fetch_stock_history_brapi,
    fetch_stock_quote_brapi,
    search_tickers_brapi,
)


@dataclass
class BrapiProvider:
    source: str = "brapi"

    def search_tickers(self, query: str, limit: int = 12) -> list[dict[str, str]]:
        rows = search_tickers_brapi(query=query, limit=limit)
        out: list[dict[str, str]] = []
        for row in rows:
            out.append(
                {
                    "symbol": str(row.get("symbol") or "").upper(),
                    "name": str(row.get("name") or row.get("symbol") or "").strip(),
                    "instrument_type": "stocks",
                    "source": self.source,
                    "confidence": 0.80,
                }
            )
        return out

    def quote(self, symbol: str) -> dict[str, object] | None:
        raw = fetch_stock_quote_brapi(symbol)
        if raw is None:
            return None
        return {
            "symbol": symbol.upper(),
            "last_price": raw.get("last_price"),
            "as_of": raw.get("as_of"),
            "error": raw.get("raw_error"),
            "source": self.source,
            "confidence": 0.82,
        }

    def history(self, symbol: str, range_key: str) -> dict[str, object] | None:
        raw = fetch_stock_history_brapi(symbol=symbol, range_key=range_key)
        if raw is None:
            return None
        return {
            "symbol": raw.get("symbol") or symbol.upper(),
            "range": range_key,
            "points": raw.get("points") or [],
            "error": raw.get("raw_error"),
            "source": self.source,
            "confidence": 0.80,
        }
