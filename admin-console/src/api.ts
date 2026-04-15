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
  params: { q?: string; skip?: number; limit?: number },
): Promise<AdminUserListResponse> {
  const sp = new URLSearchParams()
  if (params.q) sp.set('q', params.q)
  if (params.skip != null) sp.set('skip', String(params.skip))
  if (params.limit != null) sp.set('limit', String(params.limit))
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
  is_active: boolean,
): Promise<void> {
  try {
    const res = await fetch(`${getApiBase()}/admin/users/${userId}`, {
      method: 'PATCH',
      headers: {
        Authorization: `Bearer ${accessToken}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ is_active }),
    })
    if (!res.ok) throw new Error(await parseDetail(res))
  } catch (e) {
    throw explainNetworkError(e)
  }
}
