from __future__ import annotations

import json
import urllib.error
import urllib.parse
import urllib.request

from fastapi import HTTPException, status

from app.core.config import get_settings

_SITEVERIFY = "https://challenges.cloudflare.com/turnstile/v0/siteverify"
_FAIL_DETAIL = "Não foi possível criar a conta."


def turnstile_configured() -> bool:
    s = get_settings()
    return bool(s.turnstile_secret_key.strip() and s.turnstile_site_key.strip())


def assert_turnstile(token: str | None, remote_ip: str | None) -> None:
    """Exige um token válido quando as chaves existem. Sem chaves, não bloqueia (dev)."""
    if not turnstile_configured():
        return
    raw = (token or "").strip()
    if not raw:
        raise HTTPException(status.HTTP_400_BAD_REQUEST, _FAIL_DETAIL)
    secret = get_settings().turnstile_secret_key.strip()
    payload = urllib.parse.urlencode(
        {
            "secret": secret,
            "response": raw,
            **({"remoteip": remote_ip} if remote_ip else {}),
        }
    ).encode()
    req = urllib.request.Request(
        _SITEVERIFY,
        data=payload,
        method="POST",
        headers={"Content-Type": "application/x-www-form-urlencoded"},
    )
    try:
        with urllib.request.urlopen(req, timeout=8) as resp:
            body = json.loads(resp.read().decode("utf-8"))
    except (urllib.error.URLError, TimeoutError, json.JSONDecodeError, ValueError) as exc:
        raise HTTPException(status.HTTP_400_BAD_REQUEST, _FAIL_DETAIL) from exc
    if not isinstance(body, dict) or body.get("success") is not True:
        raise HTTPException(status.HTTP_400_BAD_REQUEST, _FAIL_DETAIL)
