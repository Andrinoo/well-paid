import uuid
from datetime import datetime

from pydantic import BaseModel, ConfigDict, Field


class GoalCreate(BaseModel):
    title: str = Field(min_length=1, max_length=200)
    target_cents: int = Field(gt=0)
    current_cents: int = Field(default=0, ge=0)
    is_active: bool = True


class GoalUpdate(BaseModel):
    title: str | None = Field(default=None, min_length=1, max_length=200)
    target_cents: int | None = Field(default=None, gt=0)
    current_cents: int | None = Field(default=None, ge=0)
    is_active: bool | None = None


class GoalContribute(BaseModel):
    amount_cents: int = Field(gt=0)


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
