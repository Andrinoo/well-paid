"""Pesquisa de produtos por texto (Mercado Livre API pública; SerpAPI opcional)."""

from __future__ import annotations

import logging
from typing import Any

import httpx

logger = logging.getLogger(__name__)

_USER_AGENT = (
    "WellPaid/1.0 (+https://wellpaid.app) product search via Mercado Libre public API"
)


def _price_to_cents(price: Any) -> int:
    if price is None:
        return 0
    try:
        v = float(price)
    except (TypeError, ValueError):
        return 0
    return int(round(v * 100))


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


def search_serpapi_google_shopping(
    query: str,
    *,
    api_key: str,
    limit: int = 10,
    timeout_s: float = 20.0,
) -> list[dict[str, Any]]:
    """
    Resultados Google Shopping via SerpAPI (mesma ideia que app.py /api/search-price).
    Requer SERPAPI_KEY; sem chave o router não chama isto.
    """
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


def search_mercadolibre(
    query: str,
    *,
    site_id: str = "MLB",
    limit: int = 15,
    timeout_s: float = 15.0,
) -> list[dict[str, Any]]:
    """
    Devolve lista de dicts: title, price_cents, currency_id, url, thumbnail, external_id.
    site_id MLB = Brasil; MLM México; MLA Argentina, etc.
    """
    q = query.strip()
    if len(q) < 2:
        return []
    lim = max(1, min(int(limit), 20))
    url = f"https://api.mercadolibre.com/sites/{site_id}/search"
    try:
        with httpx.Client(
            timeout=timeout_s,
            headers={"User-Agent": _USER_AGENT},
        ) as client:
            r = client.get(url, params={"q": q, "limit": lim})
            r.raise_for_status()
            data = r.json()
    except Exception as e:
        logger.warning("Mercado Libre search failed: %s", e)
        return []

    out: list[dict[str, Any]] = []
    for item in data.get("results") or []:
        if not isinstance(item, dict):
            continue
        title = str(item.get("title") or "").strip()[:500]
        permalink = str(item.get("permalink") or "").strip()
        if not title or not permalink:
            continue
        price_cents = _price_to_cents(item.get("price"))
        if price_cents <= 0:
            continue
        currency = str(item.get("currency_id") or "BRL")[:8]
        thumb = item.get("thumbnail")
        thumb_s = str(thumb).strip() if thumb else None
        eid = item.get("id")
        out.append(
            {
                "title": title,
                "price_cents": price_cents,
                "currency_id": currency,
                "url": permalink,
                "thumbnail": thumb_s,
                "external_id": str(eid) if eid is not None else None,
                "source": "mercadolibre",
            }
        )
    return out
