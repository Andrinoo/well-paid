import type { AdminProductFunnel } from '../api'
import { SectionHeader, StatCard } from '../components/admin-ui'

type FunnelSectionProps = {
  metrics: AdminProductFunnel | null
  busy: boolean
  pctOf: (part: number, whole: number) => string
}

export function FunnelSection({ metrics, busy, pctOf }: FunnelSectionProps) {
  return (
    <section className="wp-card">
      <SectionHeader title="Funil de produto" subtitle="Contagens globais; percentagem face ao total de contas" />
      {metrics ? (
        <div className="wp-kpi-grid">
          <StatCard label="Contas registadas" value={metrics.users_total} />
          <StatCard label="E-mail verificado" value={metrics.email_verified_total} hint={pctOf(metrics.email_verified_total, metrics.users_total)} />
          <StatCard label="Utilizadores em família" value={metrics.users_with_family_total} hint={pctOf(metrics.users_with_family_total, metrics.users_total)} />
          <StatCard label="Com >=1 despesa ativa" value={metrics.users_with_expense_total} hint={pctOf(metrics.users_with_expense_total, metrics.users_total)} />
          <StatCard label="Com >=1 receita" value={metrics.users_with_income_total} hint={pctOf(metrics.users_with_income_total, metrics.users_total)} />
          <StatCard label="app_open distintos (7d)" value={metrics.users_app_open_7d} hint={pctOf(metrics.users_app_open_7d, metrics.users_total)} />
          <StatCard label="Novas contas (7d)" value={metrics.signups_7d} />
          <StatCard label="Novas contas (30d)" value={metrics.signups_30d} />
        </div>
      ) : <p className="wp-muted">{busy ? 'A carregar…' : 'Sem dados.'}</p>}
    </section>
  )
}
