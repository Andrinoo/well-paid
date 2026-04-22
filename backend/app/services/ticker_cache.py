from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from threading import Lock
from typing import Any

from app.services.market_data_router import market_data_router


@dataclass
class _TickerCacheState:
    loaded_at: datetime | None = None
    by_prefix: dict[str, list[dict[str, Any]]] | None = None


class TickerCacheService:
    def __init__(self, ttl_minutes: int = 60) -> None:
        self._ttl = timedelta(minutes=max(5, ttl_minutes))
        self._lock = Lock()
        self._state = _TickerCacheState(by_prefix={})

    def _is_fresh(self) -> bool:
        if self._state.loaded_at is None:
            return False
        return datetime.now(timezone.utc) - self._state.loaded_at < self._ttl

    def warm_up(self) -> None:
        # Prefixos mais comuns para reduzir latência inicial de autocomplete.
        prefixes = ("pet", "vale", "bbas", "itub", "b3", "abev")
        with self._lock:
            for prefix in prefixes:
                if prefix in self._state.by_prefix:
                    continue
                self._state.by_prefix[prefix] = market_data_router.search_tickers(prefix, limit=20)
            self._state.loaded_at = datetime.now(timezone.utc)

    def search(self, query: str, *, limit: int = 12) -> list[dict[str, Any]]:
        q = (query or "").strip().lower()
        if len(q) < 2:
            return []
        with self._lock:
            if self._is_fresh() and q in self._state.by_prefix:
                return self._state.by_prefix[q][:limit]
        rows = market_data_router.search_tickers(q, limit=max(limit, 20))
        with self._lock:
            self._state.by_prefix[q] = rows
            if self._state.loaded_at is None or not self._is_fresh():
                self._state.loaded_at = datetime.now(timezone.utc)
        return rows[:limit]


ticker_cache_service = TickerCacheService()
