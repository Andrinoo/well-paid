from __future__ import annotations

import uuid
from datetime import UTC, datetime
from types import SimpleNamespace
from unittest.mock import MagicMock

from app.services import goal_price_tracking


def test_tracking_opportunity_lowers_goal_to_market_price(monkeypatch) -> None:
    now = datetime.now(UTC)
    goal = SimpleNamespace(
        id=uuid.uuid4(),
        owner_user_id=uuid.uuid4(),
        title="Notebook",
        description="Notebook Gamer",
        target_cents=100_000,
        current_cents=95_000,
        reference_price_cents=100_000,
        reference_currency="BRL",
        reference_product_name="Notebook Gamer",
        price_source="manual",
        price_checked_at=None,
        last_price_track_at=None,
        next_track_after=None,
        tracking_failures=0,
        price_check_interval_hours=6,
        target_url="https://example.com/notebook",
    )
    db = MagicMock()
    db.execute.return_value.scalar.return_value = True
    db.query.return_value.filter.return_value.all.return_value = [goal]

    monkeypatch.setattr(
        goal_price_tracking,
        "get_settings",
        lambda: SimpleNamespace(goal_tracking_drop_threshold_pct=1),
    )
    monkeypatch.setattr(goal_price_tracking, "fetch_product_hints", lambda *_args, **_kwargs: {})
    monkeypatch.setattr(
        goal_price_tracking,
        "_title_or_description_hints",
        lambda *_args, **_kwargs: {
            "price_cents": 90_000,
            "source": "google_shopping",
            "url": "https://shop.example/notebook",
            "title": "Notebook Gamer em oferta",
        },
    )

    result = goal_price_tracking.run_goal_price_tracking_cycle(db)

    assert result == {"scanned": 1, "updated": 1, "alerts": 1, "skipped": 0}
    assert goal.reference_price_cents == 90_000
    assert goal.target_cents == 90_000
    assert goal.current_cents == 90_000
    assert goal.reference_product_name == "Notebook Gamer em oferta"
    assert goal.price_source == "google_shopping"
    assert goal.price_checked_at is not None
    assert goal.last_price_track_at is not None
    db.commit.assert_called_once()
