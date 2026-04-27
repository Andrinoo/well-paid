from datetime import date as date_type
from uuid import UUID

from pydantic import BaseModel, Field


class EmergencyReserveUpdate(BaseModel):
    monthly_target_cents: int = Field(ge=0, description="Meta de poupança mensal (centavos)")


class EmergencyReserveResponse(BaseModel):
    monthly_target_cents: int = Field(ge=0)
    balance_cents: int
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
    details: str | None = None
    is_family: bool = False
    monthly_target_cents: int = Field(ge=0)
    target_cents: int | None = Field(default=None, ge=0)
    balance_cents: int
    opening_balance_cents: int = Field(default=0, ge=0)
    tracking_start: date_type
    target_end_date: date_type | None = None
    plan_duration_months: int | None = None
    months_total: int | None = None
    months_passed: int | None = None
    months_remaining: int | None = None
    monthly_needed_cents: int | None = Field(default=None, ge=0)
    pace_status: str = "unknown"
    pace_delta_cents: int = 0
    status: str
    completed_at: date_type | None = None


class EmergencyReservePlanCreate(BaseModel):
    title: str = Field(default="", max_length=200)
    details: str | None = Field(default=None, max_length=1200)
    is_family: bool = False
    monthly_target_cents: int = Field(ge=0)
    target_cents: int | None = Field(default=None, ge=0)
    tracking_start: date_type | None = None
    target_end_date: date_type | None = None
    plan_duration_months: int | None = Field(default=None, ge=1, le=600)
    opening_balance_cents: int | None = Field(
        default=None,
        ge=0,
        description="Aporte inicial em centavos (dinheiro já na reserva ao criar o plano).",
    )


class EmergencyReservePlanUpdate(BaseModel):
    title: str = Field(default="", max_length=200)
    details: str | None = Field(default=None, max_length=1200)
    is_family: bool | None = None
    monthly_target_cents: int = Field(ge=0)
    target_cents: int | None = Field(default=None, ge=0)
    tracking_start: date_type | None = None
    target_end_date: date_type | None = None
    plan_duration_months: int | None = Field(default=None, ge=1, le=600)
    opening_balance_cents: int | None = Field(
        default=None,
        ge=0,
        description="Aporte inicial (centavos). Omitir para não alterar.",
    )


class EmergencyReserveMonthRow(BaseModel):
    year: int
    month: int
    expected_cents: int = Field(ge=0)
    deposited_cents: int = Field(ge=0)
    shortfall_cents: int = Field(ge=0)
    cumulative_expected_cents: int = Field(ge=0)
    cumulative_deposited_cents: int = Field(ge=0)
    cumulative_delta_cents: int
    pace_status: str = "unknown"


class EmergencyReserveCompleteBody(BaseModel):
    goal_id: UUID | None = None
    to_plan_id: UUID | None = None


class EmergencyReserveContributionAllocation(BaseModel):
    plan_id: UUID
    amount_cents: int = Field(gt=0)


class EmergencyReserveContributionCreate(BaseModel):
    contribution_date: date_type | None = None
    total_amount_cents: int = Field(gt=0)
    allocations: list[EmergencyReserveContributionAllocation] = Field(min_length=1)
    note: str | None = Field(default=None, max_length=500)


class EmergencyReserveContributionItem(BaseModel):
    plan_id: UUID
    amount_cents: int = Field(gt=0)


class EmergencyReserveContributionResponse(BaseModel):
    id: UUID
    contribution_date: date_type
    total_amount_cents: int = Field(gt=0)
    note: str | None = None
    created_at: date_type | None = None
    items: list[EmergencyReserveContributionItem]
