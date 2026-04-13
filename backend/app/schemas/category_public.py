import uuid

from pydantic import BaseModel, ConfigDict, Field


class CategoryPublic(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    key: str
    name: str
    sort_order: int


class CategoryCreate(BaseModel):
    name: str = Field(min_length=1, max_length=100)
