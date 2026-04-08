import uuid
from datetime import datetime

from pydantic import BaseModel, ConfigDict, Field


class IncomeCategoryResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    key: str
    name: str
    sort_order: int = Field(ge=0)
    created_at: datetime
    updated_at: datetime
