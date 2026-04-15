import uuid
from datetime import datetime

from pydantic import BaseModel, Field


class AdminMeResponse(BaseModel):
    email: str
    is_admin: bool = True


class AdminUserRow(BaseModel):
    id: uuid.UUID
    email: str
    full_name: str | None = None
    display_name: str | None = None
    phone: str | None = None
    is_active: bool
    is_admin: bool
    email_verified_at: datetime | None = None
    last_seen_at: datetime | None = None
    created_at: datetime
    updated_at: datetime

    model_config = {"from_attributes": True}


class AdminUserListResponse(BaseModel):
    items: list[AdminUserRow]
    total: int
    skip: int
    limit: int


class AdminUsagePoint(BaseModel):
    day: str
    events: int
    active_users: int


class AdminUsageSummaryResponse(BaseModel):
    events_24h: int
    dau_7d: int
    mau_30d: int
    series_days: int
    series: list[AdminUsagePoint]


class AdminUserPatch(BaseModel):
    is_active: bool | None = Field(default=None, description="Ativar ou desativar conta")

    def has_updates(self) -> bool:
        return self.is_active is not None


class AdminUserPatchResponse(BaseModel):
    id: uuid.UUID
    email: str
    is_active: bool
