import { useCallback, useEffect, useState } from 'react'
import {
  adminMe,
  clearTokens,
  getFamilyDetail,
  getFinanceSummary,
  listAuditEvents,
  getProductFunnel,
  getUserDetail,
  getUsageSummary,
  getApiBase,
  listFamilies,
  listUsers,
  loadStoredTokens,
  loginRequest,
  patchUserActive,
  saveTokens,
  type AdminFamilyDetailResponse,
  type AdminFamilyRow,
  type AdminFinanceSummary,
  type AdminAuditEventRow,
  type AdminProductFunnel,
  type AdminUserDetailResponse,
  type AdminUsageSummary,
  type AdminUserRow,
} from './api'

type View = 'boot' | 'login' | 'users'
type AdminSection = 'users' | 'families' | 'finance' | 'funnel' | 'audit'
type TriState = 'all' | 'yes' | 'no'

function formatDt(iso: string | null): string {
  if (!iso) return '—'
  try {
    return new Date(iso).toLocaleString('pt-PT')
  } catch {
    return iso
  }
}

function formatEurFromCents(cents: number): string {
  try {
    return (cents / 100).toLocaleString('pt-PT', { style: 'currency', currency: 'EUR' })
  } catch {
    return String(cents / 100)
  }
}

function pctOf(part: number, whole: number): string {
  if (whole <= 0) return '—'
  return `${((100 * part) / whole).toFixed(1)}%`
}

function formatAuditDetails(details: Record<string, unknown> | null): string {
  if (details == null) return '—'
  try {
    return JSON.stringify(details)
  } catch {
    return String(details)
  }
}

function escapeCsvCell(s: string): string {
  if (/[",\n\r]/.test(s)) return `"${s.replace(/"/g, '""')}"`
  return s
}

async function fetchAllUsersForExport(
  token: string,
  q: string,
  filters: {
    isActive: TriState
    isAdmin: TriState
    emailVerified: TriState
    createdFrom: string
    createdTo: string
    orderBy: 'created_at' | 'last_seen_at' | 'email'
    orderDir: 'asc' | 'desc'
  },
): Promise<AdminUserRow[]> {
  const pageSize = 100
  const out: AdminUserRow[] = []
  let skip = 0
  while (true) {
    const data = await listUsers(token, {
      q: q || undefined,
      skip,
      limit: pageSize,
      is_active: filters.isActive === 'all' ? undefined : filters.isActive === 'yes',
      is_admin: filters.isAdmin === 'all' ? undefined : filters.isAdmin === 'yes',
      email_verified: filters.emailVerified === 'all' ? undefined : filters.emailVerified === 'yes',
      created_from: filters.createdFrom ? `${filters.createdFrom}T00:00:00Z` : undefined,
      created_to: filters.createdTo ? `${filters.createdTo}T23:59:59Z` : undefined,
      order_by: filters.orderBy,
      order_dir: filters.orderDir,
    })
    out.push(...data.items)
    if (data.items.length < pageSize || out.length >= data.total) break
    skip += pageSize
  }
  return out
}

function downloadUsersCsv(rows: AdminUserRow[]): void {
  const headers = [
    'id',
    'email',
    'full_name',
    'display_name',
    'phone',
    'is_active',
    'is_admin',
    'email_verified_at',
    'last_seen_at',
    'created_at',
    'updated_at',
  ]
  const lines = [
    headers.join(','),
    ...rows.map((r) =>
      [
        r.id,
        r.email,
        r.full_name ?? '',
        r.display_name ?? '',
        r.phone ?? '',
        String(r.is_active),
        String(r.is_admin),
        r.email_verified_at ?? '',
        r.last_seen_at ?? '',
        r.created_at,
        r.updated_at,
      ]
        .map((c) => escapeCsvCell(String(c)))
        .join(','),
    ),
  ]
  const blob = new Blob(['\ufeff' + lines.join('\n')], {
    type: 'text/csv;charset=utf-8',
  })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `wellpaid-contas-${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  URL.revokeObjectURL(url)
}

function maxEventsInSeries(summary: AdminUsageSummary | null): number {
  if (!summary || summary.series.length === 0) return 0
  return summary.series.reduce((mx, p) => Math.max(mx, p.events), 0)
}

export default function App() {
  const [view, setView] = useState<View>('boot')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [info, setInfo] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [accessToken, setAccessToken] = useState<string | null>(null)
  const [adminEmail, setAdminEmail] = useState<string | null>(null)
  const [rows, setRows] = useState<AdminUserRow[]>([])
  const [total, setTotal] = useState(0)
  const [summary, setSummary] = useState<AdminUsageSummary | null>(null)
  const [selectedUser, setSelectedUser] = useState<AdminUserDetailResponse | null>(null)
  const [q, setQ] = useState('')
  const [qDebounced, setQDebounced] = useState('')
  const [isActiveFilter, setIsActiveFilter] = useState<TriState>('all')
  const [isAdminFilter, setIsAdminFilter] = useState<TriState>('all')
  const [emailVerifiedFilter, setEmailVerifiedFilter] = useState<TriState>('all')
  const [createdFrom, setCreatedFrom] = useState('')
  const [createdTo, setCreatedTo] = useState('')
  const [orderBy, setOrderBy] = useState<'created_at' | 'last_seen_at' | 'email'>('created_at')
  const [orderDir, setOrderDir] = useState<'asc' | 'desc'>('desc')
  const [skip, setSkip] = useState(0)
  const limit = 30

  const [adminSection, setAdminSection] = useState<AdminSection>('users')
  const [familyRows, setFamilyRows] = useState<AdminFamilyRow[]>([])
  const [familyTotal, setFamilyTotal] = useState(0)
  const [familyQ, setFamilyQ] = useState('')
  const [familyQDebounced, setFamilyQDebounced] = useState('')
  const [familySkip, setFamilySkip] = useState(0)
  const [familyOrderBy, setFamilyOrderBy] = useState<'created_at' | 'name'>('created_at')
  const [familyOrderDir, setFamilyOrderDir] = useState<'asc' | 'desc'>('desc')
  const [selectedFamily, setSelectedFamily] = useState<AdminFamilyDetailResponse | null>(null)
  const [financeSummary, setFinanceSummary] = useState<AdminFinanceSummary | null>(null)
  const [funnelMetrics, setFunnelMetrics] = useState<AdminProductFunnel | null>(null)
  const [auditItems, setAuditItems] = useState<AdminAuditEventRow[]>([])
  const [auditTotal, setAuditTotal] = useState(0)
  const [auditSkip, setAuditSkip] = useState(0)

  const logout = useCallback(() => {
    clearTokens()
    setAccessToken(null)
    setAdminEmail(null)
    setRows([])
    setFamilyRows([])
    setFinanceSummary(null)
    setFunnelMetrics(null)
    setAuditItems([])
    setAuditTotal(0)
    setAdminSection('users')
    setView('login')
  }, [])

  const loadList = useCallback(
    async (token: string, opts: { q: string; skip: number }) => {
      const [data, usage] = await Promise.all([
        listUsers(token, {
          q: opts.q || undefined,
          skip: opts.skip,
          limit,
          is_active: isActiveFilter === 'all' ? undefined : isActiveFilter === 'yes',
          is_admin: isAdminFilter === 'all' ? undefined : isAdminFilter === 'yes',
          email_verified: emailVerifiedFilter === 'all' ? undefined : emailVerifiedFilter === 'yes',
          created_from: createdFrom ? `${createdFrom}T00:00:00Z` : undefined,
          created_to: createdTo ? `${createdTo}T23:59:59Z` : undefined,
          order_by: orderBy,
          order_dir: orderDir,
        }),
        getUsageSummary(token, 14),
      ])
      setRows(data.items)
      setTotal(data.total)
      setSummary(usage)
    },
    [isActiveFilter, isAdminFilter, emailVerifiedFilter, createdFrom, createdTo, orderBy, orderDir],
  )

  const loadFamilies = useCallback(
    async (token: string, opts: { q: string; skip: number }) => {
      const [data, usage] = await Promise.all([
        listFamilies(token, {
          q: opts.q || undefined,
          skip: opts.skip,
          limit,
          order_by: familyOrderBy,
          order_dir: familyOrderDir,
        }),
        getUsageSummary(token, 14),
      ])
      setFamilyRows(data.items)
      setFamilyTotal(data.total)
      setSummary(usage)
    },
    [familyOrderBy, familyOrderDir],
  )

  const loadFinance = useCallback(async (token: string) => {
    const [fin, usage] = await Promise.all([
      getFinanceSummary(token),
      getUsageSummary(token, 14),
    ])
    setFinanceSummary(fin)
    setSummary(usage)
  }, [])

  const loadFunnel = useCallback(async (token: string) => {
    const [funnel, usage] = await Promise.all([
      getProductFunnel(token),
      getUsageSummary(token, 14),
    ])
    setFunnelMetrics(funnel)
    setSummary(usage)
  }, [])

  const loadAudit = useCallback(async (token: string, opts: { skip: number }) => {
    const [data, usage] = await Promise.all([
      listAuditEvents(token, { skip: opts.skip, limit }),
      getUsageSummary(token, 14),
    ])
    setAuditItems(data.items)
    setAuditTotal(data.total)
    setSummary(usage)
  }, [])

  useEffect(() => {
    const t = window.setTimeout(() => setQDebounced(q.trim()), 350)
    return () => window.clearTimeout(t)
  }, [q])

  useEffect(() => {
    const t = window.setTimeout(() => setFamilyQDebounced(familyQ.trim()), 350)
    return () => window.clearTimeout(t)
  }, [familyQ])

  useEffect(() => {
    if (view !== 'users' || !accessToken || adminSection !== 'users') return
    let cancelled = false
    setBusy(true)
    loadList(accessToken, { q: qDebounced, skip })
      .catch((e) => {
        if (!cancelled) {
          setInfo(null)
          setError(e instanceof Error ? e.message : 'Erro ao carregar')
        }
      })
      .finally(() => {
        if (!cancelled) setBusy(false)
      })
    return () => {
      cancelled = true
    }
  }, [view, accessToken, adminSection, qDebounced, skip, loadList])

  useEffect(() => {
    if (view !== 'users' || !accessToken || adminSection !== 'families') return
    let cancelled = false
    setBusy(true)
    loadFamilies(accessToken, { q: familyQDebounced, skip: familySkip })
      .catch((e) => {
        if (!cancelled) {
          setInfo(null)
          setError(e instanceof Error ? e.message : 'Erro ao carregar famílias')
        }
      })
      .finally(() => {
        if (!cancelled) setBusy(false)
      })
    return () => {
      cancelled = true
    }
  }, [view, accessToken, adminSection, familyQDebounced, familySkip, loadFamilies])

  useEffect(() => {
    if (view !== 'users' || !accessToken || adminSection !== 'finance') return
    let cancelled = false
    setBusy(true)
    loadFinance(accessToken)
      .catch((e) => {
        if (!cancelled) {
          setInfo(null)
          setError(e instanceof Error ? e.message : 'Erro ao carregar finanças')
        }
      })
      .finally(() => {
        if (!cancelled) setBusy(false)
      })
    return () => {
      cancelled = true
    }
  }, [view, accessToken, adminSection, loadFinance])

  useEffect(() => {
    if (view !== 'users' || !accessToken || adminSection !== 'funnel') return
    let cancelled = false
    setBusy(true)
    loadFunnel(accessToken)
      .catch((e) => {
        if (!cancelled) {
          setInfo(null)
          setError(e instanceof Error ? e.message : 'Erro ao carregar funil')
        }
      })
      .finally(() => {
        if (!cancelled) setBusy(false)
      })
    return () => {
      cancelled = true
    }
  }, [view, accessToken, adminSection, loadFunnel])

  useEffect(() => {
    if (view !== 'users' || !accessToken || adminSection !== 'audit') return
    let cancelled = false
    setBusy(true)
    loadAudit(accessToken, { skip: auditSkip })
      .catch((e) => {
        if (!cancelled) {
          setInfo(null)
          setError(e instanceof Error ? e.message : 'Erro ao carregar auditoria')
        }
      })
      .finally(() => {
        if (!cancelled) setBusy(false)
      })
    return () => {
      cancelled = true
    }
  }, [view, accessToken, adminSection, auditSkip, loadAudit])

  useEffect(() => {
    setSkip(0)
  }, [qDebounced, isActiveFilter, isAdminFilter, emailVerifiedFilter, createdFrom, createdTo, orderBy, orderDir])

  useEffect(() => {
    setFamilySkip(0)
  }, [familyQDebounced, familyOrderBy, familyOrderDir])

  useEffect(() => {
    let cancelled = false
    ;(async () => {
      const { access, refresh } = loadStoredTokens()
      if (!access || !refresh) {
        if (!cancelled) setView('login')
        return
      }
      try {
        const me = await adminMe(access)
        if (cancelled) return
        setAccessToken(access)
        setAdminEmail(me.email)
        setView('users')
      } catch {
        clearTokens()
        if (!cancelled) setView('login')
      }
    })()
    return () => {
      cancelled = true
    }
  }, [])

  async function handleLogin(ev: React.FormEvent) {
    ev.preventDefault()
    setError(null)
    setInfo(null)
    setBusy(true)
    try {
      const pair = await loginRequest(email, password)
      saveTokens(pair.access_token, pair.refresh_token)
      const me = await adminMe(pair.access_token)
      setAccessToken(pair.access_token)
      setAdminEmail(me.email)
      setView('users')
    } catch (e) {
      clearTokens()
      setError(e instanceof Error ? e.message : 'Falha no login')
    } finally {
      setBusy(false)
    }
  }

  async function exportCsv() {
    if (!accessToken) return
    setError(null)
    setInfo(null)
    setBusy(true)
    try {
      const all = await fetchAllUsersForExport(accessToken, qDebounced, {
        isActive: isActiveFilter,
        isAdmin: isAdminFilter,
        emailVerified: emailVerifiedFilter,
        createdFrom,
        createdTo,
        orderBy,
        orderDir,
      })
      downloadUsersCsv(all)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Erro ao exportar')
    } finally {
      setBusy(false)
    }
  }

  async function toggleActive(row: AdminUserRow) {
    if (!accessToken) return
    const next = !row.is_active
    const label = next ? 'reativar' : 'desativar'
    if (!window.confirm(`Tem a certeza que pretende ${label} ${row.email}?`)) return
    setError(null)
    setInfo(null)
    setBusy(true)
    try {
      await patchUserActive(accessToken, row.id, { is_active: next })
      await loadList(accessToken, { q: qDebounced, skip })
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Erro ao atualizar')
    } finally {
      setBusy(false)
    }
  }

  async function toggleAdmin(row: AdminUserRow) {
    if (!accessToken) return
    const next = !row.is_admin
    const label = next ? 'promover a admin' : 'rebaixar de admin'
    if (!window.confirm(`Tem a certeza que pretende ${label} ${row.email}?`)) return
    setError(null)
    setInfo(null)
    setBusy(true)
    try {
      await patchUserActive(accessToken, row.id, { is_admin: next })
      await loadList(accessToken, { q: qDebounced, skip })
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Erro ao atualizar')
    } finally {
      setBusy(false)
    }
  }

  async function revokeSessions(row: AdminUserRow) {
    if (!accessToken) return
    if (!window.confirm(`Revogar sessões ativas de ${row.email}?`)) return
    setError(null)
    setInfo(null)
    setBusy(true)
    try {
      const res = await patchUserActive(accessToken, row.id, { revoke_sessions: true })
      await loadList(accessToken, { q: qDebounced, skip })
      if (res.revoked_sessions > 0) {
        setInfo(`Sessões revogadas: ${res.revoked_sessions}`)
      } else {
        setInfo('Nenhuma sessão ativa para revogar.')
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Erro ao atualizar')
    } finally {
      setBusy(false)
    }
  }

  async function openUserDetail(row: AdminUserRow) {
    if (!accessToken) return
    setError(null)
    setInfo(null)
    setBusy(true)
    try {
      const detail = await getUserDetail(accessToken, row.id)
      setSelectedUser(detail)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Erro ao carregar detalhe')
    } finally {
      setBusy(false)
    }
  }

  async function openFamilyDetail(row: AdminFamilyRow) {
    if (!accessToken) return
    setError(null)
    setInfo(null)
    setBusy(true)
    try {
      const detail = await getFamilyDetail(accessToken, row.id)
      setSelectedFamily(detail)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Erro ao carregar família')
    } finally {
      setBusy(false)
    }
  }

  if (view === 'boot') {
    return (
      <div className="wp-card">
        <p className="wp-muted" style={{ margin: 0 }}>
          A carregar…
        </p>
      </div>
    )
  }

  if (view === 'login') {
    return (
      <div>
        <h1>Well Paid — Admin</h1>
        <p className="wp-muted">API: {getApiBase()}</p>
        <div className="wp-card" style={{ maxWidth: 420, margin: '0 auto' }}>
          <form onSubmit={handleLogin}>
            {error ? <div className="wp-error">{error}</div> : null}
            <div className="wp-field">
              <label htmlFor="email">E-mail</label>
              <input
                id="email"
                type="email"
                autoComplete="username"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </div>
            <div className="wp-field">
              <label htmlFor="password">Senha</label>
              <input
                id="password"
                type="password"
                autoComplete="current-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </div>
            <button className="wp-btn" type="submit" disabled={busy} style={{ width: '100%' }}>
              {busy ? 'A entrar…' : 'Entrar'}
            </button>
          </form>
        </div>
      </div>
    )
  }

  const maxSkip = Math.max(0, total - limit)
  const page = Math.floor(skip / limit) + 1
  const totalPages = Math.max(1, Math.ceil(total / limit))
  const familyMaxSkip = Math.max(0, familyTotal - limit)
  const familyPage = Math.floor(familySkip / limit) + 1
  const familyTotalPages = Math.max(1, Math.ceil(familyTotal / limit))
  const auditMaxSkip = Math.max(0, auditTotal - limit)
  const auditPage = Math.floor(auditSkip / limit) + 1
  const auditTotalPages = Math.max(1, Math.ceil(auditTotal / limit))
  const peakEvents = maxEventsInSeries(summary)

  return (
    <div>
      <div className="wp-toolbar">
        <div>
          <div
            style={{
              display: 'flex',
              gap: '0.5rem',
              alignItems: 'center',
              flexWrap: 'wrap',
              marginBottom: '0.35rem',
            }}
          >
            <button
              type="button"
              className={adminSection === 'users' ? 'wp-btn' : 'wp-btn wp-btn-ghost'}
              style={{ fontSize: '0.88rem' }}
              onClick={() => {
                setAdminSection('users')
                setSelectedFamily(null)
              }}
            >
              Contas
            </button>
            <button
              type="button"
              className={adminSection === 'families' ? 'wp-btn' : 'wp-btn wp-btn-ghost'}
              style={{ fontSize: '0.88rem' }}
              onClick={() => {
                setAdminSection('families')
                setSelectedUser(null)
              }}
            >
              Famílias
            </button>
            <button
              type="button"
              className={adminSection === 'finance' ? 'wp-btn' : 'wp-btn wp-btn-ghost'}
              style={{ fontSize: '0.88rem' }}
              onClick={() => {
                setAdminSection('finance')
                setSelectedUser(null)
                setSelectedFamily(null)
              }}
            >
              Finanças
            </button>
            <button
              type="button"
              className={adminSection === 'funnel' ? 'wp-btn' : 'wp-btn wp-btn-ghost'}
              style={{ fontSize: '0.88rem' }}
              onClick={() => {
                setAdminSection('funnel')
                setSelectedUser(null)
                setSelectedFamily(null)
              }}
            >
              Funil
            </button>
            <button
              type="button"
              className={adminSection === 'audit' ? 'wp-btn' : 'wp-btn wp-btn-ghost'}
              style={{ fontSize: '0.88rem' }}
              onClick={() => {
                setAdminSection('audit')
                setSelectedUser(null)
                setSelectedFamily(null)
              }}
            >
              Auditoria
            </button>
          </div>
          <p className="wp-muted" style={{ margin: 0 }}>
            Sessão: {adminEmail ?? '—'}
          </p>
        </div>
        <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
          {adminSection === 'users' ? (
            <button
              type="button"
              className="wp-btn wp-btn-ghost"
              onClick={() => void exportCsv()}
              disabled={busy}
            >
              Exportar CSV
            </button>
          ) : null}
          <button type="button" className="wp-btn wp-btn-ghost" onClick={logout}>
            Sair
          </button>
        </div>
      </div>

      {error ? <div className="wp-error">{error}</div> : null}
      {info ? <div className="wp-info">{info}</div> : null}

      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))',
          gap: '0.75rem',
          marginBottom: '0.9rem',
        }}
      >
        <div className="wp-card" style={{ padding: '0.85rem 1rem' }}>
          <div className="wp-muted" style={{ margin: 0, fontSize: '0.75rem' }}>
            Eventos (24h)
          </div>
          <div style={{ fontSize: '1.3rem', fontWeight: 700 }}>{summary?.events_24h ?? '—'}</div>
        </div>
        <div className="wp-card" style={{ padding: '0.85rem 1rem' }}>
          <div className="wp-muted" style={{ margin: 0, fontSize: '0.75rem' }}>
            Utilizadores ativos (7d)
          </div>
          <div style={{ fontSize: '1.3rem', fontWeight: 700 }}>{summary?.dau_7d ?? '—'}</div>
        </div>
        <div className="wp-card" style={{ padding: '0.85rem 1rem' }}>
          <div className="wp-muted" style={{ margin: 0, fontSize: '0.75rem' }}>
            Utilizadores ativos (30d)
          </div>
          <div style={{ fontSize: '1.3rem', fontWeight: 700 }}>{summary?.mau_30d ?? '—'}</div>
        </div>
      </div>

      <div className="wp-card" style={{ marginBottom: '0.9rem' }}>
        <div className="wp-toolbar" style={{ marginTop: 0, marginBottom: '0.6rem' }}>
          <h1 style={{ margin: 0, fontSize: '1.05rem' }}>Tendência de uso (14 dias)</h1>
          <span className="wp-muted" style={{ fontSize: '0.8rem' }}>
            Eventos de autenticação por dia
          </span>
        </div>
        {summary && summary.series.length > 0 ? (
          <div style={{ display: 'flex', alignItems: 'flex-end', gap: '0.3rem', minHeight: 120 }}>
            {summary.series.map((p) => {
              const h = peakEvents > 0 ? Math.max(6, Math.round((p.events / peakEvents) * 90)) : 6
              return (
                <div
                  key={p.day}
                  title={`${p.day}: ${p.events} eventos / ${p.active_users} ativos`}
                  style={{
                    flex: 1,
                    minWidth: 8,
                    height: h,
                    borderRadius: 4,
                    background: 'linear-gradient(180deg, rgba(201,169,78,0.95), rgba(184,148,61,0.85))',
                  }}
                />
              )
            })}
          </div>
        ) : (
          <p className="wp-muted" style={{ margin: 0 }}>
            Ainda sem eventos suficientes para mostrar tendência.
          </p>
        )}
      </div>

      {adminSection === 'users' ? (
        <div className="wp-card">
        <div className="wp-toolbar" style={{ marginTop: 0 }}>
          <input
            className="wp-search"
            type="search"
            placeholder="Filtrar por e-mail…"
            value={q}
            onChange={(e) => setQ(e.target.value)}
            aria-label="Filtrar por e-mail"
          />
          <select
            className="wp-search"
            style={{ maxWidth: 170 }}
            value={isActiveFilter}
            onChange={(e) => setIsActiveFilter(e.target.value as TriState)}
            aria-label="Filtrar por ativo"
          >
            <option value="all">Ativo: todos</option>
            <option value="yes">Ativo: sim</option>
            <option value="no">Ativo: não</option>
          </select>
          <select
            className="wp-search"
            style={{ maxWidth: 170 }}
            value={isAdminFilter}
            onChange={(e) => setIsAdminFilter(e.target.value as TriState)}
            aria-label="Filtrar por admin"
          >
            <option value="all">Admin: todos</option>
            <option value="yes">Admin: sim</option>
            <option value="no">Admin: não</option>
          </select>
          <select
            className="wp-search"
            style={{ maxWidth: 190 }}
            value={emailVerifiedFilter}
            onChange={(e) => setEmailVerifiedFilter(e.target.value as TriState)}
            aria-label="Filtrar por e-mail verificado"
          >
            <option value="all">Verificado: todos</option>
            <option value="yes">Verificado: sim</option>
            <option value="no">Verificado: não</option>
          </select>
          <input
            className="wp-search"
            style={{ maxWidth: 170 }}
            type="date"
            value={createdFrom}
            onChange={(e) => setCreatedFrom(e.target.value)}
            aria-label="Criado de"
            title="Criado de"
          />
          <input
            className="wp-search"
            style={{ maxWidth: 170 }}
            type="date"
            value={createdTo}
            onChange={(e) => setCreatedTo(e.target.value)}
            aria-label="Criado até"
            title="Criado até"
          />
          <select
            className="wp-search"
            style={{ maxWidth: 180 }}
            value={orderBy}
            onChange={(e) => setOrderBy(e.target.value as 'created_at' | 'last_seen_at' | 'email')}
            aria-label="Ordenar por"
          >
            <option value="created_at">Ordem: criado</option>
            <option value="last_seen_at">Ordem: última atividade</option>
            <option value="email">Ordem: e-mail</option>
          </select>
          <select
            className="wp-search"
            style={{ maxWidth: 130 }}
            value={orderDir}
            onChange={(e) => setOrderDir(e.target.value as 'asc' | 'desc')}
            aria-label="Direção da ordenação"
          >
            <option value="desc">Desc</option>
            <option value="asc">Asc</option>
          </select>
          <span className="wp-muted" style={{ fontSize: '0.85rem' }}>
            {total} conta(s)
            {busy ? ' · a atualizar…' : ''}
          </span>
        </div>
        <div className="wp-table-wrap">
          <table className="wp-table">
            <thead>
              <tr>
                <th>E-mail</th>
                <th>Nome</th>
                <th>Ativo</th>
                <th>Admin</th>
                <th>E-mail verificado</th>
                <th>Última actividade</th>
                <th>Criado</th>
                <th>Ações</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((r) => (
                <tr key={r.id}>
                  <td>{r.email}</td>
                  <td>{r.display_name || r.full_name || '—'}</td>
                  <td>
                    {r.is_active ? (
                      <span className="wp-badge wp-badge-ok">Sim</span>
                    ) : (
                      <span className="wp-badge wp-badge-no">Não</span>
                    )}
                  </td>
                  <td>{r.is_admin ? 'Sim' : '—'}</td>
                  <td>{formatDt(r.email_verified_at)}</td>
                  <td>{formatDt(r.last_seen_at)}</td>
                  <td>{formatDt(r.created_at)}</td>
                  <td>
                    <div style={{ display: 'flex', gap: '0.35rem', flexWrap: 'wrap' }}>
                      <button
                        type="button"
                        className="wp-btn wp-btn-ghost"
                        style={{ padding: '0.35rem 0.65rem', fontSize: '0.8rem' }}
                        onClick={() => openUserDetail(r)}
                        disabled={busy}
                      >
                        Detalhe
                      </button>
                      <button
                        type="button"
                        className="wp-btn wp-btn-ghost"
                        style={{ padding: '0.35rem 0.65rem', fontSize: '0.8rem' }}
                        onClick={() => toggleActive(r)}
                        disabled={busy}
                      >
                        {r.is_active ? 'Desativar' : 'Reativar'}
                      </button>
                      <button
                        type="button"
                        className="wp-btn wp-btn-ghost"
                        style={{ padding: '0.35rem 0.65rem', fontSize: '0.8rem' }}
                        onClick={() => toggleAdmin(r)}
                        disabled={busy}
                      >
                        {r.is_admin ? 'Rebaixar admin' : 'Promover admin'}
                      </button>
                      <button
                        type="button"
                        className="wp-btn wp-btn-ghost"
                        style={{ padding: '0.35rem 0.65rem', fontSize: '0.8rem' }}
                        onClick={() => revokeSessions(r)}
                        disabled={busy}
                      >
                        Revogar sessões
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {total > limit ? (
          <div
            style={{
              display: 'flex',
              gap: '0.75rem',
              alignItems: 'center',
              justifyContent: 'flex-end',
              marginTop: '1rem',
            }}
          >
            <button
              type="button"
              className="wp-btn wp-btn-ghost"
              disabled={skip <= 0 || busy}
              onClick={() => setSkip((s) => Math.max(0, s - limit))}
            >
              Anterior
            </button>
            <span className="wp-muted" style={{ fontSize: '0.85rem' }}>
              Página {page} / {totalPages}
            </span>
            <button
              type="button"
              className="wp-btn wp-btn-ghost"
              disabled={skip >= maxSkip || busy}
              onClick={() => setSkip((s) => s + limit)}
            >
              Seguinte
            </button>
          </div>
        ) : null}
        </div>
      ) : adminSection === 'families' ? (
        <div className="wp-card">
          <div className="wp-toolbar" style={{ marginTop: 0 }}>
            <input
              className="wp-search"
              type="search"
              placeholder="Filtrar por nome…"
              value={familyQ}
              onChange={(e) => setFamilyQ(e.target.value)}
              aria-label="Filtrar por nome"
            />
            <select
              className="wp-search"
              style={{ maxWidth: 180 }}
              value={familyOrderBy}
              onChange={(e) => setFamilyOrderBy(e.target.value as 'created_at' | 'name')}
              aria-label="Ordenar famílias por"
            >
              <option value="created_at">Ordem: criado</option>
              <option value="name">Ordem: nome</option>
            </select>
            <select
              className="wp-search"
              style={{ maxWidth: 130 }}
              value={familyOrderDir}
              onChange={(e) => setFamilyOrderDir(e.target.value as 'asc' | 'desc')}
              aria-label="Direção da ordenação (famílias)"
            >
              <option value="desc">Desc</option>
              <option value="asc">Asc</option>
            </select>
            <span className="wp-muted" style={{ fontSize: '0.85rem' }}>
              {familyTotal} família(s)
              {busy ? ' · a atualizar…' : ''}
            </span>
          </div>
          <div className="wp-table-wrap">
            <table className="wp-table">
              <thead>
                <tr>
                  <th>Nome</th>
                  <th>Membros</th>
                  <th>Criado</th>
                  <th>Ações</th>
                </tr>
              </thead>
              <tbody>
                {familyRows.map((r) => (
                  <tr key={r.id}>
                    <td>{r.name}</td>
                    <td>{r.member_count}</td>
                    <td>{formatDt(r.created_at)}</td>
                    <td>
                      <button
                        type="button"
                        className="wp-btn wp-btn-ghost"
                        style={{ padding: '0.35rem 0.65rem', fontSize: '0.8rem' }}
                        onClick={() => openFamilyDetail(r)}
                        disabled={busy}
                      >
                        Detalhe
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {familyTotal > limit ? (
            <div
              style={{
                display: 'flex',
                gap: '0.75rem',
                alignItems: 'center',
                justifyContent: 'flex-end',
                marginTop: '1rem',
              }}
            >
              <button
                type="button"
                className="wp-btn wp-btn-ghost"
                disabled={familySkip <= 0 || busy}
                onClick={() => setFamilySkip((s) => Math.max(0, s - limit))}
              >
                Anterior
              </button>
              <span className="wp-muted" style={{ fontSize: '0.85rem' }}>
                Página {familyPage} / {familyTotalPages}
              </span>
              <button
                type="button"
                className="wp-btn wp-btn-ghost"
                disabled={familySkip >= familyMaxSkip || busy}
                onClick={() => setFamilySkip((s) => s + limit)}
              >
                Seguinte
              </button>
            </div>
          ) : null}
        </div>
      ) : adminSection === 'finance' ? (
        <div className="wp-card">
          <div className="wp-toolbar" style={{ marginTop: 0, marginBottom: '0.75rem' }}>
            <h1 style={{ margin: 0, fontSize: '1.05rem' }}>Núcleo financeiro</h1>
            <span className="wp-muted" style={{ fontSize: '0.8rem' }}>
              Agregados na base de dados (sem linhas individuais)
            </span>
          </div>
          {financeSummary ? (
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
                gap: '0.75rem',
              }}
            >
              <div className="wp-card" style={{ padding: '0.75rem 0.9rem' }}>
                <div className="wp-muted" style={{ margin: 0, fontSize: '0.72rem' }}>
                  Despesas (total)
                </div>
                <div style={{ fontSize: '1.15rem', fontWeight: 700 }}>{financeSummary.expenses_total}</div>
              </div>
              <div className="wp-card" style={{ padding: '0.75rem 0.9rem' }}>
                <div className="wp-muted" style={{ margin: 0, fontSize: '0.72rem' }}>
                  Despesas ativas
                </div>
                <div style={{ fontSize: '1.15rem', fontWeight: 700 }}>{financeSummary.expenses_active}</div>
              </div>
              <div className="wp-card" style={{ padding: '0.75rem 0.9rem' }}>
                <div className="wp-muted" style={{ margin: 0, fontSize: '0.72rem' }}>
                  Despesas removidas (soft delete)
                </div>
                <div style={{ fontSize: '1.15rem', fontWeight: 700 }}>{financeSummary.expenses_deleted}</div>
              </div>
              <div className="wp-card" style={{ padding: '0.75rem 0.9rem' }}>
                <div className="wp-muted" style={{ margin: 0, fontSize: '0.72rem' }}>
                  Despesas partilhadas
                </div>
                <div style={{ fontSize: '1.15rem', fontWeight: 700 }}>{financeSummary.expenses_shared}</div>
              </div>
              <div className="wp-card" style={{ padding: '0.75rem 0.9rem' }}>
                <div className="wp-muted" style={{ margin: 0, fontSize: '0.72rem' }}>
                  Soma despesas (30d, ativas)
                </div>
                <div style={{ fontSize: '1.15rem', fontWeight: 700 }}>
                  {formatEurFromCents(financeSummary.expenses_sum_cents_30d)}
                </div>
              </div>
              <div className="wp-card" style={{ padding: '0.75rem 0.9rem' }}>
                <div className="wp-muted" style={{ margin: 0, fontSize: '0.72rem' }}>
                  Receitas (linhas)
                </div>
                <div style={{ fontSize: '1.15rem', fontWeight: 700 }}>{financeSummary.incomes_total}</div>
              </div>
              <div className="wp-card" style={{ padding: '0.75rem 0.9rem' }}>
                <div className="wp-muted" style={{ margin: 0, fontSize: '0.72rem' }}>
                  Soma receitas (30d)
                </div>
                <div style={{ fontSize: '1.15rem', fontWeight: 700 }}>
                  {formatEurFromCents(financeSummary.incomes_sum_cents_30d)}
                </div>
              </div>
              <div className="wp-card" style={{ padding: '0.75rem 0.9rem' }}>
                <div className="wp-muted" style={{ margin: 0, fontSize: '0.72rem' }}>
                  Objetivos
                </div>
                <div style={{ fontSize: '1.15rem', fontWeight: 700 }}>{financeSummary.goals_total}</div>
              </div>
              <div className="wp-card" style={{ padding: '0.75rem 0.9rem' }}>
                <div className="wp-muted" style={{ margin: 0, fontSize: '0.72rem' }}>
                  Contribuições para objetivos
                </div>
                <div style={{ fontSize: '1.15rem', fontWeight: 700 }}>
                  {financeSummary.goal_contributions_total}
                </div>
              </div>
              <div className="wp-card" style={{ padding: '0.75rem 0.9rem' }}>
                <div className="wp-muted" style={{ margin: 0, fontSize: '0.72rem' }}>
                  Listas de compras
                </div>
                <div style={{ fontSize: '1.15rem', fontWeight: 700 }}>{financeSummary.shopping_lists_total}</div>
              </div>
              <div className="wp-card" style={{ padding: '0.75rem 0.9rem' }}>
                <div className="wp-muted" style={{ margin: 0, fontSize: '0.72rem' }}>
                  Linhas em listas
                </div>
                <div style={{ fontSize: '1.15rem', fontWeight: 700 }}>
                  {financeSummary.shopping_list_items_total}
                </div>
              </div>
              <div className="wp-card" style={{ padding: '0.75rem 0.9rem' }}>
                <div className="wp-muted" style={{ margin: 0, fontSize: '0.72rem' }}>
                  Reservas de emergência
                </div>
                <div style={{ fontSize: '1.15rem', fontWeight: 700 }}>
                  {financeSummary.emergency_reserves_total}
                </div>
              </div>
              <div className="wp-card" style={{ padding: '0.75rem 0.9rem' }}>
                <div className="wp-muted" style={{ margin: 0, fontSize: '0.72rem' }}>
                  Créditos mensais (reserva)
                </div>
                <div style={{ fontSize: '1.15rem', fontWeight: 700 }}>
                  {financeSummary.emergency_reserve_accruals_total}
                </div>
              </div>
              <div className="wp-card" style={{ padding: '0.75rem 0.9rem' }}>
                <div className="wp-muted" style={{ margin: 0, fontSize: '0.72rem' }}>
                  Categorias de despesa
                </div>
                <div style={{ fontSize: '1.15rem', fontWeight: 700 }}>{financeSummary.categories_total}</div>
              </div>
              <div className="wp-card" style={{ padding: '0.75rem 0.9rem' }}>
                <div className="wp-muted" style={{ margin: 0, fontSize: '0.72rem' }}>
                  Categorias de receita
                </div>
                <div style={{ fontSize: '1.15rem', fontWeight: 700 }}>{financeSummary.income_categories_total}</div>
              </div>
            </div>
          ) : (
            <p className="wp-muted" style={{ margin: 0 }}>
              {busy ? 'A carregar…' : 'Sem dados.'}
            </p>
          )}
        </div>
      ) : adminSection === 'funnel' ? (
        <div className="wp-card">
          <div className="wp-toolbar" style={{ marginTop: 0, marginBottom: '0.75rem' }}>
            <h1 style={{ margin: 0, fontSize: '1.05rem' }}>Funil de produto</h1>
            <span className="wp-muted" style={{ fontSize: '0.8rem' }}>
              Contagens globais; % face ao total de contas
            </span>
          </div>
          {funnelMetrics ? (
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
                gap: '0.75rem',
              }}
            >
              <div className="wp-card" style={{ padding: '0.75rem 0.9rem' }}>
                <div className="wp-muted" style={{ margin: 0, fontSize: '0.72rem' }}>
                  Contas registadas
                </div>
                <div style={{ fontSize: '1.15rem', fontWeight: 700 }}>{funnelMetrics.users_total}</div>
              </div>
              <div className="wp-card" style={{ padding: '0.75rem 0.9rem' }}>
                <div className="wp-muted" style={{ margin: 0, fontSize: '0.72rem' }}>
                  E-mail verificado
                </div>
                <div style={{ fontSize: '1.15rem', fontWeight: 700 }}>
                  {funnelMetrics.email_verified_total}
                  <span className="wp-muted" style={{ fontSize: '0.78rem', marginLeft: '0.35rem' }}>
                    ({pctOf(funnelMetrics.email_verified_total, funnelMetrics.users_total)})
                  </span>
                </div>
              </div>
              <div className="wp-card" style={{ padding: '0.75rem 0.9rem' }}>
                <div className="wp-muted" style={{ margin: 0, fontSize: '0.72rem' }}>
                  Utilizadores em família
                </div>
                <div style={{ fontSize: '1.15rem', fontWeight: 700 }}>
                  {funnelMetrics.users_with_family_total}
                  <span className="wp-muted" style={{ fontSize: '0.78rem', marginLeft: '0.35rem' }}>
                    ({pctOf(funnelMetrics.users_with_family_total, funnelMetrics.users_total)})
                  </span>
                </div>
              </div>
              <div className="wp-card" style={{ padding: '0.75rem 0.9rem' }}>
                <div className="wp-muted" style={{ margin: 0, fontSize: '0.72rem' }}>
                  Com ≥1 despesa ativa
                </div>
                <div style={{ fontSize: '1.15rem', fontWeight: 700 }}>
                  {funnelMetrics.users_with_expense_total}
                  <span className="wp-muted" style={{ fontSize: '0.78rem', marginLeft: '0.35rem' }}>
                    ({pctOf(funnelMetrics.users_with_expense_total, funnelMetrics.users_total)})
                  </span>
                </div>
              </div>
              <div className="wp-card" style={{ padding: '0.75rem 0.9rem' }}>
                <div className="wp-muted" style={{ margin: 0, fontSize: '0.72rem' }}>
                  Com ≥1 receita
                </div>
                <div style={{ fontSize: '1.15rem', fontWeight: 700 }}>
                  {funnelMetrics.users_with_income_total}
                  <span className="wp-muted" style={{ fontSize: '0.78rem', marginLeft: '0.35rem' }}>
                    ({pctOf(funnelMetrics.users_with_income_total, funnelMetrics.users_total)})
                  </span>
                </div>
              </div>
              <div className="wp-card" style={{ padding: '0.75rem 0.9rem' }}>
                <div className="wp-muted" style={{ margin: 0, fontSize: '0.72rem' }}>
                  app_open distintos (7d)
                </div>
                <div style={{ fontSize: '1.15rem', fontWeight: 700 }}>
                  {funnelMetrics.users_app_open_7d}
                  <span className="wp-muted" style={{ fontSize: '0.78rem', marginLeft: '0.35rem' }}>
                    ({pctOf(funnelMetrics.users_app_open_7d, funnelMetrics.users_total)})
                  </span>
                </div>
              </div>
              <div className="wp-card" style={{ padding: '0.75rem 0.9rem' }}>
                <div className="wp-muted" style={{ margin: 0, fontSize: '0.72rem' }}>
                  Novas contas (7d)
                </div>
                <div style={{ fontSize: '1.15rem', fontWeight: 700 }}>{funnelMetrics.signups_7d}</div>
              </div>
              <div className="wp-card" style={{ padding: '0.75rem 0.9rem' }}>
                <div className="wp-muted" style={{ margin: 0, fontSize: '0.72rem' }}>
                  Novas contas (30d)
                </div>
                <div style={{ fontSize: '1.15rem', fontWeight: 700 }}>{funnelMetrics.signups_30d}</div>
              </div>
            </div>
          ) : (
            <p className="wp-muted" style={{ margin: 0 }}>
              {busy ? 'A carregar…' : 'Sem dados.'}
            </p>
          )}
        </div>
      ) : (
        <div className="wp-card">
          <div className="wp-toolbar" style={{ marginTop: 0, marginBottom: '0.6rem' }}>
            <h1 style={{ margin: 0, fontSize: '1.05rem' }}>Auditoria admin</h1>
            <span className="wp-muted" style={{ fontSize: '0.85rem' }}>
              {auditTotal} evento(s)
              {busy ? ' · a atualizar…' : ''}
            </span>
          </div>
          <div className="wp-table-wrap">
            <table className="wp-table">
              <thead>
                <tr>
                  <th>Quando</th>
                  <th>Quem</th>
                  <th>Ação</th>
                  <th>Alvo</th>
                  <th>Detalhes</th>
                </tr>
              </thead>
              <tbody>
                {auditItems.map((ev) => (
                  <tr key={ev.id}>
                    <td style={{ whiteSpace: 'nowrap' }}>{formatDt(ev.created_at)}</td>
                    <td>{ev.actor_email}</td>
                    <td>{ev.action}</td>
                    <td>{ev.target_email ?? '—'}</td>
                    <td
                      style={{
                        maxWidth: 280,
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        fontSize: '0.78rem',
                      }}
                      title={formatAuditDetails(ev.details)}
                    >
                      {formatAuditDetails(ev.details)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {auditTotal > limit ? (
            <div
              style={{
                display: 'flex',
                gap: '0.75rem',
                alignItems: 'center',
                justifyContent: 'flex-end',
                marginTop: '1rem',
              }}
            >
              <button
                type="button"
                className="wp-btn wp-btn-ghost"
                disabled={auditSkip <= 0 || busy}
                onClick={() => setAuditSkip((s) => Math.max(0, s - limit))}
              >
                Anterior
              </button>
              <span className="wp-muted" style={{ fontSize: '0.85rem' }}>
                Página {auditPage} / {auditTotalPages}
              </span>
              <button
                type="button"
                className="wp-btn wp-btn-ghost"
                disabled={auditSkip >= auditMaxSkip || busy}
                onClick={() => setAuditSkip((s) => s + limit)}
              >
                Seguinte
              </button>
            </div>
          ) : null}
        </div>
      )}
      {selectedUser ? (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            background: 'rgba(0,0,0,0.45)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '1rem',
            zIndex: 50,
          }}
          onClick={() => setSelectedUser(null)}
        >
          <div
            className="wp-card"
            style={{ width: 'min(760px, 95vw)', maxHeight: '85vh', overflow: 'auto' }}
            onClick={(e) => e.stopPropagation()}
          >
            <div className="wp-toolbar" style={{ marginTop: 0 }}>
              <h1 style={{ margin: 0, fontSize: '1.1rem' }}>Detalhe da conta</h1>
              <button type="button" className="wp-btn wp-btn-ghost" onClick={() => setSelectedUser(null)}>
                Fechar
              </button>
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(170px, 1fr))', gap: '0.7rem' }}>
              <div>
                <div className="wp-muted" style={{ marginBottom: '0.2rem', fontSize: '0.78rem' }}>E-mail</div>
                <div>{selectedUser.user.email}</div>
              </div>
              <div>
                <div className="wp-muted" style={{ marginBottom: '0.2rem', fontSize: '0.78rem' }}>Nome</div>
                <div>{selectedUser.user.display_name || selectedUser.user.full_name || '—'}</div>
              </div>
              <div>
                <div className="wp-muted" style={{ marginBottom: '0.2rem', fontSize: '0.78rem' }}>Última atividade</div>
                <div>{formatDt(selectedUser.user.last_seen_at)}</div>
              </div>
              <div>
                <div className="wp-muted" style={{ marginBottom: '0.2rem', fontSize: '0.78rem' }}>Criado em</div>
                <div>{formatDt(selectedUser.user.created_at)}</div>
              </div>
              <div>
                <div className="wp-muted" style={{ marginBottom: '0.2rem', fontSize: '0.78rem' }}>Eventos 7d</div>
                <div>{selectedUser.events_7d}</div>
              </div>
              <div>
                <div className="wp-muted" style={{ marginBottom: '0.2rem', fontSize: '0.78rem' }}>Eventos 30d</div>
                <div>{selectedUser.events_30d}</div>
              </div>
            </div>
            <div style={{ marginTop: '0.9rem' }}>
              <div className="wp-muted" style={{ marginBottom: '0.4rem', fontSize: '0.78rem' }}>
                Tipos de evento (30d)
              </div>
              {Object.keys(selectedUser.event_types_30d).length > 0 ? (
                <div style={{ display: 'grid', gap: '0.35rem' }}>
                  {Object.entries(selectedUser.event_types_30d).map(([k, v]) => (
                    <div key={k} style={{ display: 'flex', justifyContent: 'space-between' }}>
                      <span>{k}</span>
                      <strong>{v}</strong>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="wp-muted" style={{ margin: 0 }}>
                  Sem eventos no período.
                </div>
              )}
            </div>
            <div style={{ marginTop: '0.9rem' }}>
              <div className="wp-muted" style={{ marginBottom: '0.4rem', fontSize: '0.78rem' }}>
                Últimos eventos (até 30)
              </div>
              {selectedUser.recent_events.length > 0 ? (
                <ul
                  style={{
                    margin: 0,
                    paddingLeft: '1.1rem',
                    fontSize: '0.85rem',
                    display: 'grid',
                    gap: '0.35rem',
                  }}
                >
                  {selectedUser.recent_events.map((ev, i) => (
                    <li key={`${ev.occurred_at}-${ev.event_type}-${i}`} style={{ lineHeight: 1.35 }}>
                      <span className="wp-muted">{formatDt(ev.occurred_at)}</span>
                      {' · '}
                      <span>{ev.event_type}</span>
                    </li>
                  ))}
                </ul>
              ) : (
                <div className="wp-muted" style={{ margin: 0 }}>
                  Sem eventos registados.
                </div>
              )}
            </div>
          </div>
        </div>
      ) : null}
      {selectedFamily ? (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            background: 'rgba(0,0,0,0.45)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '1rem',
            zIndex: 50,
          }}
          onClick={() => setSelectedFamily(null)}
        >
          <div
            className="wp-card"
            style={{ width: 'min(760px, 95vw)', maxHeight: '85vh', overflow: 'auto' }}
            onClick={(e) => e.stopPropagation()}
          >
            <div className="wp-toolbar" style={{ marginTop: 0 }}>
              <h1 style={{ margin: 0, fontSize: '1.1rem' }}>Detalhe da família</h1>
              <button type="button" className="wp-btn wp-btn-ghost" onClick={() => setSelectedFamily(null)}>
                Fechar
              </button>
            </div>
            <div
              style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))', gap: '0.7rem' }}
            >
              <div>
                <div className="wp-muted" style={{ marginBottom: '0.2rem', fontSize: '0.78rem' }}>Nome</div>
                <div>{selectedFamily.name}</div>
              </div>
              <div>
                <div className="wp-muted" style={{ marginBottom: '0.2rem', fontSize: '0.78rem' }}>
                  Membros
                </div>
                <div>
                  {selectedFamily.member_count} / {selectedFamily.max_members}
                </div>
              </div>
              <div>
                <div className="wp-muted" style={{ marginBottom: '0.2rem', fontSize: '0.78rem' }}>Criado</div>
                <div>{formatDt(selectedFamily.created_at)}</div>
              </div>
            </div>
            <div style={{ marginTop: '0.9rem' }}>
              <div className="wp-muted" style={{ marginBottom: '0.4rem', fontSize: '0.78rem' }}>
                Membros
              </div>
              {selectedFamily.members.length > 0 ? (
                <div className="wp-table-wrap">
                  <table className="wp-table">
                    <thead>
                      <tr>
                        <th>E-mail</th>
                        <th>Nome</th>
                        <th>Papel</th>
                        <th>Ativo</th>
                      </tr>
                    </thead>
                    <tbody>
                      {selectedFamily.members.map((m) => (
                        <tr key={m.user_id}>
                          <td>{m.email}</td>
                          <td>{m.display_name || m.full_name || '—'}</td>
                          <td>{m.role}</td>
                          <td>{m.is_active ? 'Sim' : 'Não'}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <p className="wp-muted" style={{ margin: 0 }}>
                  Sem membros.
                </p>
              )}
            </div>
            <div style={{ marginTop: '0.9rem' }}>
              <div className="wp-muted" style={{ marginBottom: '0.4rem', fontSize: '0.78rem' }}>
                Convites (sem token — só metadados)
              </div>
              {selectedFamily.invites.length > 0 ? (
                <div className="wp-table-wrap">
                  <table className="wp-table">
                    <thead>
                      <tr>
                        <th>Expira</th>
                        <th>Estado</th>
                      </tr>
                    </thead>
                    <tbody>
                      {selectedFamily.invites.map((inv) => {
                        const expired =
                          !inv.used && new Date(inv.expires_at).getTime() < Date.now()
                        const stateLabel = inv.used ? 'Utilizado' : expired ? 'Expirado' : 'Pendente'
                        return (
                          <tr key={inv.id}>
                            <td>{formatDt(inv.expires_at)}</td>
                            <td>{stateLabel}</td>
                          </tr>
                        )
                      })}
                    </tbody>
                  </table>
                </div>
              ) : (
                <p className="wp-muted" style={{ margin: 0 }}>
                  Sem convites registados.
                </p>
              )}
            </div>
          </div>
        </div>
      ) : null}
    </div>
  )
}
