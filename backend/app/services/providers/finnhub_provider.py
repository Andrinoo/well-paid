from __future__ import annotations

from dataclasses import dataclass

from app.services.investment_finnhub import fetch_crypto_quote_finnhub


@dataclass
class FinnhubProvider:
    source: str = "finnhub"

    def quote_crypto(self, symbol: str) -> dict[str, object] | None:
        raw = fetch_crypto_quote_finnhub(symbol)
        if raw is None:
            return None
        return {
            "symbol": symbol.upper(),
            "last_price": raw.get("last_price"),
            "as_of": raw.get("as_of"),
            "error": raw.get("raw_error"),
            "source": self.source,
            "confidence": 0.74,
        }
