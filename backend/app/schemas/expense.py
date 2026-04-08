import uuid
from datetime import date, datetime

from pydantic import BaseModel, ConfigDict, Field, field_validator, model_validator

from app.schemas.dashboard import ExpenseStatus


class ExpenseCreate(BaseModel):
    description: str = Field(min_length=1, max_length=500)
    amount_cents: int = Field(gt=0)
    expense_date: date
    due_date: date | None = None
    category_id: uuid.UUID
    status: ExpenseStatus = ExpenseStatus.PENDING
    installment_total: int = Field(default=1, ge=1, le=24)
    recurring_frequency: str | None = Field(default=None, max_length=32)

    @field_validator("recurring_frequency")
    @classmethod
    def recurring_ok(cls, v: str | None) -> str | None:
        if v is None or (isinstance(v, str) and not v.strip()):
            return None
        allowed = frozenset({"monthly", "weekly", "yearly"})
        if v not in allowed:
            raise ValueError("recurring_frequency deve ser monthly, weekly ou yearly")
        return v

    @model_validator(mode="after")
    def parcelas_ou_recorrente(self) -> ExpenseCreate:
        if self.installment_total > 1 and self.recurring_frequency is not None:
            raise ValueError(
                "Use parcelas OU recorrência, não ambos (Telas §5.6)."
            )
        return self


class ExpenseUpdate(BaseModel):
    description: str | None = Field(default=None, min_length=1, max_length=500)
    amount_cents: int | None = Field(default=None, gt=0)
    expense_date: date | None = None
    due_date: date | None = None
    category_id: uuid.UUID | None = None
    status: ExpenseStatus | None = None
    sync_status: int | None = Field(default=None, ge=0, le=2)
    recurring_frequency: str | None = Field(default=None, max_length=32)

    @field_validator("recurring_frequency")
    @classmethod
    def recurring_ok_u(cls, v: str | None) -> str | None:
        if v is None:
            return None
        if isinstance(v, str) and not v.strip():
            return None
        allowed = frozenset({"monthly", "weekly", "yearly"})
        if v not in allowed:
            raise ValueError("recurring_frequency deve ser monthly, weekly ou yearly")
        return v


class ExpenseResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    description: str
    amount_cents: int
    expense_date: date
    due_date: date | None
    status: str
    category_id: uuid.UUID
    category_key: str
    category_name: str
    sync_status: int
    installment_total: int
    installment_number: int
    installment_group_id: uuid.UUID | None
    recurring_frequency: str | None
    recurring_series_id: uuid.UUID | None = None
    recurring_generated_until: date | None = None
    created_at: datetime
    updated_at: datetime
