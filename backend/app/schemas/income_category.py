import uuid
from datetime import datetime

from pydantic import BaseModel, ConfigDict, Field


class IncomeCategoryCreate(BaseModel):
    name: str = Field(min_length=1, max_length=100)


class IncomeCategoryResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    key: str
    name: str
    sort_order: int = Field(ge=0)
    created_at: datetime
    updated_at: datetime
