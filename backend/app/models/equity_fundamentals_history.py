from __future__ import annotations

import uuid
from datetime import datetime

from sqlalchemy import DateTime, Float, String, func
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base


class EquityFundamentalsHistory(Base):
    __tablename__ = "equity_fundamentals_history"

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        primary_key=True,
        default=uuid.uuid4,
    )
    symbol: Mapped[str] = mapped_column(String(12), nullable=False, index=True)
    pl: Mapped[str | None] = mapped_column(String(64), nullable=True)
    pvp: Mapped[str | None] = mapped_column(String(64), nullable=True)
    dividend_yield: Mapped[str | None] = mapped_column(String(64), nullable=True)
    roe: Mapped[str | None] = mapped_column(String(64), nullable=True)
    ev_ebitda: Mapped[str | None] = mapped_column(String(64), nullable=True)
    net_margin: Mapped[str | None] = mapped_column(String(64), nullable=True)
    net_debt_ebitda: Mapped[str | None] = mapped_column(String(64), nullable=True)
    eps: Mapped[str | None] = mapped_column(String(64), nullable=True)
    source: Mapped[str] = mapped_column(String(32), nullable=False, default="fundamentus")
    confidence: Mapped[float | None] = mapped_column(Float, nullable=True)
    collected_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        nullable=False,
        server_default=func.now(),
        index=True,
    )
