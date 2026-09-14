import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  ApiError,
  fetchCashflow,
  fetchHomeBanner,
  fetchMe,
  fetchOverview,
  type DashboardCashflow,
  type DashboardOverview,
  type GoalSummaryItem,
  type HomeBanner,
  type PendingExpenseItem,
} from "../api";
import {
  daysUntil,
  formatBrlFromCents,
  formatDueDate,
  greetingFirstName,
  monthLabel,
  shiftMonth,
} from "../format";
import { useToggleShellMenu } from "../shell";
import { MoneyCount } from "./home/count-up";
import { GoalThumb } from "./home/GoalThumb";
import { MonthOrbit, MonthTide, MonthWave } from "./home/stage";

export function DashboardPage() {
  const navigate = useNavigate();
  const toggleMenu = useToggleShellMenu();
  const now = new Date();
  const [period, setPeriod] = useState({
    year: now.getFullYear(),
    month: now.getMonth() + 1,
  });
  const [name, setName] = useState<string | null>(null);
  const [overview, setOverview] = useState<DashboardOverview | null>(null);
  const [cashflow, setCashflow] = useState<DashboardCashflow | null>(null);
  const [banner, setBanner] = useState<HomeBanner | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(true);

  useEffect(() => {
    let cancelled = false;
    setBusy(true);
    setError(null);
    void (async () => {
      try {
        const [user, ov, cf, recado] = await Promise.all([
          fetchMe(),
          fetchOverview(period.year, period.month),
          fetchCashflow({
            dynamic: true,
            forecastMonths: 3,
            year: period.year,
            month: period.month,
          }),
          fetchHomeBanner(),
        ]);
        if (cancelled) return;
        setName(greetingFirstName(user));
        setOverview(ov);
        setCashflow(cf);
        setBanner(recado);
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

  const pending = overview?.pending_preview?.length
    ? overview.pending_preview
    : (overview?.upcoming_due ?? []);
  const goals = overview?.goals_preview ?? [];
  const monthTitle =
    monthLabel(period.year, period.month).charAt(0).toLocaleUpperCase("pt-BR") +
    monthLabel(period.year, period.month).slice(1);
  const income = overview?.month_income_cents ?? 0;
  const spent = overview?.month_expense_total_cents ?? 0;
  const balance = overview?.month_balance_cents ?? 0;
  const story = monthStory(balance, pending, income, spent);
  const tight = balance < 0;

  return (
    <div className="relative min-h-full overflow-hidden bg-paper font-ui text-navy-deep">
      <div
        className={`pointer-events-none absolute -left-24 -top-24 h-[28rem] w-[28rem] rounded-full blur-3xl wp-float ${
          tight ? "bg-peach" : "bg-sky"
        }`}
      />
      <div className="pointer-events-none absolute -right-16 top-32 h-72 w-72 rounded-full bg-peach/80 blur-3xl wp-float-alt" />
      <div className="pointer-events-none absolute bottom-20 left-1/3 h-56 w-56 rounded-full bg-sun/20 blur-3xl wp-float" />

      <header className="relative flex flex-wrap items-end justify-between gap-4 px-5 pt-6 sm:px-8">
        <div className="min-w-0">
          <p className="text-[11px] font-semibold uppercase tracking-[0.28em] text-teal-deep">
            {name ? `${name}, o seu mês` : "O seu mês"}
          </p>
          <h1 className="mt-1 font-display text-4xl font-semibold tracking-tight text-navy-deep sm:text-5xl">
            {monthTitle}
          </h1>
        </div>
        <div className="flex items-center gap-2">
          <div className="flex items-center rounded-full border border-navy/10 bg-white/70 p-1 backdrop-blur">
            <button
              type="button"
              className="rounded-full px-3 py-2 text-navy/70 hover:bg-sage hover:text-navy"
              onClick={() => setPeriod((p) => shiftMonth(p.year, p.month, -1))}
              aria-label="Mês anterior"
            >
              ‹
            </button>
            <button
              type="button"
              className="rounded-full px-3 py-2 text-navy/70 hover:bg-sage hover:text-navy"
              onClick={() => setPeriod((p) => shiftMonth(p.year, p.month, 1))}
              aria-label="Mês seguinte"
            >
              ›
            </button>
          </div>
          <button
            type="button"
            className="rounded-full px-3 py-2 text-sm text-navy md:hidden"
            onClick={toggleMenu}
          >
            Menu
          </button>
        </div>
      </header>

      {error ? (
        <p className="relative mx-5 mt-4 rounded-2xl border border-red-200 bg-red-50 px-4 py-2 text-sm text-red-800 sm:mx-8">
          {error}
        </p>
      ) : null}
      {banner ? (
        <p className="relative mx-5 mt-4 rounded-full bg-white/70 px-4 py-2 text-sm text-navy/80 sm:mx-8">
          {banner.title}
        </p>
      ) : null}

      {busy && !overview ? (
        <div className="relative grid gap-6 px-5 py-10 lg:grid-cols-[1.15fr_0.85fr] sm:px-8">
          <p className="sr-only">A carregar {monthTitle}…</p>
          <div className="mx-auto aspect-square w-full max-w-[420px] overflow-hidden rounded-full bg-white/60">
            <div className="h-full w-full wp-shimmer" />
          </div>
          <div className="h-80 overflow-hidden rounded-[28px] bg-white/70">
            <div className="h-full w-full wp-shimmer" />
          </div>
        </div>
      ) : (
        <div className="relative grid items-start gap-10 px-5 pb-24 pt-4 lg:grid-cols-[minmax(0,1.15fr)_minmax(18rem,0.85fr)] lg:gap-12 sm:px-8 sm:pt-6">
          <section className="wp-rise min-w-0">
            <MonthOrbit
              spending={overview?.spending_by_category ?? []}
              balanceCents={balance}
              story={story}
            />
            <div className="mx-auto mt-2 max-w-lg">
              <MonthTide inCents={income} outCents={spent} />
              {cashflow ? <div className="mt-6"><MonthWave data={cashflow} /></div> : null}
            </div>
          </section>

          <aside className="wp-rise lg:sticky lg:top-6" style={{ ["--wp-delay" as string]: "120ms" }}>
            <ComingNext items={pending} totalCents={overview?.pending_total_cents ?? 0} />
            <GrowingNow goals={goals} />
            <p className="mt-6 text-sm leading-relaxed text-navy/60">
              Despesas, proventos e metas continuam nos sítios de sempre. Aqui o mês
              aparece inteiro — o que sobra, o que orbita e o que se aproxima.
            </p>
          </aside>
        </div>
      )}
    </div>
  );
}

function monthStory(
  balance: number,
  pending: PendingExpenseItem[],
  income: number,
  spent: number,
): string {
  const soon = pending.filter((item) => {
    const days = daysUntil(item.due_date);
    return days != null && days <= 5;
  }).length;
  if (income === 0 && spent === 0) {
    return "Ainda não há movimento neste mês. Quando entrar e sair dinheiro, este palco ganha vida.";
  }
  if (balance < 0) {
    return "A folga ficou negativa. Vale olhar o que ainda sai antes do fim do mês.";
  }
  if (soon > 0) {
    return soon === 1
      ? "Há uma conta nos próximos dias. A folga ainda está à vista."
      : `Há ${soon} contas nos próximos dias. A folga ainda está à vista.`;
  }
  return "Folga à vista. O mês está nas suas mãos.";
}

function ComingNext({
  items,
  totalCents,
}: {
  items: PendingExpenseItem[];
  totalCents: number;
}) {
  return (
    <section>
      <div className="flex items-end justify-between gap-3">
        <h2 className="font-display text-2xl font-semibold text-navy-deep">
          O que se aproxima
        </h2>
        <Link
          to="/app/despesas?filtro=pagar"
          className="text-sm font-medium text-teal-deep hover:underline"
        >
          A pagar
        </Link>
      </div>
      {items.length === 0 ? (
        <p className="mt-4 text-sm text-muted">Nada a vencer por agora.</p>
      ) : (
        <ol className="relative mt-5 space-y-0 border-l border-navy/10 pl-5">
          {items.slice(0, 5).map((item, i) => {
            const days = daysUntil(item.due_date);
            const urgent = days != null && days <= 3;
            return (
              <li
                key={item.id}
                className="wp-rise relative pb-5"
                style={{ ["--wp-delay" as string]: `${180 + i * 70}ms` }}
              >
                <span
                  className={`absolute -left-[26px] top-1.5 h-2.5 w-2.5 rounded-full ${
                    urgent ? "bg-red-600 wp-pulse-dot" : "bg-teal"
                  }`}
                />
                <p className={`text-xs ${urgent ? "font-semibold text-red-700" : "text-muted"}`}>
                  {formatDueDate(item.due_date)}
                  {days != null && days >= 0 ? ` · ${days === 0 ? "hoje" : `${days}d`}` : ""}
                </p>
                <p className="truncate text-navy">{item.description}</p>
                <p className="tabular-nums text-sm text-navy-deep">
                  {formatBrlFromCents(item.amount_cents)}
                </p>
              </li>
            );
          })}
        </ol>
      )}
      <p className="text-sm text-navy/70">
        Total a pagar{" "}
        <MoneyCount cents={totalCents} className="font-semibold tabular-nums text-navy-deep" />
      </p>
    </section>
  );
}

function GrowingNow({ goals }: { goals: GoalSummaryItem[] }) {
  return (
    <section className="mt-8">
      <div className="flex items-end justify-between gap-3">
        <h2 className="font-display text-2xl font-semibold text-navy-deep">A crescer</h2>
        <Link to="/app/metas" className="text-sm font-medium text-teal-deep hover:underline">
          Metas
        </Link>
      </div>
      {goals.length === 0 ? (
        <p className="mt-4 text-sm text-muted">Nenhuma meta activa neste momento.</p>
      ) : (
        <ul className="mt-5 space-y-4">
          {goals.slice(0, 3).map((goal, i) => {
            const pct = Math.min(
              100,
              Math.round((goal.current_cents / Math.max(1, goal.target_cents)) * 100),
            );
            return (
              <li
                key={goal.id}
                className="wp-rise flex items-center gap-4"
                style={{ ["--wp-delay" as string]: `${280 + i * 80}ms` }}
              >
                <GoalThumb url={goal.reference_thumbnail_url} className="h-14 w-14" />
                <div className="min-w-0 flex-1">
                  <p className="truncate font-medium text-navy">{goal.title}</p>
                  <p className="text-xs text-muted">
                    {formatBrlFromCents(goal.current_cents)} de{" "}
                    {formatBrlFromCents(goal.target_cents)}
                  </p>
                </div>
                <span className="shrink-0 text-sm font-semibold text-teal-deep">{pct}%</span>
              </li>
            );
          })}
        </ul>
      )}
    </section>
  );
}
