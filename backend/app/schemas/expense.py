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
    is_shared: bool = False
    shared_with_user_id: uuid.UUID | None = None

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
        if not self.is_shared and self.shared_with_user_id is not None:
            raise ValueError("shared_with_user_id exige is_shared true")
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
    is_shared: bool | None = None
    shared_with_user_id: uuid.UUID | None = None

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

    @model_validator(mode="after")
    def share_update_consistency(self) -> ExpenseUpdate:
        if self.is_shared is False and self.shared_with_user_id is not None:
            raise ValueError("Não uses shared_with_user_id com is_shared false")
        return self


class ExpenseResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    owner_user_id: uuid.UUID
    is_mine: bool = True
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
    is_shared: bool = False
    shared_with_user_id: uuid.UUID | None = None
    shared_with_label: str | None = Field(
        default=None,
        description="Nome ou e-mail do membro (só quando shared_with_user_id definido)",
    )
    created_at: datetime
    updated_at: datetime
    paid_at: datetime | None = None
    installment_plan_has_paid: bool | None = Field(
        default=None,
        description="Só em GET /expenses/{id}: se há parcelas pagas no plano.",
    )


class ExpenseCreateOutcome(BaseModel):
    """POST /expenses: uma linha ou plano parcelado (mesmo installment_group_id)."""

    installment_group_id: uuid.UUID | None = Field(
        default=None,
        description="Identificador comum do plano quando installment_total > 1; "
        "liga todas as parcelas (não é uma linha extra na BD).",
    )
    expenses: list[ExpenseResponse]
