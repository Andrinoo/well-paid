import uuid
from datetime import date

from sqlalchemy import BigInteger, Date, ForeignKey, Integer, String
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.models.base import Base, TimestampMixin


class Income(Base, TimestampMixin):
    __tablename__ = "incomes"

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
    income_date: Mapped[date] = mapped_column(Date, nullable=False, index=True)
    income_category_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("income_categories.id", ondelete="RESTRICT"),
        index=True,
    )
    notes: Mapped[str | None] = mapped_column(String(500), nullable=True)
    sync_status: Mapped[int] = mapped_column(Integer, nullable=False, default=0)

    owner: Mapped["User"] = relationship("User", back_populates="incomes")
    income_category: Mapped["IncomeCategory"] = relationship(
        "IncomeCategory",
        back_populates="incomes",
    )
