import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  ApiError,
  fetchCashflow,
  fetchMe,
  fetchOverview,
  logout,
  type DashboardCashflow,
  type DashboardOverview,
  type UserMe,
} from "../api";
import {
  formatBrlFromCents,
  formatDueDate,
  monthLabel,
  shortMonth,
} from "../format";
import { CashflowChart, DonutChart, Wordmark } from "../ui";

function shiftMonth(year: number, month: number, delta: number) {
  const d = new Date(year, month - 1 + delta, 1);
  return { year: d.getFullYear(), month: d.getMonth() + 1 };
}

export function DashboardPage() {
  const navigate = useNavigate();
  const now = new Date();
  const [period, setPeriod] = useState({
    year: now.getFullYear(),
    month: now.getMonth() + 1,
  });
  const [me, setMe] = useState<UserMe | null>(null);
  const [overview, setOverview] = useState<DashboardOverview | null>(null);
  const [cashflow, setCashflow] = useState<DashboardCashflow | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(true);

  useEffect(() => {
    let cancelled = false;
    setBusy(true);
    setError(null);
    void (async () => {
      try {
        const [user, ov, cf] = await Promise.all([
          fetchMe(),
          fetchOverview(period.year, period.month),
          fetchCashflow(),
        ]);
        if (cancelled) return;
        setMe(user);
        setOverview(ov);
        setCashflow(cf);
      } catch (err) {
        if (cancelled) return;
        if (err instanceof ApiError && err.status === 401) {
          navigate("/login", { replace: true });
          return;
        }
        setError(
          err instanceof ApiError ? err.message : "Não foi possível carregar o mês.",
        );
      } finally {
        if (!cancelled) setBusy(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [period.year, period.month, navigate]);

  async function onLogout() {
    await logout();
    navigate("/", { replace: true });
  }

  const greeting = me?.display_name || me?.full_name || me?.email || "olá";
  const pending = overview?.pending_preview?.length
    ? overview.pending_preview
    : (overview?.upcoming_due ?? []);

  return (
    <div className="min-h-dvh bg-cream">
      <header className="bg-gradient-to-b from-navy-deep to-navy">
        <div className="mx-auto flex max-w-5xl items-center justify-between px-5 py-4">
          <Wordmark light />
          <button
            type="button"
            className="text-sm text-cream/80 hover:text-gold"
            onClick={() => void onLogout()}
          >
            Sair
          </button>
        </div>
        <div className="mx-auto max-w-5xl px-5 pb-8 pt-2">
          <p className="text-cream/60 text-sm">Olá, {greeting}</p>
          <div className="mt-3 flex flex-wrap items-end justify-between gap-4">
            <div>
              <p className="text-[11px] uppercase tracking-[0.22em] text-gold/80">
                Saldo do mês
              </p>
              <p className="mt-1 font-serif text-4xl text-cream">
                {overview
                  ? formatBrlFromCents(overview.month_balance_cents)
                  : "—"}
              </p>
            </div>
            <div className="flex items-center gap-2 rounded-xl bg-black/20 p-1">
              <button
                type="button"
                className="px-3 py-2 text-cream"
                onClick={() => setPeriod((p) => shiftMonth(p.year, p.month, -1))}
              >
                ‹
              </button>
              <span className="min-w-36 text-center text-sm capitalize text-cream">
                {monthLabel(period.year, period.month)}
              </span>
              <button
                type="button"
                className="px-3 py-2 text-cream"
                onClick={() => setPeriod((p) => shiftMonth(p.year, p.month, 1))}
              >
                ›
              </button>
            </div>
          </div>
          {overview ? (
            <dl className="mt-6 grid grid-cols-2 gap-3 text-sm sm:grid-cols-3">
              <div className="rounded-xl bg-white/5 px-4 py-3">
                <dt className="text-cream/50">Receitas</dt>
                <dd className="mt-1 text-cream">
                  {formatBrlFromCents(overview.month_income_cents)}
                </dd>
              </div>
              <div className="rounded-xl bg-white/5 px-4 py-3">
                <dt className="text-cream/50">Despesas</dt>
                <dd className="mt-1 text-cream">
                  {formatBrlFromCents(overview.month_expense_total_cents)}
                </dd>
              </div>
              <div className="col-span-2 rounded-xl bg-white/5 px-4 py-3 sm:col-span-1">
                <dt className="text-cream/50">A pagar</dt>
                <dd className="mt-1 text-cream">
                  {formatBrlFromCents(overview.pending_total_cents)}
                </dd>
              </div>
            </dl>
          ) : null}
        </div>
      </header>

      <main className="mx-auto grid max-w-5xl gap-4 px-5 py-6 lg:grid-cols-2">
        {error ? (
          <p className="lg:col-span-2 rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800">
            {error}
          </p>
        ) : null}
        {busy && !overview ? (
          <p className="lg:col-span-2 py-12 text-center text-muted">A carregar…</p>
        ) : null}

        <section className="rounded-2xl border border-navy/8 bg-white/70 p-5">
          <h2 className="font-serif text-xl text-navy-deep">Categorias</h2>
          <div className="mt-4">
            <DonutChart slices={overview?.spending_by_category ?? []} />
          </div>
        </section>

        <section className="rounded-2xl border border-navy/8 bg-white/70 p-5">
          <h2 className="font-serif text-xl text-navy-deep">Fluxo</h2>
          <div className="mt-4">
            <CashflowChart
              labels={(cashflow?.months ?? []).map((m) =>
                shortMonth(m.year, m.month),
              )}
              income={cashflow?.income_cents ?? []}
              expense={cashflow?.expense_paid_cents ?? []}
            />
          </div>
        </section>

        <section className="rounded-2xl border border-navy/8 bg-white/70 p-5">
          <h2 className="font-serif text-xl text-navy-deep">A pagar</h2>
          {pending.length === 0 ? (
            <p className="mt-6 text-sm text-muted">Nada pendente neste recorte.</p>
          ) : (
            <ul className="mt-4 divide-y divide-navy/8">
              {pending.map((item) => (
                <li
                  key={item.id}
                  className="flex items-baseline justify-between gap-3 py-3 text-sm"
                >
                  <div className="min-w-0">
                    <p className="truncate text-navy">{item.description}</p>
                    <p className="text-xs text-muted">
                      {formatDueDate(item.due_date)}
                    </p>
                  </div>
                  <span className="shrink-0 font-medium text-navy-deep">
                    {formatBrlFromCents(item.amount_cents)}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </section>

        <section className="rounded-2xl border border-navy/8 bg-white/70 p-5">
          <h2 className="font-serif text-xl text-navy-deep">Metas</h2>
          {(overview?.goals_preview ?? []).length === 0 ? (
            <p className="mt-6 text-sm text-muted">
              Sem metas activas. Crie-as no aplicativo.
            </p>
          ) : (
            <ul className="mt-4 space-y-4">
              {overview?.goals_preview.map((goal) => {
                const pct = Math.min(
                  100,
                  Math.round((goal.current_cents / goal.target_cents) * 100),
                );
                return (
                  <li key={goal.id}>
                    <div className="flex justify-between text-sm">
                      <span className="text-navy">{goal.title}</span>
                      <span className="text-muted">{pct}%</span>
                    </div>
                    <div className="mt-2 h-2 overflow-hidden rounded-full bg-cream-muted">
                      <div
                        className="h-full rounded-full bg-gold"
                        style={{ width: `${pct}%` }}
                      />
                    </div>
                  </li>
                );
              })}
            </ul>
          )}
        </section>
      </main>
    </div>
  );
}
