from __future__ import annotations

import uuid
from unittest.mock import MagicMock

import pytest
from fastapi.testclient import TestClient

from app.api.deps import get_current_user
from app.core.database import get_db
from app.main import app
from app.models.user import User


@pytest.fixture
def client() -> TestClient:
    return TestClient(app)


@pytest.fixture(autouse=True)
def clear_overrides() -> None:
    yield
    app.dependency_overrides.clear()


def _fake_user() -> User:
    u = User(
        email="user@test.com",
        hashed_password="x",
        is_active=True,
        email_verified_at=None,
    )
    u.id = uuid.uuid4()
    return u


def test_ping_deduped_same_day(client: TestClient) -> None:
    user = _fake_user()
    app.dependency_overrides[get_current_user] = lambda: user

    db = MagicMock()
    db.scalar.return_value = uuid.uuid4()

    def _db() -> object:
        yield db

    app.dependency_overrides[get_db] = _db

    r = client.post("/telemetry/ping", headers={"Authorization": "Bearer x"}, json={})
    assert r.status_code == 200
    data = r.json()
    assert data["accepted"] is True
    assert data["deduped"] is True
    assert data["event_type"] == "app_open"
    db.commit.assert_not_called()


def test_ping_inserts_when_no_event_today(client: TestClient) -> None:
    user = _fake_user()
    app.dependency_overrides[get_current_user] = lambda: user

    db = MagicMock()
    db.scalar.return_value = None

    def _db() -> object:
        yield db

    app.dependency_overrides[get_db] = _db

    r = client.post(
        "/telemetry/ping",
        headers={"Authorization": "Bearer x"},
        json={"event_type": "app_open"},
    )
    assert r.status_code == 200
    data = r.json()
    assert data["accepted"] is True
    assert data["deduped"] is False
    assert data["event_type"] == "app_open"
    assert user.last_seen_at is not None
    db.commit.assert_called_once()
