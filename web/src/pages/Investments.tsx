import { useEffect, useState, type FormEvent } from "react";
import {
  createPosition,
  fetchInvestmentOverview,
  fetchPositions,
  type InvestmentOverview,
  type InvestmentPosition,
} from "../api";
import { formatBrlFromCents } from "../format";
import {
  ApiError,
  ErrorNote,
  InField,
  MoneyForm,
  PageTitle,
  parseBrlToCents,
} from "./common";

export function InvestmentsPage() {
  const [overview, setOverview] = useState<InvestmentOverview | null>(null);
  const [rows, setRows] = useState<InvestmentPosition[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [name, setName] = useState("");
  const [principal, setPrincipal] = useState("");
  const [rate, setRate] = useState("10");
  const [type, setType] = useState("cdb");

  async function load() {
    setError(null);
    try {
      const [ov, list] = await Promise.all([
        fetchInvestmentOverview(),
        fetchPositions(),
      ]);
      setOverview(ov);
      setRows(list);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Falha ao carregar.");
    }
  }

  useEffect(() => {
    void load();
  }, []);

  async function onCreate(e: FormEvent) {
    e.preventDefault();
    const cents = parseBrlToCents(principal);
    const bps = Math.round(Number(rate.replace(",", ".")) * 100);
    if (!cents || !Number.isFinite(bps)) return;
    setBusy(true);
    try {
      await createPosition({
        instrument_type: type,
        name: name.trim(),
        principal_cents: cents,
        annual_rate_bps: bps,
      });
      setName("");
      setPrincipal("");
      await load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Não foi possível criar.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="mx-auto max-w-5xl space-y-5">
      <PageTitle kicker="Módulo" title="Investimentos" />
      <ErrorNote message={error} />
      {overview ? (
        <div className="grid gap-3 sm:grid-cols-3">
          {[
            ["Alocado", overview.total_allocated_cents],
            ["Rendimento", overview.total_yield_cents],
            ["Estimado / mês", overview.estimated_monthly_yield_cents],
          ].map(([label, cents]) => (
            <div key={String(label)} className="rounded-2xl bg-navy-deep px-4 py-4 text-cream">
              <p className="text-[11px] uppercase tracking-wider text-gold/80">{label}</p>
              <p className="mt-1 font-serif text-2xl">
                {formatBrlFromCents(cents as number)}
              </p>
            </div>
          ))}
        </div>
      ) : null}
      <MoneyForm onSubmit={onCreate} submitLabel={busy ? "…" : "Nova posição"} busy={busy} extra={
        <>
          <InField label="Nome" value={name} required onChange={setName} />
          <InField label="Principal" value={principal} required onChange={setPrincipal} />
          <InField label="% a.a." value={rate} required onChange={setRate} />
          <label className="block">
            <span className="mb-1 block text-xs uppercase tracking-wide text-muted">Tipo</span>
            <select
              className="w-full rounded-lg border border-navy/10 bg-white px-3 py-2.5 text-sm"
              value={type}
              onChange={(e) => setType(e.target.value)}
            >
              <option value="cdb">CDB</option>
              <option value="treasury">Tesouro</option>
              <option value="stock">Acção</option>
              <option value="crypto">Cripto</option>
            </select>
          </label>
        </>
      } />
      <ul className="divide-y divide-navy/8 overflow-hidden rounded-2xl border border-navy/8 bg-white/80">
        {rows.length === 0 ? (
          <li className="px-4 py-8 text-center text-sm text-muted">Sem posições.</li>
        ) : (
          rows.map((row) => (
            <li key={row.id} className="flex justify-between px-4 py-3 text-sm">
              <div>
                <p>{row.name}</p>
                <p className="text-xs text-muted">
                  {row.instrument_type} · {(row.annual_rate_bps / 100).toFixed(2)}% a.a.
                </p>
              </div>
              <span className="font-medium">{formatBrlFromCents(row.principal_cents)}</span>
            </li>
          ))
        )}
      </ul>
    </div>
  );
}
