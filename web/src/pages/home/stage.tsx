import { useEffect, useMemo, useState } from "react";
import type { DashboardCashflow } from "../../api";
import { formatBrlFromCents, shortMonth } from "../../format";
import { MoneyCount } from "./count-up";

const PALETTE = ["#12a888", "#1B2C41", "#e3b23c", "#B85C4A", "#3D5A80", "#0c6e5c"];

export type SpendSlice = { category_key?: string; name: string; amount_cents: number };

function topSlices(spending: SpendSlice[]): { name: string; amount_cents: number; color: string }[] {
  const sorted = spending
    .filter((s) => s.amount_cents > 0)
    .sort((a, b) => b.amount_cents - a.amount_cents);
  const head = sorted.slice(0, 6);
  const rest = sorted.slice(6);
  const rows = head.map((s) => ({ name: s.name, amount_cents: s.amount_cents }));
  if (rest.length > 0) {
    rows.push({
      name: "Outros",
      amount_cents: rest.reduce((sum, s) => sum + s.amount_cents, 0),
    });
  }
  return rows.map((row, i) => ({ ...row, color: PALETTE[i % PALETTE.length] }));
}

export function MonthOrbit({
  spending,
  balanceCents,
  story,
}: {
  spending: SpendSlice[];
  balanceCents: number;
  story: string;
}) {
  const slices = useMemo(() => topSlices(spending), [spending]);
  const total = slices.reduce((sum, s) => sum + s.amount_cents, 0);
  const [picked, setPicked] = useState<number | null>(null);
  const active = picked != null ? slices[picked] : null;
  const cx = 200;
  const cy = 200;
  const ring = 138;

  return (
    <div className="relative mx-auto aspect-square w-full max-w-[420px]">
      <svg viewBox="0 0 400 400" className="h-full w-full overflow-visible" aria-hidden>
        <defs>
          <radialGradient id="wp-sun-core" cx="50%" cy="45%" r="55%">
            <stop offset="0%" stopColor="#3ad4b0" stopOpacity="0.95" />
            <stop offset="55%" stopColor="#12a888" stopOpacity="0.35" />
            <stop offset="100%" stopColor="#12a888" stopOpacity="0" />
          </radialGradient>
        </defs>
        <circle cx={cx} cy={cy} r="118" fill="url(#wp-sun-core)" className="wp-sun" />
        <circle
          cx={cx}
          cy={cy}
          r={ring}
          fill="none"
          stroke="rgba(20,28,42,0.08)"
          strokeWidth="1.5"
          strokeDasharray="3 10"
        />
        {slices.length > 0 ? (
          <g className="wp-orbit">
            {slices.map((slice, i) => {
              const angle = ((i / slices.length) * Math.PI * 2) - Math.PI / 2;
              const x = cx + Math.cos(angle) * ring;
              const y = cy + Math.sin(angle) * ring;
              const r = 7 + (total > 0 ? (slice.amount_cents / total) * 16 : 0);
              return (
                <g key={slice.name} transform={`translate(${x} ${y})`}>
                  <g className="wp-orbit-counter">
                    <circle
                      r={r}
                      fill={slice.color}
                      className={`cursor-pointer ${picked === i ? "wp-pulse-dot" : ""}`}
                      onClick={() => setPicked(i === picked ? null : i)}
                    />
                  </g>
                </g>
              );
            })}
          </g>
        ) : null}
      </svg>
      <div className="pointer-events-none absolute inset-[22%] flex flex-col items-center justify-center text-center">
        <p className="text-[11px] font-semibold uppercase tracking-[0.22em] text-teal-deep">
          {active ? active.name : "Sobra"}
        </p>
        <p
          className={`mt-1 font-display text-3xl font-semibold tabular-nums tracking-tight sm:text-4xl ${
            !active && balanceCents < 0 ? "text-expense-line" : "text-navy-deep"
          }`}
        >
          <MoneyCount cents={active ? active.amount_cents : balanceCents} />
        </p>
        <p className="mt-2 max-w-[16rem] text-xs leading-relaxed text-navy/65">{story}</p>
      </div>
    </div>
  );
}

export function MonthWave({ data }: { data: DashboardCashflow }) {
  const months = data.months ?? [];
  const [visible, setVisible] = useState({ income: true, paid: true, forecast: true });
  const [focused, setFocused] = useState<number | null>(null);
  const [windowSize, setWindowSize] = useState(6);
  const [windowEnd, setWindowEnd] = useState(months.length);
  useEffect(() => { setWindowEnd(months.length); }, [months.length]);
  const allSeries = [
    { key: "income" as const, label: "Entradas", values: data.income_cents ?? [], color: "#12a888", dashed: false },
    { key: "paid" as const, label: "Despesas", values: data.expense_paid_cents ?? [], color: "#B85C4A", dashed: false },
    { key: "forecast" as const, label: "Previstas", values: data.expense_forecast_cents ?? [], color: "#e3b23c", dashed: true },
  ];
  if (months.length < 2) return null;
  const size = Math.min(windowSize, months.length);
  const end = Math.max(size, Math.min(windowEnd, months.length));
  const start = Math.max(0, end - size);
  const shownMonths = months.slice(start, end);
  const series = allSeries.map((item) => ({ ...item, values: item.values.slice(start, end) }));
  const peak = Math.max(1, ...series.flatMap((item) => item.values));
  const w = 640;
  const h = 218;
  const padLeft = 48;
  const padRight = 14;
  const padTop = 44;
  const padBottom = 26;
  const xAt = (index: number) => padLeft + (index * (w - padLeft - padRight)) / Math.max(shownMonths.length - 1, 1);
  const yAt = (value: number) => h - padBottom - (Math.max(0, value) / peak) * (h - padTop - padBottom);
  const pathFor = (input: number[]) => {
    const points = input.slice(0, shownMonths.length).map((value, index) => ({ x: xAt(index), y: yAt(value ?? 0) }));
    if (points.length < 2) return "";
    let path = `M ${points[0].x} ${points[0].y}`;
    for (let index = 0; index < points.length - 1; index += 1) {
      const before = points[index - 1] ?? points[index];
      const current = points[index];
      const next = points[index + 1];
      const after = points[index + 2] ?? next;
      path += ` C ${current.x + (next.x - before.x) / 6} ${current.y + (next.y - before.y) / 6}, ${next.x - (after.x - current.x) / 6} ${next.y - (after.y - current.y) / 6}, ${next.x} ${next.y}`;
    }
    return path;
  };
  const areaFor = (input: number[]) => `${pathFor(input)} L ${xAt(shownMonths.length - 1)} ${h - padBottom} L ${xAt(0)} ${h - padBottom} Z`;
  const activeIndex = focused ?? shownMonths.length - 1;
  const forecastTotal = (data.expense_forecast_cents ?? []).reduce((sum, value) => sum + value, 0);

  return (
    <div className="wp-rise flex h-full min-h-0 flex-col" style={{ ["--wp-delay" as string]: "220ms" }}>
      <div className="mb-1.5 flex shrink-0 flex-wrap items-center justify-between gap-1.5">
        <div className="flex flex-wrap gap-1" aria-label="Séries do gráfico">
          {series.map((item) => <button key={item.key} type="button" aria-pressed={visible[item.key]} onClick={() => setVisible((current) => ({ ...current, [item.key]: !current[item.key] }))} className={`flex h-7 items-center gap-1.5 rounded-lg border px-2 text-[10px] font-bold transition ${visible[item.key] ? "border-navy/10 bg-white text-navy shadow-sm" : "border-transparent bg-cream/50 text-muted opacity-55"}`}><span className="h-1.5 w-1.5 rounded-full" style={{ backgroundColor: item.color }} />{item.label}</button>)}
        </div>
        <div className="flex h-8 items-center rounded-lg border border-navy/8 bg-white/70 p-0.5 shadow-sm">
          <button type="button" disabled={start === 0} onClick={() => { setFocused(null); setWindowEnd((current) => Math.max(size, current - 1)); }} className="grid h-7 w-7 place-items-center rounded-md text-navy transition hover:bg-sage disabled:opacity-25" aria-label="Período anterior">‹</button>
          <button type="button" onClick={() => { const nextSize = windowSize === 6 ? Math.min(12, months.length) : 6; setWindowSize(nextSize); setWindowEnd(months.length); setFocused(null); }} className="min-w-16 px-1 text-[9px] font-bold uppercase tracking-wide text-navy">{size} meses</button>
          <button type="button" disabled={end === months.length} onClick={() => { setFocused(null); setWindowEnd((current) => Math.min(months.length, current + 1)); }} className="grid h-7 w-7 place-items-center rounded-md text-navy transition hover:bg-sage disabled:opacity-25" aria-label="Próximo período">›</button>
        </div>
      </div>
      <div className="relative min-h-0 flex-1 overflow-hidden rounded-2xl border border-navy/10 bg-gradient-to-b from-white to-cream/30 shadow-[inset_0_1px_0_rgba(255,255,255,0.75)]">
        <div className="pointer-events-none absolute left-3 top-2 z-10 flex max-w-[calc(100%-1.5rem)] flex-wrap items-center gap-x-2 gap-y-0.5 rounded-xl border border-white/80 bg-white/80 px-2.5 py-1.5 text-[9px] shadow-md backdrop-blur-md">
          <strong className="uppercase tracking-wide text-navy">{shortMonth(shownMonths[activeIndex].year, shownMonths[activeIndex].month)}</strong>
          {series.filter((item) => visible[item.key]).map((item) => <span key={item.key} className="text-muted"><i className="mr-1 inline-block h-1.5 w-1.5 rounded-full" style={{ backgroundColor: item.color }} />{item.label} <b className="text-navy-deep">{formatBrlFromCents(item.values[activeIndex] ?? 0)}</b></span>)}
        </div>
        <svg viewBox={`0 0 ${w} ${h}`} preserveAspectRatio="none" className="h-full min-h-[190px] w-full cursor-crosshair" onPointerMove={(event) => { const rect = event.currentTarget.getBoundingClientRect(); const plotX = Math.max(0, Math.min(rect.width, event.clientX - rect.left)); setFocused(Math.max(0, Math.min(shownMonths.length - 1, Math.round((plotX / rect.width) * (shownMonths.length - 1))))); }} onPointerLeave={() => setFocused(null)}>
          <defs>
            <linearGradient id="wp-income-area" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stopColor="#12a888" stopOpacity="0.24"/><stop offset="100%" stopColor="#12a888" stopOpacity="0"/></linearGradient>
            <linearGradient id="wp-paid-area" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stopColor="#B85C4A" stopOpacity="0.13"/><stop offset="100%" stopColor="#B85C4A" stopOpacity="0"/></linearGradient>
            <filter id="wp-line-glow" x="-20%" y="-20%" width="140%" height="140%"><feGaussianBlur stdDeviation="2" result="blur"/><feMerge><feMergeNode in="blur"/><feMergeNode in="SourceGraphic"/></feMerge></filter>
          </defs>
          {[0, 0.5, 1].map((ratio) => { const y = h - padBottom - ratio * (h - padTop - padBottom); return <g key={ratio}><line x1={padLeft} y1={y} x2={w - padRight} y2={y} stroke="currentColor" strokeOpacity="0.07" strokeDasharray="3 7"/><text x={padLeft - 7} y={y + 3} textAnchor="end" fontSize="8" fill="currentColor" opacity="0.45">{compactMoney(peak * ratio)}</text></g>; })}
          {visible.income ? <path d={areaFor(series[0].values)} fill="url(#wp-income-area)" className="wp-area-in"/> : null}
          {visible.paid ? <path d={areaFor(series[1].values)} fill="url(#wp-paid-area)" className="wp-area-in"/> : null}
          {series.map((item) => visible[item.key] ? <g key={item.key}><path d={pathFor(item.values)} fill="none" stroke={item.color} strokeWidth="3.2" strokeLinecap="round" strokeLinejoin="round" pathLength={item.dashed ? undefined : 1} className={item.dashed ? undefined : "wp-line-draw"} filter={item.dashed ? undefined : "url(#wp-line-glow)"} style={item.dashed ? { strokeDasharray: "8 7" } : undefined}/>{focused != null ? <circle cx={xAt(focused)} cy={yAt(item.values[focused] ?? 0)} r="4.8" fill={item.color} stroke="white" strokeWidth="2"/> : null}</g> : null)}
          {focused != null ? <line x1={xAt(focused)} y1={padTop} x2={xAt(focused)} y2={h - padBottom} stroke="currentColor" strokeOpacity="0.22" strokeDasharray="3 4"/> : null}
          {shownMonths.map((month, index) => <text key={`${month.year}-${month.month}`} x={xAt(index)} y={h - 7} textAnchor="middle" fontSize="8" fontWeight="700" fill="currentColor" opacity="0.48">{shortMonth(month.year, month.month)}</text>)}
        </svg>
      </div>
      <div className="mt-1.5 flex shrink-0 flex-wrap justify-between gap-2 text-[9px] font-semibold text-muted"><span>Previsão acumulada <b className="text-gold-pressed">{formatBrlFromCents(forecastTotal)}</b></span><span>Realizado + projeção</span></div>
    </div>
  );
}

export function MonthTide({
  inCents,
  outCents,
}: {
  inCents: number;
  outCents: number;
}) {
  const sum = Math.max(1, inCents + outCents);
  const inPct = Math.round((inCents / sum) * 100);
  return (
    <div className="wp-rise grid gap-3 sm:grid-cols-2" style={{ ["--wp-delay" as string]: "160ms" }}>
      <div>
        <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-teal-deep">
          Entrou
        </p>
        <p className="mt-1 font-display text-2xl font-semibold tabular-nums text-navy-deep">
          <MoneyCount cents={inCents} />
        </p>
      </div>
      <div className="sm:text-right">
        <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-expense-line">
          Saiu
        </p>
        <p className="mt-1 font-display text-2xl font-semibold tabular-nums text-navy-deep">
          <MoneyCount cents={outCents} />
        </p>
      </div>
      <div className="h-2.5 overflow-hidden rounded-full bg-cream-muted sm:col-span-2">
        <div className="flex h-full w-full">
          <div
            className="wp-bar-fill h-full bg-teal"
            style={{ width: `${inPct}%` }}
          />
          <div className="h-full flex-1 bg-peach" />
        </div>
      </div>
      <p className="sr-only">
        {formatBrlFromCents(inCents)} entrou e {formatBrlFromCents(outCents)} saiu.
      </p>
    </div>
  );
}

function compactMoney(cents: number): string {
  return new Intl.NumberFormat("pt-BR", {
    notation: "compact",
    maximumFractionDigits: 1,
  }).format(cents / 100);
}
