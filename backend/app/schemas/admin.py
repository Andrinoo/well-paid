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
    is_admin: bool | None = Field(default=None, description="Promover ou rebaixar admin")
    revoke_sessions: bool = Field(
        default=False,
        description="Revogar sessões ativas (refresh tokens) do utilizador",
    )

    def has_updates(self) -> bool:
        return (
            self.is_active is not None
            or self.is_admin is not None
            or self.revoke_sessions
        )


class AdminUserPatchResponse(BaseModel):
    id: uuid.UUID
    email: str
    is_active: bool
    is_admin: bool
    revoked_sessions: int = 0


class AdminUserRecentEvent(BaseModel):
    occurred_at: datetime
    event_type: str


class AdminUserDetailResponse(BaseModel):
    user: AdminUserRow
    events_7d: int
    events_30d: int
    event_types_30d: dict[str, int]
    recent_events: list[AdminUserRecentEvent]


class AdminFamilyRow(BaseModel):
    id: uuid.UUID
    name: str
    member_count: int
    created_at: datetime
    updated_at: datetime
    created_by_user_id: uuid.UUID | None = None

    model_config = {"from_attributes": True}


class AdminFamilyListResponse(BaseModel):
    items: list[AdminFamilyRow]
    total: int
    skip: int
    limit: int


class AdminFamilyMemberRow(BaseModel):
    user_id: uuid.UUID
    email: str
    full_name: str | None = None
    display_name: str | None = None
    role: str
    is_active: bool


class AdminFamilyInviteRow(BaseModel):
    id: uuid.UUID
    expires_at: datetime
    used: bool


class AdminFamilyDetailResponse(BaseModel):
    id: uuid.UUID
    name: str
    member_count: int
    max_members: int
    created_at: datetime
    updated_at: datetime
    created_by_user_id: uuid.UUID | None = None
    members: list[AdminFamilyMemberRow]
    invites: list[AdminFamilyInviteRow]


class AdminFinanceSummaryResponse(BaseModel):
    """Agregados de baixo custo para visão operacional do núcleo financeiro."""

    expenses_total: int
    expenses_active: int
    expenses_deleted: int
    expenses_shared: int
    incomes_total: int
    goals_total: int
    goal_contributions_total: int
    shopping_lists_total: int
    shopping_list_items_total: int
    emergency_reserves_total: int
    emergency_reserve_accruals_total: int
    categories_total: int
    income_categories_total: int
    expenses_sum_cents_30d: int
    incomes_sum_cents_30d: int


class AdminProductFunnelResponse(BaseModel):
    """Funil de produto: contagens distintas baratas (snapshot)."""

    users_total: int
    email_verified_total: int
    users_with_family_total: int
    users_with_expense_total: int
    users_with_income_total: int
    users_app_open_7d: int
    signups_7d: int
    signups_30d: int


class AdminAuditEventOut(BaseModel):
    id: uuid.UUID
    created_at: datetime
    actor_email: str
    action: str
    target_email: str | None = None
    details: dict[str, object] | None = None


class AdminAuditListResponse(BaseModel):
    items: list[AdminAuditEventOut]
    total: int
    skip: int
    limit: int
