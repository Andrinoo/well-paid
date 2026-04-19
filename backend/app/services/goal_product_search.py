"""Pesquisa de produtos por texto via SerpAPI (Google Shopping). Requer SERPAPI_KEY."""

from __future__ import annotations

import logging
from typing import Any

import httpx

logger = logging.getLogger(__name__)

_USER_AGENT = "WellPaid/1.0 (+https://wellpaid.app) SerpAPI Google Shopping price search"


def _parse_brl_price_to_cents(price: Any) -> int | None:
    """Aceita número ou string tipo 'R$ 1.234,56' (pt-BR)."""
    if price is None:
        return None
    if isinstance(price, (int, float)) and not isinstance(price, bool):
        v = float(price)
        return int(round(v * 100)) if v > 0 else None
    s = str(price).strip()
    if not s:
        return None
    s = s.replace("R$", "").replace(" ", "").replace("\u00a0", "")
    if "," in s:
        s = s.replace(".", "").replace(",", ".")
    try:
        v = float(s)
    except ValueError:
        return None
    return int(round(v * 100)) if v > 0 else None


def build_grocery_search_query(
    label: str,
    unit: str | None,
    location_hint: str | None,
) -> str:
    """Mercearia: supermercado + opcional cidade (alinhado ao legado app.py)."""
    parts: list[str] = [label.strip()]
    u = (unit or "").strip()
    if u:
        parts.append(u)
    parts.append("supermercado")
    loc = (location_hint or "").strip()
    if loc:
        parts.append(loc)
    q = " ".join(parts).strip()
    return q[:200]


def search_products_google_shopping(
    query: str,
    *,
    serpapi_key: str | None,
    serp_limit: int = 12,
    serp_timeout_s: float = 12.0,
    max_total: int = 25,
) -> list[dict[str, Any]]:
    """Google Shopping via SerpAPI (única fonte de sugestões de preço na app)."""
    q = query.strip()
    if len(q) < 2:
        return []
    key = (serpapi_key or "").strip()
    if not key:
        logger.warning("search_products_google_shopping: SERPAPI_KEY ausente")
        return []
    lim = max(1, min(int(serp_limit), int(max_total), 15))
    return search_serpapi_google_shopping(
        q,
        api_key=key,
        limit=lim,
        timeout_s=serp_timeout_s,
    )[:max_total]


def search_serpapi_google_shopping(
    query: str,
    *,
    api_key: str,
    limit: int = 10,
    timeout_s: float = 20.0,
) -> list[dict[str, Any]]:
    """Resultados Google Shopping via SerpAPI."""
    key = (api_key or "").strip()
    if not key:
        return []
    q = query.strip()
    if len(q) < 2:
        return []
    lim = max(1, min(int(limit), 15))
    params = {
        "engine": "google_shopping",
        "q": q,
        "api_key": key,
        "gl": "br",
        "hl": "pt-br",
        "num": lim,
    }
    try:
        with httpx.Client(
            timeout=timeout_s,
            headers={"User-Agent": _USER_AGENT},
        ) as client:
            r = client.get("https://serpapi.com/search", params=params)
            r.raise_for_status()
            data = r.json()
    except Exception as e:
        logger.warning("SerpAPI google_shopping failed: %s", e)
        return []

    out: list[dict[str, Any]] = []
    for item in data.get("shopping_results") or []:
        if not isinstance(item, dict):
            continue
        title = str(item.get("title") or "").strip()[:500]
        link = str(
            item.get("product_link") or item.get("link") or item.get("product_link_clean") or ""
        ).strip()
        if not title or not link:
            continue
        raw_price = item.get("extracted_price")
        if raw_price is None:
            raw_price = item.get("price")
        price_cents = _parse_brl_price_to_cents(raw_price)
        if price_cents is None or price_cents <= 0:
            continue
        thumb = item.get("thumbnail")
        thumb_s = str(thumb).strip() if thumb else None
        out.append(
            {
                "title": title,
                "price_cents": price_cents,
                "currency_id": "BRL",
                "url": link,
                "thumbnail": thumb_s,
                "external_id": None,
                "source": "google_shopping",
            }
        )
        if len(out) >= lim:
            break
    return out
