import uuid
from datetime import datetime

from sqlalchemy import Boolean, DateTime, ForeignKey, Integer, String, Text, UniqueConstraint
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.models.base import Base, TimestampMixin


class Payment(Base, TimestampMixin):
    __tablename__ = "payments"

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=uuid.uuid4
    )
    user_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("users.id", ondelete="CASCADE"),
        index=True,
    )
    amount_cents: Mapped[int] = mapped_column(Integer, nullable=False)
    currency: Mapped[str] = mapped_column(String(8), default="BRL", nullable=False)
    method: Mapped[str] = mapped_column(String(32), default="pix", nullable=False)
    status: Mapped[str] = mapped_column(String(32), default="pending", nullable=False)
    payer_name: Mapped[str | None] = mapped_column(String(200), nullable=True)
    pix_id: Mapped[str | None] = mapped_column(String(128), nullable=True)
    pix_copy: Mapped[str | None] = mapped_column(Text, nullable=True)
    pix_qr: Mapped[str | None] = mapped_column(Text, nullable=True)
    paid_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )
    due_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True, index=True
    )
    pix_received_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )
    notes: Mapped[str | None] = mapped_column(String(500), nullable=True)

    user: Mapped["User"] = relationship("User", back_populates="payments")


class UserModule(Base, TimestampMixin):
    __tablename__ = "user_modules"
    __table_args__ = (
        UniqueConstraint("user_id", "module_id", name="uq_user_modules_user_module"),
    )

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=uuid.uuid4
    )
    user_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("users.id", ondelete="CASCADE"),
        index=True,
    )
    module_id: Mapped[str] = mapped_column(String(64), nullable=False)
    enabled: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)

    user: Mapped["User"] = relationship("User", back_populates="modules")
