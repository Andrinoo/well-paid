"""Extracção best-effort de nome/preço a partir de um URL de produto."""

from __future__ import annotations

import json
import re
from datetime import datetime, timezone
from html import unescape
from typing import Any

import httpx

_USER_AGENT = (
    "WellPaid/1.0 (+https://wellpaid.app) httpx; price-hint fetch (best-effort, not for commerce SLA)"
)


def _meta_content(html: str, prop: str) -> str | None:
    m = re.search(
        rf'<meta[^>]+property=["\']{re.escape(prop)}["\'][^>]+content=["\']([^"\']+)["\']',
        html,
        re.I,
    )
    if m:
        return unescape(m.group(1).strip())
    m = re.search(
        rf'<meta[^>]+content=["\']([^"\']+)["\'][^>]+property=["\']{re.escape(prop)}["\']',
        html,
        re.I,
    )
    return unescape(m.group(1).strip()) if m else None


def _meta_name_content(html: str, name: str) -> str | None:
    m = re.search(
        rf'<meta[^>]+name=["\']{re.escape(name)}["\'][^>]+content=["\']([^"\']+)["\']',
        html,
        re.I,
    )
    return unescape(m.group(1).strip()) if m else None


def _first_json_ld_product(html: str) -> dict[str, Any] | None:
    for m in re.finditer(
        r'<script[^>]+type=["\']application/ld\+json["\'][^>]*>(.*?)</script>',
        html,
        re.I | re.S,
    ):
        raw = m.group(1).strip()
        try:
            data = json.loads(raw)
        except json.JSONDecodeError:
            continue
        items = data if isinstance(data, list) else [data]
        for item in items:
            if not isinstance(item, dict):
                continue
            t = item.get("@type")
            types = t if isinstance(t, list) else [t] if t else []
            if "Product" in types or item.get("@type") == "Product":
                return item
    return None


def _parse_price_to_cents(value: Any) -> int | None:
    if value is None:
        return None
    if isinstance(value, (int, float)):
        return int(round(float(value) * 100))
    s = str(value).strip()
    s = re.sub(r"[^\d,.]", "", s).replace(".", "").replace(",", ".")
    try:
        return int(round(float(s) * 100))
    except ValueError:
        return None


def fetch_product_hints(url: str, *, timeout_s: float = 12.0) -> dict[str, Any]:
    """
    Devolve dict com reference_product_name, reference_price_cents,
    reference_currency, price_checked_at, price_source, price_alternatives (lista).
    """
    out: dict[str, Any] = {
        "reference_product_name": None,
        "reference_price_cents": None,
        "reference_currency": "BRL",
        "price_checked_at": None,
        "price_alternatives": [],
        "price_source": "unavailable",
    }
    try:
        with httpx.Client(
            follow_redirects=True,
            timeout=timeout_s,
            headers={"User-Agent": _USER_AGENT},
        ) as client:
            r = client.get(url)
            r.raise_for_status()
            html = r.text
    except Exception:
        return out

    title = _meta_content(html, "og:title") or _meta_name_content(html, "title")
    if title:
        out["reference_product_name"] = title[:500]

    price_s = _meta_content(html, "product:price:amount") or _meta_content(
        html, "og:price:amount"
    )
    if price_s:
        pc = _parse_price_to_cents(price_s)
        if pc is not None:
            out["reference_price_cents"] = pc
            out["price_source"] = "fetched"

    product = _first_json_ld_product(html)
    if product:
        if not out["reference_product_name"] and product.get("name"):
            out["reference_product_name"] = str(product["name"])[:500]
        offers = product.get("offers")
        if isinstance(offers, dict):
            offers_list = [offers]
        elif isinstance(offers, list):
            offers_list = [x for x in offers if isinstance(x, dict)]
        else:
            offers_list = []
        for off in offers_list:
            p = off.get("price")
            pc = _parse_price_to_cents(p)
            if pc is not None and out["reference_price_cents"] is None:
                out["reference_price_cents"] = pc
                out["price_source"] = "fetched"
            cur = str(off.get("priceCurrency") or "BRL")[:8]
            if cur and cur != "BRL":
                out["reference_currency"] = cur

    if out["reference_price_cents"] is not None:
        out["price_checked_at"] = datetime.now(timezone.utc)
        if out["price_source"] != "fetched":
            out["price_source"] = "fetched"
    return out
