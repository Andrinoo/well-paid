"""Testes das rotas /admin (overrides de dependências; sem BD real)."""

from __future__ import annotations

import uuid
from datetime import UTC, datetime
from types import SimpleNamespace
from unittest.mock import MagicMock

import pytest
from fastapi.testclient import TestClient

from app.api.deps import get_current_admin_user
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


def _fake_admin() -> User:
    u = User(
        email="admin@test.com",
        hashed_password="x",
        is_admin=True,
        is_active=True,
    )
    u.id = uuid.uuid4()
    return u


def test_admin_me_401_sem_authorization(client: TestClient) -> None:
    r = client.get("/admin/me")
    assert r.status_code == 401


def test_admin_me_200_com_override(client: TestClient) -> None:
    admin = _fake_admin()
    app.dependency_overrides[get_current_admin_user] = lambda: admin
    r = client.get("/admin/me", headers={"Authorization": "Bearer test"})
    assert r.status_code == 200
    data = r.json()
    assert data["email"] == "admin@test.com"
    assert data["is_admin"] is True


def test_list_users_returns_items(client: TestClient) -> None:
    admin = _fake_admin()
    app.dependency_overrides[get_current_admin_user] = lambda: admin

    uid = uuid.uuid4()
    now = datetime.now(UTC)
    row = SimpleNamespace(
        id=uid,
        email="user@test.com",
        full_name=None,
        display_name="U",
        phone=None,
        is_active=True,
        is_admin=False,
        email_verified_at=now,
        last_seen_at=None,
        created_at=now,
        updated_at=now,
    )

    db = MagicMock()
    db.scalar.return_value = 1
    scal = MagicMock()
    scal.all.return_value = [row]
    db.scalars.return_value = scal

    def _db() -> object:
        yield db

    app.dependency_overrides[get_db] = _db

    r = client.get("/admin/users", headers={"Authorization": "Bearer x"})
    assert r.status_code == 200
    data = r.json()
    assert data["total"] == 1
    assert len(data["items"]) == 1
    assert data["items"][0]["email"] == "user@test.com"


def test_patch_user_404(client: TestClient) -> None:
    admin = _fake_admin()
    app.dependency_overrides[get_current_admin_user] = lambda: admin

    db = MagicMock()
    db.get.return_value = None

    def _db() -> object:
        yield db

    app.dependency_overrides[get_db] = _db

    missing = uuid.uuid4()
    r = client.patch(
        f"/admin/users/{missing}",
        headers={"Authorization": "Bearer x"},
        json={"is_active": False},
    )
    assert r.status_code == 404


def test_patch_user_updates_active(client: TestClient) -> None:
    admin = _fake_admin()
    app.dependency_overrides[get_current_admin_user] = lambda: admin

    uid = uuid.uuid4()
    target = SimpleNamespace(
        id=uid,
        email="t@test.com",
        is_active=True,
    )

    db = MagicMock()
    db.get.return_value = target

    def _db() -> object:
        yield db

    app.dependency_overrides[get_db] = _db

    r = client.patch(
        f"/admin/users/{uid}",
        headers={"Authorization": "Bearer x"},
        json={"is_active": False},
    )
    assert r.status_code == 200
    assert r.json()["is_active"] is False
    assert target.is_active is False
    db.commit.assert_called_once()


def test_usage_summary_returns_metrics(client: TestClient) -> None:
    admin = _fake_admin()
    app.dependency_overrides[get_current_admin_user] = lambda: admin

    db = MagicMock()
    db.scalar.side_effect = [12, 5, 17]
    db.execute.return_value.all.return_value = [
        SimpleNamespace(day=datetime(2026, 4, 14, tzinfo=UTC), events=7, active_users=4),
        SimpleNamespace(day=datetime(2026, 4, 15, tzinfo=UTC), events=3, active_users=2),
    ]

    def _db() -> object:
        yield db

    app.dependency_overrides[get_db] = _db

    r = client.get("/admin/usage/summary", headers={"Authorization": "Bearer x"})
    assert r.status_code == 200
    data = r.json()
    assert data["events_24h"] == 12
    assert data["dau_7d"] == 5
    assert data["mau_30d"] == 17
    assert len(data["series"]) == 2
    assert data["series"][0]["day"] == "2026-04-14"
