import { parseBrlToCents, parsePercentToBps } from "../../format";

export type ExpenseKind = "single" | "installments" | "recurring";
export type ExpenseFrequency = "monthly" | "weekly" | "yearly";

export type ExpenseCreatePayload = {
  description: string;
  amount_cents: number;
  expense_date: string;
  due_date: string | null;
  category_id: string;
  status: "pending" | "paid";
  monthly_interest_bps: number | null;
  start_date: string | null;
  installment_total: number;
  recurring_frequency: ExpenseFrequency | null;
  is_shared: boolean;
  is_family: boolean;
  shared_with_user_id?: string | null;
  split_mode?: "amount" | "percent" | null;
  owner_share_cents?: number | null;
  peer_share_cents?: number | null;
  owner_percent_bps?: number | null;
  peer_percent_bps?: number | null;
};

export type ExpenseDraft = {
  kind: ExpenseKind;
  description: string;
  amount: string;
  paid: boolean;
  hasDue: boolean;
  expenseDate: string;
  dueDate: string;
  interest: string;
  installments: string;
  frequency: ExpenseFrequency;
  categoryId: string;
  familyMode: boolean;
  isFamily: boolean;
  share: boolean;
  peerId: string;
  splitPercent: boolean;
  ownerPart: string;
};

export type BuildExpenseResult =
  | { ok: true; payload: ExpenseCreatePayload }
  | { ok: false; message: string; field?: string };

export function formatBrlInput(cents: number): string {
  return (Math.max(0, cents) / 100).toFixed(2).replace(".", ",");
}
export function formatPercentInput(bps: number): string {
  return (Math.min(10_000, Math.max(0, bps)) / 100).toFixed(2).replace(".", ",");
}

export function defaultAmountSplit(totalCents: number): [number, number] {
  const owner = Math.ceil(Math.max(0, totalCents) / 2);
  return [owner, Math.max(0, totalCents - owner)];
}

export function amountSplit(totalCents: number, ownerText: string): [number, number] | null {
  const owner = ownerText.trim() === "" ? null : parseNonNegativeBrlToCents(ownerText);
  if (owner == null || owner < 0 || owner > totalCents) return null;
  return [owner, totalCents - owner];
}

export function percentSplit(totalCents: number, ownerPercentText: string): {
  ownerBps: number;
  peerBps: number;
  ownerCents: number;
  peerCents: number;
} | null {
  const ownerBps = parsePercentToBps(ownerPercentText);
  if (ownerBps == null) return null;
  const ownerCents = Math.floor((totalCents * ownerBps + 5_000) / 10_000);
  return {
    ownerBps,
    peerBps: 10_000 - ownerBps,
    ownerCents,
    peerCents: totalCents - ownerCents,
  };
}

export function amountToOwnerPercent(totalCents: number, ownerText: string): string {
  const owner = parseNonNegativeBrlToCents(ownerText);
  if (owner == null || totalCents <= 0) return "50,00";
  const bps = Math.min(10_000, Math.max(0, Math.floor((owner * 10_000) / totalCents)));
  return formatPercentInput(bps);
}

export function buildExpensePayload(draft: ExpenseDraft): BuildExpenseResult {
  const description = draft.description.trim();
  if (!description) return invalid("Indique uma descrição.", "description");
  const amountCents = parseBrlToCents(draft.amount);
  if (amountCents <= 0) return invalid("Indique um valor válido maior que zero.", "amount");
  if (!draft.categoryId) return invalid("Escolha uma categoria.", "categoryId");
  if (!isIsoDate(draft.expenseDate)) return invalid("Indique uma data da despesa válida.", "expenseDate");

  const needsDue = draft.kind !== "single" || draft.hasDue;
  if (needsDue && !isIsoDate(draft.dueDate)) {
    return invalid("Indique uma data de vencimento válida.", "dueDate");
  }

  let installmentTotal = 1;
  let interestBps: number | null = null;
  if (draft.kind === "installments") {
    installmentTotal = Number(draft.installments);
    if (!Number.isInteger(installmentTotal) || installmentTotal < 2 || installmentTotal > 999) {
      return invalid("Indique um número de parcelas entre 2 e 999.", "installments");
    }
    interestBps = parsePercentToBps(draft.interest);
    if (interestBps == null) {
      return invalid("Parcelamento exige juro mensal entre 0% e 100%.", "interest");
    }
  }

  const dueDate = needsDue ? draft.dueDate : null;
  const payload: ExpenseCreatePayload = {
    description,
    amount_cents: amountCents,
    expense_date: draft.kind === "installments" ? draft.dueDate : draft.expenseDate,
    start_date: draft.kind === "installments" ? null : draft.expenseDate,
    due_date: dueDate,
    category_id: draft.categoryId,
    status: draft.paid ? "paid" : "pending",
    installment_total: installmentTotal,
    recurring_frequency: draft.kind === "recurring" ? draft.frequency : null,
    monthly_interest_bps: interestBps,
    is_family: draft.familyMode && draft.isFamily,
    is_shared: draft.familyMode && draft.share,
  };

  if (payload.is_shared) {
    if (!draft.peerId) return invalid("Escolha um membro da família para dividir.", "peerId");
    payload.shared_with_user_id = draft.peerId;
    if (draft.splitPercent) {
      const split = percentSplit(amountCents, draft.ownerPart || "50");
      if (!split) return invalid("Indique uma percentagem entre 0 e 100.", "ownerPart");
      payload.split_mode = "percent";
      payload.owner_percent_bps = split.ownerBps;
      payload.peer_percent_bps = split.peerBps;
    } else {
      const split = amountSplit(amountCents, draft.ownerPart);
      if (!split) return invalid("A sua parte deve estar entre R$ 0,00 e o total.", "ownerPart");
      payload.split_mode = "amount";
      payload.owner_share_cents = split[0];
      payload.peer_share_cents = split[1];
    }
  }

  return { ok: true, payload };
}

function invalid(message: string, field?: string): BuildExpenseResult {
  return { ok: false, message, field };
}

function isIsoDate(value: string): boolean {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return false;
  const date = new Date(`${value}T12:00:00`);
  return !Number.isNaN(date.getTime()) && date.toISOString().slice(0, 10) === value;
}

function parseNonNegativeBrlToCents(raw: string): number | null {
  const cleaned = raw.trim().replace(/\s/g, "").replace("R$", "");
  if (!cleaned) return null;
  const normalized = cleaned.includes(",")
    ? cleaned.replace(/\./g, "").replace(",", ".")
    : cleaned;
  const value = Number(normalized);
  if (!Number.isFinite(value) || value < 0) return null;
  return Math.round(value * 100);
}
