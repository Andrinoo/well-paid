import { useEffect, useState, type ReactNode } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  ApiError,
  fetchDashboardSnapshot,
  fetchHomeBanner,
  fetchMe,
  type DashboardAttentionItem,
  type DashboardChange,
  type DashboardSnapshot,
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
  const [snapshot, setSnapshot] = useState<DashboardSnapshot | null>(null);
  const [banner, setBanner] = useState<HomeBanner | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(true);

  useEffect(() => {
    let cancelled = false;
    setBusy(true);
    setError(null);
    void (async () => {
      try {
        const [user, financialSnapshot, recado] = await Promise.all([
          fetchMe(),
          fetchDashboardSnapshot(period.year, period.month),
          fetchHomeBanner(),
        ]);
        if (cancelled) return;
        setName(greetingFirstName(user));
        setSnapshot(financialSnapshot);
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

  const overview = snapshot?.overview ?? null;
  const cashflow = snapshot?.cashflow ?? null;
  const pending = overview?.pending_preview?.length ? overview.pending_preview : (overview?.upcoming_due ?? []);
  const goals = overview?.goals_preview ?? [];
  const monthTitle = capitalize(monthLabel(period.year, period.month));
  const income = overview?.month_income_cents ?? 0;
  const spent = overview?.month_expense_total_cents ?? 0;
  const balance = overview?.month_balance_cents ?? 0;
  const goalsSaved = goals.reduce((sum, goal) => sum + goal.current_cents, 0);
  const tight = balance < 0;

  return (
    <div className="relative min-h-full overflow-hidden bg-paper font-ui text-navy-deep">
      <div className={`pointer-events-none absolute -left-24 -top-24 h-[28rem] w-[28rem] rounded-full blur-3xl wp-float ${tight ? "bg-peach" : "bg-sky"}`} />
      <div className="pointer-events-none absolute -right-16 top-32 h-72 w-72 rounded-full bg-peach/80 blur-3xl wp-float-alt" />

      <header className="relative shrink-0 border-b border-navy/8 bg-white/45 px-5 py-4 backdrop-blur-xl sm:px-8">
        <div className="mx-auto flex max-w-[1500px] flex-wrap items-center justify-between gap-4">
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
        {banner ? <p className="mx-auto mt-3 max-w-[1500px] text-sm text-navy/65">{banner.title}</p> : null}
      </header>

      {error ? <p className="relative mx-5 mt-4 rounded-2xl border border-red-200 bg-red-50 px-4 py-2 text-sm text-red-800 sm:mx-8">{error}</p> : null}

      {busy && !overview ? (
        <div className="relative grid gap-4 px-5 py-5 xl:grid-cols-2 sm:px-8">
          <p className="sr-only">Carregando {monthTitle}…</p>
          {[0, 1, 2, 3].map((item) => <div key={item} className="h-[360px] overflow-hidden rounded-3xl bg-white/70"><div className="h-full w-full wp-shimmer" /></div>)}
        </div>
      ) : (
        <main className="relative mx-auto max-w-[1500px] space-y-3 px-5 pb-24 pt-4 sm:px-8 2xl:px-6">
          <section className="grid grid-cols-2 gap-px overflow-hidden rounded-2xl border border-navy/8 bg-navy/10 shadow-sm xl:grid-cols-4">
            <SummaryCard label="Receitas" value={income} change={snapshot?.income_change} tone="teal" />
            <SummaryCard label="Despesas" value={spent} change={snapshot?.expense_change} tone="expense" invertTrend />
            <SummaryCard label="Resultado" value={balance} change={snapshot?.balance_change} tone={tight ? "expense" : "teal"} />
            <SummaryCard label="Reserva" value={overview?.emergency_reserve_balance_cents ?? 0} note="Patrimônio protegido" tone="navy" />
          </section>

          <section className="grid overflow-hidden rounded-2xl border border-navy/8 bg-white/80 shadow-sm backdrop-blur lg:grid-cols-[minmax(0,1.2fr)_minmax(430px,.8fr)]">
            <AttentionCenter items={snapshot?.attention ?? []} />
            <QuickActions />
          </section>

          <section className="grid gap-3 xl:grid-cols-2">
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
          </section>
        </main>
      )}
    </div>
  );
}

function SummaryCard({ label, value, change, note, tone, invertTrend = false }: { label: string; value: number; change?: DashboardChange; note?: string; tone: "teal" | "expense" | "navy"; invertTrend?: boolean }) {
  const accent = tone === "expense" ? "bg-expense-line" : tone === "teal" ? "bg-teal" : "bg-navy";
  const hasComparison = change?.delta_percent != null;
  const improvement = change ? (invertTrend ? change.delta_cents <= 0 : change.delta_cents >= 0) : true;
  const comparison = hasComparison
    ? `${change!.delta_cents >= 0 ? "↑" : "↓"} ${Math.abs(change!.delta_percent!)}% vs. mês anterior`
    : note ?? "Primeiro mês para comparação";
  return (
    <article className="relative min-w-0 overflow-hidden bg-white/85 px-4 py-3">
      <span className={`absolute inset-y-0 left-0 w-1 ${accent}`} />
      <p className="text-[10px] font-bold uppercase tracking-[0.16em] text-muted">{label}</p>
      <p className="truncate font-display text-lg font-semibold tabular-nums text-navy-deep sm:text-xl">{formatBrlFromCents(value)}</p>
      <p className={`truncate text-[10px] font-semibold ${hasComparison ? (improvement ? "text-teal-deep" : "text-expense-line") : "text-muted"}`}>{comparison}</p>
    </article>
  );
}

function AttentionCenter({ items }: { items: DashboardAttentionItem[] }) {
  return (
    <div className="flex min-w-0 flex-col gap-2 p-2.5 sm:flex-row sm:items-center">
      <div className="flex shrink-0 items-center gap-2"><h2 className="font-display text-sm font-semibold">Atenção</h2><span className="rounded-full bg-cream px-1.5 py-0.5 text-[8px] font-bold uppercase tracking-wide text-navy/65">{items.length}</span></div>
      <div className="flex min-w-0 flex-1 gap-1.5 overflow-x-auto">
        {items.map((item) => {
          const tone = item.tone === "danger" ? "border-red-200 bg-red-50/70" : item.tone === "warning" ? "border-navy/8 bg-cream/50" : "border-navy/8 bg-sage/50";
          const content = <><b className="text-[11px] text-navy-deep">{item.title}</b><span className="hidden truncate text-[10px] text-navy/60 xl:inline"> · {item.detail}</span></>;
          return item.href ? <Link key={item.key} to={item.href} className={`min-w-0 rounded-lg border px-2.5 py-1.5 transition hover:shadow-sm ${tone}`}>{content}</Link> : <div key={item.key} className={`min-w-0 rounded-lg border px-2.5 py-1.5 ${tone}`}>{content}</div>;
        })}
      </div>
    </div>
  );
}

function QuickActions() {
  const actions = [
    ["Registrar despesa", "/app/despesas", "−"],
    ["Adicionar receita", "/app/receitas", "+"],
    ["Ver investimentos", "/app/investimentos", "↗"],
  ];
  return (
    <div className="border-t border-navy/8 bg-navy-deep p-2.5 text-white lg:border-l lg:border-t-0">
      <div className="flex items-center gap-2"><h2 className="shrink-0 font-display text-sm font-semibold">Ações</h2><div className="grid min-w-0 flex-1 grid-cols-3 gap-1.5">{actions.map(([label, href, icon]) => <Link key={href} to={href} className="flex min-w-0 items-center justify-between gap-1 rounded-lg bg-white/8 px-2 py-1.5 text-[10px] font-semibold transition hover:bg-white/14"><span className="truncate">{label}</span><span className="grid h-5 w-5 shrink-0 place-items-center rounded-md bg-gold text-navy-deep">{icon}</span></Link>)}</div></div>
    </div>
  );
}

function DashboardPanel({ title, subtitle, value, valueLabel, tone, children, action, delay = "0ms" }: { title: string; subtitle: string; value: string; valueLabel: string; tone: "teal" | "expense" | "gold" | "navy"; children: ReactNode; action?: ReactNode; delay?: string }) {
  const accent = tone === "teal" ? "bg-teal" : tone === "expense" ? "bg-expense-line" : tone === "gold" ? "bg-gold" : "bg-navy";
  const valueTone = tone === "expense" ? "text-expense-line" : tone === "teal" ? "text-teal-deep" : "text-navy-deep";
  return (
    <section className="wp-rise flex min-h-[310px] min-w-0 flex-col overflow-hidden rounded-3xl border border-navy/8 bg-white/85 shadow-[0_16px_48px_rgba(20,28,42,0.07)] backdrop-blur" style={{ ["--wp-delay" as string]: delay }}>
      <div className={`h-1 w-full ${accent}`} />
      <header className="flex flex-wrap items-start justify-between gap-3 border-b border-navy/8 px-5 py-3">
        <div className="min-w-0"><h2 className="font-display text-xl font-semibold text-navy-deep">{title}</h2><div className="mt-0.5 flex flex-wrap items-center gap-x-3 gap-y-1"><p className="text-xs text-muted">{subtitle}</p>{action}</div></div>
        <div className="text-right"><p className={`font-display text-xl font-semibold tabular-nums ${valueTone}`}>{value}</p><p className="text-[10px] font-bold uppercase tracking-[0.14em] text-muted">{valueLabel}</p></div>
      </header>
      <div className="min-h-0 flex-1 overflow-auto p-3.5">{children}</div>
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
