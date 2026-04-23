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

        def extract_any(*labels: str) -> str | None:
            for lb in labels:
                found = extract(lb)
                if found:
                    return found
            return None

        pl = extract("P/L")
        pvp = extract("P/VP")
        dy = extract("Div. Yield")
        roe = extract("ROE")
        ev_ebitda = extract_any("EV/EBITDA", "EV / EBITDA")
        net_margin = extract_any("Marg. Líquida", "Margem Líquida")
        net_debt_ebitda = extract_any(
            "Dív. Líquida/EBITDA",
            "Dív Líquida/EBITDA",
            "Div. Líquida/EBITDA",
            "Dívida Líquida/EBITDA",
        )
        eps = extract_any("LPA")
        if not any([pl, pvp, dy, roe, ev_ebitda, net_margin, net_debt_ebitda, eps]):
            return None
        company_name = extract("Empresa")
        return {
            "symbol": ticker,
            "company_name": company_name,
            "pl": pl,
            "pvp": pvp,
            "dividend_yield": dy,
            "roe": roe,
            "ev_ebitda": ev_ebitda,
            "net_margin": net_margin,
            "net_debt_ebitda": net_debt_ebitda,
            "eps": eps,
            "source": self.source,
            "confidence": 0.65,
        }
