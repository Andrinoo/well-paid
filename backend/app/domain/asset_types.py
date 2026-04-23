from __future__ import annotations

from typing import Final

CANONICAL_ASSET_TYPES: Final[set[str]] = {
    "stock",
    "fii",
    "bdr",
    "etf",
    "treasury",
    "cdb",
    "cdi",
    "fixed_income",
}

ASSET_TYPE_ALIASES: Final[dict[str, str]] = {
    "stocks": "stock",
    "acao": "stock",
    "acoes": "stock",
    "equity": "stock",
    "reits": "fii",
    "funds": "fii",
    "fund": "fii",
    "fiis": "fii",
    "treasuries": "treasury",
    "tesouro": "treasury",
    "renda_fixa": "fixed_income",
    "renda fixa": "fixed_income",
}


def normalize_asset_type(raw: str | None, *, default: str = "stock") -> str:
    value = (raw or "").strip().lower()
    if not value:
        return default
    canonical = ASSET_TYPE_ALIASES.get(value, value)
    if canonical in CANONICAL_ASSET_TYPES:
        return canonical
    return default

