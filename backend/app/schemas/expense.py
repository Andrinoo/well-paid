from __future__ import annotations

import uuid
from datetime import date, datetime
from typing import Literal, Self

from pydantic import BaseModel, ConfigDict, Field, field_validator, model_validator

from app.schemas.dashboard import ExpenseStatus

SplitMode = Literal["amount", "percent"]


class ExpenseCreate(BaseModel):
    description: str = Field(min_length=1, max_length=500)
    amount_cents: int = Field(gt=0)
    expense_date: date
    start_date: date | None = None
    due_date: date | None = None
    category_id: uuid.UUID
    status: ExpenseStatus = ExpenseStatus.PENDING
    installment_total: int = Field(default=1, ge=1, le=999)
    recurring_frequency: str | None = Field(default=None, max_length=32)
    is_shared: bool = False
    is_family: bool = False
    shared_with_user_id: uuid.UUID | None = None
    split_mode: SplitMode | None = None
    owner_share_cents: int | None = Field(default=None, ge=0)
    peer_share_cents: int | None = Field(default=None, ge=0)
    owner_percent_bps: int | None = Field(default=None, ge=0, le=10000)
    peer_percent_bps: int | None = Field(default=None, ge=0, le=10000)

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
    def parcelas_ou_recorrente(self) -> Self:
        if self.installment_total > 1 and self.recurring_frequency is not None:
            raise ValueError(
                "Use parcelas OU recorrência, não ambos (Telas §5.6)."
            )
        if (self.installment_total > 1 or self.recurring_frequency is not None) and self.due_date is None:
            raise ValueError(
                "Parceladas/recorrentes exigem data de vencimento (primeira competência)."
            )
        if not self.is_shared and self.shared_with_user_id is not None:
            raise ValueError("shared_with_user_id exige is_shared true")
        if self.is_shared and self.shared_with_user_id is not None:
            if self.split_mode is None:
                raise ValueError("Despesa partilhada: indica split_mode (amount ou percent)")
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
    is_family: bool | None = None
    shared_with_user_id: uuid.UUID | None = None
    split_mode: SplitMode | None = None
    owner_share_cents: int | None = Field(default=None, ge=0)
    peer_share_cents: int | None = Field(default=None, ge=0)
    owner_percent_bps: int | None = Field(default=None, ge=0, le=10000)
    peer_percent_bps: int | None = Field(default=None, ge=0, le=10000)

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
    def share_update_consistency(self) -> Self:
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
    is_family: bool = False
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
    is_projected: bool = Field(
        default=False,
        description="Ocorrência recorrente mensal ainda não persistida (pré-visualização).",
    )
    is_advanced_payment: bool = Field(
        default=False,
        description="Pagamento adiantado em relação ao vencimento configurado.",
    )
    split_mode: str | None = Field(
        default=None,
        description="amount ou percent quando partilhada",
    )
    owner_percent_bps: int | None = Field(
        default=None,
        description="Parte do dono em basis points (0–10000) quando split_mode == percent",
    )
    peer_percent_bps: int | None = Field(
        default=None,
        description="Parte do outro membro em basis points quando split_mode == percent",
    )
    counterparty_label: str | None = Field(
        default=None,
        description="Nome do outro membro na partilha (visto a partir do utilizador atual)",
    )
    my_share_cents: int | None = None
    other_user_share_cents: int | None = None
    my_share_paid: bool = False
    other_share_paid: bool = False
    shared_expense_payment_alert: bool = Field(
        default=False,
        description="Vencimento passou: o outro já pagou a parte dele e falta a tua",
    )
    shared_expense_peer_declined_alert: bool = Field(
        default=False,
        description="O parceiro recusou a parte dele; o criador deve assumir a despesa nesta linha.",
    )
    my_share_declined: bool = Field(
        default=False,
        description="Recusaste a tua parte nesta linha (aguarda ação do criador).",
    )


class ExpenseCreateOutcome(BaseModel):
    """POST /expenses: uma linha ou plano parcelado (mesmo installment_group_id)."""

    installment_group_id: uuid.UUID | None = Field(
        default=None,
        description="Identificador comum do plano quando installment_total > 1; "
        "liga todas as parcelas (não é uma linha extra na BD).",
    )
    expenses: list[ExpenseResponse]


class ExpensePayRequest(BaseModel):
    allow_advance: bool = False
    amount_cents: int | None = Field(default=None, gt=0)


class ExpenseShareDeclineRequest(BaseModel):
    reason: str | None = Field(default=None, max_length=500)
