import uuid
from datetime import date, datetime

from sqlalchemy import BigInteger, Boolean, Date, DateTime, ForeignKey, Integer, String
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.models.base import Base, TimestampMixin


class Expense(Base, TimestampMixin):
    __tablename__ = "expenses"

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=uuid.uuid4
    )
    owner_user_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("users.id", ondelete="CASCADE"),
        index=True,
    )
    description: Mapped[str] = mapped_column(String(500))
    amount_cents: Mapped[int] = mapped_column(BigInteger, nullable=False)
    expense_date: Mapped[date] = mapped_column(Date, nullable=False)
    due_date: Mapped[date | None] = mapped_column(Date, nullable=True)
    status: Mapped[str] = mapped_column(String(16), nullable=False, default="pending")
    category_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("categories.id", ondelete="RESTRICT"),
        index=True,
    )
    sync_status: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    installment_total: Mapped[int] = mapped_column(Integer, nullable=False, default=1)
    installment_number: Mapped[int] = mapped_column(Integer, nullable=False, default=1)
    installment_group_id: Mapped[uuid.UUID | None] = mapped_column(
        UUID(as_uuid=True), nullable=True, index=True
    )
    recurring_frequency: Mapped[str | None] = mapped_column(String(32), nullable=True)
    recurring_series_id: Mapped[uuid.UUID | None] = mapped_column(
        UUID(as_uuid=True), nullable=True, index=True
    )
    recurring_generated_until: Mapped[date | None] = mapped_column(Date, nullable=True)
    split_mode: Mapped[str | None] = mapped_column(String(16), nullable=True)
    is_shared: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)
    is_family: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False, index=True)
    shared_with_user_id: Mapped[uuid.UUID | None] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("users.id", ondelete="SET NULL"),
        nullable=True,
        index=True,
    )
    deleted_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True),
        nullable=True,
        index=True,
    )
    paid_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True),
        nullable=True,
    )

    owner: Mapped["User"] = relationship(
        "User",
        foreign_keys=[owner_user_id],
        back_populates="expenses",
    )
    shared_with_user: Mapped["User | None"] = relationship(
        "User",
        foreign_keys=[shared_with_user_id],
        viewonly=True,
    )
    category: Mapped["Category"] = relationship(
        "Category", back_populates="expenses"
    )
    expense_shares: Mapped[list["ExpenseShare"]] = relationship(
        "ExpenseShare",
        back_populates="expense",
        cascade="all, delete-orphan",
    )
