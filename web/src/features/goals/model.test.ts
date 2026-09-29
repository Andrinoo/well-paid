import { describe, expect, it } from "vitest";
import { buildGoalPayload, canDeleteGoal, goalProgress, type GoalDraft } from "./model";

const draft: GoalDraft = {
  title: "Notebook",
  target: "5.000,00",
  initial: "500,00",
  description: "Equipamento de trabalho",
  dueDate: "2027-01-15",
  isActive: true,
  isFamily: false,
  trackingEnabled: true,
  targetUrl: "",
  picked: {
    title: "Notebook Pro",
    price_cents: 500_000,
    currency_id: "BRL",
    url: "https://example.com/notebook",
    thumbnail: "https://images.example.com/notebook.jpg",
    source: "google_shopping",
  },
};

describe("payload de metas", () => {
  it("preserva produto, miniatura e valor inicial na criação", () => {
    const result = buildGoalPayload(draft);
    expect(result.ok).toBe(true);
    if (!result.ok) return;
    expect(result.payload).toMatchObject({
      title: "Notebook",
      target_cents: 500_000,
      current_cents: 50_000,
      target_url: "https://example.com/notebook",
      reference_product_name: "Notebook Pro",
      reference_thumbnail_url: "https://images.example.com/notebook.jpg",
      due_at: "2027-01-15T00:00:00Z",
    });
  });

  it("não sobrescreve saldo atual durante edição", () => {
    const result = buildGoalPayload(
      { ...draft, initial: "" },
      {
        id: "goal",
        title: "Antiga",
        target_cents: 100,
        current_cents: 50,
        is_active: true,
      },
    );
    expect(result.ok).toBe(true);
    if (!result.ok) return;
    expect(result.payload).not.toHaveProperty("current_cents");
  });
});
describe("regras da lista", () => {
  it("limita o progresso visual a 100%", () => {
    expect(goalProgress({ current_cents: 200, target_cents: 100 })).toBe(100);
  });

  it("só permite apagar meta própria sem saldo", () => {
    expect(canDeleteGoal({ id: "1", title: "x", target_cents: 100, current_cents: 0, is_active: true })).toBe(true);
    expect(canDeleteGoal({ id: "2", title: "x", target_cents: 100, current_cents: 1, is_active: true })).toBe(false);
    expect(canDeleteGoal({ id: "3", title: "x", target_cents: 100, current_cents: 0, is_active: true, is_mine: false })).toBe(false);
  });
});
