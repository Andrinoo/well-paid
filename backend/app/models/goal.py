from __future__ import annotations

import uuid
from datetime import datetime

from sqlalchemy import BigInteger, Boolean, DateTime, ForeignKey, String, text
from sqlalchemy.dialects.postgresql import JSONB, UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.models.base import Base, TimestampMixin


class Goal(Base, TimestampMixin):
    __tablename__ = "goals"

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=uuid.uuid4
    )
    owner_user_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("users.id", ondelete="CASCADE"),
        index=True,
    )
    title: Mapped[str] = mapped_column(String(200))
    target_cents: Mapped[int] = mapped_column(BigInteger, nullable=False)
    current_cents: Mapped[int] = mapped_column(BigInteger, nullable=False, default=0)
    is_active: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True)
    target_url: Mapped[str | None] = mapped_column(String(2048), nullable=True)
    reference_product_name: Mapped[str | None] = mapped_column(String(500), nullable=True)
    reference_price_cents: Mapped[int | None] = mapped_column(BigInteger, nullable=True)
    reference_currency: Mapped[str] = mapped_column(String(8), nullable=False, default="BRL")
    price_checked_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    price_source: Mapped[str | None] = mapped_column(String(32), nullable=True)
    reference_thumbnail_url: Mapped[str | None] = mapped_column(
        String(2048), nullable=True
    )
    price_alternatives: Mapped[list] = mapped_column(
        JSONB,
        nullable=False,
        default=list,
        server_default=text("'[]'::jsonb"),
    )

    owner: Mapped["User"] = relationship("User", back_populates="goals")
    contributions: Mapped[list["GoalContribution"]] = relationship(
        "GoalContribution",
        back_populates="goal",
        cascade="all, delete-orphan",
    )
