import { useEffect, useState, type FormEvent } from "react";
import {
  createIncome,
  deleteIncome,
  fetchIncomeCategories,
  fetchIncomes,
  type Category,
  type Income,
} from "../api";
import { formatBrlFromCents, formatDueDate } from "../format";
import {
  ApiError,
  CategorySelect,
  ErrorNote,
  InField,
  MonthBar,
  PageTitle,
  parseBrlToCents,
  todayIso,
  usePeriod,
} from "./common";

export function IncomesPage() {
  const [period, setPeriod] = usePeriod();
  const [rows, setRows] = useState<Income[]>([]);
  const [cats, setCats] = useState<Category[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [description, setDescription] = useState("");
  const [amount, setAmount] = useState("");
  const [date, setDate] = useState(todayIso());
  const [notes, setNotes] = useState("");
  const [categoryId, setCategoryId] = useState("");

  async function load() {
    setError(null);
    try {
      const [list, categories] = await Promise.all([
        fetchIncomes(period.year, period.month),
        fetchIncomeCategories(),
      ]);
      setRows(list);
      setCats(categories);
      if (!categoryId && categories[0]) setCategoryId(categories[0].id);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Falha ao carregar.");
    }
  }

  useEffect(() => {
    void load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [period.year, period.month]);

  async function onCreate(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (!description.trim()) {
      setError("Indique uma descrição.");
      return;
    }
    const cents = parseBrlToCents(amount);
    if (!cents) {
      setError("Indique um valor válido maior que zero.");
      return;
    }
    if (!categoryId) {
      setError("Escolha uma categoria.");
      return;
    }
    setBusy(true);
    try {
      await createIncome({
        description: description.trim(),
        amount_cents: cents,
        income_date: date,
        income_category_id: categoryId,
        notes: notes.trim() || null,
      });
      setDescription("");
      setAmount("");
      setNotes("");
      await load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Não foi possível criar.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="mx-auto max-w-5xl space-y-5">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <PageTitle kicker="Módulo" title="Proventos" />
        <MonthBar year={period.year} month={period.month} onChange={setPeriod} />
      </div>
      <ErrorNote message={error} />
      <form
        className="space-y-4 rounded-2xl border border-navy/8 bg-white p-4 shadow-[0_8px_24px_rgba(20,28,42,0.04)]"
        onSubmit={onCreate}
      >
        <p className="font-serif text-xl text-navy-deep">Novo provento</p>
        <InField label="Descrição" value={description} required onChange={setDescription} />
        <div className="grid gap-3 sm:grid-cols-2">
          <InField
            label="Valor (R$)"
            value={amount}
            required
            placeholder="Ex.: 12,50 ou 1234,56"
            onChange={setAmount}
          />
          <InField
            label="Data do provento"
            type="date"
            value={date}
            required
            onChange={setDate}
          />
        </div>
        <InField label="Notas" value={notes} hint="Opcional." onChange={setNotes} />
        <CategorySelect
          label="Categoria"
          value={categoryId}
          categories={cats}
          onChange={setCategoryId}
        />
        <button
          type="submit"
          disabled={busy}
          className="rounded-lg bg-gold px-8 py-3 text-sm font-semibold text-navy-deep disabled:opacity-60"
        >
          {busy ? "A guardar…" : "Guardar"}
        </button>
      </form>
      <ul className="divide-y divide-navy/8 overflow-hidden rounded-2xl border border-navy/8 bg-white">
        {rows.length === 0 ? (
          <li className="px-4 py-8 text-center text-sm text-muted">Sem proventos neste mês.</li>
        ) : (
          rows.map((row) => (
            <li key={row.id} className="flex items-center justify-between gap-3 px-4 py-3 text-sm">
              <div>
                <p>{row.description}</p>
                <p className="text-xs text-muted">
                  {row.category_name} · {formatDueDate(row.income_date)}
                  {row.notes ? ` · ${row.notes}` : ""}
                </p>
              </div>
              <div className="flex items-center gap-2">
                <span className="font-medium">{formatBrlFromCents(row.amount_cents)}</span>
                <button
                  type="button"
                  className="text-xs text-muted hover:text-red-700"
                  onClick={() => void deleteIncome(row.id).then(load)}
                >
                  Apagar
                </button>
              </div>
            </li>
          ))
        )}
      </ul>
    </div>
  );
}
