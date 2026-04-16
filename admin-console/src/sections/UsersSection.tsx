import type { AdminUserRow } from '../api'
import { Dropdown, Pagination, SectionHeader } from '../components/admin-ui'

type TriState = 'all' | 'yes' | 'no'

type UsersSectionProps = {
  rows: AdminUserRow[]
  total: number
  busy: boolean
  q: string
  isActiveFilter: TriState
  isAdminFilter: TriState
  emailVerifiedFilter: TriState
  createdFrom: string
  createdTo: string
  orderBy: 'created_at' | 'last_seen_at' | 'email'
  orderDir: 'asc' | 'desc'
  onQChange: (v: string) => void
  onIsActiveFilterChange: (v: TriState) => void
  onIsAdminFilterChange: (v: TriState) => void
  onEmailVerifiedFilterChange: (v: TriState) => void
  onCreatedFromChange: (v: string) => void
  onCreatedToChange: (v: string) => void
  onOrderByChange: (v: 'created_at' | 'last_seen_at' | 'email') => void
  onOrderDirChange: (v: 'asc' | 'desc') => void
  onOpenUserDetail: (r: AdminUserRow) => void
  onToggleActive: (r: AdminUserRow) => void
  onToggleAdmin: (r: AdminUserRow) => void
  onRevokeSessions: (r: AdminUserRow) => void
  formatDt: (iso: string | null) => string
  page: number
  totalPages: number
  onPrevPage: () => void
  onNextPage: () => void
  disablePrevPage: boolean
  disableNextPage: boolean
  density: 'comfortable' | 'compact'
  onDensityChange: (v: 'comfortable' | 'compact') => void
  visibleColumns: {
    email: boolean
    name: boolean
    active: boolean
    admin: boolean
    verified: boolean
    lastSeen: boolean
    created: boolean
  }
  onToggleColumn: (k: keyof UsersSectionProps['visibleColumns']) => void
}

export function UsersSection(props: UsersSectionProps) {
  return (
    <section className="wp-card">
      <SectionHeader
        title="Contas"
        subtitle="Gestão de utilizadores, permissões e sessões"
        right={
          <Dropdown button={<button type="button" className="wp-btn wp-btn-ghost">Colunas</button>}>
            <div className="wp-columns-panel">
              <label className="wp-check">
                <input type="checkbox" checked={props.visibleColumns.email} onChange={() => props.onToggleColumn('email')} />
                <span>E-mail</span>
              </label>
              <label className="wp-check">
                <input type="checkbox" checked={props.visibleColumns.name} onChange={() => props.onToggleColumn('name')} />
                <span>Nome</span>
              </label>
              <label className="wp-check">
                <input type="checkbox" checked={props.visibleColumns.active} onChange={() => props.onToggleColumn('active')} />
                <span>Ativo</span>
              </label>
              <label className="wp-check">
                <input type="checkbox" checked={props.visibleColumns.admin} onChange={() => props.onToggleColumn('admin')} />
                <span>Admin</span>
              </label>
              <label className="wp-check">
                <input type="checkbox" checked={props.visibleColumns.verified} onChange={() => props.onToggleColumn('verified')} />
                <span>E-mail verificado</span>
              </label>
              <label className="wp-check">
                <input type="checkbox" checked={props.visibleColumns.lastSeen} onChange={() => props.onToggleColumn('lastSeen')} />
                <span>Última actividade</span>
              </label>
              <label className="wp-check">
                <input type="checkbox" checked={props.visibleColumns.created} onChange={() => props.onToggleColumn('created')} />
                <span>Criado</span>
              </label>
              <p className="wp-muted" style={{ fontSize: '0.8rem' }}>Ações fica sempre visível.</p>
            </div>
          </Dropdown>
        }
      />
      <div className="wp-filter-grid">
        <input className="wp-search" type="search" placeholder="Filtrar por e-mail…" value={props.q} onChange={(e) => props.onQChange(e.target.value)} />
        <select className="wp-search" value={props.isActiveFilter} onChange={(e) => props.onIsActiveFilterChange(e.target.value as TriState)}>
          <option value="all">Ativo: todos</option><option value="yes">Ativo: sim</option><option value="no">Ativo: não</option>
        </select>
        <select className="wp-search" value={props.isAdminFilter} onChange={(e) => props.onIsAdminFilterChange(e.target.value as TriState)}>
          <option value="all">Admin: todos</option><option value="yes">Admin: sim</option><option value="no">Admin: não</option>
        </select>
        <select className="wp-search" value={props.emailVerifiedFilter} onChange={(e) => props.onEmailVerifiedFilterChange(e.target.value as TriState)}>
          <option value="all">Verificado: todos</option><option value="yes">Verificado: sim</option><option value="no">Verificado: não</option>
        </select>
        <input className="wp-search" type="date" value={props.createdFrom} onChange={(e) => props.onCreatedFromChange(e.target.value)} />
        <input className="wp-search" type="date" value={props.createdTo} onChange={(e) => props.onCreatedToChange(e.target.value)} />
        <select className="wp-search" value={props.orderBy} onChange={(e) => props.onOrderByChange(e.target.value as 'created_at' | 'last_seen_at' | 'email')}>
          <option value="created_at">Ordem: criado</option><option value="last_seen_at">Ordem: última atividade</option><option value="email">Ordem: e-mail</option>
        </select>
        <select className="wp-search" value={props.orderDir} onChange={(e) => props.onOrderDirChange(e.target.value as 'asc' | 'desc')}>
          <option value="desc">Desc</option><option value="asc">Asc</option>
        </select>
        <select className="wp-search" value={props.density} onChange={(e) => props.onDensityChange(e.target.value as 'comfortable' | 'compact')}>
          <option value="comfortable">Densidade: confortável</option>
          <option value="compact">Densidade: compacta</option>
        </select>
      </div>
      <p className="wp-muted">{props.total} conta(s){props.busy ? ' · a atualizar…' : ''}</p>
      <div className="wp-table-wrap">
        <table className={props.density === 'compact' ? 'wp-table wp-table-sticky wp-table-compact' : 'wp-table wp-table-sticky'}>
          <thead>
            <tr>
              {props.visibleColumns.email ? <th>E-mail</th> : null}
              {props.visibleColumns.name ? <th>Nome</th> : null}
              {props.visibleColumns.active ? <th>Ativo</th> : null}
              {props.visibleColumns.admin ? <th>Admin</th> : null}
              {props.visibleColumns.verified ? <th>E-mail verificado</th> : null}
              {props.visibleColumns.lastSeen ? <th>Última actividade</th> : null}
              {props.visibleColumns.created ? <th>Criado</th> : null}
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            {props.rows.map((r) => (
              <tr key={r.id}>
                {props.visibleColumns.email ? <td>{r.email}</td> : null}
                {props.visibleColumns.name ? <td>{r.display_name || r.full_name || '—'}</td> : null}
                {props.visibleColumns.active ? (
                  <td>
                    {r.is_active ? <span className="wp-badge wp-badge-ok">Sim</span> : <span className="wp-badge wp-badge-no">Não</span>}
                  </td>
                ) : null}
                {props.visibleColumns.admin ? <td>{r.is_admin ? 'Sim' : '—'}</td> : null}
                {props.visibleColumns.verified ? <td>{props.formatDt(r.email_verified_at)}</td> : null}
                {props.visibleColumns.lastSeen ? <td>{props.formatDt(r.last_seen_at)}</td> : null}
                {props.visibleColumns.created ? <td>{props.formatDt(r.created_at)}</td> : null}
                <td>
                  <div className="wp-row-actions">
                    <button
                      type="button"
                      className="wp-btn wp-btn-ghost wp-btn-sm"
                      onClick={() => props.onOpenUserDetail(r)}
                      disabled={props.busy}
                    >
                      Detalhe
                    </button>
                    <Dropdown button={<button type="button" className="wp-btn wp-btn-ghost wp-btn-sm" disabled={props.busy}>Ações</button>}>
                      <div className="wp-menu">
                        <button type="button" className="wp-menu-item" onClick={() => props.onToggleActive(r)} disabled={props.busy}>
                          {r.is_active ? 'Desativar conta' : 'Reativar conta'}
                        </button>
                        <button type="button" className="wp-menu-item" onClick={() => props.onToggleAdmin(r)} disabled={props.busy}>
                          {r.is_admin ? 'Rebaixar admin' : 'Promover admin'}
                        </button>
                        <button type="button" className="wp-menu-item wp-menu-danger" onClick={() => props.onRevokeSessions(r)} disabled={props.busy}>
                          Revogar sessões
                        </button>
                      </div>
                    </Dropdown>
                  </div>
                </td>
              </tr>
            ))}
            {props.rows.length === 0 ? (
              <tr>
                <td colSpan={1 + Object.values(props.visibleColumns).filter(Boolean).length}>
                  <div className="wp-empty-state">
                    {props.busy ? 'A carregar contas...' : 'Sem resultados para os filtros aplicados.'}
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
