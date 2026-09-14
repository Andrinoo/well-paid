"""Proxy seguro de miniaturas de produto (Google Shopping, etc.)."""

from __future__ import annotations

from urllib.parse import urljoin, urlparse

import httpx

from app.services.goal_reference_price import is_safe_public_http_url

_BROWSER_UA = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
)
_MAX_BYTES = 512_000
_ALLOWED_SUFFIXES = (
    "gstatic.com",
    "googleusercontent.com",
    "ggpht.com",
    "google.com",
    "serpapi.com",
    "mlstatic.com",
    "ssl-images-amazon.com",
    "media-amazon.com",
    "amazon.com",
    "amazon.com.br",
)


def normalize_thumbnail_url(raw: str | None) -> str | None:
    t = (raw or "").strip()
    if not t:
        return None
    if t.startswith("//"):
        t = "https:" + t
    elif t.startswith("www."):
        t = "https://" + t
    if t.startswith("http://"):
        host = (urlparse(t).hostname or "").lower()
        if _host_allowed(host):
            t = "https://" + t[len("http://") :]
    if not (t.startswith("http://") or t.startswith("https://")):
        return None
    return t[:2048]


def _host_allowed(host: str) -> bool:
    h = (host or "").strip().lower().rstrip(".")
    if not h:
        return False
    for suffix in _ALLOWED_SUFFIXES:
        if h == suffix or h.endswith("." + suffix):
            return True
    return False


def is_allowed_thumbnail_url(url: str) -> bool:
    norm = normalize_thumbnail_url(url)
    if not norm:
        return False
    host = (urlparse(norm).hostname or "").lower()
    if not _host_allowed(host):
        return False
    return is_safe_public_http_url(norm)


def fetch_thumbnail_bytes(url: str) -> tuple[bytes, str] | None:
    """Devolve (bytes, content-type) ou None. Não segue redirects para hosts fora da lista."""
    current = normalize_thumbnail_url(url)
    if not current or not is_allowed_thumbnail_url(current):
        return None
    headers = {
        "User-Agent": _BROWSER_UA,
        "Accept": "image/avif,image/webp,image/apng,image/*,*/*;q=0.8",
        "Referer": "https://www.google.com/",
        "Accept-Language": "pt-BR,pt;q=0.9",
    }
    try:
        with httpx.Client(timeout=8.0, follow_redirects=False, headers=headers) as client:
            for _ in range(4):
                if not is_allowed_thumbnail_url(current):
                    return None
                response = client.get(current)
                if response.status_code in {301, 302, 303, 307, 308}:
                    loc = (response.headers.get("location") or "").strip()
                    if not loc:
                        return None
                    current = urljoin(current, loc)
                    continue
                if response.status_code != 200:
                    return None
                body = response.content or b""
                if not body or len(body) > _MAX_BYTES:
                    return None
                ctype = (response.headers.get("content-type") or "").split(";")[0].strip().lower()
                if not ctype.startswith("image/"):
                    return None
                if ctype in {"image/svg+xml", "image/svg"}:
                    return None
                return body, ctype
    except httpx.HTTPError:
        return None
    return None
