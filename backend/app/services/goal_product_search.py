"""Pesquisa de produtos por texto via SerpAPI (Google Shopping) com fallback opcional Tavily."""

from __future__ import annotations

import logging
import re
from typing import Any

import httpx

logger = logging.getLogger(__name__)

_USER_AGENT = "WellPaid/1.0 (+https://wellpaid.app) SerpAPI Google Shopping price search"
_TAVILY_URL = "https://api.tavily.com/search"


def _parse_brl_price_to_cents(price: Any) -> int | None:
    """Aceita número ou string tipo 'R$ 1.234,56' (pt-BR). Ignora moeda estrangeira explícita."""
    if price is None:
        return None
    if isinstance(price, (int, float)) and not isinstance(price, bool):
        v = float(price)
        return int(round(v * 100)) if v > 0 else None
    s = str(price).strip()
    if not s:
        return None
    s_low = s.lower()
    if "us$" in s_low or "usd" in s_low or "u.s." in s_low:
        return None
    s = s.replace("R$", "").replace(" ", "").replace("\u00a0", "")
    if "apartir" in s_low.replace(" ", "") or "a partir" in s_low:
        s = re.sub(r"^.*?a\s*partir\s+de\s*", "", s, flags=re.IGNORECASE).strip()
    if "," in s:
        s = s.replace(".", "").replace(",", ".")
    else:
        # "3.299" como milhar (pt): um ponto e 3 dígitos decimais → milhares
        parts = s.split(".")
        if len(parts) == 2 and len(parts[1]) == 3 and parts[1].isdigit():
            s = parts[0] + parts[1]
    try:
        v = float(s)
    except ValueError:
        return None
    return int(round(v * 100)) if v > 0 else None


def _dedupe_shopping_items(items: list[dict[str, Any]]) -> list[dict[str, Any]]:
    seen: set[str] = set()
    out: list[dict[str, Any]] = []
    for item in items:
        link = _product_url_from_item(item)
        dedupe = link or str(item.get("title") or "")
        if dedupe in seen:
            continue
        seen.add(dedupe)
        out.append(item)
    return out


def _normalize_shopping_url(raw: str) -> str:
    """Google/SerpAPI muitas vezes devolvem URLs protocol-relative (//...) ou só www."""
    t = raw.strip()
    if not t:
        return ""
    if t.startswith("//"):
        return "https:" + t
    if t.startswith("www."):
        return "https://" + t
    if t.startswith("http://") or t.startswith("https://"):
        return t
    return ""


def _product_url_from_item(item: dict[str, Any]) -> str:
    """SerpAPI usa nomes diferentes por bloco (shopping vs inline)."""
    for k in (
        "product_link",
        "link",
        "product_link_clean",
        "tracking_link",
        "direct_link",
        "serpapi_link",
    ):
        v = item.get(k)
        if not isinstance(v, str):
            continue
        norm = _normalize_shopping_url(v)
        if norm:
            return norm
    return ""


def _collect_serpapi_shopping_blocks(data: dict[str, Any]) -> list[dict[str, Any]]:
    """Agrega todos os blocos onde o Google Shopping devolve produtos."""
    raw: list[dict[str, Any]] = []
    for key in ("shopping_results", "inline_shopping_results", "immersive_products"):
        block = data.get(key)
        if not isinstance(block, list):
            continue
        raw.extend([x for x in block if isinstance(x, dict)])

    cats = data.get("categorized_shopping_results")
    if isinstance(cats, list):
        for cat in cats:
            if not isinstance(cat, dict):
                continue
            for nk in ("shopping_results", "products", "items"):
                nested = cat.get(nk)
                if isinstance(nested, list):
                    raw.extend([x for x in nested if isinstance(x, dict)])

    return _dedupe_shopping_items(raw)


def _extract_price_raw(item: dict[str, Any]) -> Any:
    for k in ("extracted_price", "price", "extracted_old_price", "old_price"):
        v = item.get(k)
        if v is not None and v != "":
            return v
    inst = item.get("installment")
    if isinstance(inst, dict):
        for k in ("extracted_price", "price"):
            v = inst.get(k)
            if v is not None and v != "":
                return v
    return None


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


def _extract_first_brl_price_from_text(text: str) -> int | None:
    t = (text or "").strip()
    if not t:
        return None
    # Exemplos: "R$ 12,90", "R$12,90", "12,90", "1.234,56".
    for raw in re.findall(r"R\$\s*\d{1,3}(?:\.\d{3})*(?:,\d{2})|R\$\s*\d+(?:,\d{2})|\d{1,3}(?:\.\d{3})*,\d{2}", t):
        cents = _parse_brl_price_to_cents(raw)
        if cents is not None and cents > 0:
            return cents
    return None


def search_products_tavily(
    query: str,
    *,
    tavily_api_key: str | None,
    limit: int = 12,
    timeout_s: float = 12.0,
) -> list[dict[str, Any]]:
    """Pesquisa web via Tavily e extrai preços BRL dos snippets como fallback."""
    q = query.strip()
    if len(q) < 2:
        return []
    key = (tavily_api_key or "").strip()
    if not key:
        return []
    lim = max(1, min(int(limit), 15))
    payload: dict[str, Any] = {
        "api_key": key,
        "query": q,
        "search_depth": "advanced",
        "max_results": lim,
        "include_raw_content": False,
    }
    try:
        with httpx.Client(timeout=timeout_s, headers={"User-Agent": _USER_AGENT}) as client:
            r = client.post(_TAVILY_URL, json=payload)
        if r.status_code != 200:
            logger.warning("Tavily search HTTP %s: %s", r.status_code, (r.text or "")[:300])
            return []
        data = r.json()
    except Exception:
        logger.exception("Tavily search unexpected error")
        return []
    if not isinstance(data, dict):
        return []
    rows: list[dict[str, Any]] = []
    for it in (data.get("results") or []):
        if not isinstance(it, dict):
            continue
        title = str(it.get("title") or "").strip()[:500]
        url = str(it.get("url") or "").strip()
        if not title or not url:
            continue
        snippet = " ".join(
            p
            for p in (
                str(it.get("title") or ""),
                str(it.get("content") or ""),
                str(it.get("raw_content") or ""),
            )
            if p
        )
        cents = _extract_first_brl_price_from_text(snippet)
        if cents is None or cents <= 0:
            continue
        rows.append(
            {
                "title": title,
                "price_cents": cents,
                "currency_id": "BRL",
                "url": url,
                "thumbnail": None,
                "external_id": None,
                "source": "tavily_search",
            }
        )
        if len(rows) >= lim:
            break
    return rows


def merge_and_dedupe_product_rows(
    primary: list[dict[str, Any]],
    secondary: list[dict[str, Any]],
    *,
    max_total: int,
) -> list[dict[str, Any]]:
    """Mantém ordem (primary primeiro), removendo duplicados por URL/título."""
    out: list[dict[str, Any]] = []
    seen: set[str] = set()
    for row in [*primary, *secondary]:
        title = str(row.get("title") or "").strip().lower()
        url = str(row.get("url") or "").strip().lower()
        key = url or title
        if not key or key in seen:
            continue
        seen.add(key)
        out.append(row)
        if len(out) >= max_total:
            break
    return out


def _shopping_blocks_to_rows(
    blocks: list[dict[str, Any]],
    lim: int,
) -> list[dict[str, Any]]:
    out: list[dict[str, Any]] = []
    for item in blocks:
        title = str(item.get("title") or "").strip()[:500]
        link = _product_url_from_item(item)
        if not title or not link:
            continue
        raw_price = _extract_price_raw(item)
        price_cents = _parse_brl_price_to_cents(raw_price)
        if price_cents is None or price_cents <= 0:
            continue
        thumb = item.get("thumbnail") or item.get("serpapi_thumbnail")
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
    base = {
        "engine": "google_shopping",
        "q": q,
        "api_key": key,
        "gl": "br",
        "hl": "pt-br",
        "num": lim,
    }
    # mobile costuma trazer inline; desktop traz shopping_results — tentamos os dois.
    attempts: list[dict[str, str]] = [
        {"device": "mobile"},
        {"device": "desktop"},
        {},
    ]
    last_data: dict[str, Any] | None = None
    last_blocks: list[dict[str, Any]] = []

    try:
        with httpx.Client(
            timeout=timeout_s,
            headers={"User-Agent": _USER_AGENT},
        ) as client:
            for extra in attempts:
                params = {**base, **extra}
                try:
                    r = client.get("https://serpapi.com/search", params=params)
                except httpx.RequestError as e:
                    logger.warning("SerpAPI google_shopping request failed: %s", type(e).__name__)
                    continue
                if r.status_code != 200:
                    snippet = (r.text or "")[:400]
                    logger.warning(
                        "SerpAPI google_shopping HTTP %s: %s",
                        r.status_code,
                        snippet,
                    )
                    continue
                try:
                    data = r.json()
                except ValueError:
                    continue
                if not isinstance(data, dict):
                    continue
                if data.get("error"):
                    logger.warning("SerpAPI google_shopping error: %s", data.get("error"))
                    continue
                last_data = data
                blocks = _collect_serpapi_shopping_blocks(data)
                last_blocks = blocks
                out = _shopping_blocks_to_rows(blocks, lim)
                if out:
                    return out
    except Exception:
        logger.exception("SerpAPI google_shopping unexpected error")
        return []

    if last_blocks:
        sample = next((b for b in last_blocks if isinstance(b, dict)), None)
        keys = list(sample.keys())[:24] if sample else []
        logger.warning(
            "SerpAPI google_shopping: %d itens brutos mas 0 após parse; chaves exemplo: %s",
            len(last_blocks),
            keys,
        )
    elif last_data is not None:
        logger.warning(
            "SerpAPI google_shopping: 0 blocos de produto; chaves JSON: %s",
            list(last_data.keys())[:40],
        )
    return []
