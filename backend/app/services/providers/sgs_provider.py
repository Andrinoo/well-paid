from __future__ import annotations

import logging
from dataclasses import dataclass
from datetime import date, timedelta

import httpx

logger = logging.getLogger(__name__)

_SGS_BASE = "https://api.bcb.gov.br/dados/serie/bcdata.sgs"


@dataclass
class SgsProvider:
    source: str = "sgs"

    def series(self, series_code: int, start: date, end: date) -> list[dict[str, object]]:
        url = f"{_SGS_BASE}.{series_code}/dados"
        params = {
            "formato": "json",
            "dataInicial": start.strftime("%d/%m/%Y"),
            "dataFinal": end.strftime("%d/%m/%Y"),
        }
        try:
            with httpx.Client(timeout=10.0) as client:
                r = client.get(url, params=params)
        except Exception:
            logger.exception("SGS request failed for series %s", series_code)
            return []
        if r.status_code != 200:
            return []
        try:
            rows = r.json()
        except Exception:
            return []
        out: list[dict[str, object]] = []
        for row in rows:
            if not isinstance(row, dict):
                continue
            raw_value = str(row.get("valor") or "").replace(",", ".").strip()
            try:
                value = float(raw_value)
            except ValueError:
                continue
            out.append(
                {
                    "date": row.get("data"),
                    "value": value,
                    "source": self.source,
                    "confidence": 0.99,
                }
            )
        return out

    def macro_snapshot(self) -> dict[str, object]:
        end = date.today()
        start = end - timedelta(days=35)
        # 12 = CDI diário, 432 = SELIC meta, 433 = IPCA mensal (proxy curto para snapshot)
        cdi_rows = self.series(12, start, end)
        selic_rows = self.series(432, start, end)
        ipca_rows = self.series(433, start - timedelta(days=60), end)
        return {
            "cdi": cdi_rows[-1]["value"] if cdi_rows else None,
            "selic": selic_rows[-1]["value"] if selic_rows else None,
            "ipca": ipca_rows[-1]["value"] if ipca_rows else None,
            "source": self.source,
            "confidence": 0.99,
        }
