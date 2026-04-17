"""Decline / assume-full helpers and API behaviour (unit-level)."""

from __future__ import annotations

import uuid
from datetime import date
from types import SimpleNamespace
from unittest.mock import MagicMock

from app.services.expense_splits import compute_share_extras, share_resolved


def test_share_resolved_excludes_declined() -> None:
    assert share_resolved("declined") is False


def test_compute_share_extras_peer_declined_alerts_owner() -> None:
    owner = uuid.uuid4()
    peer = uuid.uuid4()
    exp = SimpleNamespace(
        is_shared=True,
        shared_with_user_id=peer,
        owner_user_id=owner,
        split_mode="amount",
        due_date=date(2026, 1, 1),
    )
    s_owner = MagicMock()
    s_owner.user_id = owner
    s_owner.status = "pending"
    s_owner.share_cents = 50
    s_peer = MagicMock()
    s_peer.user_id = peer
    s_peer.status = "declined"
    s_peer.share_cents = 50
    out = compute_share_extras(
        viewer_id=owner,
        expense=exp,
        shares=[s_owner, s_peer],
        today=date(2026, 1, 15),
    )
    assert out["shared_expense_peer_declined_alert"] is True
    assert out["my_share_declined"] is False


def test_compute_share_extras_my_declined_for_peer() -> None:
    owner = uuid.uuid4()
    peer = uuid.uuid4()
    exp = SimpleNamespace(
        is_shared=True,
        shared_with_user_id=peer,
        owner_user_id=owner,
        split_mode="amount",
        due_date=date(2026, 1, 1),
    )
    s_owner = MagicMock()
    s_owner.user_id = owner
    s_owner.status = "pending"
    s_owner.share_cents = 50
    s_peer = MagicMock()
    s_peer.user_id = peer
    s_peer.status = "declined"
    s_peer.share_cents = 50
    out = compute_share_extras(
        viewer_id=peer,
        expense=exp,
        shares=[s_owner, s_peer],
        today=date(2026, 1, 15),
    )
    assert out["my_share_declined"] is True
    assert out["shared_expense_peer_declined_alert"] is False
