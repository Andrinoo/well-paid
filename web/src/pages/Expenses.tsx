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
    <div className="mx-auto max-w-5xl space-y-5">
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
      {!pendingOnly ? <ExpenseCreateForm categories={cats} onCreated={load} /> : null}
      <ul className="divide-y divide-navy/8 overflow-hidden rounded-2xl border border-navy/8 bg-white">
        {visible.length === 0 ? (
          <li className="px-4 py-8 text-center text-sm text-muted">
            {pendingOnly ? "Nada a pagar neste mês." : "Sem despesas neste mês."}
          </li>
        ) : (
          visible.map((row) => (
            <li
              key={row.id}
              className="flex flex-wrap items-center justify-between gap-3 px-4 py-3 text-sm"
            >
              <div>
                <p className="text-navy">{row.description}</p>
                <p className="text-xs text-muted">
                  {row.category_name}
                  {row.due_date ? ` · vence ${formatDueDate(row.due_date)}` : ` · ${formatDueDate(row.expense_date)}`}
                  {row.installment_total && row.installment_total > 1
                    ? ` · parcela ${row.installment_number ?? 1}/${row.installment_total}`
                    : ""}
                  {row.recurring_frequency ? ` · ${freqLabel(row.recurring_frequency)}` : ""}
                  {` · ${row.status === "paid" ? "Paga" : "Pendente"}`}
                </p>
              </div>
              <div className="flex items-center gap-2">
                <span className="font-medium">{formatBrlFromCents(row.amount_cents)}</span>
                {row.status !== "paid" ? (
                  <button
                    type="button"
                    className="rounded-lg bg-navy-deep px-2 py-1 text-xs text-cream"
                    onClick={() =>
                      void payExpense(row.id)
                        .then(load)
                        .catch((err) =>
                          setError(err instanceof Error ? err.message : "Falha ao pagar."),
                        )
                    }
                  >
                    Pagar
                  </button>
                ) : null}
                <button
                  type="button"
                  className="text-xs text-muted hover:text-red-700"
                  onClick={() =>
                    void deleteExpense(row.id)
                      .then(load)
                      .catch((err) =>
                        setError(err instanceof Error ? err.message : "Falha ao apagar."),
                      )
                  }
                >
                  Apagar
                </button>
              </div>
            </li>
          ))
        )}
      </ul>
    </div>
  );
}

function freqLabel(freq: string): string {
  if (freq === "weekly") return "recorrente semanal";
  if (freq === "yearly") return "recorrente anual";
  return "recorrente mensal";
}
