import uuid
from datetime import date, datetime

from pydantic import BaseModel, ConfigDict, Field


class IncomeCreate(BaseModel):
    description: str = Field(min_length=1, max_length=500)
    amount_cents: int = Field(gt=0, description="Valor em centavos (inteiro)")
    income_date: date
    income_category_id: uuid.UUID
    notes: str | None = Field(default=None, max_length=500)


class IncomeUpdate(BaseModel):
    description: str | None = Field(default=None, min_length=1, max_length=500)
    amount_cents: int | None = Field(default=None, gt=0)
    income_date: date | None = None
    income_category_id: uuid.UUID | None = None
    notes: str | None = Field(default=None, max_length=500)
    sync_status: int | None = Field(default=None, ge=0, le=2)


class IncomeResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    owner_user_id: uuid.UUID
    is_mine: bool = True
    description: str
    amount_cents: int
    income_date: date
    income_category_id: uuid.UUID
    category_key: str
    category_name: str
    notes: str | None
    sync_status: int
    created_at: datetime
    updated_at: datetime
