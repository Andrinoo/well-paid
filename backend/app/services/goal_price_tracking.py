from __future__ import annotations

import logging
from datetime import UTC, datetime, timedelta
from typing import Any

from sqlalchemy import and_, or_
from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.models.announcement import Announcement
from app.models.goal import Goal
from app.models.goal_price_history import GoalPriceHistory
from app.services.goal_product_search import search_products_google_shopping
from app.services.goal_reference_price import fetch_product_hints

logger = logging.getLogger(__name__)


def _title_or_description_hints(query: str) -> dict[str, Any]:
    settings = get_settings()
    if not (settings.serpapi_key or "").strip():
        return {}
    q = (query or "").strip()
    if len(q) < 2:
        return {}
    rows = search_products_google_shopping(
        q,
        serpapi_key=settings.serpapi_key,
        serp_limit=12,
        serp_timeout_s=12.0,
        max_total=25,
    )
    if not rows:
        return {}
    first = rows[0]
    try:
        price = int(first.get("price_cents") or 0)
    except (TypeError, ValueError):
        price = 0
    if price <= 0:
        return {}
    return {
        "price_cents": price,
        "title": str(first.get("title") or "")[:500] or None,
        "url": str(first.get("url") or "").strip() or None,
        "source": str(first.get("source") or "google_shopping")[:64],
    }


def _record_history(
    db: Session,
    goal: Goal,
    *,
    price_cents: int,
    capture_type: str,
    source: str | None,
    observed_url: str | None,
    observed_title: str | None,
) -> None:
    if price_cents <= 0:
        return
    db.add(
        GoalPriceHistory(
            goal_id=goal.id,
            price_cents=price_cents,
            currency=(goal.reference_currency or "BRL")[:8],
            source=(source or "unavailable")[:64],
            observed_url=observed_url,
            observed_title=(observed_title or goal.reference_product_name or goal.title)[:500],
            capture_type=capture_type[:24],
        )
    )


def _create_opportunity_announcement(
    db: Session,
    goal: Goal,
    *,
    old_price: int,
    new_price: int,
    source: str | None,
    url: str | None,
    title: str | None,
) -> None:
    now = datetime.now(UTC)
    dedupe_cutoff = now - timedelta(hours=24)
    existing = (
        db.query(Announcement)
        .filter(
            Announcement.target_user_id == goal.owner_user_id,
            Announcement.kind == "goal_opportunity",
            Announcement.is_active.is_(True),
            Announcement.created_at >= dedupe_cutoff,
            Announcement.cta_url == (url or goal.target_url),
        )
        .first()
    )
    if existing is not None:
        return
    saved = max(old_price - new_price, 0)
    body = (
        f"Meta '{goal.title}': apareceu oportunidade por {new_price / 100:.2f} BRL "
        f"(antes {old_price / 100:.2f} BRL). Economia de {saved / 100:.2f} BRL."
    )
    if title:
        body += f" Oferta: {title}."
    if source:
        body += f" Fonte: {source}."
    db.add(
        Announcement(
            title="Oportunidade em Meta",
            body=body[:5000],
            kind="goal_opportunity",
            placement="home_feed",
            priority=95,
            cta_label="Ver meta",
            cta_url=(url or goal.target_url),
            is_active=True,
            starts_at=now,
            ends_at=now + timedelta(days=7),
            target_user_id=goal.owner_user_id,
        )
    )


def run_goal_price_tracking_cycle(db: Session) -> dict[str, int]:
    settings = get_settings()
    now = datetime.now(UTC)
    eligible = (
        db.query(Goal)
        .filter(
            Goal.is_active.is_(True),
            or_(Goal.due_at.is_(None), Goal.due_at >= now),
            or_(
                and_(Goal.target_url.isnot(None), Goal.target_url != ""),
                Goal.reference_product_name.isnot(None),
                Goal.title.isnot(None),
            ),
        )
        .all()
    )
    scanned = 0
    updated = 0
    alerts = 0
    for goal in eligible:
        interval_hours = max(6, min(24, int(goal.price_check_interval_hours or 12)))
        if goal.last_price_track_at is not None and goal.last_price_track_at > (
            now - timedelta(hours=interval_hours)
        ):
            continue
        scanned += 1
        try:
            primary: dict[str, Any] = {}
            if goal.target_url and goal.target_url.strip():
                primary = fetch_product_hints(goal.target_url.strip())
            elif (goal.reference_product_name or "").strip():
                primary = _title_or_description_hints(goal.reference_product_name or "")
            else:
                primary = _title_or_description_hints(goal.title or "")

            primary_price = int(primary.get("reference_price_cents") or primary.get("price_cents") or 0)
            old_price = int(goal.reference_price_cents or 0)
            if primary_price > 0:
                goal.reference_price_cents = primary_price
                goal.price_source = str(primary.get("price_source") or primary.get("source") or goal.price_source or "unavailable")
                goal.price_checked_at = now
                goal.last_price_track_at = now
                if old_price <= 0 or primary_price < old_price:
                    goal.target_cents = primary_price
                    if int(goal.current_cents or 0) > primary_price:
                        goal.current_cents = primary_price
                _record_history(
                    db,
                    goal,
                    price_cents=primary_price,
                    capture_type="scheduled",
                    source=goal.price_source,
                    observed_url=goal.target_url,
                    observed_title=primary.get("reference_product_name") or primary.get("title"),
                )
                updated += 1
            # Opportunities from secondary query (name/description) only produce alerts.
            secondary_query = (goal.description or goal.reference_product_name or goal.title or "").strip()
            secondary = _title_or_description_hints(secondary_query)
            secondary_price = int(secondary.get("price_cents") or 0)
            drop_threshold = max(0.0, float(settings.goal_tracking_drop_threshold_pct))
            threshold_price = int(old_price * (1 - (drop_threshold / 100.0))) if old_price > 0 else 0
            if secondary_price > 0 and old_price > 0 and secondary_price < threshold_price:
                _create_opportunity_announcement(
                    db,
                    goal,
                    old_price=old_price,
                    new_price=secondary_price,
                    source=secondary.get("source"),
                    url=secondary.get("url"),
                    title=secondary.get("title"),
                )
                alerts += 1
        except Exception:
            logger.exception("Goal tracking failed for goal_id=%s", goal.id)
    db.commit()
    return {"scanned": scanned, "updated": updated, "alerts": alerts}
