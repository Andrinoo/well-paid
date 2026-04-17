"""Eventos family_financial (serviço e rota GET)."""

from __future__ import annotations

import uuid
from unittest.mock import MagicMock

import pytest
from fastapi.testclient import TestClient

from app.api.deps import get_current_user
from app.core.database import get_db
from app.main import app
from app.models.family_financial_event import FamilyFinancialEvent
from app.models.user import User
from app.services.family_financial_events import (
    record_peer_declined_share,
    record_receivable_settled,
)


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
    )
    u.id = uuid.uuid4()
    return u


def test_financial_events_401_sem_auth(client: TestClient) -> None:
    r = client.get("/family/financial-events")
    assert r.status_code == 401


def test_financial_events_200_empty_sem_familia(client: TestClient) -> None:
    user = _fake_user()
    app.dependency_overrides[get_current_user] = lambda: user
    db = MagicMock()
    db.scalar.return_value = None

    def _db() -> object:
        yield db

    app.dependency_overrides[get_db] = _db
    r = client.get("/family/financial-events", headers={"Authorization": "Bearer x"})
    assert r.status_code == 200
    assert r.json() == []


def test_record_peer_declined_skips_without_family() -> None:
    db = MagicMock()
    db.scalar.return_value = None
    uid = uuid.uuid4()
    record_peer_declined_share(
        db,
        peer_user_id=uid,
        owner_user_id=uuid.uuid4(),
        expense_id=uuid.uuid4(),
        expense_share_id=uuid.uuid4(),
        share_amount_cents=100,
        decline_reason=None,
    )
    db.add.assert_not_called()


def test_record_peer_declined_adds_row() -> None:
    db = MagicMock()
    fid = uuid.uuid4()
    db.scalar.return_value = fid
    peer = uuid.uuid4()
    owner = uuid.uuid4()
    eid = uuid.uuid4()
    sid = uuid.uuid4()
    record_peer_declined_share(
        db,
        peer_user_id=peer,
        owner_user_id=owner,
        expense_id=eid,
        expense_share_id=sid,
        share_amount_cents=250,
        decline_reason="test",
    )
    db.add.assert_called_once()
    ev = db.add.call_args[0][0]
    assert isinstance(ev, FamilyFinancialEvent)
    assert ev.family_id == fid
    assert ev.event_type == "peer_declined_share"
    assert ev.actor_user_id == peer
    assert ev.counterparty_user_id == owner
    assert ev.amount_cents == 250
    assert ev.source_expense_id == eid
    assert ev.source_expense_share_id == sid


def test_record_receivable_settled_adds_row() -> None:
    db = MagicMock()
    fid = uuid.uuid4()
    db.scalar.return_value = fid
    cr = uuid.uuid4()
    dr = uuid.uuid4()
    rid = uuid.uuid4()
    eid = uuid.uuid4()
    record_receivable_settled(
        db,
        creditor_user_id=cr,
        debtor_user_id=dr,
        receivable_id=rid,
        source_expense_id=eid,
        amount_cents=500,
    )
    db.add.assert_called_once()
    ev = db.add.call_args[0][0]
    assert isinstance(ev, FamilyFinancialEvent)
    assert ev.event_type == "receivable_settled"
    assert ev.actor_user_id == cr
    assert ev.counterparty_user_id == dr
    assert ev.source_receivable_id == rid


def test_list_financial_events_invalid_before_400(client: TestClient) -> None:
    user = _fake_user()
    app.dependency_overrides[get_current_user] = lambda: user
    fid = uuid.uuid4()
    db = MagicMock()
    db.scalar.return_value = fid
    db.get.return_value = None

    def _db() -> object:
        yield db

    app.dependency_overrides[get_db] = _db
    bad = uuid.uuid4()
    r = client.get(
        f"/family/financial-events?before={bad}",
        headers={"Authorization": "Bearer x"},
    )
    assert r.status_code == 400
