import { useMemo, useState } from "react";
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
  const leftover = (data.income_cents ?? []).map(
    (inc, i) => inc - ((data.expense_paid_cents ?? [])[i] ?? 0),
  );
  if (months.length < 2 || leftover.length < 2) return null;
  const peak = Math.max(1, ...leftover.map((v) => Math.abs(v)));
  const w = 640;
  const h = 110;
  const pad = 10;
  const mid = h / 2;
  const pts = leftover.map((v, i) => ({
    x: pad + (i * (w - pad * 2)) / Math.max(leftover.length - 1, 1),
    y: mid - (v / peak) * (mid - pad),
  }));
  let line = `M ${pts[0].x} ${pts[0].y}`;
  for (let i = 0; i < pts.length - 1; i += 1) {
    const p0 = pts[i - 1] ?? pts[i];
    const p1 = pts[i];
    const p2 = pts[i + 1];
    const p3 = pts[i + 2] ?? p2;
    line += ` C ${p1.x + (p2.x - p0.x) / 6} ${p1.y + (p2.y - p0.y) / 6}, ${
      p2.x - (p3.x - p1.x) / 6
    } ${p2.y - (p3.y - p1.y) / 6}, ${p2.x} ${p2.y}`;
  }
  const last = pts[pts.length - 1];
  const area = `${line} L ${last.x} ${h - pad} L ${pts[0].x} ${h - pad} Z`;

  return (
    <div className="wp-rise" style={{ ["--wp-delay" as string]: "220ms" }}>
      <p className="mb-2 text-[11px] font-semibold uppercase tracking-[0.2em] text-muted">
        Pulso dos últimos meses
      </p>
      <svg viewBox={`0 0 ${w} ${h}`} className="h-24 w-full overflow-visible">
        <defs>
          <linearGradient id="wp-wave-fill" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#12a888" stopOpacity="0.35" />
            <stop offset="100%" stopColor="#12a888" stopOpacity="0" />
          </linearGradient>
        </defs>
        <g className="wp-wave-shift">
          <path d={area} fill="url(#wp-wave-fill)" className="wp-area-in" />
          <path
            d={line}
            fill="none"
            stroke="#12a888"
            strokeWidth="2.6"
            strokeLinecap="round"
            pathLength={1}
            className="wp-line-draw"
          />
        </g>
      </svg>
      <div className="flex justify-between text-[11px] uppercase tracking-wide text-muted">
        <span>{shortMonth(months[0].year, months[0].month)}</span>
        <span>{shortMonth(months[months.length - 1].year, months[months.length - 1].month)}</span>
      </div>
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
