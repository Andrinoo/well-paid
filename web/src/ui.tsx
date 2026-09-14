import { Link } from "react-router-dom";
import { useEffect, type ReactNode } from "react";
import { LangToggle } from "./i18n/LangToggle";
import { useLocale } from "./i18n/LocaleProvider";
import { formatBrlFromCents, monthLabel, shiftMonth } from "./format";

export function Wordmark({
  light = false,
  to = "/",
  accent = "gold",
  size = "sm",
  withMark = false,
}: {
  light?: boolean;
  to?: string;
  accent?: "gold" | "teal";
  size?: "sm" | "md" | "lg";
  withMark?: boolean;
}) {
  const paid = accent === "teal" ? "text-teal" : "text-gold";
  const type =
    size === "lg"
      ? "font-display text-3xl font-semibold sm:text-4xl"
      : size === "md"
        ? "font-display text-2xl font-semibold sm:text-[1.7rem]"
        : "font-serif text-xl tracking-tight";
  const markSize =
    size === "lg"
      ? "h-11 w-11 text-base"
      : size === "md"
        ? "h-9 w-9 text-sm"
        : "h-7 w-7 text-[11px]";
  return (
    <Link
      to={to}
      className={`inline-flex items-center gap-2.5 ${type} ${light ? "text-cream" : "text-navy-deep"}`}
      aria-label="Well Paid"
    >
      {withMark && (
        <span
          className={`inline-flex shrink-0 items-center justify-center rounded-xl bg-teal font-display font-semibold tracking-tight text-white ${markSize}`}
        >
          WP
        </span>
      )}
      <span className="whitespace-nowrap">
        Well <span className={paid}>Paid</span>
      </span>
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
      <span className="mb-1.5 block text-sm font-medium text-navy/70">{label}</span>
      <input
        className="w-full rounded-xl border border-navy/12 bg-white px-3 py-3 text-base text-navy-deep outline-none ring-teal/30 placeholder:text-muted focus:border-teal/40 focus:ring-2"
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
      className="w-full rounded-full bg-teal py-3 text-sm font-semibold tracking-wide text-white transition hover:bg-teal-deep disabled:opacity-60"
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
  const { t, auth } = useLocale();
  useEffect(() => {
    document.title = `Well Paid — ${title}`;
  }, [title]);
  return (
    <div className="site-root relative flex min-h-dvh flex-col overflow-hidden bg-paper px-4 py-8 font-ui text-navy-deep">
      <div className="pointer-events-none absolute -left-16 -top-20 h-64 w-64 rounded-full bg-peach/90 blur-3xl" />
      <div className="pointer-events-none absolute -right-10 top-10 h-56 w-56 rounded-full bg-sky blur-3xl" />
      <header className="relative mx-auto flex w-full max-w-md items-center justify-between gap-3">
        <Wordmark accent="teal" size="md" withMark />
        <LangToggle />
      </header>
      <main className="relative mx-auto mt-10 w-full max-w-md">
        <section className="rounded-[28px] border border-navy/8 bg-white/90 p-6 shadow-[0_24px_60px_-36px_rgba(20,28,42,0.45)] sm:p-8">
          <p className="text-[11px] font-semibold uppercase tracking-[0.28em] text-teal-deep">
            {auth.brandKicker}
          </p>
          <h1 className="mt-3 font-display text-3xl font-semibold text-navy-deep">
            {title}
          </h1>
          {subtitle ? (
            <p className="mt-2 text-sm leading-relaxed text-navy/70">{subtitle}</p>
          ) : null}
          <div className="mt-6 space-y-4">{children}</div>
        </section>
        {footer ? (
          <p className="mt-6 text-center text-sm text-muted">{footer}</p>
        ) : null}
        <p className="mt-4 text-center text-xs leading-relaxed text-muted">
          {auth.trust}
        </p>
        <p className="mt-8 text-center text-xs text-muted">{t.footer.copyright}</p>
      </main>
    </div>
  );
}

export function ErrorText({ message }: { message: string | null }) {
  if (!message) return null;
  return (
    <p className="rounded-xl border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800">
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
          <li key={slice.name} className="flex items-center justify-between gap-2">
            <span className="flex min-w-0 items-center gap-2">
              <span
                className="h-2 w-2 shrink-0 rounded-full"
                style={{ background: DONUT_COLORS[i % DONUT_COLORS.length] }}
              />
              <span className="truncate text-navy">{slice.name}</span>
            </span>
            <span className="shrink-0 text-xs text-muted">
              {formatBrlFromCents(slice.amount_cents)}
            </span>
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
  forecast = [],
}: {
  labels: string[];
  income: number[];
  expense: number[];
  forecast?: number[];
}) {
  const w = 320;
  const h = 140;
  const pad = 8;
  const max = Math.max(1, ...income, ...expense, ...forecast);
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
        {forecast.some((v) => v > 0) ? (
          <polyline
            fill="none"
            stroke="#7A756D"
            strokeWidth="2"
            strokeDasharray="5 4"
            points={points(forecast)}
          />
        ) : null}
      </svg>
      <div className="mt-2 flex justify-between text-[11px] uppercase tracking-wide text-muted">
        <span>{labels[0]}</span>
        <span>{labels[labels.length - 1]}</span>
      </div>
      <div className="mt-3 flex flex-wrap gap-4 text-xs text-navy">
        <span className="inline-flex items-center gap-1.5">
          <span className="h-1.5 w-4 rounded-full bg-[#32CD32]" /> Receitas
        </span>
        <span className="inline-flex items-center gap-1.5">
          <span className="h-1.5 w-4 rounded-full bg-gold" /> Despesas pagas
        </span>
        {forecast.some((v) => v > 0) ? (
          <span className="inline-flex items-center gap-1.5">
            <span className="h-1.5 w-4 rounded-full bg-muted" /> Previsto
          </span>
        ) : null}
      </div>
    </div>
  );
}

export function MonthBar({
  year,
  month,
  onChange,
}: {
  year: number;
  month: number;
  onChange: (next: { year: number; month: number }) => void;
}) {
  return (
    <div className="flex items-center gap-1 rounded-xl bg-navy-deep/90 p-1">
      <button
        type="button"
        className="px-3 py-2 text-cream"
        onClick={() => onChange(shiftMonth(year, month, -1))}
      >
        ‹
      </button>
      <span className="min-w-36 text-center text-sm text-cream">
        {monthLabel(year, month).replace(/^./, (ch) => ch.toLocaleUpperCase("pt-BR"))}
      </span>
      <button
        type="button"
        className="px-3 py-2 text-cream"
        onClick={() => onChange(shiftMonth(year, month, 1))}
      >
        ›
      </button>
    </div>
  );
}

export function InField({
  label,
  type = "text",
  value,
  onChange,
  required,
  hint,
  placeholder,
}: {
  label: string;
  type?: string;
  value: string;
  onChange: (value: string) => void;
  required?: boolean;
  hint?: string;
  placeholder?: string;
}) {
  return (
    <label className="block">
      <span className="mb-1 block text-xs uppercase tracking-wide text-muted">
        {label}
      </span>
      <input
        className="w-full rounded-lg border border-navy/10 bg-white px-3 py-2.5 text-sm text-navy outline-none ring-gold/40 focus:ring-2"
        type={type}
        value={value}
        required={required}
        placeholder={placeholder}
        onChange={(e) => onChange(e.target.value)}
      />
      {hint ? <span className="mt-1 block text-[11px] text-muted">{hint}</span> : null}
    </label>
  );
}

export function Widget({
  title,
  action,
  children,
}: {
  title: string;
  action?: ReactNode;
  children: ReactNode;
}) {
  return (
    <section className="flex h-full min-h-[240px] flex-col overflow-hidden rounded-xl border border-navy/8 border-l-4 border-l-gold bg-white p-4 shadow-[0_8px_24px_rgba(20,28,42,0.06)] lg:min-h-0">
      <div className="mb-4 flex items-center justify-between gap-3">
        <h2 className="font-serif text-xl text-navy-deep">{title}</h2>
        {action}
      </div>
      <div className="min-h-0 flex-1">{children}</div>
    </section>
  );
}
