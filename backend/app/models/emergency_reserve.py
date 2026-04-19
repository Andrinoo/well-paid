import uuid
from datetime import date, datetime

from sqlalchemy import BigInteger, Date, DateTime, ForeignKey, Integer, String, UniqueConstraint, func, text
from sqlalchemy.dialects.postgresql import JSONB, UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.models.base import Base, TimestampMixin


class EmergencyReservePlan(Base, TimestampMixin):
    """Plano de reserva (vários por família ou utilizador solo)."""

    __tablename__ = "emergency_reserve_plans"

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=uuid.uuid4
    )
    family_id: Mapped[uuid.UUID | None] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("families.id", ondelete="CASCADE"),
        nullable=True,
        index=True,
    )
    solo_user_id: Mapped[uuid.UUID | None] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("users.id", ondelete="CASCADE"),
        nullable=True,
        index=True,
    )
    title: Mapped[str] = mapped_column(String(200), nullable=False, default="")
    monthly_target_cents: Mapped[int] = mapped_column(BigInteger, nullable=False, default=0)
    balance_cents: Mapped[int] = mapped_column(BigInteger, nullable=False, default=0)
    tracking_start: Mapped[date] = mapped_column(Date, nullable=False)
    accrual_skip_months: Mapped[list[str]] = mapped_column(
        JSONB,
        nullable=False,
        default=list,
        server_default=text("'[]'::jsonb"),
    )
    plan_duration_months: Mapped[int | None] = mapped_column(Integer, nullable=True)
    status: Mapped[str] = mapped_column(String(20), nullable=False, default="active")
    completed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    transfer_goal_id: Mapped[uuid.UUID | None] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("goals.id", ondelete="SET NULL"),
        nullable=True,
    )
    transfer_to_plan_id: Mapped[uuid.UUID | None] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("emergency_reserve_plans.id", ondelete="SET NULL"),
        nullable=True,
    )

    accruals: Mapped[list["EmergencyReserveAccrual"]] = relationship(
        "EmergencyReserveAccrual",
        back_populates="plan",
        cascade="all, delete-orphan",
    )


class EmergencyReserveAccrual(Base):
    """Crédito mensal idempotente num plano de reserva."""

    __tablename__ = "emergency_reserve_accruals"
    __table_args__ = (
        UniqueConstraint(
            "plan_id",
            "year",
            "month",
            name="uq_emergency_reserve_accruals_plan_year_month",
        ),
    )

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=uuid.uuid4
    )
    plan_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("emergency_reserve_plans.id", ondelete="CASCADE"),
        index=True,
    )
    year: Mapped[int] = mapped_column(Integer, nullable=False)
    month: Mapped[int] = mapped_column(Integer, nullable=False)
    amount_cents: Mapped[int] = mapped_column(BigInteger, nullable=False)

    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )

    plan: Mapped["EmergencyReservePlan"] = relationship(
        "EmergencyReservePlan", back_populates="accruals"
    )
