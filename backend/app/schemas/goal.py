import uuid
from datetime import datetime

from pydantic import BaseModel, ConfigDict, Field


class GoalPriceAlternativeItem(BaseModel):
    label: str = ""
    price_cents: int = Field(default=0, ge=0)
    url: str | None = None


class GoalCreate(BaseModel):
    title: str = Field(min_length=1, max_length=200)
    target_cents: int = Field(gt=0)
    current_cents: int = Field(default=0, ge=0)
    is_active: bool = True
    target_url: str | None = Field(default=None, max_length=2048)
    reference_product_name: str | None = Field(default=None, max_length=500)
    reference_price_cents: int | None = Field(default=None, gt=0)
    reference_currency: str = Field(default="BRL", max_length=8)
    price_source: str | None = Field(default=None, max_length=32)


class GoalUpdate(BaseModel):
    title: str | None = Field(default=None, min_length=1, max_length=200)
    target_cents: int | None = Field(default=None, gt=0)
    current_cents: int | None = Field(default=None, ge=0)
    is_active: bool | None = None
    target_url: str | None = Field(default=None, max_length=2048)
    reference_product_name: str | None = Field(default=None, max_length=500)
    reference_price_cents: int | None = Field(default=None, gt=0)
    reference_currency: str | None = Field(default=None, max_length=8)
    price_source: str | None = Field(default=None, max_length=32)


class GoalContribute(BaseModel):
    amount_cents: int = Field(gt=0)
    note: str | None = Field(default=None, max_length=500)


class GoalContributionResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    goal_id: uuid.UUID
    amount_cents: int
    note: str | None
    recorded_at: datetime


class GoalResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    owner_user_id: uuid.UUID
    is_mine: bool = True
    title: str
    target_cents: int
    current_cents: int
    is_active: bool
    created_at: datetime
    updated_at: datetime
    target_url: str | None = None
    reference_product_name: str | None = None
    reference_price_cents: int | None = None
    reference_currency: str = "BRL"
    price_checked_at: datetime | None = None
    price_source: str | None = None
    price_alternatives: list[dict] = Field(default_factory=list)


class GoalRefreshPriceResponse(BaseModel):
    reference_product_name: str | None = None
    reference_price_cents: int | None = None
    reference_currency: str = "BRL"
    price_checked_at: datetime | None = None
    price_source: str | None = None
    price_alternatives: list[GoalPriceAlternativeItem] = Field(default_factory=list)
