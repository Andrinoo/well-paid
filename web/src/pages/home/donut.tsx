import { useMemo, useState } from "react";
import { formatBrlFromCents } from "../../format";

const PALETTE = [
  ["#C9A94E", "#E3C56A"],
  ["#1B2C41", "#3D5573"],
  ["#2A7A6E", "#4AA394"],
  ["#B85C4A", "#D47A68"],
  ["#3D5A80", "#6B8BB0"],
  ["#8B6914", "#C9A94E"],
  ["#5C7A6A", "#88A898"],
  ["#7A4E3D", "#B8836C"],
];

type SliceIn = { category_key?: string; name: string; amount_cents: number };

type Slice = {
  key: string;
  name: string;
  amount_cents: number;
  from: string;
  to: string;
};

function aggregate(spending: SliceIn[]): Slice[] {
  const sorted = spending
    .filter((s) => s.amount_cents > 0)
    .sort((a, b) => b.amount_cents - a.amount_cents);
  if (sorted.length === 0) return [];
  const headCount = sorted.length > 7 ? 7 : sorted.length;
  const head = sorted.slice(0, headCount);
  const tail = sorted.slice(headCount);
  const raw: SliceIn[] = head.map((s) => ({
    category_key: s.category_key ?? s.name,
    name: s.name,
    amount_cents: s.amount_cents,
  }));
  if (tail.length > 0) {
    raw.push({
      category_key: "outros",
      name: "Outros",
      amount_cents: tail.reduce((sum, s) => sum + s.amount_cents, 0),
    });
  }
  const keys = [...new Set(raw.map((s) => s.category_key ?? s.name))].sort();
  const indexByKey = new Map(keys.map((k, i) => [k, i]));
  return raw.map((s) => {
    const key = s.category_key ?? s.name;
    const pair = PALETTE[(indexByKey.get(key) ?? 0) % PALETTE.length];
    return {
      key,
      name: s.name,
      amount_cents: s.amount_cents,
      from: pair[0],
      to: pair[1],
    };
  });
}

function cssId(raw: string): string {
  return raw.replace(/[^a-zA-Z0-9_-]/g, "") || "cat";
}

export function CategoryDonut({
  spending,
  totalCents,
}: {
  spending: SliceIn[];
  totalCents: number;
}) {
  const slices = useMemo(() => aggregate(spending), [spending]);
  const [selected, setSelected] = useState(0);
  const total = slices.reduce((sum, s) => sum + s.amount_cents, 0) || totalCents;
  const active = slices[selected] ?? slices[0];

  if (total <= 0 || slices.length === 0) {
    return (
      <div className="flex h-full min-h-[200px] flex-col items-center justify-center text-center">
        <div className="h-32 w-32 rounded-full border-[18px] border-cream-muted" />
        <p className="mt-4 text-sm text-muted">Sem despesas registadas neste mês</p>
      </div>
    );
  }

  const r = 62;
  const c = 2 * Math.PI * r;
  const gap = slices.length > 1 ? 8 : 0;
  const usable = c - gap * slices.length;
  let offset = 0;

  return (
    <div className="flex h-full min-h-0 flex-col gap-4 lg:flex-row lg:items-center">
      <div className="relative mx-auto h-48 w-48 shrink-0">
        <svg viewBox="0 0 160 160" className="h-full w-full -rotate-90">
          <defs>
            {slices.map((slice) => (
              <linearGradient
                key={slice.key}
                id={`donut-${cssId(slice.key)}`}
                x1="0%"
                y1="0%"
                x2="100%"
                y2="100%"
              >
                <stop offset="0%" stopColor={slice.from} />
                <stop offset="100%" stopColor={slice.to} />
              </linearGradient>
            ))}
          </defs>
          <circle
            cx="80"
            cy="80"
            r={r}
            fill="none"
            stroke="#EAE6DD"
            strokeWidth="16"
          />
          {slices.map((slice, i) => {
            const len = Math.max(4, (slice.amount_cents / total) * usable);
            const dash = `${len} ${c - len}`;
            const el = (
              <circle
                key={slice.key}
                cx="80"
                cy="80"
                r={r}
                fill="none"
                stroke={`url(#donut-${cssId(slice.key)})`}
                strokeWidth={i === selected ? 20 : 16}
                strokeLinecap="round"
                strokeDasharray={dash}
                strokeDashoffset={-offset}
                className="cursor-pointer transition-[stroke-width] duration-200"
                onClick={() => setSelected(i)}
              />
            );
            offset += len + gap;
            return el;
          })}
        </svg>
        <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center text-center">
          <p className="text-[10px] uppercase tracking-wider text-muted">
            {active ? active.name : "Total"}
          </p>
          <p className="mt-0.5 text-sm font-semibold text-navy-deep">
            {formatBrlFromCents(active ? active.amount_cents : totalCents || total)}
          </p>
          <p className="text-[11px] text-gold-pressed">
            {active ? `${Math.round((active.amount_cents / total) * 100)}%` : null}
          </p>
        </div>
      </div>
      <ul className="min-h-0 min-w-0 flex-1 space-y-1 overflow-auto text-sm">
        {slices.map((slice, i) => (
          <li key={slice.key}>
            <button
              type="button"
              className={`flex w-full items-center justify-between gap-2 rounded-lg px-2 py-1.5 text-left transition ${
                i === selected ? "bg-cream-muted" : "hover:bg-cream-muted/60"
              }`}
              onClick={() => setSelected(i)}
            >
              <span className="flex min-w-0 items-center gap-2">
                <span
                  className="h-2.5 w-2.5 shrink-0 rounded-full"
                  style={{ background: slice.from }}
                />
                <span className="truncate text-navy">{slice.name}</span>
              </span>
              <span className="shrink-0 text-xs text-muted">
                {Math.round((slice.amount_cents / total) * 100)}% ·{" "}
                {formatBrlFromCents(slice.amount_cents)}
              </span>
            </button>
          </li>
        ))}
      </ul>
    </div>
  );
}
