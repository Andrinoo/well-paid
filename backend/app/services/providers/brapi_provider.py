from __future__ import annotations

from dataclasses import dataclass

from app.services.investment_brapi import (
    fetch_stock_history_brapi,
    fetch_stock_quote_brapi,
    fetch_stock_quote_snapshot_brapi,
    search_tickers_brapi,
)


@dataclass
class BrapiProvider:
    source: str = "brapi"
    _ibov_universe: tuple[str, ...] = (
        "PETR4", "VALE3", "ITUB4", "BBDC4", "BBAS3", "ABEV3", "WEGE3", "B3SA3",
        "RENT3", "MGLU3", "LREN3", "SUZB3", "PRIO3", "JBSS3", "ELET3", "ELET6",
        "GGBR4", "RAIL3", "CCRO3", "BRFS3", "EQTL3", "CPLE6", "CSAN3", "RADL3",
        "BPAC11", "NTCO3", "VIVT3", "HAPV3", "HYPE3", "EMBR3", "TIMS3", "MRFG3",
    )

    def search_tickers(self, query: str, limit: int = 12) -> list[dict[str, str]]:
        rows = search_tickers_brapi(query=query, limit=limit)
        out: list[dict[str, str]] = []
        for row in rows:
            out.append(
                {
                    "symbol": str(row.get("symbol") or "").upper(),
                    "name": str(row.get("name") or row.get("symbol") or "").strip(),
                    "instrument_type": "stock",
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

    def top_movers(self, window: str, limit: int = 10) -> list[dict[str, object]]:
        range_map = {
            "hour": "12h",
            "day": "1d",
            "week": "1w",
        }
        range_key = range_map.get((window or "").strip().lower())
        if range_key is None:
            return []
        rows: list[dict[str, object]] = []
        for symbol in self._ibov_universe:
            snapshot = fetch_stock_quote_snapshot_brapi(symbol)
            if snapshot is None:
                continue
            history = fetch_stock_history_brapi(symbol=symbol, range_key=range_key)
            points = list((history or {}).get("points") or [])
            change_percent: float | None = snapshot.get("change_percent")  # type: ignore[assignment]
            if len(points) >= 2:
                first = float(points[0].get("close") or 0.0)
                last = float(points[-1].get("close") or 0.0)
                if first > 0 and last > 0:
                    change_percent = ((last - first) / first) * 100.0
            if change_percent is None:
                continue
            rows.append(
                {
                    "symbol": symbol,
                    "name": str(snapshot.get("name") or symbol),
                    "change_percent": float(change_percent),
                    "volume": float(snapshot.get("volume") or 0.0),
                    "window": window,
                    "source": self.source,
                    "confidence": 0.78,
                }
            )
        rows.sort(key=lambda item: (float(item.get("change_percent") or 0.0), float(item.get("volume") or 0.0)), reverse=True)
        return rows[: max(1, min(limit, 30))]
