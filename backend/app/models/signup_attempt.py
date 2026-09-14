from datetime import datetime

from sqlalchemy import DateTime, Integer, String
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base, TimestampMixin


class SignupAttempt(Base, TimestampMixin):
    __tablename__ = "signup_attempts"

    ip_hash: Mapped[str] = mapped_column(String(64), primary_key=True)
    success_count: Mapped[int] = mapped_column(Integer, default=0, nullable=False)
    window_started_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )
    locked_until: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )
    last_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )
