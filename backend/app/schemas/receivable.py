from __future__ import annotations

import uuid
from datetime import date, datetime

from pydantic import BaseModel, ConfigDict, Field


class ExpenseCoverRequest(BaseModel):
    """Pedido de cobertura da tua parte por outro membro (gera dívida a favor do outro)."""

    settle_by: date = Field(description="Prazo em que te comprometes a devolver ao credor")


class ReceivableOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    creditor_user_id: uuid.UUID
    debtor_user_id: uuid.UUID
    amount_cents: int
    settle_by: date
    source_expense_id: uuid.UUID | None = None
    status: str
    settled_at: datetime | None = None
    created_at: datetime
    updated_at: datetime
    debtor_display_name: str | None = Field(
        default=None,
        description="Nome ou e-mail do devedor (para o credor)",
    )
    creditor_display_name: str | None = Field(
        default=None,
        description="Nome ou e-mail do credor (para o devedor)",
    )


class SettleReceivableRequest(BaseModel):
    create_income: bool = False
    income_category_id: uuid.UUID | None = None
    income_date: date | None = Field(
        default=None,
        description="Competência do provento; default hoje",
    )
