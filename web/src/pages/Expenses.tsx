import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import {
  deleteExpense,
  fetchCategories,
  fetchExpenses,
  payExpense,
  type Category,
  type Expense,
} from "../api";
import { formatBrlFromCents, formatDueDate } from "../format";
import { ErrorNote, MonthBar, PageTitle, usePeriod } from "./common";
import { ExpenseCreateForm } from "./ExpenseForm";

export function ExpensesPage() {
  const [params] = useSearchParams();
  const pendingOnly = params.get("filtro") === "pagar";
  const [period, setPeriod] = usePeriod();
  const [rows, setRows] = useState<Expense[]>([]);
  const [cats, setCats] = useState<Category[]>([]);
  const [error, setError] = useState<string | null>(null);

  async function load() {
    setError(null);
    try {
      const [list, categories] = await Promise.all([
        fetchExpenses(period.year, period.month),
        fetchCategories(),
      ]);
      setRows(list);
      setCats(categories);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Falha ao carregar.");
    }
  }

  useEffect(() => {
    void load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [period.year, period.month]);

  const visible = pendingOnly ? rows.filter((row) => row.status !== "paid") : rows;

  return (
    <div className="mx-auto max-w-7xl space-y-5">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <PageTitle kicker="Módulo" title={pendingOnly ? "A pagar" : "Despesas"} />
        <MonthBar year={period.year} month={period.month} onChange={setPeriod} />
      </div>
      {pendingOnly ? (
        <p className="text-sm text-muted">
          Só contas pendentes.{" "}
          <Link to="/app/despesas" className="text-gold-pressed">
            Ver todas
          </Link>
        </p>
      ) : null}
      <ErrorNote message={error} />
      <div className={pendingOnly ? "" : "grid items-start gap-6 xl:grid-cols-[minmax(0,1.18fr)_minmax(360px,0.82fr)]"}>
        {!pendingOnly ? (
          <div className="min-w-0">
            <ExpenseCreateForm categories={cats} onCreated={load} />
          </div>
        ) : null}
        <section className="min-w-0 overflow-hidden rounded-3xl border border-navy/8 bg-white shadow-[0_12px_38px_rgba(20,28,42,0.05)] xl:sticky xl:top-6">
          <div className="flex items-center justify-between border-b border-navy/8 px-4 py-3.5 sm:px-5">
            <div>
              <h2 className="font-serif text-xl text-navy-deep">
                {pendingOnly ? "Contas pendentes" : "Lançamentos do mês"}
              </h2>
              <p className="mt-0.5 text-xs text-muted">
                {visible.length} {visible.length === 1 ? "despesa" : "despesas"}
              </p>
            </div>
            <span className="rounded-full bg-cream px-3 py-1 text-xs font-semibold text-navy">
              {formatBrlFromCents(visible.reduce((sum, row) => sum + row.amount_cents, 0))}
            </span>
          </div>
          <ul className="divide-y divide-navy/8 xl:max-h-[calc(100vh-15rem)] xl:overflow-y-auto">
            {visible.length === 0 ? (
              <li className="px-4 py-12 text-center text-sm text-muted">
                {pendingOnly ? "Nada a pagar neste mês." : "Sem despesas neste mês."}
              </li>
            ) : (
              visible.map((row) => (
                <li key={row.id} className="px-4 py-4 text-sm transition hover:bg-cream/30 sm:px-5">
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <p className="truncate font-medium text-navy">{row.description}</p>
                      <p className="mt-1 text-xs leading-5 text-muted">
                        {row.category_name}
                        {row.due_date ? ` · vence ${formatDueDate(row.due_date)}` : ` · ${formatDueDate(row.expense_date)}`}
                        {row.installment_total && row.installment_total > 1
                          ? ` · parcela ${row.installment_number ?? 1}/${row.installment_total}`
                          : ""}
                        {row.recurring_frequency ? ` · ${freqLabel(row.recurring_frequency)}` : ""}
                      </p>
                    </div>
                    <div className="shrink-0 text-right">
                      <p className="font-semibold tabular-nums text-navy-deep">{formatBrlFromCents(row.amount_cents)}</p>
                      <span className={`mt-1 inline-block rounded-full px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide ${row.status === "paid" ? "bg-sage text-teal-deep" : "bg-gold/15 text-navy"}`}>
                        {row.status === "paid" ? "Paga" : "Pendente"}
                      </span>
                    </div>
                  </div>
                  <div className="mt-3 flex justify-end gap-2">
                    {row.status !== "paid" ? (
                      <button
                        type="button"
                        className="rounded-lg bg-navy-deep px-3 py-1.5 text-xs font-semibold text-cream transition hover:-translate-y-0.5 hover:bg-navy"
                        onClick={() => void payExpense(row.id).then(load).catch((err) => setError(err instanceof Error ? err.message : "Falha ao pagar."))}
                      >
                        Marcar como paga
                      </button>
                    ) : null}
                    <button
                      type="button"
                      className="rounded-lg border border-red-100 px-3 py-1.5 text-xs font-semibold text-red-700 transition hover:bg-red-50"
                      onClick={() => void deleteExpense(row.id).then(load).catch((err) => setError(err instanceof Error ? err.message : "Falha ao apagar."))}
                    >
                      Apagar
                    </button>
                  </div>
                </li>
              ))
            )}
          </ul>
        </section>
      </div>
    </div>
  );
}

function freqLabel(freq: string): string {
  if (freq === "weekly") return "recorrente semanal";
  if (freq === "yearly") return "recorrente anual";
  return "recorrente mensal";
}
