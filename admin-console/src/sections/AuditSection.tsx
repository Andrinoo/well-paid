import type { AdminAuditEventRow } from '../api'
import { Dropdown, Pagination, SectionHeader } from '../components/admin-ui'

type AuditSectionProps = {
  items: AdminAuditEventRow[]
  total: number
  busy: boolean
  formatDt: (iso: string | null) => string
  formatAuditDetails: (d: Record<string, unknown> | null) => string
  filters: {
    actorEmail: string
    action: string
    from: string
    to: string
  }
  onFiltersChange: (next: AuditSectionProps['filters']) => void
  page: number
  totalPages: number
  onPrevPage: () => void
  onNextPage: () => void
  disablePrevPage: boolean
  disableNextPage: boolean
  density: 'comfortable' | 'compact'
  onDensityChange: (v: 'comfortable' | 'compact') => void
  visibleColumns: {
    when: boolean
    who: boolean
    action: boolean
    target: boolean
    details: boolean
  }
  onToggleColumn: (k: keyof AuditSectionProps['visibleColumns']) => void
}

export function AuditSection(props: AuditSectionProps) {
  return (
    <section className="wp-card">
      <SectionHeader
        title="Auditoria admin"
        subtitle={`${props.total} evento(s)${props.busy ? ' · a atualizar…' : ''}`}
        right={
          <div className="wp-toolbar">
            <Dropdown button={<button type="button" className="wp-btn wp-btn-ghost">Colunas</button>}>
              <div className="wp-columns-panel">
                <label className="wp-check">
                  <input type="checkbox" checked={props.visibleColumns.when} onChange={() => props.onToggleColumn('when')} />
                  <span>Quando</span>
                </label>
                <label className="wp-check">
                  <input type="checkbox" checked={props.visibleColumns.who} onChange={() => props.onToggleColumn('who')} />
                  <span>Quem</span>
                </label>
                <label className="wp-check">
                  <input type="checkbox" checked={props.visibleColumns.action} onChange={() => props.onToggleColumn('action')} />
                  <span>Ação</span>
                </label>
                <label className="wp-check">
                  <input type="checkbox" checked={props.visibleColumns.target} onChange={() => props.onToggleColumn('target')} />
                  <span>Alvo</span>
                </label>
                <label className="wp-check">
                  <input type="checkbox" checked={props.visibleColumns.details} onChange={() => props.onToggleColumn('details')} />
                  <span>Detalhes</span>
                </label>
              </div>
            </Dropdown>
            <select className="wp-search" value={props.density} onChange={(e) => props.onDensityChange(e.target.value as 'comfortable' | 'compact')}>
              <option value="comfortable">Densidade: confortável</option>
              <option value="compact">Densidade: compacta</option>
            </select>
          </div>
        }
      />
      <div className="wp-filter-grid">
        <input
          className="wp-search"
          type="search"
          placeholder="Filtrar por ator (e-mail)…"
          value={props.filters.actorEmail}
          onChange={(e) => props.onFiltersChange({ ...props.filters, actorEmail: e.target.value })}
        />
        <input
          className="wp-search"
          type="search"
          placeholder="Filtrar por ação…"
          value={props.filters.action}
          onChange={(e) => props.onFiltersChange({ ...props.filters, action: e.target.value })}
        />
        <input
          className="wp-search"
          type="date"
          value={props.filters.from}
          onChange={(e) => props.onFiltersChange({ ...props.filters, from: e.target.value })}
          title="De"
        />
        <input
          className="wp-search"
          type="date"
          value={props.filters.to}
          onChange={(e) => props.onFiltersChange({ ...props.filters, to: e.target.value })}
          title="Até"
        />
        <button
          type="button"
          className="wp-btn wp-btn-ghost"
          onClick={() => props.onFiltersChange({ actorEmail: '', action: '', from: '', to: '' })}
          disabled={props.busy}
        >
          Limpar
        </button>
      </div>
      <div className="wp-table-wrap">
        <table className={props.density === 'compact' ? 'wp-table wp-table-sticky wp-table-compact' : 'wp-table wp-table-sticky'}>
          <thead>
            <tr>
              {props.visibleColumns.when ? <th>Quando</th> : null}
              {props.visibleColumns.who ? <th>Quem</th> : null}
              {props.visibleColumns.action ? <th>Ação</th> : null}
              {props.visibleColumns.target ? <th>Alvo</th> : null}
              {props.visibleColumns.details ? <th>Detalhes</th> : null}
            </tr>
          </thead>
          <tbody>
            {props.items.map((ev) => (
              <tr key={ev.id}>
                {props.visibleColumns.when ? <td className="wp-nowrap">{props.formatDt(ev.created_at)}</td> : null}
                {props.visibleColumns.who ? <td>{ev.actor_email}</td> : null}
                {props.visibleColumns.action ? <td>{ev.action}</td> : null}
                {props.visibleColumns.target ? <td>{ev.target_email ?? '—'}</td> : null}
                {props.visibleColumns.details ? (
                  <td className="wp-audit-details" title={props.formatAuditDetails(ev.details)}>
                    {props.formatAuditDetails(ev.details)}
                  </td>
                ) : null}
              </tr>
            ))}
            {props.items.length === 0 ? (
              <tr>
                <td colSpan={Math.max(1, Object.values(props.visibleColumns).filter(Boolean).length)}>
                  <div className="wp-empty-state">
                    {props.busy ? 'A carregar auditoria...' : 'Sem eventos de auditoria no período atual.'}
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
    </section>
  )
}
