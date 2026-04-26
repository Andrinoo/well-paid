from __future__ import annotations

import re
from dataclasses import dataclass, field
from typing import Any

from app.domain.asset_types import normalize_asset_type
from app.services.providers.alpha_vantage_provider import AlphaVantageProvider
from app.services.providers.b3_provider import B3Provider
from app.services.providers.brapi_provider import BrapiProvider
from app.services.providers.finnhub_provider import FinnhubProvider
from app.services.investment_brapi import (
    fetch_brapi_key_statistics_enrichment,
    fetch_stock_quote_snapshot_brapi,
)
from app.services.providers.fundamentus_provider import FundamentusProvider
from app.services.providers.sgs_provider import SgsProvider
from app.services.providers.twelve_data_provider import TwelveDataProvider

_TICKER_RX = re.compile(r"^[A-Z]{4}\d{1,2}$")
_TICKER_FAMILY_RX = re.compile(r"^([A-Z]{4})(\d{1,2})?$")
_CRYPTO_SYMBOL_RX = re.compile(r"^[A-Z]{2,10}$")
_ALLOWED_RANGES = {"5m", "30m", "60m", "3h", "12h", "1d", "1w", "1m", "3m", "6m", "1y", "2y", "3y"}
_RANGE_ALIASES = {
    "3mo": "3m",
    "6mo": "6m",
    "12m": "1y",
    "24m": "2y",
    "36m": "3y",
}
_ALLOWED_MOVER_WINDOWS = {"hour", "day", "week"}
_KNOWN_CRYPTO_SYMBOLS = {"BTC", "ETH", "SOL", "BNB", "XRP", "ADA", "DOGE", "LTC", "USDT", "USDC"}


@dataclass
class MarketDataRouterService:
    b3: B3Provider = field(default_factory=B3Provider)
    brapi: BrapiProvider = field(default_factory=BrapiProvider)
    alpha_vantage: AlphaVantageProvider = field(default_factory=AlphaVantageProvider)
    twelve_data: TwelveDataProvider = field(default_factory=TwelveDataProvider)
    finnhub: FinnhubProvider = field(default_factory=FinnhubProvider)
    sgs: SgsProvider = field(default_factory=SgsProvider)
    fundamentus: FundamentusProvider = field(default_factory=FundamentusProvider)

    def _normalize_ticker(self, symbol: str) -> str:
        s = (symbol or "").strip().upper()
        if not _TICKER_RX.match(s) and not self._is_crypto_symbol(s):
            raise ValueError("ticker_invalid")
        return s

    def _is_crypto_symbol(self, symbol: str) -> bool:
        s = (symbol or "").strip().upper()
        return bool(_CRYPTO_SYMBOL_RX.match(s)) and s in _KNOWN_CRYPTO_SYMBOLS

    def _normalize_range(self, range_key: str) -> str:
        key = (range_key or "").strip().lower()
        key = _RANGE_ALIASES.get(key, key)
        if key not in _ALLOWED_RANGES:
            raise ValueError("range_invalid")
        return key

    def search_tickers(self, query: str, limit: int = 12) -> list[dict[str, str]]:
        q = (query or "").strip().upper()
        if len(q) < 3:
            return []
        synthetic = self._synthetic_fixed_income_rows(q)
        synthetic_crypto = self._synthetic_crypto_rows(q)
        raw_rows = self.b3.search_tickers(query=q, limit=max(40, limit))
        if not raw_rows:
            raw_rows = self.brapi.search_tickers(query=q, limit=max(40, limit))
        return self._rank_and_trim_search_rows([*synthetic, *synthetic_crypto, *raw_rows], query=q, limit=limit)

    def quote(self, symbol: str) -> dict[str, Any] | None:
        ticker = self._normalize_ticker(symbol)
        if self._is_crypto_symbol(ticker):
            providers = [self.alpha_vantage, self.twelve_data, self.finnhub]
            first_any: dict[str, Any] | None = None
            for idx, provider in enumerate(providers):
                candidate = provider.quote_crypto(ticker)
                if candidate is None:
                    continue
                if first_any is None:
                    first_any = candidate
                if not candidate.get("error") and candidate.get("last_price") is not None:
                    candidate["fallback_used"] = idx > 0
                    candidate["provider_strategy"] = "fallback_chain"
                    candidate["stale"] = False
                    return candidate
            if first_any is not None:
                first_any["fallback_used"] = False
                first_any["provider_strategy"] = "fallback_chain"
                first_any["stale"] = True
            return first_any
        quote = self.b3.quote(ticker)
        if quote and not quote.get("error"):
            quote["fallback_used"] = False
            quote["provider_strategy"] = "hybrid"
            quote["stale"] = False
            return quote
        fallback_quote = self.brapi.quote(ticker)
        if fallback_quote is not None:
            fallback_quote["fallback_used"] = True
            fallback_quote["provider_strategy"] = "hybrid"
            fallback_quote["stale"] = bool(fallback_quote.get("error"))
        return fallback_quote

    def history(self, symbol: str, range_key: str) -> dict[str, Any] | None:
        ticker = self._normalize_ticker(symbol)
        rk = self._normalize_range(range_key)
        data = self.b3.history(ticker, rk)
        if data and data.get("points"):
            data["fallback_used"] = False
            data["provider_strategy"] = "hybrid"
            data["stale"] = False
            return data
        fallback_data = self.brapi.history(ticker, rk)
        if fallback_data is not None:
            fallback_data["fallback_used"] = True
            fallback_data["provider_strategy"] = "hybrid"
            fallback_data["stale"] = bool(fallback_data.get("error"))
        return fallback_data

    def macro_snapshot(self) -> dict[str, Any]:
        return self.sgs.macro_snapshot()

    def fundamentals(self, symbol: str) -> dict[str, Any] | None:
        ticker = self._normalize_ticker(symbol)
        data = self.fundamentus.fundamentals(ticker)
        brapi_ks = fetch_brapi_key_statistics_enrichment(ticker)
        if data is None:
            if not brapi_ks:
                return None
            # Minimal fallback payload when Fundamentus is unavailable.
            data = {
                "symbol": ticker,
                "company_name": brapi_ks.get("company_name"),
                "pl": None,
                "pvp": brapi_ks.get("pvp"),
                "daily_liquidity": brapi_ks.get("daily_liquidity"),
                "dividend_yield": brapi_ks.get("dividend_yield"),
                "dividend_yield_12m": brapi_ks.get("dividend_yield_12m"),
                "vacancy_financial": brapi_ks.get("vacancy_financial"),
                "contract_term_wault": brapi_ks.get("contract_term_wault"),
                "atypical_contracts_ratio": brapi_ks.get("atypical_contracts_ratio"),
                "top5_tenants_concentration": brapi_ks.get("top5_tenants_concentration"),
                "roe": None,
                "ev_ebitda": brapi_ks.get("ev_ebitda"),
                "net_margin": None,
                "net_debt_ebitda": brapi_ks.get("net_debt_ebitda"),
                "eps": None,
                "source": "brapi",
                "confidence": 0.72,
            }
            return data
        # EV/EBITDA/PVP/DY: prefer BRAPI values when available.
        if brapi_ks:
            if brapi_ks.get("ev_ebitda"):
                data["ev_ebitda"] = brapi_ks["ev_ebitda"]
            if brapi_ks.get("net_debt_ebitda"):
                data["net_debt_ebitda"] = brapi_ks["net_debt_ebitda"]
            if brapi_ks.get("pvp"):
                data["pvp"] = brapi_ks["pvp"]
            if brapi_ks.get("daily_liquidity"):
                data["daily_liquidity"] = brapi_ks["daily_liquidity"]
            if brapi_ks.get("dividend_yield"):
                data["dividend_yield"] = brapi_ks["dividend_yield"]
            if brapi_ks.get("dividend_yield_12m"):
                data["dividend_yield_12m"] = brapi_ks["dividend_yield_12m"]
            if brapi_ks.get("vacancy_financial"):
                data["vacancy_financial"] = brapi_ks["vacancy_financial"]
            if brapi_ks.get("contract_term_wault"):
                data["contract_term_wault"] = brapi_ks["contract_term_wault"]
            if brapi_ks.get("atypical_contracts_ratio"):
                data["atypical_contracts_ratio"] = brapi_ks["atypical_contracts_ratio"]
            if brapi_ks.get("top5_tenants_concentration"):
                data["top5_tenants_concentration"] = brapi_ks["top5_tenants_concentration"]
            n = (brapi_ks.get("company_name") or "").strip()
            if n:
                data["company_name"] = n
        if not (data.get("company_name") or "").strip():
            # Nome: snapshot simples se o módulo key stats não devolveu nome.
            snap = fetch_stock_quote_snapshot_brapi(ticker)
            if snap:
                name = str(snap.get("name") or "").strip()
                if name:
                    data["company_name"] = name
        return data

    def top_movers(self, window: str, limit: int = 10) -> list[dict[str, Any]]:
        w = (window or "").strip().lower()
        if w not in _ALLOWED_MOVER_WINDOWS:
            raise ValueError("window_invalid")
        top = self.b3.top_movers(window=w, limit=limit)
        if top:
            for row in top:
                row["fallback_used"] = False
                row["provider_strategy"] = "hybrid"
            return top
        fallback_rows = self.brapi.top_movers(window=w, limit=limit)
        for row in fallback_rows:
            row["fallback_used"] = True
            row["provider_strategy"] = "hybrid"
        return fallback_rows

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
                    "instrument_type": self._infer_asset_type(
                        symbol=symbol,
                        name=str(row.get("name") or symbol).strip(),
                        raw_type=str(row.get("instrument_type") or "stock"),
                    ),
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

    def _synthetic_fixed_income_rows(self, query: str) -> list[dict[str, Any]]:
        q = query.upper()
        rows: list[dict[str, Any]] = []
        if "CDB" in q:
            rows.append(
                {
                    "symbol": "CDB",
                    "name": "CDB - Certificado de Deposito Bancario",
                    "instrument_type": "cdb",
                    "source": "wellpaid",
                    "confidence": 0.99,
                }
            )
        if "CDI" in q:
            rows.append(
                {
                    "symbol": "CDI",
                    "name": "CDI - Certificado de Deposito Interbancario",
                    "instrument_type": "cdi",
                    "source": "wellpaid",
                    "confidence": 0.99,
                }
            )
        if "TESOURO" in q or "IPCA" in q or "SELIC" in q:
            rows.append(
                {
                    "symbol": "TESOURO",
                    "name": "Tesouro Direto",
                    "instrument_type": "treasury",
                    "source": "wellpaid",
                    "confidence": 0.96,
                }
            )
        if "RENDA FIXA" in q:
            rows.append(
                {
                    "symbol": "RENDA_FIXA",
                    "name": "Renda fixa (geral)",
                    "instrument_type": "fixed_income",
                    "source": "wellpaid",
                    "confidence": 0.95,
                }
            )
        return rows

    def _synthetic_crypto_rows(self, query: str) -> list[dict[str, Any]]:
        q = query.upper().strip()
        catalog = [
            ("BTC", "Bitcoin (BTC)"),
            ("ETH", "Ethereum (ETH)"),
            ("SOL", "Solana (SOL)"),
            ("BNB", "BNB (BNB)"),
            ("XRP", "XRP (XRP)"),
            ("ADA", "Cardano (ADA)"),
            ("DOGE", "Dogecoin (DOGE)"),
            ("LTC", "Litecoin (LTC)"),
            ("USDT", "Tether USD (USDT)"),
            ("USDC", "USD Coin (USDC)"),
        ]
        rows: list[dict[str, Any]] = []
        for sym, name in catalog:
            if q and q not in sym and q not in name.upper():
                continue
            rows.append(
                {
                    "symbol": sym,
                    "name": name,
                    "instrument_type": "crypto",
                    "source": "alpha_vantage",
                    "confidence": 0.90,
                }
            )
        return rows

    def _infer_asset_type(self, *, symbol: str, name: str, raw_type: str) -> str:
        normalized = normalize_asset_type(raw_type, default="stock")
        if normalized != "stock":
            return normalized

        upper_symbol = symbol.upper()
        upper_name = name.upper()
        if upper_symbol in _KNOWN_CRYPTO_SYMBOLS:
            return "crypto"
        # BDRs are usually 4 letters + 34/35/39 suffix.
        if upper_symbol.endswith(("34", "35", "39")):
            return "bdr"
        # FII/ETF shares often end with 11, we disambiguate by text hints.
        if upper_symbol.endswith("11"):
            if any(token in upper_name for token in ("ETF", "INDICE", "ÍNDICE")):
                return "etf"
            if any(token in upper_name for token in ("FII", "FUNDO IMOBILI", "IMOBILIARIO", "IMOBILIÁRIO")):
                return "fii"
        return "stock"


market_data_router = MarketDataRouterService()
