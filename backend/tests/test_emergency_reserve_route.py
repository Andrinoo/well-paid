from datetime import date
from types import SimpleNamespace
from unittest.mock import MagicMock, patch

import pytest
from fastapi import HTTPException

from app.api.routes.emergency_reserve import (
    delete_emergency_reserve,
    delete_emergency_reserve_accrual,
    list_emergency_reserve_accruals,
    patch_emergency_reserve_accrual,
    read_emergency_reserve,
    update_emergency_reserve,
)
from app.schemas.emergency_reserve import (
    EmergencyReserveAccrualPatch,
    EmergencyReserveUpdate,
)


def test_read_emergency_reserve_returns_default_when_tables_missing() -> None:
    db = MagicMock()
    user = SimpleNamespace(id="user-1")

    with patch("app.api.routes.emergency_reserve._tables_ready", return_value=False):
        out = read_emergency_reserve(user, db)

    assert out.configured is False
    assert out.monthly_target_cents == 0
    assert out.balance_cents == 0
    assert out.tracking_start == date.today().replace(day=1)


def test_update_emergency_reserve_returns_503_when_tables_missing() -> None:
    db = MagicMock()
    user = SimpleNamespace(id="user-1")
    body = EmergencyReserveUpdate(monthly_target_cents=1000)

    with patch("app.api.routes.emergency_reserve._tables_ready", return_value=False):
        with pytest.raises(HTTPException) as exc:
            update_emergency_reserve(body, user, db)

    assert exc.value.status_code == 503
    assert "alembic upgrade head" in str(exc.value.detail)


def test_update_emergency_reserve_blocks_non_owner_family_member() -> None:
    db = MagicMock()
    user = SimpleNamespace(id="user-1")
    body = EmergencyReserveUpdate(monthly_target_cents=2500)
    db.scalar.return_value = "member"

    with patch("app.api.routes.emergency_reserve._tables_ready", return_value=True):
        with pytest.raises(HTTPException) as exc:
            update_emergency_reserve(body, user, db)

    assert exc.value.status_code == 403
    assert "titular" in str(exc.value.detail)


def test_update_emergency_reserve_allows_owner_and_returns_payload() -> None:
    db = MagicMock()
    user = SimpleNamespace(id="user-1")
    body = EmergencyReserveUpdate(monthly_target_cents=4000)
    fake_reserve = SimpleNamespace(
        monthly_target_cents=4000,
        balance_cents=12000,
        tracking_start=date(2026, 4, 1),
    )
    db.scalar.return_value = "owner"

    with (
        patch("app.api.routes.emergency_reserve._tables_ready", return_value=True),
        patch(
            "app.api.routes.emergency_reserve.upsert_monthly_target",
            return_value=fake_reserve,
        ),
        patch("app.api.routes.emergency_reserve.ensure_accruals", return_value=True),
    ):
        out = update_emergency_reserve(body, user, db)

    assert out.configured is True
    assert out.monthly_target_cents == 4000
    assert out.balance_cents == 12000
    assert out.tracking_start == date(2026, 4, 1)


def test_list_accruals_returns_empty_when_tables_missing() -> None:
    db = MagicMock()
    user = SimpleNamespace(id="user-1")
    with patch("app.api.routes.emergency_reserve._tables_ready", return_value=False):
        out = list_emergency_reserve_accruals(user, db, limit=12)
    assert out == []


def test_list_accruals_returns_mapped_items() -> None:
    db = MagicMock()
    user = SimpleNamespace(id="user-1")
    rows = [
        SimpleNamespace(
            year=2026,
            month=4,
            amount_cents=50000,
            created_at=None,
        ),
    ]
    with (
        patch("app.api.routes.emergency_reserve._tables_ready", return_value=True),
        patch("app.api.routes.emergency_reserve.list_accruals_for_user", return_value=rows),
    ):
        out = list_emergency_reserve_accruals(user, db, limit=12)
    assert len(out) == 1
    assert out[0].year == 2026
    assert out[0].month == 4
    assert out[0].amount_cents == 50000


def test_patch_accrual_returns_503_when_tables_missing() -> None:
    db = MagicMock()
    user = SimpleNamespace(id="user-1")
    body = EmergencyReserveAccrualPatch(amount_cents=1000)
    with patch("app.api.routes.emergency_reserve._tables_ready", return_value=False):
        with pytest.raises(HTTPException) as exc:
            patch_emergency_reserve_accrual(2026, 4, body, user, db)
    assert exc.value.status_code == 503


def test_delete_accrual_returns_503_when_tables_missing() -> None:
    db = MagicMock()
    user = SimpleNamespace(id="user-1")
    with patch("app.api.routes.emergency_reserve._tables_ready", return_value=False):
        with pytest.raises(HTTPException) as exc:
            delete_emergency_reserve_accrual(2026, 4, user, db)
    assert exc.value.status_code == 503


def test_delete_reserve_returns_503_when_tables_missing() -> None:
    db = MagicMock()
    user = SimpleNamespace(id="user-1")
    with patch("app.api.routes.emergency_reserve._tables_ready", return_value=False):
        with pytest.raises(HTTPException) as exc:
            delete_emergency_reserve(user, db)
    assert exc.value.status_code == 503


def test_patch_accrual_blocks_non_owner_family_member() -> None:
    db = MagicMock()
    user = SimpleNamespace(id="user-1")
    body = EmergencyReserveAccrualPatch(amount_cents=1000)
    db.scalar.return_value = "member"
    with patch("app.api.routes.emergency_reserve._tables_ready", return_value=True):
        with pytest.raises(HTTPException) as exc:
            patch_emergency_reserve_accrual(2026, 4, body, user, db)
    assert exc.value.status_code == 403


def test_delete_entire_reserve_blocks_non_owner() -> None:
    db = MagicMock()
    user = SimpleNamespace(id="user-1")
    db.scalar.return_value = "member"
    with patch("app.api.routes.emergency_reserve._tables_ready", return_value=True):
        with pytest.raises(HTTPException) as exc:
            delete_emergency_reserve(user, db)
    assert exc.value.status_code == 403

