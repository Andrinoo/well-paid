from __future__ import annotations

import uuid
from datetime import UTC, datetime
from types import SimpleNamespace
from unittest.mock import MagicMock

from app.api.routes import goals as goals_route
from app.schemas.goal import GoalContribute, GoalUpdate


def _fake_user() -> SimpleNamespace:
    return SimpleNamespace(id=uuid.uuid4())


def _fake_goal(owner_id: uuid.UUID) -> SimpleNamespace:
    now = datetime.now(UTC)
    return SimpleNamespace(
        id=uuid.uuid4(),
        owner_user_id=owner_id,
        title="Meta",
        target_cents=10000,
        current_cents=1000,
        is_active=True,
        is_family=False,
        created_at=now,
        updated_at=now,
        target_url=None,
        reference_product_name=None,
        reference_price_cents=None,
        reference_currency="BRL",
        price_checked_at=None,
        price_source=None,
        reference_thumbnail_url=None,
        description=None,
        due_at=None,
        price_check_interval_hours=12,
        last_price_track_at=None,
        tracking_enabled=True,
        tracking_failures=0,
        next_track_after=None,
        price_alternatives=[],
    )


def test_contribute_goal_performs_atomic_update(monkeypatch) -> None:
    user = _fake_user()
    row = _fake_goal(user.id)
    db = MagicMock()
    query = db.query.return_value
    filtered = query.filter.return_value

    monkeypatch.setattr(goals_route, "_owned_goal", lambda *_: row)
    body = GoalContribute(amount_cents=250, note="aporte")

    _ = goals_route.contribute_goal(row.id, body, user, db)

    filtered.update.assert_called_once()
    db.commit.assert_called_once()
    db.refresh.assert_called_once_with(row)


def test_update_goal_ignores_current_cents_field(monkeypatch) -> None:
    user = _fake_user()
    row = _fake_goal(user.id)
    db = MagicMock()

    monkeypatch.setattr(goals_route, "_owned_goal", lambda *_: row)
    body = GoalUpdate(
        title="Meta editada",
        target_cents=15000,
        current_cents=999999,
        is_active=True,
    )

    _ = goals_route.update_goal(row.id, body, user, db)

    assert row.current_cents == 1000
    assert row.target_cents == 15000
    assert row.title == "Meta editada"
