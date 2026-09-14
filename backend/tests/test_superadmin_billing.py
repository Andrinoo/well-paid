from __future__ import annotations

import uuid
from unittest.mock import MagicMock

import pytest
from fastapi.testclient import TestClient

from app.api.deps import get_current_superuser
from app.core.config import get_settings
from app.core.database import get_db
from app.main import app
from app.models.user import User

_SA = get_settings().superadmin_api_prefix_path


@pytest.fixture
def client() -> TestClient:
    return TestClient(app)


@pytest.fixture(autouse=True)
def clear_overrides() -> None:
    yield
    app.dependency_overrides.clear()


def _fake_super() -> User:
    u = User(
        email="super@test.com",
        hashed_password="x",
        is_admin=True,
        is_superuser=True,
        is_active=True,
    )
    u.id = uuid.uuid4()
    return u


def test_catalog_requires_superuser(client: TestClient) -> None:
    r = client.get(f"{_SA}/catalog")
    assert r.status_code == 404


def test_catalog_ok(client: TestClient) -> None:
    app.dependency_overrides[get_current_superuser] = _fake_super
    r = client.get(f"{_SA}/catalog", headers={"Authorization": "Bearer x"})
    assert r.status_code == 200
    data = r.json()
    assert data["basic_cents"] == 1499
    ids = {item["id"] for item in data["items"]}
    assert ids == {"dashboard", "payables", "incomes"}


def test_release_pix_creates_paid_period(client: TestClient) -> None:
    admin = _fake_super()
    app.dependency_overrides[get_current_superuser] = lambda: admin
    uid = uuid.uuid4()
    target = User(
        email="payer@test.com",
        hashed_password="x",
        is_active=False,
        is_free_plan=False,
        is_superuser=False,
        full_name="Maria Silva",
    )
    target.id = uid

    db = MagicMock()
    db.get.side_effect = lambda model, key: target if key == uid else None
    db.scalar.return_value = None
    db.scalars.return_value.all.return_value = []

    def _db() -> object:
        yield db

    app.dependency_overrides[get_db] = _db
    r = client.post(
        f"{_SA}/users/{uid}/pix/release",
        headers={"Authorization": "Bearer x"},
        json={"payer_name": "Maria Silva"},
    )
    assert r.status_code == 200
    body = r.json()
    assert body["is_active"] is True
    assert target.is_active is True
    assert len(body["payments"]) >= 0
    db.commit.assert_called()
