import uuid
from datetime import datetime
from typing import Literal

from pydantic import BaseModel, Field, HttpUrl, field_validator

AnnouncementKind = Literal["info", "warning", "tip", "material"]
AnnouncementPlacement = Literal["home_banner", "home_feed", "finance_tab"]


class AnnouncementRow(BaseModel):
    id: uuid.UUID
    title: str
    body: str
    kind: AnnouncementKind
    placement: AnnouncementPlacement
    priority: int
    cta_label: str | None = None
    cta_url: str | None = None
    is_active: bool
    starts_at: datetime | None = None
    ends_at: datetime | None = None
    created_by_user_id: uuid.UUID | None = None
    created_at: datetime
    updated_at: datetime

    model_config = {"from_attributes": True}


class AnnouncementListResponse(BaseModel):
    items: list[AnnouncementRow]
    total: int
    skip: int
    limit: int


class AnnouncementWrite(BaseModel):
    title: str = Field(min_length=3, max_length=140)
    body: str = Field(min_length=3, max_length=5000)
    kind: AnnouncementKind = "info"
    placement: AnnouncementPlacement = "home_banner"
    priority: int = Field(default=0, ge=0, le=100)
    cta_label: str | None = Field(default=None, max_length=80)
    cta_url: HttpUrl | None = None
    is_active: bool = False
    starts_at: datetime | None = None
    ends_at: datetime | None = None

    @field_validator("title", "body", mode="before")
    @classmethod
    def strip_text(cls, value: object) -> object:
        if isinstance(value, str):
            return value.strip()
        return value

    @field_validator("cta_label", mode="before")
    @classmethod
    def normalize_cta_label(cls, value: object) -> object:
        if value is None:
            return None
        if isinstance(value, str):
            s = value.strip()
            return s or None
        return value

    @field_validator("ends_at")
    @classmethod
    def validate_date_window(
        cls, ends_at: datetime | None, info
    ) -> datetime | None:
        starts_at = info.data.get("starts_at")
        if starts_at is not None and ends_at is not None and ends_at < starts_at:
            raise ValueError("ends_at deve ser maior ou igual a starts_at")
        return ends_at


class AnnouncementPatch(BaseModel):
    title: str | None = Field(default=None, min_length=3, max_length=140)
    body: str | None = Field(default=None, min_length=3, max_length=5000)
    kind: AnnouncementKind | None = None
    placement: AnnouncementPlacement | None = None
    priority: int | None = Field(default=None, ge=0, le=100)
    cta_label: str | None = Field(default=None, max_length=80)
    cta_url: HttpUrl | None = None
    is_active: bool | None = None
    starts_at: datetime | None = None
    ends_at: datetime | None = None

    @field_validator("title", "body", mode="before")
    @classmethod
    def strip_text(cls, value: object) -> object:
        if isinstance(value, str):
            return value.strip()
        return value

