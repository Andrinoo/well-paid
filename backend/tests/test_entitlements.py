from __future__ import annotations

import uuid
from datetime import UTC, datetime, timedelta
from types import SimpleNamespace
from unittest.mock import MagicMock

from app.services.entitlements import (
    PLAN_EXPIRED,
    PLAN_FREE,
    PLAN_PAID,
    PLAN_SUPER,
    PLAN_TRIAL,
    expire_trials,
    plan_status,
    start_trial,
)


def test_plan_status_super_and_free() -> None:
    db = MagicMock()
    super_u = SimpleNamespace(
        id=uuid.uuid4(), is_superuser=True, is_free_plan=False, trial_ends_at=None
    )
    free_u = SimpleNamespace(
        id=uuid.uuid4(), is_superuser=False, is_free_plan=True, trial_ends_at=None
    )
    assert plan_status(db, super_u) == PLAN_SUPER
    assert plan_status(db, free_u) == PLAN_FREE
    db.scalar.assert_not_called()


def test_plan_status_paid_trial_expired() -> None:
    db = MagicMock()
    user = SimpleNamespace(
        id=uuid.uuid4(),
        is_superuser=False,
        is_free_plan=False,
        trial_ends_at=datetime.now(UTC) + timedelta(hours=2),
    )
    db.scalar.return_value = SimpleNamespace(due_at=datetime.now(UTC) + timedelta(days=10))
    assert plan_status(db, user) == PLAN_PAID

    db.scalar.return_value = None
    assert plan_status(db, user) == PLAN_TRIAL

    user.trial_ends_at = datetime.now(UTC) - timedelta(hours=1)
    assert plan_status(db, user) == PLAN_EXPIRED


def test_start_trial_skips_free() -> None:
    user = SimpleNamespace(is_free_plan=True, is_superuser=False, trial_ends_at=None)
    start_trial(user)
    assert user.trial_ends_at is None


def test_expire_trials_deactivates_expired_only() -> None:
    expired = SimpleNamespace(
        id=uuid.uuid4(),
        is_active=True,
        is_superuser=False,
        is_free_plan=False,
        trial_ends_at=datetime.now(UTC) - timedelta(hours=1),
    )
    db = MagicMock()
    db.scalars.return_value.all.return_value = [expired]
    db.scalar.return_value = None
    n = expire_trials(db)
    assert n == 1
    assert expired.is_active is False
    db.commit.assert_called_once()
