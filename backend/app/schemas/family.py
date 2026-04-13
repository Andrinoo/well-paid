from __future__ import annotations

import uuid
from datetime import datetime

from pydantic import BaseModel, ConfigDict, Field


class FamilyCreate(BaseModel):
    name: str | None = Field(default=None, max_length=200)


class FamilyMemberOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    user_id: uuid.UUID
    email: str
    full_name: str | None
    role: str
    is_self: bool = False


class FamilyOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    name: str
    members: list[FamilyMemberOut]


class FamilyMeResponse(BaseModel):
    family: FamilyOut | None


class FamilyUpdate(BaseModel):
    name: str = Field(min_length=1, max_length=200)


class FamilyInviteCreateResponse(BaseModel):
    token: str
    expires_at: datetime
    invite_url: str


class FamilyJoinRequest(BaseModel):
    token: str = Field(min_length=10, max_length=500)
