import uuid

from pydantic import BaseModel, ConfigDict


class CategoryPublic(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    key: str
    name: str
    sort_order: int
