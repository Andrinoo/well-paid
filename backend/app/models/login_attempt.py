from datetime import datetime

from sqlalchemy import DateTime, Integer, String
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base, TimestampMixin


class LoginAttempt(Base, TimestampMixin):
    __tablename__ = "login_attempts"

    email: Mapped[str] = mapped_column(String(320), primary_key=True)
    fail_count: Mapped[int] = mapped_column(Integer, default=0, nullable=False)
    locked_until: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )
    last_failed_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )
