import { useCallback, useEffect, useState } from 'react'
import {
  adminMe,
  clearTokens,
  getUsageSummary,
  getApiBase,
  listUsers,
  loadStoredTokens,
  loginRequest,
  patchUserActive,
  saveTokens,
  type AdminUsageSummary,
  type AdminUserRow,
} from './api'

type View = 'boot' | 'login' | 'users'

function formatDt(iso: string | null): string {
  if (!iso) return '—'
  try {
    return new Date(iso).toLocaleString('pt-PT')
  } catch {
    return iso
  }
}

function escapeCsvCell(s: string): string {
  if (/[",\n\r]/.test(s)) return `"${s.replace(/"/g, '""')}"`
  return s
}

async function fetchAllUsersForExport(
  token: string,
  q: string,
): Promise<AdminUserRow[]> {
  const pageSize = 100
  const out: AdminUserRow[] = []
  let skip = 0
  while (true) {
    const data = await listUsers(token, {
      q: q || undefined,
      skip,
      limit: pageSize,
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
  const [busy, setBusy] = useState(false)
  const [accessToken, setAccessToken] = useState<string | null>(null)
  const [adminEmail, setAdminEmail] = useState<string | null>(null)
  const [rows, setRows] = useState<AdminUserRow[]>([])
  const [total, setTotal] = useState(0)
  const [summary, setSummary] = useState<AdminUsageSummary | null>(null)
  const [q, setQ] = useState('')
  const [qDebounced, setQDebounced] = useState('')
  const [skip, setSkip] = useState(0)
  const limit = 30

  const logout = useCallback(() => {
    clearTokens()
    setAccessToken(null)
    setAdminEmail(null)
    setRows([])
    setView('login')
  }, [])

  const loadList = useCallback(
    async (token: string, opts: { q: string; skip: number }) => {
      const [data, usage] = await Promise.all([
        listUsers(token, { q: opts.q || undefined, skip: opts.skip, limit }),
        getUsageSummary(token, 14),
      ])
      setRows(data.items)
      setTotal(data.total)
      setSummary(usage)
    },
    [],
  )

  useEffect(() => {
    const t = window.setTimeout(() => setQDebounced(q.trim()), 350)
    return () => window.clearTimeout(t)
  }, [q])

  useEffect(() => {
    if (view !== 'users' || !accessToken) return
    let cancelled = false
    setBusy(true)
    loadList(accessToken, { q: qDebounced, skip })
      .catch((e) => {
        if (!cancelled) setError(e instanceof Error ? e.message : 'Erro ao carregar')
      })
      .finally(() => {
        if (!cancelled) setBusy(false)
      })
    return () => {
      cancelled = true
    }
  }, [view, accessToken, qDebounced, skip, loadList])

  useEffect(() => {
    setSkip(0)
  }, [qDebounced])

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
    setBusy(true)
    try {
      const all = await fetchAllUsersForExport(accessToken, qDebounced)
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
    setBusy(true)
    try {
      await patchUserActive(accessToken, row.id, next)
      await loadList(accessToken, { q: qDebounced, skip })
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Erro ao atualizar')
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
  const peakEvents = maxEventsInSeries(summary)

  return (
    <div>
      <div className="wp-toolbar">
        <div>
          <h1 style={{ marginBottom: '0.25rem' }}>Contas</h1>
          <p className="wp-muted" style={{ margin: 0 }}>
            Sessão: {adminEmail ?? '—'}
          </p>
        </div>
        <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
          <button
            type="button"
            className="wp-btn wp-btn-ghost"
            onClick={() => void exportCsv()}
            disabled={busy}
          >
            Exportar CSV
          </button>
          <button type="button" className="wp-btn wp-btn-ghost" onClick={logout}>
            Sair
          </button>
        </div>
      </div>

      {error ? <div className="wp-error">{error}</div> : null}

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
                    <button
                      type="button"
                      className="wp-btn wp-btn-ghost"
                      style={{ padding: '0.35rem 0.65rem', fontSize: '0.8rem' }}
                      onClick={() => toggleActive(r)}
                      disabled={busy}
                    >
                      {r.is_active ? 'Desativar' : 'Reativar'}
                    </button>
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
    </div>
  )
}
