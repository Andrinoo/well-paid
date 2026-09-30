import { useState, type FormEvent, type ReactNode } from "react";
import { ApiError, type Category } from "../api";
import { maskBrlInput, parseBrlToCents, todayIso } from "../format";
import { InField, MonthBar } from "../ui";

export function usePeriod() {
  const now = new Date();
  return useState({ year: now.getFullYear(), month: now.getMonth() + 1 });
}

export function ErrorNote({ message }: { message: string | null }) {
  if (!message) return null;
  return (
    <p className="rounded-xl border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800">
      {message}
    </p>
  );
}

export function PageTitle({
  kicker,
  title,
}: {
  kicker: string;
  title: string;
}) {
  return (
    <div>
      <p className="text-[11px] uppercase tracking-[0.28em] text-muted">{kicker}</p>
      <h1 className="mt-1 font-serif text-3xl text-navy-deep">{title}</h1>
    </div>
  );
}

export function MoneyForm({
  onSubmit,
  extra,
  submitLabel,
  busy,
}: {
  onSubmit: (e: FormEvent) => void;
  extra?: ReactNode;
  submitLabel: string;
  busy: boolean;
}) {
  return (
    <form
      className="grid gap-3 rounded-2xl border border-navy/8 bg-white/80 p-4 sm:grid-cols-2 lg:grid-cols-4"
      onSubmit={onSubmit}
    >
      {extra}
      <div className="flex items-end">
        <button
          type="submit"
          disabled={busy}
          className="w-full rounded-lg bg-gold py-2.5 text-sm font-semibold text-navy-deep disabled:opacity-60"
        >
          {submitLabel}
        </button>
      </div>
    </form>
  );
}

export function CategorySelect({
  label,
  value,
  categories,
  onChange,
}: {
  label: string;
  value: string;
  categories: Category[];
  onChange: (id: string) => void;
}) {
  return (
    <label className="block">
      <span className="mb-1 block text-xs uppercase tracking-wide text-muted">
        {label}
      </span>
      <select
        className="w-full rounded-lg border border-navy/10 bg-white px-3 py-2.5 text-sm"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        required
      >
        <option value="">Escolher</option>
        {categories.map((c) => (
          <option key={c.id} value={c.id}>
            {c.name}
          </option>
        ))}
      </select>
    </label>
  );
}

export function SwitchRow({
  label,
  sub,
  checked,
  onChange,
}: {
  label: string;
  sub?: string;
  checked: boolean;
  onChange: (next: boolean) => void;
}) {
  return (
    <label className="flex cursor-pointer items-center justify-between gap-4 rounded-xl border border-navy/8 bg-cream/40 px-3.5 py-3 transition hover:border-teal/25 hover:bg-teal/[0.035]">
      <span className="min-w-0 flex-1">
        <span className="block text-sm font-medium text-navy">{label}</span>
        {sub ? <span className="mt-0.5 block text-[11px] text-muted">{sub}</span> : null}
      </span>
      <input
        type="checkbox"
        role="switch"
        className="peer sr-only"
        checked={checked}
        onChange={(e) => onChange(e.target.checked)}
      />
      <span
        aria-hidden="true"
        className="relative h-7 w-12 shrink-0 rounded-full bg-navy/20 transition-colors peer-checked:bg-teal peer-focus-visible:outline peer-focus-visible:outline-2 peer-focus-visible:outline-offset-2 peer-focus-visible:outline-teal after:absolute after:left-1 after:top-1 after:h-5 after:w-5 after:rounded-full after:bg-white after:shadow-sm after:transition-transform peer-checked:after:translate-x-5"
      />
    </label>
  );
}

export function ChipRow<T extends string>({
  value,
  onChange,
  options,
}: {
  value: T;
  onChange: (next: T) => void;
  options: { id: T; label: string }[];
}) {
  return (
    <div className="flex flex-wrap gap-2">
      {options.map((opt) => (
        <button
          key={opt.id}
          type="button"
          className={`rounded-full px-3 py-1.5 text-sm ${
            value === opt.id
              ? "bg-navy-deep text-cream"
              : "border border-navy/10 bg-white text-navy hover:bg-cream-muted"
          }`}
          onClick={() => onChange(opt.id)}
        >
          {opt.label}
        </button>
      ))}
    </div>
  );
}

export { InField, MonthBar, maskBrlInput, parseBrlToCents, todayIso, ApiError };
