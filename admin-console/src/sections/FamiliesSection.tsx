import type { AdminFamilyRow } from '../api'
import { Dropdown, Pagination, SectionHeader } from '../components/admin-ui'

type FamiliesSectionProps = {
  rows: AdminFamilyRow[]
  total: number
  busy: boolean
  q: string
  orderBy: 'created_at' | 'name'
  orderDir: 'asc' | 'desc'
  onQChange: (v: string) => void
  onOrderByChange: (v: 'created_at' | 'name') => void
  onOrderDirChange: (v: 'asc' | 'desc') => void
  onOpenDetail: (r: AdminFamilyRow) => void
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
    name: boolean
    members: boolean
    created: boolean
  }
  onToggleColumn: (k: keyof FamiliesSectionProps['visibleColumns']) => void
}

export function FamiliesSection(props: FamiliesSectionProps) {
  return (
    <section className="wp-card">
      <SectionHeader
        title="Famílias"
        subtitle="Visão consolidada de grupos e membros"
        right={
          <Dropdown button={<button type="button" className="wp-btn wp-btn-ghost">Colunas</button>}>
            <div className="wp-columns-panel">
              <label className="wp-check">
                <input type="checkbox" checked={props.visibleColumns.name} onChange={() => props.onToggleColumn('name')} />
                <span>Nome</span>
              </label>
              <label className="wp-check">
                <input type="checkbox" checked={props.visibleColumns.members} onChange={() => props.onToggleColumn('members')} />
                <span>Membros</span>
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
      <div className="wp-filter-grid wp-filter-grid-compact">
        <input className="wp-search" type="search" placeholder="Filtrar por nome…" value={props.q} onChange={(e) => props.onQChange(e.target.value)} />
        <select className="wp-search" value={props.orderBy} onChange={(e) => props.onOrderByChange(e.target.value as 'created_at' | 'name')}>
          <option value="created_at">Ordem: criado</option><option value="name">Ordem: nome</option>
        </select>
        <select className="wp-search" value={props.orderDir} onChange={(e) => props.onOrderDirChange(e.target.value as 'asc' | 'desc')}>
          <option value="desc">Desc</option><option value="asc">Asc</option>
        </select>
        <select className="wp-search" value={props.density} onChange={(e) => props.onDensityChange(e.target.value as 'comfortable' | 'compact')}>
          <option value="comfortable">Densidade: confortável</option>
          <option value="compact">Densidade: compacta</option>
        </select>
      </div>
      <p className="wp-muted">{props.total} família(s){props.busy ? ' · a atualizar…' : ''}</p>
      <div className="wp-table-wrap">
        <table className={props.density === 'compact' ? 'wp-table wp-table-sticky wp-table-compact' : 'wp-table wp-table-sticky'}>
          <thead>
            <tr>
              {props.visibleColumns.name ? <th>Nome</th> : null}
              {props.visibleColumns.members ? <th>Membros</th> : null}
              {props.visibleColumns.created ? <th>Criado</th> : null}
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            {props.rows.map((r) => (
              <tr key={r.id}>
                {props.visibleColumns.name ? <td>{r.name}</td> : null}
                {props.visibleColumns.members ? <td>{r.member_count}</td> : null}
                {props.visibleColumns.created ? <td>{props.formatDt(r.created_at)}</td> : null}
                <td><button type="button" className="wp-btn wp-btn-ghost wp-btn-sm" onClick={() => props.onOpenDetail(r)} disabled={props.busy}>Detalhe</button></td>
              </tr>
            ))}
            {props.rows.length === 0 ? (
              <tr>
                <td colSpan={1 + Object.values(props.visibleColumns).filter(Boolean).length}>
                  <div className="wp-empty-state">
                    {props.busy ? 'A carregar famílias...' : 'Sem famílias para os filtros aplicados.'}
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
