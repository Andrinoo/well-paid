from __future__ import annotations

import uuid
from datetime import datetime

from sqlalchemy import BigInteger, DateTime, ForeignKey, String, func
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.models.base import Base


class GoalPriceHistory(Base):
    __tablename__ = "goal_price_history"

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        primary_key=True,
        default=uuid.uuid4,
    )
    goal_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("goals.id", ondelete="CASCADE"),
        index=True,
        nullable=False,
    )
    price_cents: Mapped[int] = mapped_column(BigInteger, nullable=False)
    currency: Mapped[str] = mapped_column(String(8), nullable=False, default="BRL")
    source: Mapped[str | None] = mapped_column(String(64), nullable=True)
    observed_url: Mapped[str | None] = mapped_column(String(2048), nullable=True)
    observed_title: Mapped[str | None] = mapped_column(String(500), nullable=True)
    capture_type: Mapped[str] = mapped_column(String(24), nullable=False, default="manual")
    recorded_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        nullable=False,
        index=True,
    )

    goal: Mapped["Goal"] = relationship("Goal", back_populates="price_history")
