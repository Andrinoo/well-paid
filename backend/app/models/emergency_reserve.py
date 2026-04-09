import uuid
from datetime import date, datetime

from sqlalchemy import BigInteger, Date, DateTime, ForeignKey, Integer, UniqueConstraint, func
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.models.base import Base, TimestampMixin


class EmergencyReserve(Base, TimestampMixin):
    """Uma reserva por família ou por utilizador sem família."""

    __tablename__ = "emergency_reserves"

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=uuid.uuid4
    )
    family_id: Mapped[uuid.UUID | None] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("families.id", ondelete="CASCADE"),
        nullable=True,
        unique=True,
    )
    solo_user_id: Mapped[uuid.UUID | None] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("users.id", ondelete="CASCADE"),
        nullable=True,
        unique=True,
    )
    monthly_target_cents: Mapped[int] = mapped_column(BigInteger, nullable=False, default=0)
    balance_cents: Mapped[int] = mapped_column(BigInteger, nullable=False, default=0)
    tracking_start: Mapped[date] = mapped_column(Date, nullable=False)

    accruals: Mapped[list["EmergencyReserveAccrual"]] = relationship(
        "EmergencyReserveAccrual",
        back_populates="reserve",
        cascade="all, delete-orphan",
    )


class EmergencyReserveAccrual(Base):
    """Um crédito mensal idempotente na reserva."""

    __tablename__ = "emergency_reserve_accruals"
    __table_args__ = (
        UniqueConstraint(
            "reserve_id",
            "year",
            "month",
            name="uq_emergency_reserve_accruals_reserve_year_month",
        ),
    )

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=uuid.uuid4
    )
    reserve_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("emergency_reserves.id", ondelete="CASCADE"),
        index=True,
    )
    year: Mapped[int] = mapped_column(Integer, nullable=False)
    month: Mapped[int] = mapped_column(Integer, nullable=False)
    amount_cents: Mapped[int] = mapped_column(BigInteger, nullable=False)

    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )

    reserve: Mapped["EmergencyReserve"] = relationship(
        "EmergencyReserve", back_populates="accruals"
    )
