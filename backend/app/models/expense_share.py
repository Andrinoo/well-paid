import uuid
from datetime import datetime

from sqlalchemy import BigInteger, DateTime, ForeignKey, Integer, String
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.models.base import Base, TimestampMixin


class ExpenseShare(Base, TimestampMixin):
    __tablename__ = "expense_shares"

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=uuid.uuid4
    )
    expense_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("expenses.id", ondelete="CASCADE"),
        index=True,
    )
    user_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("users.id", ondelete="CASCADE"),
        index=True,
    )
    share_cents: Mapped[int] = mapped_column(BigInteger, nullable=False)
    share_percent_bps: Mapped[int | None] = mapped_column(Integer, nullable=True)
    paid_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )
    status: Mapped[str] = mapped_column(String(24), nullable=False, default="pending")
    covered_by_user_id: Mapped[uuid.UUID | None] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("users.id", ondelete="SET NULL"),
        nullable=True,
    )
    covered_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )
    declined_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )
    decline_reason: Mapped[str | None] = mapped_column(String(500), nullable=True)

    expense: Mapped["Expense"] = relationship(
        "Expense",
        back_populates="expense_shares",
    )
    user: Mapped["User"] = relationship(
        "User",
        foreign_keys=[user_id],
        viewonly=True,
    )
    covered_by_user: Mapped["User | None"] = relationship(
        "User",
        foreign_keys=[covered_by_user_id],
        viewonly=True,
    )
