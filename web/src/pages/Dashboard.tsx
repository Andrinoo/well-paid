import { useEffect, useState, type ReactNode } from "react";
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
import { daysUntil, formatBrlFromCents, formatDueDate, greetingFirstName, monthLabel, shiftMonth } from "../format";
import { useToggleShellMenu } from "../shell";
import { GoalThumb } from "./home/GoalThumb";
import { MonthOrbit, MonthWave } from "./home/stage";

export function DashboardPage() {
  const navigate = useNavigate();
  const toggleMenu = useToggleShellMenu();
  const now = new Date();
  const [period, setPeriod] = useState({ year: now.getFullYear(), month: now.getMonth() + 1 });
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
          fetchCashflow({ dynamic: true, forecastMonths: 3, year: period.year, month: period.month }),
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
        setError(err instanceof ApiError ? err.message : "Não foi possível carregar o mês.");
      } finally {
        if (!cancelled) setBusy(false);
      }
    })();
    return () => { cancelled = true; };
  }, [period.year, period.month, navigate]);

  const pending = overview?.pending_preview?.length ? overview.pending_preview : (overview?.upcoming_due ?? []);
  const goals = overview?.goals_preview ?? [];
  const monthTitle = capitalize(monthLabel(period.year, period.month));
  const income = overview?.month_income_cents ?? 0;
  const spent = overview?.month_expense_total_cents ?? 0;
  const balance = overview?.month_balance_cents ?? 0;
  const goalsSaved = goals.reduce((sum, goal) => sum + goal.current_cents, 0);
  const tight = balance < 0;

  return (
    <div className="relative min-h-full overflow-hidden bg-paper font-ui text-navy-deep xl:flex xl:h-full xl:min-h-0 xl:flex-col">
      <div className={`pointer-events-none absolute -left-24 -top-24 h-[28rem] w-[28rem] rounded-full blur-3xl wp-float ${tight ? "bg-peach" : "bg-sky"}`} />
      <div className="pointer-events-none absolute -right-16 top-32 h-72 w-72 rounded-full bg-peach/80 blur-3xl wp-float-alt" />

      <header className="relative shrink-0 border-b border-navy/8 bg-white/45 px-5 py-4 backdrop-blur-xl sm:px-8">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div className="min-w-0">
            <p className="text-[11px] font-bold uppercase tracking-[0.28em] text-teal-deep">{name ? `${name}, sua visão financeira` : "Sua visão financeira"}</p>
            <div className="mt-1 flex flex-wrap items-baseline gap-x-4 gap-y-1">
              <h1 className="font-display text-3xl font-semibold tracking-tight text-navy-deep sm:text-4xl">{monthTitle}</h1>
              <p className={`text-sm font-semibold ${tight ? "text-expense-line" : "text-teal-deep"}`}>{tight ? "Mês pede atenção" : "Finanças sob controle"}</p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <div className="flex items-center rounded-2xl border border-navy/10 bg-white/80 p-1 shadow-sm">
              <button type="button" className="grid h-9 w-9 place-items-center rounded-xl text-xl text-navy/70 transition hover:bg-sage hover:text-navy" onClick={() => setPeriod((p) => shiftMonth(p.year, p.month, -1))} aria-label="Mês anterior">‹</button>
              <span className="min-w-28 px-2 text-center text-xs font-bold uppercase tracking-wide text-navy">{monthTitle}</span>
              <button type="button" className="grid h-9 w-9 place-items-center rounded-xl text-xl text-navy/70 transition hover:bg-sage hover:text-navy" onClick={() => setPeriod((p) => shiftMonth(p.year, p.month, 1))} aria-label="Mês seguinte">›</button>
            </div>
            <button type="button" className="rounded-xl px-3 py-2 text-sm text-navy md:hidden" onClick={toggleMenu}>Menu</button>
          </div>
        </div>
        {banner ? <p className="mt-3 max-w-3xl text-sm text-navy/65">{banner.title}</p> : null}
      </header>

      {error ? <p className="relative mx-5 mt-4 rounded-2xl border border-red-200 bg-red-50 px-4 py-2 text-sm text-red-800 sm:mx-8">{error}</p> : null}

      {busy && !overview ? (
        <div className="relative grid gap-4 px-5 py-5 xl:grid-cols-2 sm:px-8">
          <p className="sr-only">Carregando {monthTitle}…</p>
          {[0, 1, 2, 3].map((item) => <div key={item} className="h-[360px] overflow-hidden rounded-3xl bg-white/70"><div className="h-full w-full wp-shimmer" /></div>)}
        </div>
      ) : (
        <main className="relative grid gap-3 px-5 pb-24 pt-4 xl:min-h-0 xl:flex-1 xl:grid-cols-2 xl:grid-rows-2 xl:overflow-hidden xl:pb-4 sm:px-8">
          <DashboardPanel title="Despesas por categoria" subtitle="Onde seu dinheiro foi usado neste mês" value={formatBrlFromCents(spent)} valueLabel="total lançado" tone="expense">
            <MonthOrbit spending={overview?.spending_by_category ?? []} balanceCents={balance} story={monthStory(balance, pending, income, spent)} year={period.year} month={period.month} />
          </DashboardPanel>

          <DashboardPanel title="Fluxo financeiro" subtitle="Histórico real e despesas previstas" value={formatBrlFromCents(balance)} valueLabel="saldo do mês" tone={tight ? "expense" : "teal"} delay="70ms" action={<span className="text-[11px] font-semibold text-muted"><b className="text-teal-deep">Entradas {formatBrlFromCents(income)}</b><span className="mx-2 text-navy/20">•</span><b className="text-expense-line">Despesas {formatBrlFromCents(spent)}</b></span>}>
            {cashflow ? <MonthWave data={cashflow} /> : <EmptyState>Sem histórico suficiente para o gráfico.</EmptyState>}
          </DashboardPanel>

          <DashboardPanel title="Próximos pagamentos" subtitle="Compromissos que precisam de atenção" value={formatBrlFromCents(overview?.pending_total_cents ?? 0)} valueLabel={`${pending.length} próximos`} tone="gold" delay="140ms" action={<Link to="/app/despesas?filtro=pagar" className="text-xs font-bold text-teal-deep hover:underline">Ver despesas →</Link>}>
            <ComingNext items={pending} />
          </DashboardPanel>

          <DashboardPanel title="Metas em andamento" subtitle="Acompanhe o que está crescendo" value={formatBrlFromCents(goalsSaved)} valueLabel={`${goals.length} metas ativas`} tone="navy" delay="210ms" action={<Link to="/app/metas" className="text-xs font-bold text-teal-deep hover:underline">Ver metas →</Link>}>
            <GrowingNow goals={goals} />
          </DashboardPanel>
        </main>
      )}
    </div>
  );
}

function DashboardPanel({ title, subtitle, value, valueLabel, tone, children, action, delay = "0ms" }: { title: string; subtitle: string; value: string; valueLabel: string; tone: "teal" | "expense" | "gold" | "navy"; children: ReactNode; action?: ReactNode; delay?: string }) {
  const accent = tone === "teal" ? "bg-teal" : tone === "expense" ? "bg-expense-line" : tone === "gold" ? "bg-gold" : "bg-navy";
  const valueTone = tone === "expense" ? "text-expense-line" : tone === "teal" ? "text-teal-deep" : "text-navy-deep";
  return (
    <section className="wp-rise flex min-h-[360px] min-w-0 flex-col overflow-hidden rounded-3xl border border-navy/8 bg-white/85 shadow-[0_16px_48px_rgba(20,28,42,0.07)] backdrop-blur xl:h-full xl:min-h-0" style={{ ["--wp-delay" as string]: delay }}>
      <div className={`h-1 w-full ${accent}`} />
      <header className="flex flex-wrap items-start justify-between gap-3 border-b border-navy/8 px-5 py-3">
        <div className="min-w-0"><h2 className="font-display text-xl font-semibold text-navy-deep">{title}</h2><div className="mt-0.5 flex flex-wrap items-center gap-x-3 gap-y-1"><p className="text-xs text-muted">{subtitle}</p>{action}</div></div>
        <div className="text-right"><p className={`font-display text-xl font-semibold tabular-nums ${valueTone}`}>{value}</p><p className="text-[10px] font-bold uppercase tracking-[0.14em] text-muted">{valueLabel}</p></div>
      </header>
      <div className="min-h-0 flex-1 overflow-auto p-4">{children}</div>
    </section>
  );
}

function monthStory(balance: number, pending: PendingExpenseItem[], income: number, spent: number): string {
  const soon = pending.filter((item) => { const days = daysUntil(item.due_date); return days != null && days <= 5; }).length;
  if (income === 0 && spent === 0) return "Ainda não há movimento neste mês.";
  if (balance < 0) return "A folga ficou negativa. Revise as próximas saídas.";
  if (soon > 0) return `${soon} ${soon === 1 ? "conta vence" : "contas vencem"} nos próximos dias.`;
  return "Folga positiva para conduzir o restante do mês.";
}

function ComingNext({ items }: { items: PendingExpenseItem[] }) {
  if (items.length === 0) return <EmptyState>Nada a vencer por agora.</EmptyState>;
  return (
    <ol className="divide-y divide-navy/8">
      {items.slice(0, 5).map((item, index) => {
        const days = daysUntil(item.due_date);
        const urgent = days != null && days <= 3;
        return <li key={item.id} className="expense-list-row grid grid-cols-[auto_minmax(0,1fr)_auto] items-center gap-3 py-3" style={{ ["--expense-delay" as string]: `${index * 55}ms` }}><span className={`grid h-10 w-10 place-items-center rounded-xl text-xs font-bold ${urgent ? "bg-red-100 text-red-700" : "bg-sage text-teal-deep"}`}>{days == null ? "—" : days === 0 ? "Hoje" : `${days}d`}</span><div className="min-w-0"><p className="truncate text-sm font-semibold text-navy">{item.description}</p><p className="text-xs text-muted">{formatDueDate(item.due_date)}</p></div><p className="text-sm font-bold tabular-nums text-navy-deep">{formatBrlFromCents(item.amount_cents)}</p></li>;
      })}
    </ol>
  );
}

function GrowingNow({ goals }: { goals: GoalSummaryItem[] }) {
  if (goals.length === 0) return <EmptyState>Nenhuma meta ativa neste momento.</EmptyState>;
  return (
    <ul className="divide-y divide-navy/8">
      {goals.slice(0, 4).map((goal, index) => {
        const pct = Math.min(100, Math.round((goal.current_cents / Math.max(1, goal.target_cents)) * 100));
        return <li key={goal.id} className="expense-list-row flex items-center gap-3 py-3" style={{ ["--expense-delay" as string]: `${index * 55}ms` }}><GoalThumb url={goal.reference_thumbnail_url} className="h-11 w-11" /><div className="min-w-0 flex-1"><div className="flex justify-between gap-3"><p className="truncate text-sm font-semibold text-navy">{goal.title}</p><span className="text-xs font-bold text-teal-deep">{pct}%</span></div><div className="mt-2 h-1.5 overflow-hidden rounded-full bg-cream-muted"><div className="h-full rounded-full bg-teal transition-[width] duration-700" style={{ width: `${pct}%` }} /></div><p className="mt-1 text-[11px] text-muted">{formatBrlFromCents(goal.current_cents)} de {formatBrlFromCents(goal.target_cents)}</p></div></li>;
      })}
    </ul>
  );
}

function EmptyState({ children }: { children: ReactNode }) { return <div className="grid min-h-36 place-items-center rounded-2xl border border-dashed border-navy/12 bg-cream/30 p-6 text-center text-sm text-muted">{children}</div>; }
function capitalize(value: string) { return value.charAt(0).toLocaleUpperCase("pt-BR") + value.slice(1); }
