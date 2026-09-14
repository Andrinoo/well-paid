from datetime import UTC, datetime, timedelta

import pytest
from fastapi import HTTPException

from app.models.signup_attempt import SignupAttempt
from app.services.public_id import looks_like_public_id, new_public_id, normalize_public_id
from app.services.signup_guard import (
    SIGNUPS_PER_WINDOW,
    SIGNUP_LOCKED_DETAIL,
    is_disposable_email,
    raise_if_signup_blocked,
    record_signup_attempt,
)


class _FakeDb:
    def __init__(self, row: SignupAttempt | None = None) -> None:
        self.row = row

    def scalar(self, _stmt):  # noqa: ANN001
        return self.row

    def add(self, obj: SignupAttempt) -> None:
        self.row = obj

    def flush(self) -> None:
        return None

    def commit(self) -> None:
        return None


def test_public_id_format_and_normalize() -> None:
    pid = new_public_id()
    assert looks_like_public_id(pid)
    assert looks_like_public_id(pid.lower())
    assert normalize_public_id(f" {pid.lower()} ") == pid
    assert not looks_like_public_id("user@example.com")
    ids = {new_public_id() for _ in range(40)}
    assert len(ids) == 40


def test_disposable_email_is_rejected() -> None:
    assert is_disposable_email("bot@mailinator.com")
    assert is_disposable_email("Bot@Yopmail.com")
    assert not is_disposable_email("andrino@wellpaid.com.br")


def test_signup_lock_after_window_max(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setattr("app.services.signup_guard._unverified_count", lambda *_: 0)
    db = _FakeDb()
    ip_hash = "abc"
    for _ in range(SIGNUPS_PER_WINDOW - 1):
        record_signup_attempt(db, ip_hash)
        raise_if_signup_blocked(db, ip_hash)
    record_signup_attempt(db, ip_hash)
    with pytest.raises(HTTPException) as exc:
        raise_if_signup_blocked(db, ip_hash)
    assert exc.value.status_code == 429
    assert exc.value.detail == SIGNUP_LOCKED_DETAIL


def test_expired_signup_lock_clears(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setattr("app.services.signup_guard._unverified_count", lambda *_: 0)
    row = SignupAttempt(
        ip_hash="abc",
        success_count=3,
        locked_until=datetime.now(UTC) - timedelta(minutes=1),
    )
    db = _FakeDb(row)
    raise_if_signup_blocked(db, "abc")
    assert row.success_count == 0
    assert row.locked_until is None


def test_unverified_cap_blocks(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setattr("app.services.signup_guard._unverified_count", lambda *_: 2)
    db = _FakeDb()
    with pytest.raises(HTTPException) as exc:
        raise_if_signup_blocked(db, "abc")
    assert exc.value.status_code == 429
