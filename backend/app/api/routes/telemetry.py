from datetime import UTC, datetime, time, timedelta
from typing import Annotated

from fastapi import APIRouter, Depends, Request
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.api.deps import get_current_user
from app.core.database import get_db
from app.core.limiter import limiter
from app.models.app_usage_event import AppUsageEvent
from app.models.user import User
from app.schemas.telemetry import TelemetryPingRequest, TelemetryPingResponse

router = APIRouter(prefix="/telemetry", tags=["telemetry"])

# Estratégia econômica para plano gratuito:
# no máximo 1 registo por dia (UTC) para cada user + event_type.
_ALLOWED_EVENT_TYPES = frozenset({"app_open"})


@router.post("/ping", response_model=TelemetryPingResponse)
@limiter.limit("30/minute")
def ping(
    request: Request,
    body: TelemetryPingRequest,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> TelemetryPingResponse:
    event_type = (body.event_type or "app_open").strip().lower()
    if event_type not in _ALLOWED_EVENT_TYPES:
        event_type = "app_open"

    now = datetime.now(UTC)
    start_day = datetime.combine(now.date(), time.min, tzinfo=UTC)
    next_day = start_day + timedelta(days=1)

    existing = db.scalar(
        select(AppUsageEvent.id).where(
            AppUsageEvent.user_id == user.id,
            AppUsageEvent.event_type == event_type,
            AppUsageEvent.occurred_at >= start_day,
            AppUsageEvent.occurred_at < next_day,
        )
    )
    if existing is not None:
        return TelemetryPingResponse(
            accepted=True,
            deduped=True,
            event_type=event_type,
        )

    db.add(
        AppUsageEvent(
            user_id=user.id,
            event_type=event_type,
            occurred_at=now,
        )
    )
    user.last_seen_at = now
    db.add(user)
    db.commit()
    return TelemetryPingResponse(
        accepted=True,
        deduped=False,
        event_type=event_type,
    )
