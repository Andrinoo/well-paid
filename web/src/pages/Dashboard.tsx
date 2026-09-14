import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  ApiError,
  contributeGoal,
  fetchCashflow,
  fetchHomeBanner,
  fetchMe,
  fetchOverview,
  type DashboardCashflow,
  type DashboardOverview,
  type HomeBanner,
} from "../api";
import {
  daysUntil,
  formatBrlFromCents,
  formatDueDate,
  greetingFirstName,
  monthLabel,
  parseBrlToCents,
} from "../format";
import { MonthBar, Widget } from "../ui";
import { useToggleShellMenu } from "../shell";
import { HomeCashflow } from "./home/cashflow";
import { CategoryDonut } from "./home/donut";

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
  const [dynamic, setDynamic] = useState(true);
  const [forecastMonths, setForecastMonths] = useState(3);
  const [contrib, setContrib] = useState<Record<string, string>>({});

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
            dynamic,
            forecastMonths,
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
  }, [period.year, period.month, dynamic, forecastMonths, navigate]);

  const pending = overview?.pending_preview?.length
    ? overview.pending_preview
    : (overview?.upcoming_due ?? []);
  const goals = overview?.goals_preview ?? [];
  const monthTitle =
    monthLabel(period.year, period.month).charAt(0).toLocaleUpperCase("pt-BR") +
    monthLabel(period.year, period.month).slice(1);

  async function onContribute(id: string) {
    const cents = parseBrlToCents(contrib[id] ?? "");
    if (!cents) return;
    try {
      await contributeGoal(id, cents);
      const ov = await fetchOverview(period.year, period.month);
      setOverview(ov);
      setContrib((c) => ({ ...c, [id]: "" }));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Não foi possível aportar.");
    }
  }

  return (
    <div className="flex h-full min-h-0 flex-col px-3 py-3 sm:px-4">
      <div className="mb-3 flex flex-wrap items-center justify-between gap-3">
        <div className="min-w-0">
          <p className="text-[11px] uppercase tracking-[0.28em] text-muted">Dashboard</p>
          <h1 className="mt-0.5 truncate font-serif text-2xl text-navy-deep sm:text-3xl">
            {name ? `Olá, ${name}` : "Olá"}
          </h1>
        </div>
        <div className="flex items-center gap-2">
          <MonthBar year={period.year} month={period.month} onChange={setPeriod} />
          <button
            type="button"
            className="rounded-lg px-2 py-2 text-sm text-navy md:hidden"
            onClick={toggleMenu}
          >
            Menu
          </button>
        </div>
      </div>

      {error ? (
        <p className="mb-3 rounded-xl border border-red-200 bg-red-50 px-4 py-2 text-sm text-red-800">
          {error}
        </p>
      ) : null}
      {banner ? (
        <p className="mb-3 rounded-xl border border-gold/30 bg-[#FFF8E1] px-4 py-2 text-sm text-navy">
          {banner.title}
        </p>
      ) : null}

      {busy && !overview ? (
        <p className="py-16 text-center text-muted">A carregar {monthTitle}…</p>
      ) : (
        <div className="grid min-h-0 flex-1 gap-2 overflow-auto lg:grid-cols-2 lg:grid-rows-2 lg:overflow-hidden">
          <Widget
            title="Despesas por categoria"
            action={
              <Link to="/app/despesas" className="text-sm text-gold-pressed">
                Ver mais
              </Link>
            }
          >
            <CategoryDonut
              spending={overview?.spending_by_category ?? []}
              totalCents={overview?.month_expense_total_cents ?? 0}
            />
          </Widget>

          <Widget title="Histórico mensal">
            {cashflow ? (
              <HomeCashflow
                data={cashflow}
                dynamic={dynamic}
                forecastMonths={forecastMonths}
                onDynamicChange={setDynamic}
                onForecastChange={setForecastMonths}
              />
            ) : (
              <p className="text-sm text-muted">Sem dados de séries para o gráfico.</p>
            )}
          </Widget>

          <Widget
            title="Pagamentos futuros"
            action={
              <Link to="/app/despesas?filtro=pagar" className="text-sm text-gold-pressed">
                Ver mais
              </Link>
            }
          >
            {pending.length === 0 ? (
              <p className="text-sm text-muted">Nenhuma despesa a vencer.</p>
            ) : (
              <>
                <div className="mb-1 hidden grid-cols-[7rem_1fr_auto] gap-3 text-[11px] uppercase tracking-wide text-muted sm:grid">
                  <span>Vencimento</span>
                  <span>Descrição</span>
                  <span>Valor</span>
                </div>
                <ul className="max-h-full min-h-0 divide-y divide-navy/8 overflow-auto">
                  {pending.map((item) => {
                    const urgent = (daysUntil(item.due_date) ?? 99) <= 3;
                    return (
                      <li
                        key={item.id}
                        className="grid grid-cols-1 gap-0.5 py-2.5 text-sm sm:grid-cols-[7rem_1fr_auto] sm:items-baseline sm:gap-3"
                      >
                        <p className={urgent ? "text-red-700" : "text-muted"}>
                          {formatDueDate(item.due_date)}
                        </p>
                        <p className="truncate text-navy">{item.description}</p>
                        <span className="font-medium sm:text-right">
                          {formatBrlFromCents(item.amount_cents)}
                        </span>
                      </li>
                    );
                  })}
                </ul>
                <p className="mt-3 border-t border-navy/8 pt-3 text-sm">
                  Total pendente:{" "}
                  <span className="font-medium">
                    {formatBrlFromCents(overview?.pending_total_cents ?? 0)}
                  </span>
                </p>
              </>
            )}
          </Widget>

          <Widget
            title="Metas"
            action={
              <Link to="/app/metas" className="text-sm text-gold-pressed">
                Ver mais
              </Link>
            }
          >
            {goals.length === 0 ? (
              <p className="text-sm text-muted">Nenhuma meta activa. Crie uma agora!</p>
            ) : (
              <ul className="min-h-0 space-y-4 overflow-auto">
                {goals.map((goal) => {
                  const pct = Math.min(
                    100,
                    Math.round((goal.current_cents / Math.max(1, goal.target_cents)) * 100),
                  );
                  return (
                    <li key={goal.id}>
                      <div className="flex justify-between gap-3 text-sm">
                        <span className="truncate font-medium uppercase tracking-wide">
                          {goal.title}
                        </span>
                        <span className="shrink-0 text-muted">{pct}%</span>
                      </div>
                      <div className="mt-2 h-2 overflow-hidden rounded-full bg-cream-muted">
                        <div
                          className="h-full rounded-full bg-gold"
                          style={{ width: `${pct}%` }}
                        />
                      </div>
                      <p className="mt-1 text-xs text-muted">
                        {formatBrlFromCents(goal.current_cents)} de{" "}
                        {formatBrlFromCents(goal.target_cents)}
                      </p>
                      <form
                        className="mt-2 flex gap-2"
                        onSubmit={(e) => {
                          e.preventDefault();
                          void onContribute(goal.id);
                        }}
                      >
                        <input
                          className="min-w-0 flex-1 rounded-lg border border-navy/10 px-2 py-1.5 text-sm"
                          placeholder="Aportar R$"
                          value={contrib[goal.id] ?? ""}
                          onChange={(e) =>
                            setContrib((c) => ({ ...c, [goal.id]: e.target.value }))
                          }
                        />
                        <button
                          type="submit"
                          className="rounded-lg bg-navy-deep px-3 py-1.5 text-xs text-cream"
                        >
                          Guardar
                        </button>
                      </form>
                    </li>
                  );
                })}
              </ul>
            )}
          </Widget>
        </div>
      )}
    </div>
  );
}
