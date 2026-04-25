import uuid
from datetime import datetime

from pydantic import BaseModel, ConfigDict, Field


class GoalPriceAlternativeItem(BaseModel):
    label: str = ""
    price_cents: int = Field(default=0, ge=0)
    url: str | None = None


class GoalPriceHistoryItem(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    goal_id: uuid.UUID
    price_cents: int = Field(ge=0)
    currency: str = "BRL"
    source: str | None = None
    observed_url: str | None = None
    observed_title: str | None = None
    capture_type: str = "manual"
    recorded_at: datetime


class GoalCreate(BaseModel):
    title: str = Field(min_length=1, max_length=200)
    target_cents: int = Field(gt=0)
    current_cents: int = Field(default=0, ge=0)
    is_active: bool = True
    is_family: bool = False
    target_url: str | None = Field(default=None, max_length=2048)
    reference_product_name: str | None = Field(default=None, max_length=500)
    reference_price_cents: int | None = Field(default=None, gt=0)
    reference_currency: str = Field(default="BRL", max_length=8)
    price_source: str | None = Field(default=None, max_length=32)
    reference_thumbnail_url: str | None = Field(default=None, max_length=2048)
    description: str | None = Field(default=None, max_length=1000)
    due_at: datetime | None = None
    price_check_interval_hours: int = Field(default=12, ge=6, le=24)


class GoalUpdate(BaseModel):
    title: str | None = Field(default=None, min_length=1, max_length=200)
    target_cents: int | None = Field(default=None, gt=0)
    current_cents: int | None = Field(default=None, ge=0)
    is_active: bool | None = None
    is_family: bool | None = None
    target_url: str | None = Field(default=None, max_length=2048)
    reference_product_name: str | None = Field(default=None, max_length=500)
    reference_price_cents: int | None = Field(default=None, gt=0)
    reference_currency: str | None = Field(default=None, max_length=8)
    price_source: str | None = Field(default=None, max_length=32)
    reference_thumbnail_url: str | None = Field(default=None, max_length=2048)
    description: str | None = Field(default=None, max_length=1000)
    due_at: datetime | None = None
    price_check_interval_hours: int | None = Field(default=None, ge=6, le=24)


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
    is_family: bool = False
    created_at: datetime
    updated_at: datetime
    target_url: str | None = None
    reference_product_name: str | None = None
    reference_price_cents: int | None = None
    reference_currency: str = "BRL"
    price_checked_at: datetime | None = None
    price_source: str | None = None
    reference_thumbnail_url: str | None = None
    description: str | None = None
    due_at: datetime | None = None
    price_check_interval_hours: int = 12
    last_price_track_at: datetime | None = None
    price_alternatives: list[dict] = Field(default_factory=list)


class GoalPriceHistoryResponse(BaseModel):
    goal_id: uuid.UUID
    items: list[GoalPriceHistoryItem] = Field(default_factory=list)


class GoalRefreshPriceResponse(BaseModel):
    reference_product_name: str | None = None
    reference_price_cents: int | None = None
    reference_currency: str = "BRL"
    price_checked_at: datetime | None = None
    price_source: str | None = None
    price_alternatives: list[GoalPriceAlternativeItem] = Field(default_factory=list)


class GoalPreviewFromUrlBody(BaseModel):
    url: str = Field(min_length=8, max_length=2048)


class GoalPreviewFromUrlResponse(BaseModel):
    reference_product_name: str | None = None
    reference_price_cents: int | None = None
    suggested_target_cents: int | None = None
    reference_currency: str = "BRL"
    price_source: str | None = None


class GoalProductSearchBody(BaseModel):
    query: str = Field(min_length=2, max_length=200)


class GoalProductHit(BaseModel):
    title: str
    price_cents: int = Field(ge=0)
    currency_id: str = "BRL"
    url: str
    thumbnail: str | None = None
    source: str = "google_shopping"
    external_id: str | None = None


class GoalProductSearchResponse(BaseModel):
    results: list[GoalProductHit] = Field(default_factory=list)
