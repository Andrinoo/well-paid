import { Link } from "react-router-dom";
import type { ReactNode } from "react";

export function Wordmark({ light = false }: { light?: boolean }) {
  return (
    <Link
      to="/"
      className={`font-serif text-xl tracking-tight ${light ? "text-cream" : "text-navy-deep"}`}
    >
      well <span className="text-gold">paid</span>
    </Link>
  );
}

export function Field({
  label,
  type = "text",
  value,
  onChange,
  autoComplete,
  required,
}: {
  label: string;
  type?: string;
  value: string;
  onChange: (value: string) => void;
  autoComplete?: string;
  required?: boolean;
}) {
  return (
    <label className="block">
      <span className="mb-1.5 block text-sm text-cream/70">{label}</span>
      <input
        className="w-full rounded-lg border border-gold/25 bg-navy px-3 py-3 text-base text-cream outline-none ring-gold/40 placeholder:text-muted focus:ring-2"
        type={type}
        value={value}
        autoComplete={autoComplete}
        required={required}
        onChange={(e) => onChange(e.target.value)}
      />
    </label>
  );
}

export function PrimaryButton({
  children,
  disabled,
}: {
  children: ReactNode;
  disabled?: boolean;
}) {
  return (
    <button
      type="submit"
      disabled={disabled}
      className="w-full rounded-lg bg-gold py-3 text-sm font-semibold tracking-wide text-navy-deep transition hover:bg-gold-pressed disabled:opacity-60"
    >
      {children}
    </button>
  );
}

export function AuthCard({
  title,
  subtitle,
  children,
  footer,
}: {
  title: string;
  subtitle?: string;
  children: ReactNode;
  footer?: ReactNode;
}) {
  return (
    <div className="flex min-h-dvh flex-col bg-black px-4 py-8">
      <header className="mx-auto w-full max-w-md">
        <Wordmark light />
      </header>
      <main className="mx-auto mt-10 w-full max-w-md">
        <section className="rounded-2xl border border-gold/30 bg-navy-deep p-6 shadow-[0_24px_80px_rgba(0,0,0,0.45)] sm:p-8">
          <p className="text-[11px] uppercase tracking-[0.28em] text-cream/45">
            Well Paid
          </p>
          <h1 className="mt-3 font-serif text-3xl text-cream">{title}</h1>
          {subtitle ? (
            <p className="mt-2 text-sm leading-relaxed text-cream/65">{subtitle}</p>
          ) : null}
          <div className="mt-6 space-y-4">{children}</div>
        </section>
        {footer ? (
          <p className="mt-6 text-center text-sm text-cream/55">{footer}</p>
        ) : null}
      </main>
    </div>
  );
}

export function ErrorText({ message }: { message: string | null }) {
  if (!message) return null;
  return (
    <p className="rounded-lg border border-red-400/30 bg-red-500/10 px-3 py-2 text-sm text-red-200">
      {message}
    </p>
  );
}

const DONUT_COLORS = ["#C9A94E", "#1E90FF", "#32CD32", "#E8A0BF", "#7EB8DA", "#D4C4A8"];

export function DonutChart({
  slices,
}: {
  slices: { name: string; amount_cents: number }[];
}) {
  const total = slices.reduce((sum, s) => sum + s.amount_cents, 0);
  if (total <= 0) {
    return (
      <p className="py-8 text-center text-sm text-muted">
        Sem despesas neste mês.
      </p>
    );
  }
  const r = 42;
  const c = 2 * Math.PI * r;
  let offset = 0;
  return (
    <div className="flex items-center gap-5">
      <svg viewBox="0 0 120 120" className="h-36 w-36 shrink-0">
        <circle cx="60" cy="60" r={r} fill="none" stroke="#EAE6DD" strokeWidth="16" />
        {slices.map((slice, i) => {
          const len = (slice.amount_cents / total) * c;
          const dash = `${len} ${c - len}`;
          const el = (
            <circle
              key={slice.name}
              cx="60"
              cy="60"
              r={r}
              fill="none"
              stroke={DONUT_COLORS[i % DONUT_COLORS.length]}
              strokeWidth="16"
              strokeDasharray={dash}
              strokeDashoffset={-offset}
              transform="rotate(-90 60 60)"
            />
          );
          offset += len;
          return el;
        })}
      </svg>
      <ul className="min-w-0 space-y-1.5 text-sm">
        {slices.slice(0, 6).map((slice, i) => (
          <li key={slice.name} className="flex items-center gap-2">
            <span
              className="h-2 w-2 shrink-0 rounded-full"
              style={{ background: DONUT_COLORS[i % DONUT_COLORS.length] }}
            />
            <span className="truncate text-navy">{slice.name}</span>
          </li>
        ))}
      </ul>
    </div>
  );
}

export function CashflowChart({
  labels,
  income,
  expense,
}: {
  labels: string[];
  income: number[];
  expense: number[];
}) {
  const w = 320;
  const h = 140;
  const pad = 8;
  const max = Math.max(1, ...income, ...expense);
  const n = Math.max(labels.length, 2);

  const points = (values: number[]) =>
    values
      .map((v, i) => {
        const x = pad + (i * (w - pad * 2)) / (n - 1);
        const y = h - pad - (v / max) * (h - pad * 2);
        return `${x},${y}`;
      })
      .join(" ");

  if (labels.length < 2) {
    return <p className="py-8 text-center text-sm text-muted">Sem histórico ainda.</p>;
  }

  return (
    <div>
      <svg viewBox={`0 0 ${w} ${h}`} className="h-36 w-full">
        <polyline
          fill="none"
          stroke="#32CD32"
          strokeWidth="2.5"
          points={points(income)}
        />
        <polyline
          fill="none"
          stroke="#C9A94E"
          strokeWidth="2.5"
          points={points(expense)}
        />
      </svg>
      <div className="mt-2 flex justify-between text-[11px] uppercase tracking-wide text-muted">
        <span>{labels[0]}</span>
        <span>{labels[labels.length - 1]}</span>
      </div>
      <div className="mt-3 flex gap-4 text-xs text-navy">
        <span className="inline-flex items-center gap-1.5">
          <span className="h-1.5 w-4 rounded-full bg-[#32CD32]" /> Receitas
        </span>
        <span className="inline-flex items-center gap-1.5">
          <span className="h-1.5 w-4 rounded-full bg-gold" /> Despesas pagas
        </span>
      </div>
    </div>
  );
}
