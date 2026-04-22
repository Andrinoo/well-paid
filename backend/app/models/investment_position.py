from __future__ import annotations

import uuid
from datetime import date

from sqlalchemy import BigInteger, Boolean, Date, ForeignKey, Integer, String
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base, TimestampMixin


class InvestmentPosition(Base, TimestampMixin):
    __tablename__ = "investment_positions"

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        primary_key=True,
        default=uuid.uuid4,
    )
    owner_user_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("users.id", ondelete="CASCADE"),
        index=True,
    )
    instrument_type: Mapped[str] = mapped_column(String(32), nullable=False, index=True)
    name: Mapped[str] = mapped_column(String(180), nullable=False)
    description: Mapped[str | None] = mapped_column(String(220), nullable=True)
    principal_cents: Mapped[int] = mapped_column(BigInteger, nullable=False)
    annual_rate_bps: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    maturity_date: Mapped[date | None] = mapped_column(Date, nullable=True)
    is_liquid: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True)
