import type { AdminFinanceSummary } from '../api'
import { SectionHeader, StatCard } from '../components/admin-ui'

type FinanceSectionProps = {
  summary: AdminFinanceSummary | null
  busy: boolean
  formatEurFromCents: (c: number) => string
}

export function FinanceSection({ summary, busy, formatEurFromCents }: FinanceSectionProps) {
  return (
    <section className="wp-card">
      <SectionHeader title="Núcleo financeiro" subtitle="Agregados na base de dados (sem linhas individuais)" />
      {summary ? (
        <div className="wp-kpi-grid">
          <StatCard label="Despesas (total)" value={summary.expenses_total} />
          <StatCard label="Despesas ativas" value={summary.expenses_active} />
          <StatCard label="Despesas removidas (soft delete)" value={summary.expenses_deleted} />
          <StatCard label="Despesas partilhadas" value={summary.expenses_shared} />
          <StatCard label="Soma despesas (30d, ativas)" value={formatEurFromCents(summary.expenses_sum_cents_30d)} />
          <StatCard label="Receitas (linhas)" value={summary.incomes_total} />
          <StatCard label="Soma receitas (30d)" value={formatEurFromCents(summary.incomes_sum_cents_30d)} />
          <StatCard label="Objetivos" value={summary.goals_total} />
          <StatCard label="Contribuições para objetivos" value={summary.goal_contributions_total} />
          <StatCard label="Listas de compras" value={summary.shopping_lists_total} />
          <StatCard label="Linhas em listas" value={summary.shopping_list_items_total} />
          <StatCard label="Reservas de emergência" value={summary.emergency_reserves_total} />
          <StatCard label="Créditos mensais (reserva)" value={summary.emergency_reserve_accruals_total} />
          <StatCard label="Categorias de despesa" value={summary.categories_total} />
          <StatCard label="Categorias de receita" value={summary.income_categories_total} />
        </div>
      ) : <p className="wp-muted">{busy ? 'A carregar…' : 'Sem dados.'}</p>}
    </section>
  )
}
