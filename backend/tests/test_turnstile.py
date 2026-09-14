from types import SimpleNamespace

import pytest
from fastapi import HTTPException

from app.services import turnstile as ts


def _settings(*, site: str, secret: str) -> SimpleNamespace:
    return SimpleNamespace(turnstile_site_key=site, turnstile_secret_key=secret)


def test_unconfigured_skips_verification(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setattr(ts, "get_settings", lambda: _settings(site="", secret=""))
    ts.assert_turnstile(None, "1.1.1.1")
    ts.assert_turnstile("", None)


def test_configured_rejects_missing_token(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setattr(ts, "get_settings", lambda: _settings(site="site", secret="secret"))
    with pytest.raises(HTTPException) as exc:
        ts.assert_turnstile(None, "1.1.1.1")
    assert exc.value.status_code == 400
    assert exc.value.detail == ts._FAIL_DETAIL


class _Resp:
    def __init__(self, payload: bytes) -> None:
        self._payload = payload

    def read(self) -> bytes:
        return self._payload

    def __enter__(self) -> "_Resp":
        return self

    def __exit__(self, *_exc: object) -> None:
        return None


def test_configured_accepts_siteverify_success(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setattr(ts, "get_settings", lambda: _settings(site="site", secret="secret"))
    monkeypatch.setattr(
        ts.urllib.request,
        "urlopen",
        lambda *_a, **_k: _Resp(b'{"success": true}'),
    )
    ts.assert_turnstile("tok", "8.8.8.8")


def test_configured_rejects_siteverify_failure(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setattr(ts, "get_settings", lambda: _settings(site="site", secret="secret"))
    monkeypatch.setattr(
        ts.urllib.request,
        "urlopen",
        lambda *_a, **_k: _Resp(b'{"success": false}'),
    )
    with pytest.raises(HTTPException) as exc:
        ts.assert_turnstile("tok", "8.8.8.8")
    assert exc.value.status_code == 400
