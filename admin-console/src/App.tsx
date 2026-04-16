import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  adminMe,
  clearTokens,
  createAnnouncement,
  getApiBase,
  getFamilyDetail,
  getFinanceSummary,
  getProductFunnel,
  getUsageSummary,
  getUserDetail,
  listAnnouncements,
  listAuditEvents,
  listFamilies,
  listUsers,
  loadStoredTokens,
  loginRequest,
  patchUserActive,
  patchAnnouncement,
  saveTokens,
  type AnnouncementPlacement,
  type AnnouncementRow,
  type AdminAuditEventRow,
  type AdminFamilyDetailResponse,
  type AdminFamilyRow,
  type AdminFinanceSummary,
  type AdminProductFunnel,
  type AdminUsageSummary,
  type AdminUserDetailResponse,
  type AdminUserRow,
} from './api'
import { AdminShell, Dropdown, Modal, SectionHeader, StatCard } from './components/admin-ui'
import { UsersSection } from './sections/UsersSection'
import { FamiliesSection } from './sections/FamiliesSection'
import { FinanceSection } from './sections/FinanceSection'
import { FunnelSection } from './sections/FunnelSection'
import { AuditSection } from './sections/AuditSection'
import { AnnouncementsSection } from './sections/AnnouncementsSection'

type View = 'boot' | 'login' | 'users'
type AdminSection = 'users' | 'families' | 'finance' | 'funnel' | 'audit' | 'announcements'
type TriState = 'all' | 'yes' | 'no'
type UiTheme = 'dark' | 'light'

const STORAGE_UI_THEME = 'wellpaid_admin_ui_theme'
const STORAGE_UI_DENSITY = 'wellpaid_admin_ui_density'
const STORAGE_UI_SECTION = 'wellpaid_admin_ui_section'
const STORAGE_UI_SIDEBAR = 'wellpaid_admin_ui_sidebar_collapsed'
const STORAGE_UI_USERS_COLS = 'wellpaid_admin_ui_users_cols'
const STORAGE_UI_FAMILIES_COLS = 'wellpaid_admin_ui_families_cols'
const STORAGE_UI_AUDIT_COLS = 'wellpaid_admin_ui_audit_cols'
const STORAGE_UI_AUDIT_FILTERS = 'wellpaid_admin_ui_audit_filters'
const STORAGE_UI_ANNOUNCEMENTS_FILTERS = 'wellpaid_admin_ui_announcements_filters'

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

function downloadCsv(filename: string, headers: string[], lines: string[][]): void {
  const content = [headers.join(','), ...lines.map((row) => row.map((c) => escapeCsvCell(String(c))).join(','))].join('\n')
  const blob = new Blob(['\ufeff' + content], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
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
  downloadCsv(
    `wellpaid-contas-${new Date().toISOString().slice(0, 10)}.csv`,
    headers,
    rows.map((r) => [
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
    ]),
  )
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
  const [auditActorEmail, setAuditActorEmail] = useState('')
  const [auditAction, setAuditAction] = useState('')
  const [auditFrom, setAuditFrom] = useState('')
  const [auditTo, setAuditTo] = useState('')
  const [auditActorEmailDebounced, setAuditActorEmailDebounced] = useState('')
  const [auditActionDebounced, setAuditActionDebounced] = useState('')
  const [auditFromDebounced, setAuditFromDebounced] = useState('')
  const [auditToDebounced, setAuditToDebounced] = useState('')
  const [announcementItems, setAnnouncementItems] = useState<AnnouncementRow[]>([])
  const [announcementTotal, setAnnouncementTotal] = useState(0)
  const [announcementSkip, setAnnouncementSkip] = useState(0)
  const [announcementStatusFilter, setAnnouncementStatusFilter] = useState<'all' | 'active' | 'inactive'>('all')
  const [announcementPlacementFilter, setAnnouncementPlacementFilter] = useState<'all' | AnnouncementPlacement>('all')
  const [tableDensity, setTableDensity] = useState<'comfortable' | 'compact'>('comfortable')
  const [uiTheme, setUiTheme] = useState<UiTheme>('dark')
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false)
  const [usersVisibleColumns, setUsersVisibleColumns] = useState({
    email: true,
    name: true,
    active: true,
    admin: true,
    verified: true,
    lastSeen: true,
    created: true,
  })
  const [familiesVisibleColumns, setFamiliesVisibleColumns] = useState({
    name: true,
    members: true,
    created: true,
  })
  const [auditVisibleColumns, setAuditVisibleColumns] = useState({
    when: true,
    who: true,
    action: true,
    target: true,
    details: true,
  })

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
    setAnnouncementItems([])
    setAnnouncementTotal(0)
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
      listAuditEvents(token, {
        skip: opts.skip,
        limit,
        actor_email: auditActorEmailDebounced.trim() || undefined,
        action: auditActionDebounced.trim() || undefined,
        created_from: auditFromDebounced ? `${auditFromDebounced}T00:00:00Z` : undefined,
        created_to: auditToDebounced ? `${auditToDebounced}T23:59:59Z` : undefined,
      }),
      getUsageSummary(token, 14),
    ])
    setAuditItems(data.items)
    setAuditTotal(data.total)
    setSummary(usage)
  }, [auditActorEmailDebounced, auditActionDebounced, auditFromDebounced, auditToDebounced])

  const loadAnnouncements = useCallback(async (token: string, opts: { skip: number }) => {
    const [data, usage] = await Promise.all([
      listAnnouncements(token, {
        skip: opts.skip,
        limit,
        placement: announcementPlacementFilter === 'all' ? undefined : announcementPlacementFilter,
        is_active:
          announcementStatusFilter === 'all' ? undefined : announcementStatusFilter === 'active',
      }),
      getUsageSummary(token, 14),
    ])
    setAnnouncementItems(data.items)
    setAnnouncementTotal(data.total)
    setSummary(usage)
  }, [announcementPlacementFilter, announcementStatusFilter])

  useEffect(() => {
    const t = window.setTimeout(() => setQDebounced(q.trim()), 350)
    return () => window.clearTimeout(t)
  }, [q])

  useEffect(() => {
    const t = window.setTimeout(() => setFamilyQDebounced(familyQ.trim()), 350)
    return () => window.clearTimeout(t)
  }, [familyQ])

  useEffect(() => {
    const t = window.setTimeout(() => {
      setAuditActorEmailDebounced(auditActorEmail.trim())
      setAuditActionDebounced(auditAction.trim())
      setAuditFromDebounced(auditFrom)
      setAuditToDebounced(auditTo)
    }, 450)
    return () => window.clearTimeout(t)
  }, [auditActorEmail, auditAction, auditFrom, auditTo])

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
    if (view !== 'users' || !accessToken || adminSection !== 'announcements') return
    let cancelled = false
    setBusy(true)
    loadAnnouncements(accessToken, { skip: announcementSkip })
      .catch((e) => {
        if (!cancelled) {
          setInfo(null)
          setError(e instanceof Error ? e.message : 'Erro ao carregar avisos')
        }
      })
      .finally(() => {
        if (!cancelled) setBusy(false)
      })
    return () => {
      cancelled = true
    }
  }, [view, accessToken, adminSection, announcementSkip, loadAnnouncements])

  useEffect(() => {
    setAuditSkip(0)
  }, [auditActorEmailDebounced, auditActionDebounced, auditFromDebounced, auditToDebounced])

  useEffect(() => {
    setAnnouncementSkip(0)
  }, [announcementStatusFilter, announcementPlacementFilter])

  useEffect(() => {
    setSkip(0)
  }, [qDebounced, isActiveFilter, isAdminFilter, emailVerifiedFilter, createdFrom, createdTo, orderBy, orderDir])

  useEffect(() => {
    setFamilySkip(0)
  }, [familyQDebounced, familyOrderBy, familyOrderDir])

  useEffect(() => {
    const savedTheme = sessionStorage.getItem(STORAGE_UI_THEME)
    if (savedTheme === 'dark' || savedTheme === 'light') {
      setUiTheme(savedTheme)
    }
    const savedDensity = sessionStorage.getItem(STORAGE_UI_DENSITY)
    if (savedDensity === 'comfortable' || savedDensity === 'compact') {
      setTableDensity(savedDensity)
    }
    const savedSection = sessionStorage.getItem(STORAGE_UI_SECTION)
    if (savedSection === 'users' || savedSection === 'families' || savedSection === 'finance' || savedSection === 'funnel' || savedSection === 'audit' || savedSection === 'announcements') {
      setAdminSection(savedSection)
    }
    const savedSidebar = sessionStorage.getItem(STORAGE_UI_SIDEBAR)
    if (savedSidebar === '1' || savedSidebar === '0') {
      setSidebarCollapsed(savedSidebar === '1')
    }
    const savedUserCols = sessionStorage.getItem(STORAGE_UI_USERS_COLS)
    if (savedUserCols) {
      try {
        const parsed = JSON.parse(savedUserCols) as Partial<typeof usersVisibleColumns>
        setUsersVisibleColumns((prev) => ({ ...prev, ...parsed }))
      } catch {
        /* ignore */
      }
    }
    const savedFamiliesCols = sessionStorage.getItem(STORAGE_UI_FAMILIES_COLS)
    if (savedFamiliesCols) {
      try {
        const parsed = JSON.parse(savedFamiliesCols) as Partial<typeof familiesVisibleColumns>
        setFamiliesVisibleColumns((prev) => ({ ...prev, ...parsed }))
      } catch {
        /* ignore */
      }
    }
    const savedAuditCols = sessionStorage.getItem(STORAGE_UI_AUDIT_COLS)
    if (savedAuditCols) {
      try {
        const parsed = JSON.parse(savedAuditCols) as Partial<typeof auditVisibleColumns>
        setAuditVisibleColumns((prev) => ({ ...prev, ...parsed }))
      } catch {
        /* ignore */
      }
    }

    const savedAuditFiltersRaw = sessionStorage.getItem(STORAGE_UI_AUDIT_FILTERS)
    if (savedAuditFiltersRaw) {
      try {
        const parsed = JSON.parse(savedAuditFiltersRaw) as Partial<{
          actorEmail: string
          action: string
          from: string
          to: string
        }>
        const nextActor = typeof parsed.actorEmail === 'string' ? parsed.actorEmail : ''
        const nextAction = typeof parsed.action === 'string' ? parsed.action : ''
        const nextFrom = typeof parsed.from === 'string' ? parsed.from : ''
        const nextTo = typeof parsed.to === 'string' ? parsed.to : ''
        setAuditActorEmail(nextActor)
        setAuditAction(nextAction)
        setAuditFrom(nextFrom)
        setAuditTo(nextTo)
        setAuditActorEmailDebounced(nextActor)
        setAuditActionDebounced(nextAction)
        setAuditFromDebounced(nextFrom)
        setAuditToDebounced(nextTo)
      } catch {
        /* ignore */
      }
    }

    const savedAnnouncementsFiltersRaw = sessionStorage.getItem(STORAGE_UI_ANNOUNCEMENTS_FILTERS)
    if (savedAnnouncementsFiltersRaw) {
      try {
        const parsed = JSON.parse(savedAnnouncementsFiltersRaw) as Partial<{
          status: 'all' | 'active' | 'inactive'
          placement: 'all' | AnnouncementPlacement
        }>
        if (parsed.status === 'all' || parsed.status === 'active' || parsed.status === 'inactive') {
          setAnnouncementStatusFilter(parsed.status)
        }
        if (
          parsed.placement === 'all' ||
          parsed.placement === 'home_banner' ||
          parsed.placement === 'home_feed' ||
          parsed.placement === 'finance_tab' ||
          parsed.placement === 'announcements_tab'
        ) {
          setAnnouncementPlacementFilter(parsed.placement)
        }
      } catch {
        /* ignore */
      }
    }
  }, [])

  useEffect(() => {
    sessionStorage.setItem(STORAGE_UI_THEME, uiTheme)
    document.documentElement.setAttribute('data-wp-theme', uiTheme)
  }, [uiTheme])

  useEffect(() => {
    sessionStorage.setItem(STORAGE_UI_DENSITY, tableDensity)
  }, [tableDensity])

  useEffect(() => {
    sessionStorage.setItem(STORAGE_UI_SECTION, adminSection)
  }, [adminSection])

  useEffect(() => {
    sessionStorage.setItem(STORAGE_UI_SIDEBAR, sidebarCollapsed ? '1' : '0')
  }, [sidebarCollapsed])

  useEffect(() => {
    sessionStorage.setItem(STORAGE_UI_USERS_COLS, JSON.stringify(usersVisibleColumns))
  }, [usersVisibleColumns])

  useEffect(() => {
    sessionStorage.setItem(STORAGE_UI_FAMILIES_COLS, JSON.stringify(familiesVisibleColumns))
  }, [familiesVisibleColumns])

  useEffect(() => {
    sessionStorage.setItem(STORAGE_UI_AUDIT_COLS, JSON.stringify(auditVisibleColumns))
  }, [auditVisibleColumns])

  useEffect(() => {
    sessionStorage.setItem(
      STORAGE_UI_AUDIT_FILTERS,
      JSON.stringify({ actorEmail: auditActorEmail, action: auditAction, from: auditFrom, to: auditTo }),
    )
  }, [auditActorEmail, auditAction, auditFrom, auditTo])

  useEffect(() => {
    sessionStorage.setItem(
      STORAGE_UI_ANNOUNCEMENTS_FILTERS,
      JSON.stringify({ status: announcementStatusFilter, placement: announcementPlacementFilter }),
    )
  }, [announcementStatusFilter, announcementPlacementFilter])

  const toggleUsersColumn = useCallback((k: keyof typeof usersVisibleColumns) => {
    setUsersVisibleColumns((prev) => {
      const next = { ...prev, [k]: !prev[k] }
      if (Object.values(next).some(Boolean)) return next
      return prev
    })
  }, [])

  const toggleFamiliesColumn = useCallback((k: keyof typeof familiesVisibleColumns) => {
    setFamiliesVisibleColumns((prev) => {
      const next = { ...prev, [k]: !prev[k] }
      if (Object.values(next).some(Boolean)) return next
      return prev
    })
  }, [])

  const toggleAuditColumn = useCallback((k: keyof typeof auditVisibleColumns) => {
    setAuditVisibleColumns((prev) => {
      const next = { ...prev, [k]: !prev[k] }
      if (Object.values(next).some(Boolean)) return next
      return prev
    })
  }, [])

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

  async function exportFamiliesCsv() {
    if (!accessToken) return
    setError(null)
    setInfo(null)
    setBusy(true)
    try {
      const pageSize = 200
      const all: AdminFamilyRow[] = []
      let skip = 0
      while (true) {
        const data = await listFamilies(accessToken, {
          q: familyQDebounced || undefined,
          skip,
          limit: pageSize,
          order_by: familyOrderBy,
          order_dir: familyOrderDir,
        })
        all.push(...data.items)
        if (data.items.length < pageSize || all.length >= data.total) break
        skip += pageSize
      }

      const headers: string[] = []
      if (familiesVisibleColumns.name) headers.push('name')
      if (familiesVisibleColumns.members) headers.push('member_count')
      if (familiesVisibleColumns.created) headers.push('created_at')
      headers.push('id')

      downloadCsv(
        `wellpaid-familias-${new Date().toISOString().slice(0, 10)}.csv`,
        headers,
        all.map((r) => {
          const row: string[] = []
          if (familiesVisibleColumns.name) row.push(r.name)
          if (familiesVisibleColumns.members) row.push(String(r.member_count))
          if (familiesVisibleColumns.created) row.push(r.created_at)
          row.push(r.id)
          return row
        }),
      )
      setInfo(`Exportadas ${all.length} famílias.`)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Erro ao exportar famílias')
    } finally {
      setBusy(false)
    }
  }

  async function exportAuditCsv() {
    if (!accessToken) return
    setError(null)
    setInfo(null)
    setBusy(true)
    try {
      const pageSize = 300
      const all: AdminAuditEventRow[] = []
      let skip = 0
      while (true) {
        const data = await listAuditEvents(accessToken, {
          skip,
          limit: pageSize,
        actor_email: auditActorEmailDebounced.trim() || undefined,
        action: auditActionDebounced.trim() || undefined,
        created_from: auditFromDebounced ? `${auditFromDebounced}T00:00:00Z` : undefined,
        created_to: auditToDebounced ? `${auditToDebounced}T23:59:59Z` : undefined,
        })
        all.push(...data.items)
        if (data.items.length < pageSize || all.length >= data.total) break
        skip += pageSize
      }

      const headers: string[] = []
      if (auditVisibleColumns.when) headers.push('created_at')
      if (auditVisibleColumns.who) headers.push('actor_email')
      if (auditVisibleColumns.action) headers.push('action')
      if (auditVisibleColumns.target) headers.push('target_email')
      if (auditVisibleColumns.details) headers.push('details')
      headers.push('id')

      downloadCsv(
        `wellpaid-auditoria-${new Date().toISOString().slice(0, 10)}.csv`,
        headers,
        all.map((ev) => {
          const row: string[] = []
          if (auditVisibleColumns.when) row.push(ev.created_at)
          if (auditVisibleColumns.who) row.push(ev.actor_email)
          if (auditVisibleColumns.action) row.push(ev.action)
          if (auditVisibleColumns.target) row.push(ev.target_email ?? '')
          if (auditVisibleColumns.details) row.push(formatAuditDetails(ev.details))
          row.push(ev.id)
          return row
        }),
      )
      setInfo(`Exportados ${all.length} eventos de auditoria.`)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Erro ao exportar auditoria')
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

  async function handleCreateAnnouncement(payload: {
    title: string
    body: string
    kind: 'info' | 'warning' | 'tip' | 'material'
    placement: AnnouncementPlacement
    priority: number
    cta_label: string
    cta_url: string
    is_active: boolean
    starts_at: string | null
    ends_at: string | null
    target_user_email: string
  }) {
    if (!accessToken) return
    setError(null)
    setInfo(null)
    setBusy(true)
    try {
      await createAnnouncement(accessToken, {
        ...payload,
        cta_label: payload.cta_label || null,
        cta_url: payload.cta_url || null,
        target_user_email: payload.target_user_email.trim() || null,
      })
      await loadAnnouncements(accessToken, { skip: announcementSkip })
      setInfo('Aviso criado com sucesso.')
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Erro ao criar aviso')
      throw e
    } finally {
      setBusy(false)
    }
  }

  async function handleUpdateAnnouncement(
    id: string,
    payload: {
      title: string
      body: string
      kind: 'info' | 'warning' | 'tip' | 'material'
      placement: AnnouncementPlacement
      priority: number
      cta_label: string
      cta_url: string
      is_active: boolean
      starts_at: string | null
      ends_at: string | null
      target_user_email: string
    },
  ) {
    if (!accessToken) return
    setError(null)
    setInfo(null)
    setBusy(true)
    try {
      await patchAnnouncement(accessToken, id, {
        ...payload,
        cta_label: payload.cta_label || null,
        cta_url: payload.cta_url || null,
        target_user_email: payload.target_user_email.trim(),
      })
      await loadAnnouncements(accessToken, { skip: announcementSkip })
      setInfo('Aviso atualizado com sucesso.')
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Erro ao atualizar aviso')
      throw e
    } finally {
      setBusy(false)
    }
  }

  const sectionLabel = useMemo<Record<AdminSection, string>>(
    () => ({ users: 'Contas', families: 'Famílias', finance: 'Finanças', funnel: 'Funil', audit: 'Auditoria', announcements: 'Avisos' }),
    [],
  )
  const navItems: AdminSection[] = ['users', 'families', 'finance', 'funnel', 'audit', 'announcements']

  if (view === 'boot') return <div className="wp-loading">A carregar…</div>

  if (view === 'login') {
    return (
      <div className="wp-login-shell">
        <div className="wp-login-card wp-card">
          <h1>Well Paid Admin Console</h1>
          <p className="wp-muted">API: {getApiBase()}</p>
          <form onSubmit={handleLogin}>
            {error ? <div className="wp-error">{error}</div> : null}
            <div className="wp-field"><label htmlFor="email">E-mail</label><input id="email" type="email" autoComplete="username" value={email} onChange={(e) => setEmail(e.target.value)} required /></div>
            <div className="wp-field"><label htmlFor="password">Senha</label><input id="password" type="password" autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} required /></div>
            <button className="wp-btn" type="submit" disabled={busy} style={{ width: '100%' }}>{busy ? 'A entrar…' : 'Entrar'}</button>
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
  const annMaxSkip = Math.max(0, announcementTotal - limit)
  const annPage = Math.floor(announcementSkip / limit) + 1
  const annTotalPages = Math.max(1, Math.ceil(announcementTotal / limit))
  const peakEvents = maxEventsInSeries(summary)
  return (
    <AdminShell
      sidebarCollapsed={sidebarCollapsed}
      sidebar={
        <div>
          <div className="wp-brand">{sidebarCollapsed ? 'WP' : 'Well Paid Admin'}</div>
          {sidebarCollapsed ? null : <p className="wp-muted">Sessão: {adminEmail ?? '—'}</p>}
          <nav className="wp-nav-list">
            {navItems.map((item) => (
              <button
                key={item}
                type="button"
                className={adminSection === item ? 'wp-nav-item wp-nav-item-active' : 'wp-nav-item'}
                onClick={() => {
                  setAdminSection(item)
                  setSelectedFamily(null)
                  setSelectedUser(null)
                }}
                title={sectionLabel[item]}
              >
                {sidebarCollapsed ? sectionLabel[item].slice(0, 1) : sectionLabel[item]}
              </button>
            ))}
          </nav>
        </div>
      }
      topbar={
        <div className="wp-toolbar">
          <div>
            <h1 className="wp-h1">{sectionLabel[adminSection]}</h1>
            <p className="wp-muted">Console administrativo moderno e responsivo</p>
          </div>
          <div className="wp-toolbar">
            <Dropdown
              button={<button type="button" className="wp-btn wp-btn-ghost">Preferências</button>}
            >
              <div className="wp-pref-grid">
                <div className="wp-pref-row">
                  <label>Tema</label>
                  <select
                    className="wp-search"
                    value={uiTheme}
                    onChange={(e) => setUiTheme(e.target.value as UiTheme)}
                  >
                    <option value="dark">Escuro</option>
                    <option value="light">Claro</option>
                  </select>
                </div>
                <div className="wp-pref-row">
                  <label>Densidade</label>
                  <select
                    className="wp-search"
                    value={tableDensity}
                    onChange={(e) => setTableDensity(e.target.value as 'comfortable' | 'compact')}
                  >
                    <option value="comfortable">Confortável</option>
                    <option value="compact">Compacta</option>
                  </select>
                </div>
                <div className="wp-pref-row">
                  <label>Sidebar</label>
                  <button
                    type="button"
                    className="wp-btn wp-btn-ghost"
                    onClick={() => setSidebarCollapsed((v) => !v)}
                  >
                    {sidebarCollapsed ? 'Expandir' : 'Colapsar'}
                  </button>
                </div>
              </div>
            </Dropdown>
            {adminSection === 'users' ? (
              <button type="button" className="wp-btn wp-btn-ghost" onClick={() => void exportCsv()} disabled={busy}>
                Exportar CSV
              </button>
            ) : null}
            {adminSection === 'families' ? (
              <button type="button" className="wp-btn wp-btn-ghost" onClick={() => void exportFamiliesCsv()} disabled={busy}>
                Exportar CSV
              </button>
            ) : null}
            {adminSection === 'audit' ? (
              <button type="button" className="wp-btn wp-btn-ghost" onClick={() => void exportAuditCsv()} disabled={busy}>
                Exportar CSV
              </button>
            ) : null}
            <button type="button" className="wp-btn wp-btn-ghost" onClick={logout}>Sair</button>
          </div>
        </div>
      }
    >

      {error ? <div className="wp-error">{error}</div> : null}
      {info ? <div className="wp-info">{info}</div> : null}

      <div className="wp-kpi-grid wp-kpi-grid-top">
        <StatCard label="Eventos (24h)" value={summary?.events_24h ?? '—'} />
        <StatCard label="Utilizadores ativos (7d)" value={summary?.dau_7d ?? '—'} />
        <StatCard label="Utilizadores ativos (30d)" value={summary?.mau_30d ?? '—'} />
      </div>

      <div className="wp-card">
        <SectionHeader title="Tendência de uso (14 dias)" subtitle="Eventos de autenticação por dia" />
        {summary && summary.series.length > 0 ? (
          <div className="wp-trend-bars">
            {summary.series.map((p) => {
              const h = peakEvents > 0 ? Math.max(6, Math.round((p.events / peakEvents) * 90)) : 6
              return (
                <div
                  key={p.day}
                  title={`${p.day}: ${p.events} eventos / ${p.active_users} ativos`}
                  className="wp-trend-bar"
                  style={{ height: h }}
                />
              )
            })}
          </div>
        ) : (
          <p className="wp-muted">
            Ainda sem eventos suficientes para mostrar tendência.
          </p>
        )}
      </div>

      {adminSection === 'users' ? (
        <UsersSection
          rows={rows}
          total={total}
          busy={busy}
          q={q}
          isActiveFilter={isActiveFilter}
          isAdminFilter={isAdminFilter}
          emailVerifiedFilter={emailVerifiedFilter}
          createdFrom={createdFrom}
          createdTo={createdTo}
          orderBy={orderBy}
          orderDir={orderDir}
          onQChange={setQ}
          onIsActiveFilterChange={setIsActiveFilter}
          onIsAdminFilterChange={setIsAdminFilter}
          onEmailVerifiedFilterChange={setEmailVerifiedFilter}
          onCreatedFromChange={setCreatedFrom}
          onCreatedToChange={setCreatedTo}
          onOrderByChange={setOrderBy}
          onOrderDirChange={setOrderDir}
          onOpenUserDetail={(r) => void openUserDetail(r)}
          onToggleActive={(r) => void toggleActive(r)}
          onToggleAdmin={(r) => void toggleAdmin(r)}
          onRevokeSessions={(r) => void revokeSessions(r)}
          formatDt={formatDt}
          page={page}
          totalPages={totalPages}
          onPrevPage={() => setSkip((s) => Math.max(0, s - limit))}
          onNextPage={() => setSkip((s) => s + limit)}
          disablePrevPage={skip <= 0 || busy}
          disableNextPage={skip >= maxSkip || busy}
          density={tableDensity}
          onDensityChange={setTableDensity}
          visibleColumns={usersVisibleColumns}
          onToggleColumn={toggleUsersColumn}
        />
      ) : null}
      {adminSection === 'families' ? (
        <FamiliesSection
          rows={familyRows}
          total={familyTotal}
          busy={busy}
          q={familyQ}
          orderBy={familyOrderBy}
          orderDir={familyOrderDir}
          onQChange={setFamilyQ}
          onOrderByChange={setFamilyOrderBy}
          onOrderDirChange={setFamilyOrderDir}
          onOpenDetail={(r) => void openFamilyDetail(r)}
          formatDt={formatDt}
          page={familyPage}
          totalPages={familyTotalPages}
          onPrevPage={() => setFamilySkip((s) => Math.max(0, s - limit))}
          onNextPage={() => setFamilySkip((s) => s + limit)}
          disablePrevPage={familySkip <= 0 || busy}
          disableNextPage={familySkip >= familyMaxSkip || busy}
          density={tableDensity}
          onDensityChange={setTableDensity}
          visibleColumns={familiesVisibleColumns}
          onToggleColumn={toggleFamiliesColumn}
        />
      ) : null}
      {adminSection === 'finance' ? <FinanceSection summary={financeSummary} busy={busy} formatEurFromCents={formatEurFromCents} /> : null}
      {adminSection === 'funnel' ? <FunnelSection metrics={funnelMetrics} busy={busy} pctOf={pctOf} /> : null}
      {adminSection === 'audit' ? (
        <AuditSection
          items={auditItems}
          total={auditTotal}
          busy={busy}
          formatDt={formatDt}
          formatAuditDetails={formatAuditDetails}
          filters={{ actorEmail: auditActorEmail, action: auditAction, from: auditFrom, to: auditTo }}
          onFiltersChange={(next) => {
            setAuditActorEmail(next.actorEmail)
            setAuditAction(next.action)
            setAuditFrom(next.from)
            setAuditTo(next.to)
          }}
          page={auditPage}
          totalPages={auditTotalPages}
          onPrevPage={() => setAuditSkip((s) => Math.max(0, s - limit))}
          onNextPage={() => setAuditSkip((s) => s + limit)}
          disablePrevPage={auditSkip <= 0 || busy}
          disableNextPage={auditSkip >= auditMaxSkip || busy}
          density={tableDensity}
          onDensityChange={setTableDensity}
          visibleColumns={auditVisibleColumns}
          onToggleColumn={toggleAuditColumn}
        />
      ) : null}
      {adminSection === 'announcements' ? (
        <AnnouncementsSection
          items={announcementItems}
          total={announcementTotal}
          busy={busy}
          statusFilter={announcementStatusFilter}
          placementFilter={announcementPlacementFilter}
          onStatusFilterChange={setAnnouncementStatusFilter}
          onPlacementFilterChange={setAnnouncementPlacementFilter}
          onClearFilters={() => {
            setAnnouncementStatusFilter('all')
            setAnnouncementPlacementFilter('all')
          }}
          onCreate={(payload) => handleCreateAnnouncement(payload)}
          onUpdate={(id, payload) => handleUpdateAnnouncement(id, payload)}
          formatDt={formatDt}
          page={annPage}
          totalPages={annTotalPages}
          onPrevPage={() => setAnnouncementSkip((s) => Math.max(0, s - limit))}
          onNextPage={() => setAnnouncementSkip((s) => s + limit)}
          disablePrevPage={announcementSkip <= 0 || busy}
          disableNextPage={announcementSkip >= annMaxSkip || busy}
        />
      ) : null}
      {selectedUser ? (
        <Modal title="Detalhe da conta" onClose={() => setSelectedUser(null)}>
          <div className="wp-detail-grid">
            <div className="wp-detail-item">
              <p className="wp-detail-label">E-mail</p>
              <p className="wp-detail-value" title={selectedUser.user.email}>{selectedUser.user.email}</p>
            </div>
            <div className="wp-detail-item">
              <p className="wp-detail-label">Nome</p>
              <p className="wp-detail-value" title={selectedUser.user.display_name || selectedUser.user.full_name || '—'}>
                {selectedUser.user.display_name || selectedUser.user.full_name || '—'}
              </p>
            </div>
            <div className="wp-detail-item">
              <p className="wp-detail-label">Última atividade</p>
              <p className="wp-detail-value">{formatDt(selectedUser.user.last_seen_at)}</p>
            </div>
            <div className="wp-detail-item">
              <p className="wp-detail-label">Criado em</p>
              <p className="wp-detail-value">{formatDt(selectedUser.user.created_at)}</p>
            </div>
            <div className="wp-detail-item">
              <p className="wp-detail-label">Eventos 7d</p>
              <p className="wp-detail-value">{selectedUser.events_7d}</p>
            </div>
            <div className="wp-detail-item">
              <p className="wp-detail-label">Eventos 30d</p>
              <p className="wp-detail-value">{selectedUser.events_30d}</p>
            </div>
          </div>

          <div className="wp-detail-section">
            <p className="wp-detail-label">Tipos de evento (30d)</p>
            {Object.keys(selectedUser.event_types_30d).length > 0 ? (
              <div className="wp-kv-list">
                {Object.entries(selectedUser.event_types_30d).map(([k, v]) => (
                  <div key={k} className="wp-kv-row">
                    <span>{k}</span>
                    <strong>{v}</strong>
                  </div>
                ))}
              </div>
            ) : (
              <p className="wp-muted">Sem eventos no período.</p>
            )}
          </div>

          <div className="wp-detail-section">
            <p className="wp-detail-label">Últimos eventos (até 30)</p>
            {selectedUser.recent_events.length > 0 ? (
              <ul className="wp-event-list">
                {selectedUser.recent_events.map((ev, i) => (
                  <li key={`${ev.occurred_at}-${ev.event_type}-${i}`}>
                    <span className="wp-muted">{formatDt(ev.occurred_at)}</span>
                    {' · '}
                    <span>{ev.event_type}</span>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="wp-muted">Sem eventos registados.</p>
            )}
          </div>
        </Modal>
      ) : null}
      {selectedFamily ? (
        <Modal title="Detalhe da família" onClose={() => setSelectedFamily(null)}>
          <div className="wp-detail-grid">
            <div className="wp-detail-item">
              <p className="wp-detail-label">Nome</p>
              <p className="wp-detail-value">{selectedFamily.name}</p>
            </div>
            <div className="wp-detail-item">
              <p className="wp-detail-label">Membros</p>
              <p className="wp-detail-value">
                {selectedFamily.member_count} / {selectedFamily.max_members}
              </p>
            </div>
            <div className="wp-detail-item">
              <p className="wp-detail-label">Criado</p>
              <p className="wp-detail-value">{formatDt(selectedFamily.created_at)}</p>
            </div>
          </div>

          <div className="wp-detail-section">
            <p className="wp-detail-label">Membros</p>
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
              <p className="wp-muted">Sem membros.</p>
            )}
          </div>

          <div className="wp-detail-section">
            <p className="wp-detail-label">Convites (sem token — só metadados)</p>
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
                      const expired = !inv.used && new Date(inv.expires_at).getTime() < Date.now()
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
              <p className="wp-muted">Sem convites registados.</p>
            )}
          </div>
        </Modal>
      ) : null}
    </AdminShell>
  )
}
