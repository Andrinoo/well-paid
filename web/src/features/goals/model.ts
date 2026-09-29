import { parseBrlToCents } from "../../format";
import type { Goal, GoalProductHit } from "../../api";

export type GoalDraft = {
  title: string;
  target: string;
  initial: string;
  description: string;
  dueDate: string;
  isActive: boolean;
  isFamily: boolean;
  trackingEnabled: boolean;
  targetUrl: string;
  picked: GoalProductHit | null;
};

export type GoalPayload = {
  title: string;
  target_cents: number;
  current_cents?: number;
  is_active: boolean;
  is_family: boolean;
  target_url: string | null;
  reference_product_name: string | null;
  reference_price_cents: number | null;
  reference_currency: string;
  price_source: string | null;
  reference_thumbnail_url: string | null;
  description: string | null;
  due_at: string | null;
  price_check_interval_hours: number;
  tracking_enabled: boolean;
};

export type BuildGoalResult =
  | { ok: true; payload: GoalPayload }
  | { ok: false; message: string; field: string };

export function buildGoalPayload(draft: GoalDraft, editing?: Goal): BuildGoalResult {
  const title = draft.title.trim();
  if (!title) return invalid("Indique um título para a meta.", "title");
  if (title.length > 200) return invalid("O título pode ter no máximo 200 caracteres.", "title");

  const targetCents = parseBrlToCents(draft.target);
  if (targetCents <= 0) return invalid("Indique um valor-alvo maior que zero.", "target");

  let initialCents = 0;
  if (!editing && draft.initial.trim()) {
    initialCents = parseBrlToCents(draft.initial);
    if (initialCents <= 0) return invalid("Indique um valor inicial válido.", "initial");
  }
  if (draft.description.trim().length > 1000) {
    return invalid("A descrição pode ter no máximo 1000 caracteres.", "description");
  }
  if (draft.dueDate && !isIsoDate(draft.dueDate)) {
    return invalid("Indique uma data-alvo válida.", "dueDate");
  }

  const picked = draft.picked;
  const persistedUrl = normalizeHttpUrl(draft.targetUrl || picked?.url || editing?.target_url);
  return {
    ok: true,
    payload: {
      title,
      target_cents: targetCents,
      ...(!editing ? { current_cents: initialCents } : {}),
      is_active: draft.isActive,
      is_family: draft.isFamily,
      target_url: persistedUrl,
      reference_product_name: picked?.title ?? editing?.reference_product_name ?? null,
      reference_price_cents: picked?.price_cents ?? editing?.reference_price_cents ?? null,
      reference_currency: picked?.currency_id ?? editing?.reference_currency ?? "BRL",
      price_source: picked?.source ?? editing?.price_source ?? null,
      reference_thumbnail_url: picked?.thumbnail ?? editing?.reference_thumbnail_url ?? null,
      description: draft.description.trim() || null,
      due_at: draft.dueDate ? `${draft.dueDate}T00:00:00Z` : null,
      price_check_interval_hours: 6,
      tracking_enabled: draft.trackingEnabled,
    },
  };
}
export function goalProgress(goal: Pick<Goal, "current_cents" | "target_cents">): number {
  return Math.min(100, Math.max(0, Math.round((goal.current_cents / Math.max(1, goal.target_cents)) * 100)));
}

export function canDeleteGoal(goal: Goal): boolean {
  return goal.is_mine !== false && goal.current_cents === 0;
}

export function dateInputValue(raw: string | null | undefined): string {
  return raw?.slice(0, 10) ?? "";
}

function normalizeHttpUrl(raw: string | null | undefined): string | null {
  const value = (raw ?? "").trim();
  if (!value) return null;
  return /^https?:\/\//i.test(value) ? value : null;
}

function invalid(message: string, field: string): BuildGoalResult {
  return { ok: false, message, field };
}

function isIsoDate(value: string): boolean {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return false;
  const date = new Date(`${value}T12:00:00Z`);
  return !Number.isNaN(date.getTime()) && date.toISOString().slice(0, 10) === value;
}
