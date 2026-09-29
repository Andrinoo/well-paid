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
  const h = 176;
  const padX = 14;
  const padY = 12;
  const xAt = (index: number) => padX + (index * (w - padX * 2)) / Math.max(shownMonths.length - 1, 1);
  const yAt = (value: number) => h - padY - (Math.max(0, value) / peak) * (h - padY * 2);
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
  const activeIndex = focused ?? shownMonths.length - 1;
  const forecastTotal = (data.expense_forecast_cents ?? []).reduce((sum, value) => sum + value, 0);

  return (
    <div className="wp-rise" style={{ ["--wp-delay" as string]: "220ms" }}>
      <div className="mb-2 flex flex-wrap items-center justify-between gap-2">
        <div className="flex flex-wrap gap-1.5" aria-label="Séries do gráfico">
          {series.map((item) => <button key={item.key} type="button" aria-pressed={visible[item.key]} onClick={() => setVisible((current) => ({ ...current, [item.key]: !current[item.key] }))} className={`flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-[10px] font-bold transition ${visible[item.key] ? "border-navy/10 bg-white text-navy shadow-sm" : "border-transparent bg-cream/50 text-muted opacity-60"}`}><span className="h-2 w-2 rounded-full" style={{ backgroundColor: item.color }} />{item.label}</button>)}
        </div>
        <div className="flex items-center gap-1 rounded-xl border border-navy/8 bg-white/70 p-1 shadow-sm">
          <button type="button" disabled={start === 0} onClick={() => { setFocused(null); setWindowEnd((current) => Math.max(size, current - 1)); }} className="grid h-7 w-7 place-items-center rounded-lg text-navy transition hover:bg-sage disabled:opacity-25" aria-label="Período anterior">‹</button>
          <button type="button" onClick={() => { const nextSize = windowSize === 6 ? Math.min(12, months.length) : 6; setWindowSize(nextSize); setWindowEnd(months.length); setFocused(null); }} className="min-w-20 px-2 text-[10px] font-bold uppercase tracking-wide text-navy">{size} meses</button>
          <button type="button" disabled={end === months.length} onClick={() => { setFocused(null); setWindowEnd((current) => Math.min(months.length, current + 1)); }} className="grid h-7 w-7 place-items-center rounded-lg text-navy transition hover:bg-sage disabled:opacity-25" aria-label="Próximo período">›</button>
        </div>
      </div>
      <div className="rounded-2xl border border-navy/10 bg-cream/20 p-2 shadow-[inset_0_1px_0_rgba(255,255,255,0.7)]">
        <div className="mb-1 flex min-h-9 flex-wrap items-center gap-x-4 gap-y-1 px-2 text-[11px]">
          <strong className="text-navy">{shortMonth(shownMonths[activeIndex].year, shownMonths[activeIndex].month)}</strong>
          {series.filter((item) => visible[item.key]).map((item) => <span key={item.key} className="text-muted"><i className="mr-1 inline-block h-1.5 w-1.5 rounded-full" style={{ backgroundColor: item.color }} />{item.label}: <b className="text-navy-deep">{formatBrlFromCents(item.values[activeIndex] ?? 0)}</b></span>)}
        </div>
        <svg viewBox={`0 0 ${w} ${h}`} className="h-52 w-full overflow-visible xl:h-56" onPointerMove={(event) => { const rect = event.currentTarget.getBoundingClientRect(); const x = event.clientX - rect.left; setFocused(Math.max(0, Math.min(shownMonths.length - 1, Math.round((x / rect.width) * (shownMonths.length - 1))))); }} onPointerLeave={() => setFocused(null)}>
          {[0.25, 0.5, 0.75, 1].map((ratio) => <line key={ratio} x1={padX} y1={h - padY - ratio * (h - padY * 2)} x2={w - padX} y2={h - padY - ratio * (h - padY * 2)} stroke="currentColor" strokeOpacity="0.07" strokeDasharray="3 7" />)}
          {series.map((item) => visible[item.key] ? <g key={item.key}><path d={pathFor(item.values)} fill="none" stroke={item.color} strokeWidth="3.5" strokeLinecap="round" strokeLinejoin="round" pathLength={item.dashed ? undefined : 1} className={item.dashed ? undefined : "wp-line-draw"} style={item.dashed ? { strokeDasharray: "8 7" } : undefined} />{item.values.slice(0, shownMonths.length).map((value, index) => <circle key={index} cx={xAt(index)} cy={yAt(value ?? 0)} r={focused === index ? 5 : 2.8} fill={item.color} stroke="white" strokeWidth="1.5" className="transition-all" />)}</g> : null)}
          {focused != null ? <line x1={xAt(focused)} y1={padY} x2={xAt(focused)} y2={h - padY} stroke="currentColor" strokeOpacity="0.18" strokeDasharray="3 4" /> : null}
        </svg>
        <div className="flex justify-between px-2 text-[10px] font-bold uppercase tracking-wide text-muted"><span>{shortMonth(shownMonths[0].year, shownMonths[0].month)}</span><span>{shortMonth(shownMonths[shownMonths.length - 1].year, shownMonths[shownMonths.length - 1].month)}</span></div>
      </div>
      <div className="mt-2 flex flex-wrap justify-between gap-2 text-[11px] text-muted"><span>Previsão acumulada: <b className="text-gold-pressed">{formatBrlFromCents(forecastTotal)}</b></span><span>Dados realizados + projeção</span></div>
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
