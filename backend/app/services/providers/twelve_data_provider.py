from __future__ import annotations

from dataclasses import dataclass

from app.services.investment_twelve_data import fetch_crypto_quote_twelve_data


@dataclass
class TwelveDataProvider:
    source: str = "twelve_data"

    def quote_crypto(self, symbol: str) -> dict[str, object] | None:
        raw = fetch_crypto_quote_twelve_data(symbol)
        if raw is None:
            return None
        return {
            "symbol": symbol.upper(),
            "last_price": raw.get("last_price"),
            "as_of": raw.get("as_of"),
            "error": raw.get("raw_error"),
            "source": self.source,
            "confidence": 0.76,
            "currency": "USD",
        }
