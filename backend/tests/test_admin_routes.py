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
        is_admin=False,
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
    assert r.json()["is_admin"] is False
    assert r.json()["revoked_sessions"] == 0
    assert target.is_active is False
    db.commit.assert_called_once()


def test_patch_user_updates_admin_and_revokes_sessions(client: TestClient) -> None:
    admin = _fake_admin()
    app.dependency_overrides[get_current_admin_user] = lambda: admin

    uid = uuid.uuid4()
    target = SimpleNamespace(
        id=uid,
        email="member@test.com",
        is_active=True,
        is_admin=False,
    )

    db = MagicMock()
    db.get.return_value = target
    db.scalar.return_value = 3

    def _db() -> object:
        yield db

    app.dependency_overrides[get_db] = _db

    r = client.patch(
        f"/admin/users/{uid}",
        headers={"Authorization": "Bearer x"},
        json={"is_admin": True, "revoke_sessions": True},
    )
    assert r.status_code == 200
    data = r.json()
    assert data["is_admin"] is True
    assert data["revoked_sessions"] == 3
    assert target.is_admin is True
    db.execute.assert_called_once()
    db.commit.assert_called_once()


def test_patch_user_block_self_demote(client: TestClient) -> None:
    admin = _fake_admin()
    app.dependency_overrides[get_current_admin_user] = lambda: admin

    target = SimpleNamespace(
        id=admin.id,
        email=admin.email,
        is_active=True,
        is_admin=True,
    )

    db = MagicMock()
    db.get.return_value = target

    def _db() -> object:
        yield db

    app.dependency_overrides[get_db] = _db

    r = client.patch(
        f"/admin/users/{admin.id}",
        headers={"Authorization": "Bearer x"},
        json={"is_admin": False},
    )
    assert r.status_code == 409
    db.commit.assert_not_called()


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


def test_get_user_detail_returns_activity_breakdown(client: TestClient) -> None:
    admin = _fake_admin()
    app.dependency_overrides[get_current_admin_user] = lambda: admin

    uid = uuid.uuid4()
    now = datetime.now(UTC)
    target = SimpleNamespace(
        id=uid,
        email="detail@test.com",
        full_name=None,
        display_name="Detail",
        phone=None,
        is_active=True,
        is_admin=False,
        email_verified_at=now,
        last_seen_at=now,
        created_at=now,
        updated_at=now,
    )

    db = MagicMock()
    db.get.return_value = target
    db.scalar.side_effect = [4, 9]
    db.execute.return_value.all.return_value = [
        SimpleNamespace(event_type="login_success", total=5),
        SimpleNamespace(event_type="refresh_success", total=3),
    ]
    ev1 = SimpleNamespace(
        occurred_at=now,
        event_type="app_open",
    )
    ev2 = SimpleNamespace(
        occurred_at=now,
        event_type="login_success",
    )
    scalars_result = MagicMock()
    scalars_result.all.return_value = [ev1, ev2]
    db.scalars.return_value = scalars_result

    def _db() -> object:
        yield db

    app.dependency_overrides[get_db] = _db

    r = client.get(f"/admin/users/{uid}", headers={"Authorization": "Bearer x"})
    assert r.status_code == 200
    data = r.json()
    assert data["user"]["email"] == "detail@test.com"
    assert data["events_7d"] == 4
    assert data["events_30d"] == 9
    assert data["event_types_30d"]["login_success"] == 5
    assert data["event_types_30d"]["refresh_success"] == 3
    assert len(data["recent_events"]) == 2
    assert data["recent_events"][0]["event_type"] == "app_open"
    assert data["recent_events"][1]["event_type"] == "login_success"


def test_list_families_returns_items(client: TestClient) -> None:
    admin = _fake_admin()
    app.dependency_overrides[get_current_admin_user] = lambda: admin

    fid = uuid.uuid4()
    now = datetime.now(UTC)
    fam = SimpleNamespace(
        id=fid,
        name="Silva",
        created_at=now,
        updated_at=now,
        created_by_user_id=None,
    )

    db = MagicMock()
    db.scalar.return_value = 1
    db.execute.return_value.all.return_value = [(fam, 3)]

    def _db() -> object:
        yield db

    app.dependency_overrides[get_db] = _db

    r = client.get("/admin/families", headers={"Authorization": "Bearer x"})
    assert r.status_code == 200
    data = r.json()
    assert data["total"] == 1
    assert len(data["items"]) == 1
    assert data["items"][0]["name"] == "Silva"
    assert data["items"][0]["member_count"] == 3


def test_get_family_detail_returns_members_and_invites(client: TestClient) -> None:
    admin = _fake_admin()
    app.dependency_overrides[get_current_admin_user] = lambda: admin

    fid = uuid.uuid4()
    uid = uuid.uuid4()
    now = datetime.now(UTC)
    u = SimpleNamespace(
        email="m@test.com",
        full_name=None,
        display_name="M",
        is_active=True,
    )
    mem = SimpleNamespace(user_id=uid, user=u, role="owner")
    inv = SimpleNamespace(
        id=uuid.uuid4(),
        expires_at=now,
        used=False,
    )
    fam = SimpleNamespace(
        id=fid,
        name="Fam",
        created_at=now,
        updated_at=now,
        created_by_user_id=None,
        members=[mem],
        invites=[inv],
    )

    db = MagicMock()
    mock_scalars = MagicMock()
    mock_scalars.unique.return_value.one_or_none.return_value = fam
    db.scalars.return_value = mock_scalars

    def _db() -> object:
        yield db

    app.dependency_overrides[get_db] = _db

    r = client.get(f"/admin/families/{fid}", headers={"Authorization": "Bearer x"})
    assert r.status_code == 200
    data = r.json()
    assert data["name"] == "Fam"
    assert data["member_count"] == 1
    assert data["max_members"] == 5
    assert data["members"][0]["email"] == "m@test.com"
    assert len(data["invites"]) == 1
    assert data["invites"][0]["used"] is False


def test_get_family_detail_404(client: TestClient) -> None:
    admin = _fake_admin()
    app.dependency_overrides[get_current_admin_user] = lambda: admin

    db = MagicMock()
    mock_scalars = MagicMock()
    mock_scalars.unique.return_value.one_or_none.return_value = None
    db.scalars.return_value = mock_scalars

    def _db() -> object:
        yield db

    app.dependency_overrides[get_db] = _db

    fid = uuid.uuid4()
    r = client.get(f"/admin/families/{fid}", headers={"Authorization": "Bearer x"})
    assert r.status_code == 404


def test_finance_summary_returns_totals(client: TestClient) -> None:
    admin = _fake_admin()
    app.dependency_overrides[get_current_admin_user] = lambda: admin

    db = MagicMock()
    db.scalar.side_effect = [
        100,
        90,
        10,
        5,
        40,
        12,
        30,
        3,
        25,
        2,
        18,
        8,
        4,
        9_999,
        50_000,
    ]

    def _db() -> object:
        yield db

    app.dependency_overrides[get_db] = _db

    r = client.get("/admin/finance/summary", headers={"Authorization": "Bearer x"})
    assert r.status_code == 200
    data = r.json()
    assert data["expenses_total"] == 100
    assert data["expenses_active"] == 90
    assert data["expenses_deleted"] == 10
    assert data["expenses_shared"] == 5
    assert data["incomes_total"] == 40
    assert data["goals_total"] == 12
    assert data["goal_contributions_total"] == 30
    assert data["shopping_lists_total"] == 3
    assert data["shopping_list_items_total"] == 25
    assert data["emergency_reserves_total"] == 2
    assert data["emergency_reserve_accruals_total"] == 18
    assert data["categories_total"] == 8
    assert data["income_categories_total"] == 4
    assert data["expenses_sum_cents_30d"] == 9_999
    assert data["incomes_sum_cents_30d"] == 50_000


def test_product_funnel_returns_counts(client: TestClient) -> None:
    admin = _fake_admin()
    app.dependency_overrides[get_current_admin_user] = lambda: admin

    db = MagicMock()
    db.scalar.side_effect = [
        1000,
        800,
        300,
        250,
        200,
        120,
        15,
        40,
    ]

    def _db() -> object:
        yield db

    app.dependency_overrides[get_db] = _db

    r = client.get("/admin/metrics/funnel", headers={"Authorization": "Bearer x"})
    assert r.status_code == 200
    data = r.json()
    assert data["users_total"] == 1000
    assert data["email_verified_total"] == 800
    assert data["users_with_family_total"] == 300
    assert data["users_with_expense_total"] == 250
    assert data["users_with_income_total"] == 200
    assert data["users_app_open_7d"] == 120
    assert data["signups_7d"] == 15
    assert data["signups_30d"] == 40


def test_list_audit_events_returns_items(client: TestClient) -> None:
    admin = _fake_admin()
    app.dependency_overrides[get_current_admin_user] = lambda: admin

    aid = uuid.uuid4()
    now = datetime.now(UTC)
    row = SimpleNamespace(
        id=aid,
        created_at=now,
        actor_email="admin@test.com",
        action="user.patch",
        target_email="u@test.com",
        details={"is_active": False},
    )

    db = MagicMock()
    db.scalar.return_value = 1
    scal = MagicMock()
    scal.all.return_value = [row]
    db.scalars.return_value = scal

    def _db() -> object:
        yield db

    app.dependency_overrides[get_db] = _db

    r = client.get("/admin/audit/events", headers={"Authorization": "Bearer x"})
    assert r.status_code == 200
    data = r.json()
    assert data["total"] == 1
    assert len(data["items"]) == 1
    assert data["items"][0]["action"] == "user.patch"
    assert data["items"][0]["details"]["is_active"] is False
