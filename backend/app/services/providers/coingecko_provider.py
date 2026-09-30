from __future__ import annotations

import logging
from dataclasses import dataclass
from typing import Any

import httpx

from app.core.config import get_settings

logger = logging.getLogger(__name__)

_IDS = {
    "BTC": "bitcoin", "ETH": "ethereum", "SOL": "solana", "BNB": "binancecoin",
    "XRP": "ripple", "ADA": "cardano", "DOGE": "dogecoin", "LTC": "litecoin",
    "USDT": "tether", "USDC": "usd-coin",
}


@dataclass
class CoinGeckoProvider:
    source: str = "coingecko"

    def quote_crypto(self, symbol: str) -> dict[str, Any] | None:
        ticker = (symbol or "").strip().upper()
        coin_id = _IDS.get(ticker)
        if not coin_id:
            return None
        key = (get_settings().coingecko_api_key or "").strip()
        headers = {"User-Agent": "WellPaid/1.0 coingecko-crypto"}
        if key:
            headers["x-cg-demo-api-key"] = key
        try:
            with httpx.Client(timeout=10.0, headers=headers) as client:
                response = client.get(
                    "https://api.coingecko.com/api/v3/coins/markets",
                    params={"vs_currency": "brl", "ids": coin_id, "price_change_percentage": "24h"},
                )
        except Exception:
            logger.exception("CoinGecko request failed for %s", ticker)
            return None
        if response.status_code != 200:
            return None
        try:
            rows = response.json()
        except Exception:
            return None
        if not isinstance(rows, list) or not rows or not isinstance(rows[0], dict):
            return None
        row = rows[0]
        price = row.get("current_price")
        if not isinstance(price, (int, float)) or price <= 0:
            return None
        return {
            "symbol": ticker,
            "last_price": float(price),
            "currency": "BRL",
            "as_of": row.get("last_updated"),
            "change_24h": row.get("price_change_24h"),
            "change_24h_percent": row.get("price_change_percentage_24h"),
            "day_high": row.get("high_24h"),
            "day_low": row.get("low_24h"),
            "volume_24h": row.get("total_volume"),
            "market_cap": row.get("market_cap"),
            "market_cap_rank": row.get("market_cap_rank"),
            "circulating_supply": row.get("circulating_supply"),
            "total_supply": row.get("total_supply"),
            "max_supply": row.get("max_supply"),
            "ath": row.get("ath"),
            "ath_change_percent": row.get("ath_change_percentage"),
            "image_url": row.get("image"),
            "error": None,
            "source": self.source,
            "confidence": 0.9,
        }
