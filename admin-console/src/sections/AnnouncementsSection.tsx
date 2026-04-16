import { useMemo, useState } from 'react'
import type { AnnouncementKind, AnnouncementPlacement, AnnouncementRow } from '../api'
import { Modal, Pagination, SectionHeader } from '../components/admin-ui'

type AnnouncementForm = {
  title: string
  body: string
  kind: AnnouncementKind
  placement: AnnouncementPlacement
  priority: number
  cta_label: string
  cta_url: string
  is_active: boolean
  starts_at: string
  ends_at: string
  /** Vazio = visível para todos os utilizadores. */
  target_user_email: string
}

type AnnouncementsSectionProps = {
  items: AnnouncementRow[]
  total: number
  busy: boolean
  statusFilter: 'all' | 'active' | 'inactive'
  placementFilter: 'all' | AnnouncementPlacement
  onStatusFilterChange: (v: 'all' | 'active' | 'inactive') => void
  onPlacementFilterChange: (v: 'all' | AnnouncementPlacement) => void
  onClearFilters: () => void
  onCreate: (payload: {
    title: string
    body: string
    kind: AnnouncementKind
    placement: AnnouncementPlacement
    priority: number
    cta_label: string
    cta_url: string
    is_active: boolean
    starts_at: string | null
    ends_at: string | null
    target_user_email: string
  }) => Promise<void>
  onUpdate: (
    id: string,
    payload: {
      title: string
      body: string
      kind: AnnouncementKind
      placement: AnnouncementPlacement
      priority: number
      cta_label: string
      cta_url: string
      is_active: boolean
      starts_at: string | null
      ends_at: string | null
      target_user_email: string
    },
  ) => Promise<void>
  formatDt: (iso: string | null) => string
  page: number
  totalPages: number
  onPrevPage: () => void
  onNextPage: () => void
  disablePrevPage: boolean
  disableNextPage: boolean
}

function emptyForm(): AnnouncementForm {
  return {
    title: '',
    body: '',
    kind: 'info',
    placement: 'home_banner',
    priority: 0,
    cta_label: '',
    cta_url: '',
    is_active: false,
    starts_at: '',
    ends_at: '',
    target_user_email: '',
  }
}

function toInputDateTime(iso: string | null): string {
  if (!iso) return ''
  try {
    const d = new Date(iso)
    return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 16)
  } catch {
    return ''
  }
}

function toIsoOrNull(localValue: string): string | null {
  if (!localValue.trim()) return null
  const dt = new Date(localValue)
  if (Number.isNaN(dt.getTime())) return null
  return dt.toISOString()
}

export function AnnouncementsSection(props: AnnouncementsSectionProps) {
  const [editing, setEditing] = useState<AnnouncementRow | null>(null)
  const [creating, setCreating] = useState(false)
  const [form, setForm] = useState<AnnouncementForm>(emptyForm)

  const modalTitle = useMemo(() => {
    if (creating) return 'Novo aviso'
    return 'Editar aviso'
  }, [creating])

  function closeModal() {
    setEditing(null)
    setCreating(false)
    setForm(emptyForm())
  }

  function openCreate() {
    setCreating(true)
    setEditing(null)
    setForm(emptyForm())
  }

  function openEdit(row: AnnouncementRow) {
    setCreating(false)
    setEditing(row)
    setForm({
      title: row.title,
      body: row.body,
      kind: row.kind,
      placement: row.placement,
      priority: row.priority,
      cta_label: row.cta_label ?? '',
      cta_url: row.cta_url ?? '',
      is_active: row.is_active,
      starts_at: toInputDateTime(row.starts_at),
      ends_at: toInputDateTime(row.ends_at),
      target_user_email: row.target_user_email ?? '',
    })
  }

  async function submitForm(ev: React.FormEvent) {
    ev.preventDefault()
    const payload = {
      title: form.title.trim(),
      body: form.body.trim(),
      kind: form.kind,
      placement: form.placement,
      priority: Number(form.priority) || 0,
      cta_label: form.cta_label.trim() || '',
      cta_url: form.cta_url.trim() || '',
      is_active: form.is_active,
      starts_at: toIsoOrNull(form.starts_at),
      ends_at: toIsoOrNull(form.ends_at),
      target_user_email: form.target_user_email.trim(),
    }
    if (creating) {
      await props.onCreate(payload)
      closeModal()
      return
    }
    if (editing) {
      await props.onUpdate(editing.id, payload)
      closeModal()
    }
  }

  return (
    <section className="wp-card">
      <SectionHeader
        title="Avisos e dicas"
        subtitle={`${props.total} conteúdo(s)${props.busy ? ' · a atualizar…' : ''}`}
        right={
          <button type="button" className="wp-btn" onClick={openCreate} disabled={props.busy}>
            Novo aviso
          </button>
        }
      />
      <div className="wp-filter-grid">
        <select className="wp-search" value={props.statusFilter} onChange={(e) => props.onStatusFilterChange(e.target.value as 'all' | 'active' | 'inactive')}>
          <option value="all">Estado: todos</option>
          <option value="active">Estado: ativos</option>
          <option value="inactive">Estado: inativos</option>
        </select>
        <select className="wp-search" value={props.placementFilter} onChange={(e) => props.onPlacementFilterChange(e.target.value as 'all' | AnnouncementPlacement)}>
          <option value="all">Local: todos</option>
          <option value="home_banner">Home banner</option>
          <option value="home_feed">Home feed</option>
          <option value="finance_tab">Aba finanças</option>
        </select>
        <button type="button" className="wp-btn wp-btn-ghost" onClick={props.onClearFilters} disabled={props.busy}>
          Limpar
        </button>
      </div>
      <div className="wp-table-wrap">
        <table className="wp-table wp-table-sticky">
          <thead>
            <tr>
              <th>Título</th>
              <th>Destinatário</th>
              <th>Tipo</th>
              <th>Local</th>
              <th>Estado</th>
              <th>Janela</th>
              <th>Criado</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            {props.items.map((row) => (
              <tr key={row.id}>
                <td>{row.title}</td>
                <td>{row.target_user_email?.trim() ? row.target_user_email : <span className="wp-muted">Todos</span>}</td>
                <td>{row.kind}</td>
                <td>{row.placement}</td>
                <td>{row.is_active ? <span className="wp-badge wp-badge-ok">Ativo</span> : <span className="wp-badge wp-badge-no">Inativo</span>}</td>
                <td>{props.formatDt(row.starts_at)} → {props.formatDt(row.ends_at)}</td>
                <td>{props.formatDt(row.created_at)}</td>
                <td>
                  <button type="button" className="wp-btn wp-btn-ghost wp-btn-sm" onClick={() => openEdit(row)} disabled={props.busy}>
                    Editar
                  </button>
                </td>
              </tr>
            ))}
            {props.items.length === 0 ? (
              <tr>
                <td colSpan={8}>
                  <div className="wp-empty-state">
                    {props.busy ? 'A carregar avisos...' : 'Sem conteúdos para os filtros aplicados.'}
                  </div>
                </td>
              </tr>
            ) : null}
          </tbody>
        </table>
      </div>
      {props.total > 30 ? (
        <Pagination
          page={props.page}
          totalPages={props.totalPages}
          onPrev={props.onPrevPage}
          onNext={props.onNextPage}
          disablePrev={props.disablePrevPage}
          disableNext={props.disableNextPage}
        />
      ) : null}

      {creating || editing ? (
        <Modal title={modalTitle} onClose={closeModal}>
          <form onSubmit={(ev) => void submitForm(ev)}>
            <div className="wp-field">
              <label htmlFor="ann-title">Título</label>
              <input id="ann-title" value={form.title} onChange={(e) => setForm((f) => ({ ...f, title: e.target.value }))} required maxLength={140} />
            </div>
            <div className="wp-field">
              <label htmlFor="ann-body">Conteúdo</label>
              <textarea id="ann-body" value={form.body} onChange={(e) => setForm((f) => ({ ...f, body: e.target.value }))} required rows={12} />
            </div>
            <div className="wp-field">
              <label htmlFor="ann-target-email">E-mail do destinatário (opcional)</label>
              <input
                id="ann-target-email"
                type="email"
                autoComplete="off"
                placeholder="Vazio = todos os utilizadores"
                value={form.target_user_email}
                onChange={(e) => setForm((f) => ({ ...f, target_user_email: e.target.value }))}
              />
              <p className="wp-muted" style={{ fontSize: '0.8rem', marginTop: 6 }}>
                Se preencher, só essa conta vê o recado (o e-mail tem de existir no sistema).
              </p>
            </div>
            <div className="wp-filter-grid">
              <select className="wp-search" value={form.kind} onChange={(e) => setForm((f) => ({ ...f, kind: e.target.value as AnnouncementKind }))}>
                <option value="info">Info</option>
                <option value="warning">Warning</option>
                <option value="tip">Tip</option>
                <option value="material">Material</option>
              </select>
              <select className="wp-search" value={form.placement} onChange={(e) => setForm((f) => ({ ...f, placement: e.target.value as AnnouncementPlacement }))}>
                <option value="home_banner">Home banner</option>
                <option value="home_feed">Home feed</option>
                <option value="finance_tab">Aba finanças</option>
              </select>
              <input className="wp-search" type="number" min={0} max={100} value={form.priority} onChange={(e) => setForm((f) => ({ ...f, priority: Number(e.target.value) }))} title="Prioridade" />
              <label className="wp-check">
                <input type="checkbox" checked={form.is_active} onChange={(e) => setForm((f) => ({ ...f, is_active: e.target.checked }))} />
                <span>Ativo</span>
              </label>
            </div>
            <div className="wp-filter-grid">
              <input className="wp-search" placeholder="CTA label (opcional)" value={form.cta_label} onChange={(e) => setForm((f) => ({ ...f, cta_label: e.target.value }))} />
              <input className="wp-search" placeholder="CTA URL (opcional)" value={form.cta_url} onChange={(e) => setForm((f) => ({ ...f, cta_url: e.target.value }))} />
              <input className="wp-search" type="datetime-local" value={form.starts_at} onChange={(e) => setForm((f) => ({ ...f, starts_at: e.target.value }))} title="Início" />
              <input className="wp-search" type="datetime-local" value={form.ends_at} onChange={(e) => setForm((f) => ({ ...f, ends_at: e.target.value }))} title="Fim" />
            </div>
            <div className="wp-toolbar" style={{ justifyContent: 'flex-end' }}>
              <button type="button" className="wp-btn wp-btn-ghost" onClick={closeModal} disabled={props.busy}>
                Cancelar
              </button>
              <button type="submit" className="wp-btn" disabled={props.busy}>
                Guardar
              </button>
            </div>
          </form>
        </Modal>
      ) : null}
    </section>
  )
}
