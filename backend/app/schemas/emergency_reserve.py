from datetime import date as date_type
from uuid import UUID

from pydantic import BaseModel, Field


class EmergencyReserveUpdate(BaseModel):
    monthly_target_cents: int = Field(ge=0, description="Meta de poupança mensal (centavos)")


class EmergencyReserveResponse(BaseModel):
    monthly_target_cents: int = Field(ge=0)
    balance_cents: int = Field(ge=0)
    tracking_start: date_type = Field(description="Primeiro mês contabilizado (inclusivo)")
    configured: bool = Field(
        default=False,
        description="True se já existe registo persistido (PUT pelo menos uma vez)",
    )


class EmergencyReserveAccrualItem(BaseModel):
    year: int = Field(ge=2000, le=2100)
    month: int = Field(ge=1, le=12)
    amount_cents: int = Field(ge=0)
    created_at: date_type | None = Field(
        default=None,
        description="Data de registo do crédito (quando disponível).",
    )


class EmergencyReserveAccrualPatch(BaseModel):
    amount_cents: int = Field(
        ge=0,
        description="Valor do crédito mensal em centavos; 0 remove o crédito desse mês.",
    )


class EmergencyReservePlanItem(BaseModel):
    id: UUID
    title: str
    monthly_target_cents: int = Field(ge=0)
    balance_cents: int = Field(ge=0)
    tracking_start: date_type
    plan_duration_months: int | None = None
    status: str
    completed_at: date_type | None = None


class EmergencyReservePlanCreate(BaseModel):
    title: str = Field(default="", max_length=200)
    monthly_target_cents: int = Field(ge=0)
    tracking_start: date_type | None = None
    plan_duration_months: int | None = Field(default=None, ge=1, le=600)


class EmergencyReservePlanUpdate(BaseModel):
    title: str = Field(default="", max_length=200)
    monthly_target_cents: int = Field(ge=0)
    tracking_start: date_type | None = None
    plan_duration_months: int | None = Field(default=None, ge=1, le=600)


class EmergencyReserveMonthRow(BaseModel):
    year: int
    month: int
    expected_cents: int = Field(ge=0)
    deposited_cents: int = Field(ge=0)
    shortfall_cents: int = Field(ge=0)


class EmergencyReserveCompleteBody(BaseModel):
    goal_id: UUID | None = None
    to_plan_id: UUID | None = None
