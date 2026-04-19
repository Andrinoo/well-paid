"""Pesquisa de produtos por texto (Mercado Livre API pública; SerpAPI opcional)."""

from __future__ import annotations

import logging
from concurrent.futures import ThreadPoolExecutor
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


def merge_price_hits(
    primary: list[dict[str, Any]],
    extra: list[dict[str, Any]],
    *,
    max_total: int = 25,
) -> list[dict[str, Any]]:
    rows = list(primary)
    seen = {r.get("url") for r in rows if r.get("url")}
    for r in extra:
        u = r.get("url")
        if u and u not in seen:
            seen.add(u)
            rows.append(r)
    return rows[:max_total]


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


def search_products_mercadolibre_serp_parallel(
    query: str,
    *,
    site_id: str = "MLB",
    serpapi_key: str | None = None,
    ml_limit: int = 10,
    serp_limit: int = 10,
    ml_timeout_s: float = 8.0,
    serp_timeout_s: float = 12.0,
    max_total: int = 25,
) -> list[dict[str, Any]]:
    """Mercado Livre + SerpAPI em paralelo (menos latência que sequencial)."""
    q = query.strip()
    if len(q) < 2:
        return []
    key = (serpapi_key or "").strip()

    def run_ml() -> list[dict[str, Any]]:
        return search_mercadolibre(
            q,
            site_id=site_id,
            limit=ml_limit,
            timeout_s=ml_timeout_s,
        )

    def run_serp() -> list[dict[str, Any]]:
        if not key:
            return []
        return search_serpapi_google_shopping(
            q,
            api_key=key,
            limit=serp_limit,
            timeout_s=serp_timeout_s,
        )

    with ThreadPoolExecutor(max_workers=2) as ex:
        f_ml = ex.submit(run_ml)
        f_serp = ex.submit(run_serp) if key else None
        ml_rows = f_ml.result()
        serp_rows = f_serp.result() if f_serp is not None else []
    return merge_price_hits(ml_rows, serp_rows, max_total=max_total)


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
