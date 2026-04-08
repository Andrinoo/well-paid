import uuid
from unittest.mock import MagicMock, patch

import pytest

from app.services.expense_share import (
    ExpenseShareValidationError,
    normalize_expense_share,
)


def test_not_shared_clears_partner() -> None:
    db = MagicMock()
    u = uuid.uuid4()
    assert normalize_expense_share(db, u, False, None) == (False, None)


def test_not_shared_rejects_partner() -> None:
    db = MagicMock()
    u = uuid.uuid4()
    v = uuid.uuid4()
    with pytest.raises(ExpenseShareValidationError):
        normalize_expense_share(db, u, False, v)


@patch("app.services.expense_share.family_peer_user_ids")
def test_shared_needs_two_members(mock_peers: MagicMock) -> None:
    db = MagicMock()
    u = uuid.uuid4()
    mock_peers.return_value = [u]
    with pytest.raises(ExpenseShareValidationError):
        normalize_expense_share(db, u, True, None)


@patch("app.services.expense_share.family_peer_user_ids")
def test_shared_ok_without_named_partner(mock_peers: MagicMock) -> None:
    db = MagicMock()
    u = uuid.uuid4()
    v = uuid.uuid4()
    mock_peers.return_value = [u, v]
    assert normalize_expense_share(db, u, True, None) == (True, None)


@patch("app.services.expense_share.family_peer_user_ids")
def test_shared_with_must_be_peer(mock_peers: MagicMock) -> None:
    db = MagicMock()
    u = uuid.uuid4()
    v = uuid.uuid4()
    w = uuid.uuid4()
    mock_peers.return_value = [u, v]
    with pytest.raises(ExpenseShareValidationError):
        normalize_expense_share(db, u, True, w)


@patch("app.services.expense_share.family_peer_user_ids")
def test_shared_with_peer_ok(mock_peers: MagicMock) -> None:
    db = MagicMock()
    u = uuid.uuid4()
    v = uuid.uuid4()
    mock_peers.return_value = [u, v]
    assert normalize_expense_share(db, u, True, v) == (True, v)
