from datetime import date

from pydantic import BaseModel, Field


class EmergencyReserveUpdate(BaseModel):
    monthly_target_cents: int = Field(ge=0, description="Meta de poupança mensal (centavos)")


class EmergencyReserveResponse(BaseModel):
    monthly_target_cents: int = Field(ge=0)
    balance_cents: int = Field(ge=0)
    tracking_start: date = Field(description="Primeiro mês contabilizado (inclusivo)")
    configured: bool = Field(
        default=False,
        description="True se já existe registo persistido (PUT pelo menos uma vez)",
    )


class EmergencyReserveAccrualItem(BaseModel):
    year: int = Field(ge=2000, le=2100)
    month: int = Field(ge=1, le=12)
    amount_cents: int = Field(ge=0)
    created_at: date | None = Field(
        default=None,
        description="Data de registo do crédito (quando disponível).",
    )


class EmergencyReserveAccrualPatch(BaseModel):
    amount_cents: int = Field(
        ge=0,
        description="Valor do crédito mensal em centavos; 0 remove o crédito desse mês.",
    )
