export function formatBrlFromCents(cents: number): string {
  return (cents / 100).toLocaleString("pt-BR", {
    style: "currency",
    currency: "BRL",
  });
}

export function monthLabel(year: number, month: number): string {
  const date = new Date(year, month - 1, 1);
  return date.toLocaleDateString("pt-BR", { month: "long", year: "numeric" });
}

export function shortMonth(year: number, month: number): string {
  const date = new Date(year, month - 1, 1);
  return date.toLocaleDateString("pt-BR", { month: "short" }).replace(".", "");
}

export function todayIso(): string {
  const d = new Date();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${d.getFullYear()}-${m}-${day}`;
}

export function parseBrlToCents(raw: string): number {
  const cleaned = raw.trim().replace(/\s/g, "").replace("R$", "");
  if (!cleaned) return 0;
  const normalized = cleaned.includes(",")
    ? cleaned.replace(/\./g, "").replace(",", ".")
    : cleaned;
  const value = Number(normalized);
  if (!Number.isFinite(value) || value <= 0) return 0;
  return Math.round(value * 100);
}

export function parsePercentToBps(raw: string): number | null {
  const cleaned = raw.trim().replace("%", "").replace(",", ".");
  if (!cleaned) return null;
  const value = Number(cleaned);
  if (!Number.isFinite(value) || value < 0 || value > 100) return null;
  return Math.round(value * 100);
}

export function shiftMonth(year: number, month: number, delta: number) {
  const d = new Date(year, month - 1 + delta, 1);
  return { year: d.getFullYear(), month: d.getMonth() + 1 };
}

export function formatDueDate(iso: string | null | undefined): string {
  if (!iso) return "Sem vencimento";
  const [year, month, day] = iso.slice(0, 10).split("-");
  if (!year || !month || !day) return iso;
  return `${day}/${month}/${year}`;
}

export function greetingFirstName(user: {
  display_name: string | null;
  full_name: string | null;
  email: string;
}): string | null {
  const uuidish = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
  const title = (raw: string) =>
    raw.charAt(0).toLocaleUpperCase("pt-BR") + raw.slice(1);
  const custom = user.display_name?.trim() ?? "";
  if (custom && !uuidish.test(custom)) return custom;
  const fromFull = user.full_name?.trim() ?? "";
  if (fromFull) {
    const first = fromFull.split(/\s+/)[0] ?? "";
    if (first && !uuidish.test(first)) return title(first);
  }
  const local = user.email.split("@")[0]?.trim() ?? "";
  if (!local || uuidish.test(local)) return null;
  return title(local);
}

export function daysUntil(iso: string | null | undefined): number | null {
  if (!iso) return null;
  const due = new Date(`${iso.slice(0, 10)}T12:00:00`);
  if (Number.isNaN(due.getTime())) return null;
  const today = new Date();
  today.setHours(12, 0, 0, 0);
  return Math.round((due.getTime() - today.getTime()) / 86_400_000);
}
