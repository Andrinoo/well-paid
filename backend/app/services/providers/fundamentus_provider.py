from __future__ import annotations

import logging
import re
from dataclasses import dataclass
from typing import Any

import httpx

logger = logging.getLogger(__name__)

_FUNDAMENTUS_URL = "https://www.fundamentus.com.br/detalhes.php"


@dataclass
class FundamentusProvider:
    source: str = "fundamentus"

    def fundamentals(self, symbol: str) -> dict[str, Any] | None:
        ticker = (symbol or "").strip().upper()
        if not ticker:
            return None
        try:
            with httpx.Client(timeout=10.0, headers={"User-Agent": "WellPaid/1.0"}) as client:
                r = client.get(_FUNDAMENTUS_URL, params={"papel": ticker})
        except Exception:
            logger.exception("Fundamentus request failed for %s", ticker)
            return None
        if r.status_code != 200:
            return None
        html = r.text or ""
        # Parsing simples para evitar dependência extra:
        def extract(label: str) -> str | None:
            # Busca célula textual seguida do próximo valor na tabela.
            pattern = rf"{re.escape(label)}.*?<td[^>]*>(.*?)</td>"
            m = re.search(pattern, html, flags=re.IGNORECASE | re.DOTALL)
            if not m:
                return None
            value = re.sub(r"<[^>]+>", "", m.group(1)).strip()
            return value or None

        pl = extract("P/L")
        pvp = extract("P/VP")
        dy = extract("Div. Yield")
        roe = extract("ROE")
        if not any([pl, pvp, dy, roe]):
            return None
        return {
            "symbol": ticker,
            "pl": pl,
            "pvp": pvp,
            "dividend_yield": dy,
            "roe": roe,
            "source": self.source,
            "confidence": 0.65,
        }
