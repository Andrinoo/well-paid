import { useEffect, useState, type FormEvent } from "react";
import { contributeGoal, createGoal, fetchGoals, type Goal } from "../api";
import { formatBrlFromCents } from "../format";
import {
  ApiError,
  ErrorNote,
  InField,
  MoneyForm,
  PageTitle,
  parseBrlToCents,
} from "./common";

export function GoalsPage() {
  const [rows, setRows] = useState<Goal[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [title, setTitle] = useState("");
  const [target, setTarget] = useState("");
  const [amounts, setAmounts] = useState<Record<string, string>>({});

  async function load() {
    setError(null);
    try {
      setRows(await fetchGoals());
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Falha ao carregar.");
    }
  }

  useEffect(() => {
    void load();
  }, []);

  async function onCreate(e: FormEvent) {
    e.preventDefault();
    const cents = parseBrlToCents(target);
    if (!cents) return;
    setBusy(true);
    try {
      await createGoal({ title: title.trim(), target_cents: cents });
      setTitle("");
      setTarget("");
      await load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Não foi possível criar.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="mx-auto max-w-5xl space-y-5">
      <PageTitle kicker="Módulo" title="Metas" />
      <ErrorNote message={error} />
      <MoneyForm onSubmit={onCreate} submitLabel={busy ? "…" : "Nova meta"} busy={busy} extra={
        <>
          <InField label="Título" value={title} required onChange={setTitle} />
          <InField label="Alvo" value={target} required onChange={setTarget} />
        </>
      } />
      <ul className="space-y-3">
        {rows.length === 0 ? (
          <li className="rounded-2xl border border-navy/8 bg-white/80 px-4 py-8 text-center text-sm text-muted">
            Sem metas.
          </li>
        ) : (
          rows.map((goal) => {
            const pct = Math.min(
              100,
              Math.round((goal.current_cents / Math.max(1, goal.target_cents)) * 100),
            );
            return (
              <li key={goal.id} className="rounded-2xl border border-navy/8 bg-white/80 p-4">
                <div className="flex justify-between text-sm">
                  <span className="font-medium">{goal.title}</span>
                  <span className="text-muted">
                    {formatBrlFromCents(goal.current_cents)} / {formatBrlFromCents(goal.target_cents)}
                  </span>
                </div>
                <div className="mt-2 h-2 overflow-hidden rounded-full bg-cream-muted">
                  <div className="h-full rounded-full bg-gold" style={{ width: `${pct}%` }} />
                </div>
                <form
                  className="mt-3 flex gap-2"
                  onSubmit={(e) => {
                    e.preventDefault();
                    const cents = parseBrlToCents(amounts[goal.id] ?? "");
                    if (!cents) return;
                    void contributeGoal(goal.id, cents).then(() => {
                      setAmounts((a) => ({ ...a, [goal.id]: "" }));
                      return load();
                    });
                  }}
                >
                  <input
                    className="min-w-0 flex-1 rounded-lg border border-navy/10 px-3 py-2 text-sm"
                    placeholder="Aportar"
                    value={amounts[goal.id] ?? ""}
                    onChange={(e) =>
                      setAmounts((a) => ({ ...a, [goal.id]: e.target.value }))
                    }
                  />
                  <button type="submit" className="rounded-lg bg-navy-deep px-3 py-2 text-xs text-cream">
                    Aportar
                  </button>
                </form>
              </li>
            );
          })
        )}
      </ul>
    </div>
  );
}
