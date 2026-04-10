import uuid
from datetime import date, datetime
from typing import Self

from pydantic import BaseModel, ConfigDict, Field, field_validator, model_validator

from app.schemas.dashboard import ExpenseStatus


class ShoppingListCreate(BaseModel):
    title: str | None = Field(default=None, max_length=200)
    store_name: str | None = Field(default=None, max_length=200)

    @field_validator("title", "store_name", mode="before")
    @classmethod
    def empty_to_none(cls, v: str | None) -> str | None:
        if v is None:
            return None
        if isinstance(v, str):
            s = v.strip()
            return s if s else None
        return v


class ShoppingListPatch(BaseModel):
    title: str | None = Field(default=None, max_length=200)
    store_name: str | None = Field(default=None, max_length=200)

    @field_validator("title", "store_name", mode="before")
    @classmethod
    def empty_to_none(cls, v: str | None) -> str | None:
        if v is None:
            return None
        if isinstance(v, str):
            s = v.strip()
            return s if s else None
        return v


class ShoppingListItemCreate(BaseModel):
    label: str = Field(min_length=1, max_length=500)
    line_amount_cents: int | None = Field(default=None, gt=0)

    @field_validator("label")
    @classmethod
    def strip_label(cls, v: str) -> str:
        return v.strip()


class ShoppingListItemPatch(BaseModel):
    label: str | None = Field(default=None, min_length=1, max_length=500)
    line_amount_cents: int | None = None
    sort_order: int | None = Field(default=None, ge=0)

    @field_validator("label")
    @classmethod
    def strip_label(cls, v: str | None) -> str | None:
        if v is None:
            return None
        return v.strip()

    @field_validator("line_amount_cents")
    @classmethod
    def cents_positive_when_set(cls, v: int | None) -> int | None:
        if v is not None and v <= 0:
            raise ValueError("line_amount_cents deve ser positivo")
        return v


class ShoppingListItemResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    sort_order: int
    label: str
    line_amount_cents: int | None


class ShoppingListSummaryResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    owner_user_id: uuid.UUID
    is_mine: bool
    title: str | None
    store_name: str | None
    status: str
    completed_at: datetime | None
    expense_id: uuid.UUID | None
    total_cents: int | None
    items_count: int
    created_at: datetime
    updated_at: datetime


class ShoppingListDetailResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    owner_user_id: uuid.UUID
    is_mine: bool
    title: str | None
    store_name: str | None
    status: str
    completed_at: datetime | None
    expense_id: uuid.UUID | None
    total_cents: int | None
    items: list[ShoppingListItemResponse]
    created_at: datetime
    updated_at: datetime


class ShoppingListComplete(BaseModel):
    category_id: uuid.UUID
    expense_date: date
    status: ExpenseStatus
    description: str | None = Field(default=None, max_length=500)
    total_cents: int | None = Field(default=None, gt=0)
    is_shared: bool = False
    shared_with_user_id: uuid.UUID | None = None

    @field_validator("description", mode="before")
    @classmethod
    def empty_desc_none(cls, v: str | None) -> str | None:
        if v is None:
            return None
        if isinstance(v, str):
            s = v.strip()
            return s if s else None
        return v

    @model_validator(mode="after")
    def share_consistency(self) -> Self:
        if not self.is_shared and self.shared_with_user_id is not None:
            raise ValueError("shared_with_user_id exige is_shared true")
        return self
