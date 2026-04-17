from __future__ import annotations

import uuid
from datetime import datetime
from typing import Any

from pydantic import BaseModel, ConfigDict, Field


class FamilyFinancialEventOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    event_type: str
    created_at: datetime
    amount_cents: int | None = None
    source_expense_id: uuid.UUID | None = None
    source_expense_share_id: uuid.UUID | None = None
    source_receivable_id: uuid.UUID | None = None
    payload_json: dict[str, Any] | None = None
    actor_user_id: uuid.UUID
    counterparty_user_id: uuid.UUID | None = None
    actor_label: str | None = Field(
        default=None,
        description="Nome ou e-mail do autor para UI",
    )
    counterparty_label: str | None = Field(
        default=None,
        description="Nome ou e-mail da contraparte para UI",
    )
