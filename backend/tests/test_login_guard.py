from datetime import UTC, datetime, timedelta

import pytest
from fastapi import HTTPException

from app.models.login_attempt import LoginAttempt
from app.services.login_guard import (
    LOCK_AFTER_FAILURES,
    LOGIN_FAIL_DETAIL,
    LOGIN_LOCKED_DETAIL,
    clear_failures,
    raise_if_locked,
    register_failure,
)


class _FakeDb:
    def __init__(self, row: LoginAttempt | None = None) -> None:
        self.row = row
        self.deleted = None

    def scalar(self, _stmt):  # noqa: ANN001
        return self.row

    def add(self, obj: LoginAttempt) -> None:
        self.row = obj

    def flush(self) -> None:
        return None

    def commit(self) -> None:
        return None

    def delete(self, obj: LoginAttempt) -> None:
        self.deleted = obj
        self.row = None


def test_lockout_after_max_failures() -> None:
    db = _FakeDb()
    email = "a@b.com"
    for _ in range(LOCK_AFTER_FAILURES - 1):
        register_failure(db, email)
        raise_if_locked(db, email)
    register_failure(db, email)
    with pytest.raises(HTTPException) as exc:
        raise_if_locked(db, email)
    assert exc.value.status_code == 429
    assert exc.value.detail == LOGIN_LOCKED_DETAIL


def test_expired_lock_is_cleared() -> None:
    row = LoginAttempt(
        email="a@b.com",
        fail_count=5,
        locked_until=datetime.now(UTC) - timedelta(minutes=1),
    )
    db = _FakeDb(row)
    raise_if_locked(db, "a@b.com")
    assert row.fail_count == 0
    assert row.locked_until is None


def test_clear_failures_deletes_row() -> None:
    row = LoginAttempt(email="a@b.com", fail_count=2, locked_until=None)
    db = _FakeDb(row)
    clear_failures(db, "a@b.com")
    assert db.deleted is row
    assert db.row is None


def test_fail_detail_does_not_reveal_account() -> None:
    assert "incorretos" in LOGIN_FAIL_DETAIL.lower() or "senha" in LOGIN_FAIL_DETAIL.lower()
