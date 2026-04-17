import uuid
from datetime import date, datetime

from sqlalchemy import BigInteger, Date, DateTime, ForeignKey, String
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.models.base import Base, TimestampMixin


class FamilyReceivable(Base, TimestampMixin):
    """Dívida entre membros da família (ex.: cobertura de parte de uma despesa)."""

    __tablename__ = "family_receivables"

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=uuid.uuid4
    )
    creditor_user_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("users.id", ondelete="CASCADE"),
        index=True,
    )
    debtor_user_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("users.id", ondelete="CASCADE"),
        index=True,
    )
    amount_cents: Mapped[int] = mapped_column(BigInteger, nullable=False)
    settle_by: Mapped[date] = mapped_column(Date, nullable=False)
    source_expense_id: Mapped[uuid.UUID | None] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("expenses.id", ondelete="SET NULL"),
        nullable=True,
    )
    source_expense_share_id: Mapped[uuid.UUID | None] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("expense_shares.id", ondelete="SET NULL"),
        nullable=True,
    )
    status: Mapped[str] = mapped_column(String(16), nullable=False, default="open")
    settled_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )

    creditor: Mapped["User"] = relationship(
        "User",
        foreign_keys=[creditor_user_id],
        viewonly=True,
    )
    debtor: Mapped["User"] = relationship(
        "User",
        foreign_keys=[debtor_user_id],
        viewonly=True,
    )
