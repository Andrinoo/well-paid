const STORAGE_ACCESS = 'wellpaid_admin_access'
const STORAGE_REFRESH = 'wellpaid_admin_refresh'

export function getApiBase(): string {
  const raw = (import.meta.env.VITE_API_BASE_URL || '').trim()
  if (!raw) {
    return import.meta.env.DEV ? '/wellpaid-api' : 'http://127.0.0.1:8000'
  }
  return raw.replace(/\/$/, '')
}

export function loadStoredTokens(): { access: string | null; refresh: string | null } {
  return {
    access: sessionStorage.getItem(STORAGE_ACCESS),
    refresh: sessionStorage.getItem(STORAGE_REFRESH),
  }
}

export function saveTokens(access: string, refresh: string): void {
  sessionStorage.setItem(STORAGE_ACCESS, access)
  sessionStorage.setItem(STORAGE_REFRESH, refresh)
}

export function clearTokens(): void {
  sessionStorage.removeItem(STORAGE_ACCESS)
  sessionStorage.removeItem(STORAGE_REFRESH)
}

function explainNetworkError(err: unknown): Error {
  const base = getApiBase()
  if (err instanceof TypeError) {
    const msg = (err.message || '').toLowerCase()
    if (msg.includes('fetch') || msg.includes('network') || msg.includes('failed')) {
      return new Error(
        `Não foi possível contactar a API em ${base}. ` +
          'Ligue o servidor FastAPI (ex.: na pasta backend: uvicorn app.main:app --reload --host 127.0.0.1 --port 8000). ' +
          'Confirme que admin-console/.env define VITE_API_BASE_URL com esse endereço e reinicie o npm run dev após alterar o .env.',
      )
    }
  }
  return err instanceof Error ? err : new Error(String(err))
}

async function parseDetail(res: Response): Promise<string> {
  const text = await res.text()
  try {
    const j = JSON.parse(text) as { detail?: unknown }
    const d = j.detail
    if (typeof d === 'string') return d
    if (Array.isArray(d)) {
      return d
        .map((x) => (typeof x === 'object' && x && 'msg' in x ? String((x as { msg: string }).msg) : String(x)))
        .join('; ')
    }
  } catch {
    /* ignore */
  }
  return res.statusText || 'Erro de rede'
}

export type TokenPair = { access_token: string; refresh_token: string }

export async function loginRequest(email: string, password: string): Promise<TokenPair> {
  try {
    const res = await fetch(`${getApiBase()}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password }),
    })
    if (!res.ok) throw new Error(await parseDetail(res))
    return res.json() as Promise<TokenPair>
  } catch (e) {
    throw explainNetworkError(e)
  }
}

export async function refreshRequest(refreshToken: string): Promise<TokenPair> {
  try {
    const res = await fetch(`${getApiBase()}/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refresh_token: refreshToken }),
    })
    if (!res.ok) throw new Error(await parseDetail(res))
    return res.json() as Promise<TokenPair>
  } catch (e) {
    throw explainNetworkError(e)
  }
}

export async function adminMe(accessToken: string): Promise<{ email: string; is_admin: boolean }> {
  try {
    const res = await fetch(`${getApiBase()}/admin/me`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    })
    if (!res.ok) throw new Error(await parseDetail(res))
    return res.json() as Promise<{ email: string; is_admin: boolean }>
  } catch (e) {
    throw explainNetworkError(e)
  }
}

export type AdminUserRow = {
  id: string
  email: string
  full_name: string | null
  display_name: string | null
  phone: string | null
  is_active: boolean
  is_admin: boolean
  email_verified_at: string | null
  last_seen_at: string | null
  created_at: string
  updated_at: string
}

export type AdminUserRecentEvent = {
  occurred_at: string
  event_type: string
}

export type AdminUserDetailResponse = {
  user: AdminUserRow
  events_7d: number
  events_30d: number
  event_types_30d: Record<string, number>
  recent_events: AdminUserRecentEvent[]
}

export type AdminUserListResponse = {
  items: AdminUserRow[]
  total: number
  skip: number
  limit: number
}

export type AdminUsagePoint = {
  day: string
  events: number
  active_users: number
}

export type AdminUsageSummary = {
  events_24h: number
  dau_7d: number
  mau_30d: number
  series_days: number
  series: AdminUsagePoint[]
}

export async function listUsers(
  accessToken: string,
  params: {
    q?: string
    skip?: number
    limit?: number
    is_active?: boolean
    is_admin?: boolean
    email_verified?: boolean
    created_from?: string
    created_to?: string
    order_by?: 'created_at' | 'last_seen_at' | 'email'
    order_dir?: 'asc' | 'desc'
  },
): Promise<AdminUserListResponse> {
  const sp = new URLSearchParams()
  if (params.q) sp.set('q', params.q)
  if (params.skip != null) sp.set('skip', String(params.skip))
  if (params.limit != null) sp.set('limit', String(params.limit))
  if (params.is_active != null) sp.set('is_active', String(params.is_active))
  if (params.is_admin != null) sp.set('is_admin', String(params.is_admin))
  if (params.email_verified != null) sp.set('email_verified', String(params.email_verified))
  if (params.created_from) sp.set('created_from', params.created_from)
  if (params.created_to) sp.set('created_to', params.created_to)
  if (params.order_by) sp.set('order_by', params.order_by)
  if (params.order_dir) sp.set('order_dir', params.order_dir)
  try {
    const res = await fetch(`${getApiBase()}/admin/users?${sp.toString()}`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    })
    if (!res.ok) throw new Error(await parseDetail(res))
    return res.json() as Promise<AdminUserListResponse>
  } catch (e) {
    throw explainNetworkError(e)
  }
}

export async function getUsageSummary(
  accessToken: string,
  days = 14,
): Promise<AdminUsageSummary> {
  const sp = new URLSearchParams({ days: String(days) })
  try {
    const res = await fetch(`${getApiBase()}/admin/usage/summary?${sp.toString()}`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    })
    if (!res.ok) throw new Error(await parseDetail(res))
    return res.json() as Promise<AdminUsageSummary>
  } catch (e) {
    throw explainNetworkError(e)
  }
}

export async function patchUserActive(
  accessToken: string,
  userId: string,
  payload: {
    is_active?: boolean
    is_admin?: boolean
    revoke_sessions?: boolean
  },
): Promise<{ is_active: boolean; is_admin: boolean; revoked_sessions: number }> {
  try {
    const res = await fetch(`${getApiBase()}/admin/users/${userId}`, {
      method: 'PATCH',
      headers: {
        Authorization: `Bearer ${accessToken}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(payload),
    })
    if (!res.ok) throw new Error(await parseDetail(res))
    return res.json() as Promise<{ is_active: boolean; is_admin: boolean; revoked_sessions: number }>
  } catch (e) {
    throw explainNetworkError(e)
  }
}

export async function getUserDetail(
  accessToken: string,
  userId: string,
): Promise<AdminUserDetailResponse> {
  try {
    const res = await fetch(`${getApiBase()}/admin/users/${userId}`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    })
    if (!res.ok) throw new Error(await parseDetail(res))
    return res.json() as Promise<AdminUserDetailResponse>
  } catch (e) {
    throw explainNetworkError(e)
  }
}

export type AdminFamilyRow = {
  id: string
  name: string
  member_count: number
  created_at: string
  updated_at: string
  created_by_user_id: string | null
}

export type AdminFamilyListResponse = {
  items: AdminFamilyRow[]
  total: number
  skip: number
  limit: number
}

export type AdminFamilyMemberRow = {
  user_id: string
  email: string
  full_name: string | null
  display_name: string | null
  role: string
  is_active: boolean
}

export type AdminFamilyInviteRow = {
  id: string
  expires_at: string
  used: boolean
}

export type AdminFamilyDetailResponse = {
  id: string
  name: string
  member_count: number
  max_members: number
  created_at: string
  updated_at: string
  created_by_user_id: string | null
  members: AdminFamilyMemberRow[]
  invites: AdminFamilyInviteRow[]
}

export type AdminAuditEventRow = {
  id: string
  created_at: string
  actor_email: string
  action: string
  target_email: string | null
  details: Record<string, unknown> | null
}

export type AdminAuditListResponse = {
  items: AdminAuditEventRow[]
  total: number
  skip: number
  limit: number
}

export type AnnouncementKind = 'info' | 'warning' | 'tip' | 'material'
export type AnnouncementPlacement =
  | 'home_banner'
  | 'home_feed'
  | 'finance_tab'
  | 'announcements_tab'

export type AnnouncementRow = {
  id: string
  title: string
  body: string
  kind: AnnouncementKind
  placement: AnnouncementPlacement
  priority: number
  cta_label: string | null
  cta_url: string | null
  is_active: boolean
  starts_at: string | null
  ends_at: string | null
  created_by_user_id: string | null
  target_user_id: string | null
  /** E-mail do destinatário quando o recado é só para uma conta; vazio em avisos globais. */
  target_user_email: string | null
  created_at: string
  updated_at: string
  /** Só na API pública /active: leitura pelo utilizador atual. */
  user_read_at?: string | null
  /** Histórico no admin: quantos utilizadores marcaram como lido. */
  engagement_read_count?: number
  /** Histórico no admin: quantos removeram da lista. */
  engagement_hidden_count?: number
}

export type AnnouncementListResponse = {
  items: AnnouncementRow[]
  total: number
  skip: number
  limit: number
}

export type AdminProductFunnel = {
  users_total: number
  email_verified_total: number
  users_with_family_total: number
  users_with_expense_total: number
  users_with_income_total: number
  users_app_open_7d: number
  signups_7d: number
  signups_30d: number
}

export type AdminFinanceSummary = {
  expenses_total: number
  expenses_active: number
  expenses_deleted: number
  expenses_shared: number
  incomes_total: number
  goals_total: number
  goal_contributions_total: number
  shopping_lists_total: number
  shopping_list_items_total: number
  emergency_reserves_total: number
  emergency_reserve_accruals_total: number
  categories_total: number
  income_categories_total: number
  expenses_sum_cents_30d: number
  incomes_sum_cents_30d: number
}

export async function listFamilies(
  accessToken: string,
  params: {
    q?: string
    skip?: number
    limit?: number
    order_by?: 'created_at' | 'name'
    order_dir?: 'asc' | 'desc'
  },
): Promise<AdminFamilyListResponse> {
  const sp = new URLSearchParams()
  if (params.q) sp.set('q', params.q)
  if (params.skip != null) sp.set('skip', String(params.skip))
  if (params.limit != null) sp.set('limit', String(params.limit))
  if (params.order_by) sp.set('order_by', params.order_by)
  if (params.order_dir) sp.set('order_dir', params.order_dir)
  try {
    const res = await fetch(`${getApiBase()}/admin/families?${sp.toString()}`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    })
    if (!res.ok) throw new Error(await parseDetail(res))
    return res.json() as Promise<AdminFamilyListResponse>
  } catch (e) {
    throw explainNetworkError(e)
  }
}

export async function getFamilyDetail(
  accessToken: string,
  familyId: string,
): Promise<AdminFamilyDetailResponse> {
  try {
    const res = await fetch(`${getApiBase()}/admin/families/${familyId}`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    })
    if (!res.ok) throw new Error(await parseDetail(res))
    return res.json() as Promise<AdminFamilyDetailResponse>
  } catch (e) {
    throw explainNetworkError(e)
  }
}

export async function getFinanceSummary(accessToken: string): Promise<AdminFinanceSummary> {
  try {
    const res = await fetch(`${getApiBase()}/admin/finance/summary`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    })
    if (!res.ok) throw new Error(await parseDetail(res))
    return res.json() as Promise<AdminFinanceSummary>
  } catch (e) {
    throw explainNetworkError(e)
  }
}

export async function getProductFunnel(accessToken: string): Promise<AdminProductFunnel> {
  try {
    const res = await fetch(`${getApiBase()}/admin/metrics/funnel`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    })
    if (!res.ok) throw new Error(await parseDetail(res))
    return res.json() as Promise<AdminProductFunnel>
  } catch (e) {
    throw explainNetworkError(e)
  }
}

export async function listAuditEvents(
  accessToken: string,
  params: {
    skip?: number
    limit?: number
    actor_email?: string
    action?: string
    created_from?: string
    created_to?: string
  },
): Promise<AdminAuditListResponse> {
  const sp = new URLSearchParams()
  if (params.skip != null) sp.set('skip', String(params.skip))
  if (params.limit != null) sp.set('limit', String(params.limit))
  if (params.actor_email) sp.set('actor_email', params.actor_email)
  if (params.action) sp.set('action', params.action)
  if (params.created_from) sp.set('created_from', params.created_from)
  if (params.created_to) sp.set('created_to', params.created_to)
  try {
    const res = await fetch(`${getApiBase()}/admin/audit/events?${sp.toString()}`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    })
    if (!res.ok) throw new Error(await parseDetail(res))
    return res.json() as Promise<AdminAuditListResponse>
  } catch (e) {
    throw explainNetworkError(e)
  }
}

export async function listAnnouncements(
  accessToken: string,
  params: {
    skip?: number
    limit?: number
    placement?: AnnouncementPlacement
    is_active?: boolean
  },
): Promise<AnnouncementListResponse> {
  const sp = new URLSearchParams()
  if (params.skip != null) sp.set('skip', String(params.skip))
  if (params.limit != null) sp.set('limit', String(params.limit))
  if (params.placement) sp.set('placement', params.placement)
  if (params.is_active != null) sp.set('is_active', String(params.is_active))
  try {
    const res = await fetch(`${getApiBase()}/admin/announcements?${sp.toString()}`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    })
    if (!res.ok) throw new Error(await parseDetail(res))
    return res.json() as Promise<AnnouncementListResponse>
  } catch (e) {
    throw explainNetworkError(e)
  }
}

export async function createAnnouncement(
  accessToken: string,
  payload: {
    title: string
    body: string
    kind: AnnouncementKind
    placement: AnnouncementPlacement
    priority: number
    cta_label?: string | null
    cta_url?: string | null
    is_active: boolean
    starts_at?: string | null
    ends_at?: string | null
    /** E-mail do utilizador, ou omitir / vazio para todos. */
    target_user_email?: string | null
  },
): Promise<AnnouncementRow> {
  try {
    const res = await fetch(`${getApiBase()}/admin/announcements`, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${accessToken}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(payload),
    })
    if (!res.ok) throw new Error(await parseDetail(res))
    return res.json() as Promise<AnnouncementRow>
  } catch (e) {
    throw explainNetworkError(e)
  }
}

export async function patchAnnouncement(
  accessToken: string,
  announcementId: string,
  payload: {
    title?: string
    body?: string
    kind?: AnnouncementKind
    placement?: AnnouncementPlacement
    priority?: number
    cta_label?: string | null
    cta_url?: string | null
    is_active?: boolean
    starts_at?: string | null
    ends_at?: string | null
    /** String vazia = todos; e-mail = só esse utilizador. */
    target_user_email?: string | null
  },
): Promise<AnnouncementRow> {
  try {
    const res = await fetch(`${getApiBase()}/admin/announcements/${announcementId}`, {
      method: 'PATCH',
      headers: {
        Authorization: `Bearer ${accessToken}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(payload),
    })
    if (!res.ok) throw new Error(await parseDetail(res))
    return res.json() as Promise<AnnouncementRow>
  } catch (e) {
    throw explainNetworkError(e)
  }
}
