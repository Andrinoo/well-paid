import { useEffect, useState, type FormEvent } from "react";
import { contributeReserve, createReservePlan, fetchReservePlans, type ReservePlan } from "../api";
import { formatBrlFromCents, todayIso } from "../format";
import {
  ApiError,
  ErrorNote,
  InField,
  MoneyForm,
  PageTitle,
  parseBrlToCents,
} from "./common";

export function ReservePage() {
  const [rows, setRows] = useState<ReservePlan[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [title, setTitle] = useState("");
  const [monthly, setMonthly] = useState("");
  const [amounts, setAmounts] = useState<Record<string, string>>({});

  async function load() {
    setError(null);
    try {
      setRows(await fetchReservePlans());
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Falha ao carregar.");
    }
  }

  useEffect(() => {
    void load();
  }, []);

  async function onCreate(e: FormEvent) {
    e.preventDefault();
    const cents = parseBrlToCents(monthly);
    setBusy(true);
    try {
      await createReservePlan({
        title: title.trim() || "Reserva",
        monthly_target_cents: cents,
      });
      setTitle("");
      setMonthly("");
      await load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Não foi possível criar.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="mx-auto max-w-5xl space-y-5">
      <PageTitle kicker="Módulo" title="Reserva de emergência" />
      <ErrorNote message={error} />
      <MoneyForm onSubmit={onCreate} submitLabel={busy ? "…" : "Novo plano"} busy={busy} extra={
        <>
          <InField label="Nome" value={title} onChange={setTitle} />
          <InField label="Meta mensal" value={monthly} required onChange={setMonthly} />
        </>
      } />
      <ul className="space-y-3">
        {rows.length === 0 ? (
          <li className="rounded-2xl border border-navy/8 bg-white/80 px-4 py-8 text-center text-sm text-muted">
            Sem planos.
          </li>
        ) : (
          rows.map((plan) => (
            <li key={plan.id} className="rounded-2xl border border-navy/8 bg-white/80 p-4">
              <div className="flex justify-between text-sm">
                <span className="font-medium">{plan.title}</span>
                <span className="text-muted">{plan.status}</span>
              </div>
              <p className="mt-2 text-sm">
                Saldo {formatBrlFromCents(plan.balance_cents)} · meta mensal{" "}
                {formatBrlFromCents(plan.monthly_target_cents)}
              </p>
              <form
                className="mt-3 flex gap-2"
                onSubmit={(e) => {
                  e.preventDefault();
                  const cents = parseBrlToCents(amounts[plan.id] ?? "");
                  if (!cents) return;
                  void contributeReserve(plan.id, cents, todayIso()).then(() => {
                    setAmounts((a) => ({ ...a, [plan.id]: "" }));
                    return load();
                  });
                }}
              >
                <input
                  className="min-w-0 flex-1 rounded-lg border border-navy/10 px-3 py-2 text-sm"
                  placeholder="Aportar"
                  value={amounts[plan.id] ?? ""}
                  onChange={(e) =>
                    setAmounts((a) => ({ ...a, [plan.id]: e.target.value }))
                  }
                />
                <button type="submit" className="rounded-lg bg-navy-deep px-3 py-2 text-xs text-cream">
                  Aportar
                </button>
              </form>
            </li>
          ))
        )}
      </ul>
    </div>
  );
}
