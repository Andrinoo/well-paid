import { describe, expect, it } from "vitest";
import {
  amountSplit,
  amountToOwnerPercent,
  buildExpensePayload,
  defaultAmountSplit,
  percentSplit,
  type ExpenseDraft,
} from "./create-model";

const base: ExpenseDraft = {
  kind: "single",
  description: "Energia",
  amount: "100,00",
  paid: false,
  hasDue: false,
  expenseDate: "2026-09-29",
  dueDate: "2026-10-10",
  interest: "",
  installments: "2",
  frequency: "monthly",
  categoryId: "category-id",
  familyMode: false,
  isFamily: false,
  share: false,
  peerId: "",
  splitPercent: false,
  ownerPart: "",
};

describe("divisão de despesas", () => {
  it("divide centavos ímpares como o Android", () => {
    expect(defaultAmountSplit(101)).toEqual([51, 50]);
  });

  it("aceita extremos 0/100 no modo valor", () => {
    expect(amountSplit(10_000, "0")).toEqual([0, 10_000]);
    expect(amountSplit(10_000, "100,00")).toEqual([10_000, 0]);
  });

  it("deriva contraparte e centavos no modo percentual", () => {
    expect(percentSplit(100, "33,33")).toEqual({
      ownerBps: 3333,
      peerBps: 6667,
      ownerCents: 33,
      peerCents: 67,
    });
  });

  it("preserva a proporção ao trocar valor por percentual", () => {
    expect(amountToOwnerPercent(10_000, "25,00")).toBe("25,00");
  });
});
describe("payload de criação", () => {
  it("cria despesa única sem vencimento", () => {
    const result = buildExpensePayload(base);
    expect(result.ok).toBe(true);
    if (!result.ok) return;
    expect(result.payload).toMatchObject({
      amount_cents: 10_000,
      expense_date: "2026-09-29",
      start_date: "2026-09-29",
      due_date: null,
      installment_total: 1,
      recurring_frequency: null,
    });
  });

  it("ancora parcelas no primeiro vencimento", () => {
    const result = buildExpensePayload({
      ...base,
      kind: "installments",
      dueDate: "2026-10-10",
      interest: "4,5",
      installments: "12",
    });
    expect(result.ok).toBe(true);
    if (!result.ok) return;
    expect(result.payload).toMatchObject({
      expense_date: "2026-10-10",
      start_date: null,
      due_date: "2026-10-10",
      monthly_interest_bps: 450,
      installment_total: 12,
    });
  });

  it("gera divisão percentual compatível com a API", () => {
    const result = buildExpensePayload({
      ...base,
      familyMode: true,
      share: true,
      peerId: "peer-id",
      splitPercent: true,
      ownerPart: "40",
    });
    expect(result.ok).toBe(true);
    if (!result.ok) return;
    expect(result.payload).toMatchObject({
      is_shared: true,
      shared_with_user_id: "peer-id",
      split_mode: "percent",
      owner_percent_bps: 4000,
      peer_percent_bps: 6000,
    });
  });
});
