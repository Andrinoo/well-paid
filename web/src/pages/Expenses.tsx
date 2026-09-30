import { useEffect, useMemo, useState, type FormEvent } from "react";
import { Link, useSearchParams } from "react-router-dom";
import {
  deleteExpense,
  fetchCategories,
  fetchExpenses,
  payExpense,
  updateExpense,
  type Category,
  type Expense,
} from "../api";
import { formatBrlFromCents, formatDueDate, maskBrlInput, parseBrlToCents } from "../format";
import { ErrorNote, MonthBar, PageTitle } from "./common";
import { ExpenseCreateForm } from "./ExpenseForm";

type StatusFilter = "all" | "pending" | "paid";
type SortKey = "date" | "description" | "amount" | "status";

export function ExpensesPage() {
  const [params] = useSearchParams();
  const pendingOnly = params.get("filtro") === "pagar";
  const now = new Date();
  const requestedYear = Number(params.get("ano"));
  const requestedMonth = Number(params.get("mes"));
  const [period, setPeriod] = useState({
    year: Number.isInteger(requestedYear) && requestedYear >= 2000 && requestedYear <= 2100 ? requestedYear : now.getFullYear(),
    month: Number.isInteger(requestedMonth) && requestedMonth >= 1 && requestedMonth <= 12 ? requestedMonth : now.getMonth() + 1,
  });
  const [rows, setRows] = useState<Expense[]>([]);
  const [cats, setCats] = useState<Category[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState<StatusFilter>(pendingOnly ? "pending" : "all");
  const [category, setCategory] = useState("all");
  const [sort, setSort] = useState<SortKey>("date");
  const [descending, setDescending] = useState(true);
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [working, setWorking] = useState(false);
  const [editing, setEditing] = useState<Expense | null>(null);

  async function load() {
    setLoading(true);
    setError(null);
    try {
      const [list, categories] = await Promise.all([
        fetchExpenses(period.year, period.month),
        fetchCategories(),
      ]);
      setRows(list);
      setCats(categories);
      setSelected(new Set());
    } catch (err) {
      setError(err instanceof Error ? err.message : "Falha ao carregar.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [period.year, period.month]);

  useEffect(() => {
    if (pendingOnly) setStatus("pending");
  }, [pendingOnly]);

  useEffect(() => {
    const categoryKey = params.get("categoria");
    if (!categoryKey || !cats.length) return;
    const match = cats.find((item) => item.key === categoryKey);
    if (match) setCategory(match.id);
  }, [cats, params]);

  const visible = useMemo(() => {
    const term = search.trim().toLocaleLowerCase("pt-BR");
    return rows
      .filter((row) => !pendingOnly || row.status !== "paid")
      .filter((row) => status === "all" || (status === "paid" ? row.status === "paid" : row.status !== "paid"))
      .filter((row) => category === "all" || row.category_id === category)
      .filter((row) => !term || `${row.description} ${row.category_name}`.toLocaleLowerCase("pt-BR").includes(term))
      .sort((a, b) => {
        let value = 0;
        if (sort === "amount") value = a.amount_cents - b.amount_cents;
        else if (sort === "description") value = a.description.localeCompare(b.description, "pt-BR");
        else if (sort === "status") value = a.status.localeCompare(b.status);
        else value = (a.due_date || a.expense_date).localeCompare(b.due_date || b.expense_date);
        return descending ? -value : value;
      });
  }, [rows, pendingOnly, status, category, search, sort, descending]);

  const pendingRows = rows.filter((row) => row.status !== "paid");
  const totals = {
    all: rows.reduce((sum, row) => sum + row.amount_cents, 0),
    pending: pendingRows.reduce((sum, row) => sum + row.amount_cents, 0),
    paid: rows.filter((row) => row.status === "paid").reduce((sum, row) => sum + row.amount_cents, 0),
  };
  const selectedRows = rows.filter((row) => selected.has(row.id));
  const selectable = visible.filter(canManage);
  const allSelected = selectable.length > 0 && selectable.every((row) => selected.has(row.id));

  function toggleRow(id: string) {
    setSelected((current) => {
      const next = new Set(current);
      if (next.has(id)) next.delete(id); else next.add(id);
      return next;
    });
  }

  async function markPaid(targets: Expense[]) {
    const pending = targets.filter((row) => row.status !== "paid" && canManage(row));
    if (!pending.length) return;
    setWorking(true);
    setError(null);
    try {
      for (const row of pending) await payExpense(row.id);
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Falha ao registrar pagamento.");
    } finally {
      setWorking(false);
    }
  }

  async function remove(targets: Expense[]) {
    const manageable = targets.filter(canManage);
    if (!manageable.length) return;
    const plans = manageable.filter((row) => row.installment_group_id || row.recurring_series_id).length;
    const detail = plans ? ` ${plans} item(ns) pertencem a planos; o plano completo será removido.` : "";
    if (!window.confirm(`Apagar ${manageable.length} despesa(s)?${detail} Esta ação não pode ser desfeita.`)) return;
    setWorking(true);
    setError(null);
    try {
      for (const row of manageable) {
        await deleteExpense(row.id, {
          target: row.installment_group_id || row.recurring_series_id ? "series" : "occurrence",
          scope: "all",
        });
      }
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Falha ao apagar.");
    } finally {
      setWorking(false);
    }
  }

  function exportCsv() {
    const lines = [
      ["Data", "Descrição", "Categoria", "Valor", "Status", "Parcela", "Recorrência"],
      ...visible.map((row) => [
        row.due_date || row.expense_date,
        row.description,
        row.category_name,
        (row.amount_cents / 100).toFixed(2).replace(".", ","),
        row.status === "paid" ? "Paga" : "Pendente",
        row.installment_total && row.installment_total > 1 ? `${row.installment_number || 1}/${row.installment_total}` : "",
        row.recurring_frequency ? freqLabel(row.recurring_frequency) : "",
      ]),
    ];
    const csv = lines.map((line) => line.map((cell) => `"${String(cell).replaceAll('"', '""')}"`).join(";")).join("\n");
    const url = URL.createObjectURL(new Blob(["\ufeff", csv], { type: "text/csv;charset=utf-8" }));
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = `despesas-${period.year}-${String(period.month).padStart(2, "0")}.csv`;
    anchor.click();
    URL.revokeObjectURL(url);
  }

  return (
    <div className="-mx-4 -my-5 min-h-full bg-gradient-to-br from-paper via-paper to-sky/45 px-4 py-5 sm:-mx-6 sm:px-6">
      <div className="flex w-full flex-wrap items-end justify-between gap-4">
        <PageTitle kicker="Controle mensal" title={pendingOnly ? "Contas a pagar" : "Despesas"} />
        <div className="flex flex-wrap items-center gap-2">
          <MonthBar year={period.year} month={period.month} onChange={setPeriod} />
          {!pendingOnly ? <button type="button" className="rounded-xl bg-gold px-4 py-2.5 text-sm font-bold text-navy-deep shadow-sm transition hover:-translate-y-0.5" onClick={() => setShowCreate((value) => !value)}>{showCreate ? "Fechar cadastro" : "+ Nova despesa"}</button> : null}
        </div>
      </div>
      {pendingOnly ? <p className="mt-3 text-sm text-muted">Mostrando somente compromissos pendentes. <Link to="/app/despesas" className="font-semibold text-teal-deep">Ver todas</Link></p> : null}
      <div className="mt-4"><ErrorNote message={error} /></div>

      {!pendingOnly && showCreate ? <div className="mt-4 wp-rise"><ExpenseCreateForm categories={cats} onCreated={async () => { await load(); setShowCreate(false); }} /></div> : null}

      <section className="mt-4 grid gap-3 sm:grid-cols-3">
        <SummaryCard label="Total do mês" value={totals.all} detail={`${rows.length} lançamentos`} tone="navy" />
        <SummaryCard label="Pago" value={totals.paid} detail={`${rows.filter((row) => row.status === "paid").length} concluídas`} tone="green" />
        <SummaryCard label="Pendente" value={totals.pending} detail={`${pendingRows.length} compromissos`} tone="gold" />
      </section>

      <section className="mt-4 overflow-hidden rounded-[1.6rem] border border-navy/8 bg-white/90 shadow-[0_18px_55px_rgba(20,28,42,0.08)] backdrop-blur-sm">
        <div className="border-b border-navy/8 bg-gradient-to-r from-white via-white to-sage/45 p-4 sm:p-5">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div><h2 className="font-serif text-xl text-navy-deep">Central de lançamentos</h2><p className="mt-0.5 text-xs text-muted">Filtre, ordene e execute ações sem sair da lista.</p></div>
            <button type="button" onClick={exportCsv} disabled={!visible.length} className="rounded-xl border border-navy/10 px-3 py-2 text-xs font-semibold text-navy transition hover:border-teal/40 hover:text-teal-deep disabled:opacity-40">Exportar CSV</button>
          </div>
          <div className="mt-4 grid gap-2 md:grid-cols-[minmax(220px,1fr)_160px_190px_170px]">
            <label className="relative"><span className="sr-only">Buscar despesas</span><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Buscar descrição ou categoria…" className="w-full rounded-xl border border-navy/10 bg-cream/25 px-3 py-2.5 text-sm outline-none focus:border-teal/50" /></label>
            <FilterSelect label="Status" value={status} onChange={(value) => setStatus(value as StatusFilter)} options={[["all", "Todos"], ["pending", "Pendentes"], ["paid", "Pagas"]]} />
            <FilterSelect label="Categoria" value={category} onChange={setCategory} options={[["all", "Todas as categorias"], ...cats.map((cat) => [cat.id, cat.name])]} />
            <FilterSelect label="Ordenação" value={sort} onChange={(value) => setSort(value as SortKey)} options={[["date", "Data"], ["description", "Descrição"], ["amount", "Valor"], ["status", "Status"]]} />
          </div>
          <div className="mt-3 flex flex-wrap items-center gap-2">
            <button type="button" onClick={() => setDescending((value) => !value)} className="rounded-lg bg-cream/60 px-3 py-1.5 text-xs font-semibold text-muted">{descending ? "↓ Decrescente" : "↑ Crescente"}</button>
            {selected.size ? <><span className="ml-auto text-xs font-semibold text-muted">{selected.size} selecionada(s)</span><button type="button" disabled={working} onClick={() => void markPaid(selectedRows)} className="rounded-lg bg-teal px-3 py-1.5 text-xs font-semibold text-white disabled:opacity-50">Marcar como pagas</button><button type="button" disabled={working} onClick={() => void remove(selectedRows)} className="rounded-lg bg-red-50 px-3 py-1.5 text-xs font-semibold text-red-700 disabled:opacity-50">Apagar</button></> : null}
          </div>
        </div>

        <div className="hidden max-h-[calc(100vh-23rem)] min-h-72 overflow-auto lg:block">
          <table className="w-full table-fixed text-left text-sm">
            <thead className="sticky top-0 z-10 bg-cream-muted/95 text-[10px] uppercase tracking-[0.14em] text-muted shadow-[0_1px_0_rgba(20,28,42,0.08)] backdrop-blur-md">
              <tr><th className="w-12 px-4 py-3"><input className="h-4 w-4 rounded accent-teal" type="checkbox" aria-label="Selecionar todas" checked={allSelected} onChange={() => setSelected(allSelected ? new Set() : new Set(selectable.map((row) => row.id)))} /></th><th className="w-28 px-3 py-3">Data</th><th className="px-3 py-3">Descrição</th><th className="w-44 px-3 py-3">Categoria</th><th className="w-32 px-3 py-3 text-right">Valor</th><th className="w-28 px-3 py-3">Status</th><th className="w-40 px-4 py-3 text-right">Ações</th></tr>
            </thead>
            <tbody className="divide-y divide-navy/8">
              {loading ? <ExpenseLoadingRows /> : visible.length === 0 ? <tr><td colSpan={7} className="px-4 py-16 text-center"><span className="mx-auto grid h-12 w-12 place-items-center rounded-2xl bg-cream text-muted"><EmptyIcon /></span><p className="mt-3 font-semibold text-navy">Nenhuma despesa encontrada</p><p className="mt-1 text-xs text-muted">Ajuste os filtros ou inclua um novo lançamento.</p></td></tr> : visible.map((row, index) => <ExpenseTableRow key={row.id} row={row} index={index} checked={selected.has(row.id)} onToggle={() => toggleRow(row.id)} onEdit={() => setEditing(row)} onPay={() => void markPaid([row])} onDelete={() => void remove([row])} />)}
            </tbody>
          </table>
        </div>

        <ul className="divide-y divide-navy/8 lg:hidden">
          {loading ? <li className="px-4 py-12 text-center text-sm text-muted">Carregando lançamentos…</li> : visible.length === 0 ? <li className="px-4 py-12 text-center text-sm text-muted">Nenhuma despesa encontrada.</li> : visible.map((row, index) => <ExpenseMobileCard key={row.id} row={row} index={index} checked={selected.has(row.id)} onToggle={() => toggleRow(row.id)} onEdit={() => setEditing(row)} onPay={() => void markPaid([row])} onDelete={() => void remove([row])} />)}
        </ul>
        <footer className="flex flex-wrap items-center justify-between gap-2 border-t border-navy/8 bg-cream/25 px-4 py-3 text-xs text-muted"><span>{visible.length} de {rows.length} lançamentos</span><span className="font-semibold text-navy">Total filtrado: {formatBrlFromCents(visible.reduce((sum, row) => sum + row.amount_cents, 0))}</span></footer>
      </section>
      {editing ? <EditExpenseDialog row={editing} categories={cats} onClose={() => setEditing(null)} onSaved={async () => { setEditing(null); await load(); }} /> : null}
    </div>
  );
}

function ExpenseTableRow({ row, index, checked, onToggle, onEdit, onPay, onDelete }: RowActions) {
  const manageable = canManage(row);
  return <tr className={`expense-list-row group border-l-2 ${row.status === "paid" ? "border-l-teal/45" : "border-l-gold"} ${checked ? "bg-teal/8" : ""}`} style={{ ["--expense-delay" as string]: `${Math.min(index, 10) * 28}ms` }}><td className="px-4 py-3"><input className="h-4 w-4 rounded accent-teal" type="checkbox" aria-label={`Selecionar ${row.description}`} checked={checked} disabled={!manageable} onChange={onToggle} /></td><td className="px-3 py-3"><p className="text-xs font-semibold text-navy">{formatDueDate(row.due_date || row.expense_date)}</p><p className="mt-0.5 text-[10px] text-muted">{row.due_date ? "Vencimento" : "Lançamento"}</p></td><td className="px-3 py-3"><p className="truncate font-semibold text-navy transition-colors group-hover:text-teal-deep">{row.description}</p><ExpenseMeta row={row} /></td><td className="px-3 py-3"><span className="inline-flex max-w-full items-center gap-1.5 truncate rounded-full border border-navy/8 bg-cream/65 px-2.5 py-1 text-xs font-medium text-navy"><span className="h-1.5 w-1.5 shrink-0 rounded-full bg-teal/70" />{row.category_name}</span></td><td className="px-3 py-3 text-right font-display text-base font-semibold tabular-nums text-navy-deep">{formatBrlFromCents(row.amount_cents)}</td><td className="px-3 py-3"><StatusBadge row={row} /></td><td className="px-4 py-3"><div className="flex justify-end gap-1 opacity-75 transition-opacity group-hover:opacity-100 group-focus-within:opacity-100"><ActionButton label="Editar" onClick={onEdit} disabled={!manageable} />{row.status !== "paid" ? <ActionButton label="Pagar" onClick={onPay} disabled={!manageable} primary /> : null}<ActionButton label="Apagar" onClick={onDelete} disabled={!manageable} danger /></div></td></tr>;
}

function ExpenseMobileCard({ row, index, checked, onToggle, onEdit, onPay, onDelete }: RowActions) {
  const manageable = canManage(row);
  return <li className={`expense-list-row border-l-2 p-4 ${row.status === "paid" ? "border-l-teal/45" : "border-l-gold"} ${checked ? "bg-teal/8" : ""}`} style={{ ["--expense-delay" as string]: `${Math.min(index, 10) * 28}ms` }}><div className="flex items-start gap-3"><input className="mt-1 h-4 w-4 accent-teal" type="checkbox" checked={checked} disabled={!manageable} onChange={onToggle} aria-label={`Selecionar ${row.description}`} /><div className="min-w-0 flex-1"><div className="flex items-start justify-between gap-3"><div className="min-w-0"><p className="truncate font-semibold text-navy">{row.description}</p><p className="mt-1 text-xs text-muted">{row.category_name} · {formatDueDate(row.due_date || row.expense_date)}</p></div><p className="shrink-0 font-display font-semibold tabular-nums text-navy-deep">{formatBrlFromCents(row.amount_cents)}</p></div><div className="mt-2 flex flex-wrap items-center gap-2"><StatusBadge row={row} /><ExpenseMeta row={row} /></div><div className="mt-3 flex justify-end gap-1"><ActionButton label="Editar" onClick={onEdit} disabled={!manageable} />{row.status !== "paid" ? <ActionButton label="Pagar" onClick={onPay} disabled={!manageable} primary /> : null}<ActionButton label="Apagar" onClick={onDelete} disabled={!manageable} danger /></div></div></div></li>;
}

type RowActions = { row: Expense; index: number; checked: boolean; onToggle: () => void; onEdit: () => void; onPay: () => void; onDelete: () => void };

function ExpenseMeta({ row }: { row: Expense }) {
  return <span className="mt-1 flex flex-wrap gap-x-2 text-[11px] text-muted">{row.installment_total && row.installment_total > 1 ? <span>Parcela {row.installment_number || 1}/{row.installment_total}</span> : null}{row.recurring_frequency ? <span>{freqLabel(row.recurring_frequency)}</span> : null}{row.is_shared ? <span>Compartilhada{row.counterparty_label ? ` com ${row.counterparty_label}` : ""}</span> : null}{row.is_projected ? <span>Projetada</span> : null}</span>;
}

function StatusBadge({ row }: { row: Expense }) {
  return <span className={`inline-flex rounded-full px-2.5 py-1 text-[10px] font-bold uppercase tracking-wide ${row.status === "paid" ? "bg-sage text-teal-deep" : "bg-gold/15 text-gold-pressed"}`}>{row.status === "paid" ? "Paga" : "Pendente"}</span>;
}

function ActionButton({ label, onClick, disabled, primary, danger }: { label: string; onClick: () => void; disabled?: boolean; primary?: boolean; danger?: boolean }) {
  const tone = primary ? "bg-teal/10 text-teal-deep hover:bg-teal/20" : danger ? "text-red-700 hover:bg-red-50" : "text-navy hover:bg-cream-muted";
  const icon = label === "Editar"
    ? <><path d="M4 20h4l10.5-10.5a2.1 2.1 0 0 0-4-4L4 16v4Z" /><path d="m13.5 6.5 4 4" /></>
    : label === "Pagar"
      ? <><circle cx="12" cy="12" r="9" /><path d="m8 12 2.5 2.5L16 9" /></>
      : <><path d="M4 7h16M9 7V4h6v3M7 7l1 13h8l1-13M10 11v5M14 11v5" /></>;
  return <button type="button" title={label} aria-label={label} disabled={disabled} onClick={onClick} className={`grid h-8 w-8 place-items-center rounded-lg transition disabled:cursor-not-allowed disabled:opacity-35 ${tone}`}><svg viewBox="0 0 24 24" className="h-4 w-4" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{icon}</svg></button>;
}

function SummaryCard({ label, value, detail, tone }: { label: string; value: number; detail: string; tone: "navy" | "green" | "gold" }) {
  const border = tone === "green" ? "border-l-teal" : tone === "gold" ? "border-l-gold" : "border-l-navy";
  return <article className={`group relative overflow-hidden rounded-2xl border border-navy/8 border-l-4 ${border} bg-white/90 px-4 py-3.5 shadow-[0_8px_25px_rgba(20,28,42,0.045)] transition duration-300 hover:-translate-y-0.5 hover:shadow-[0_14px_32px_rgba(20,28,42,0.09)]`}><span className="pointer-events-none absolute -right-6 -top-8 h-20 w-20 rounded-full bg-sage/50 transition-transform duration-500 group-hover:scale-125" /><p className="relative text-[10px] font-bold uppercase tracking-[0.16em] text-muted">{label}</p><p className="relative mt-1 font-display text-2xl font-semibold tabular-nums text-navy-deep">{formatBrlFromCents(value)}</p><p className="relative mt-1 text-xs text-muted">{detail}</p></article>;
}

function ExpenseLoadingRows() {
  return <>{Array.from({ length: 5 }, (_, index) => <tr key={index} className="border-b border-navy/8"><td className="px-4 py-4" colSpan={7}><div className="h-5 rounded-lg bg-cream-muted wp-shimmer" style={{ width: `${82 - index * 6}%` }} /></td></tr>)}</>;
}

function EmptyIcon() {
  return <svg viewBox="0 0 24 24" className="h-6 w-6" fill="none" stroke="currentColor" strokeWidth="1.6"><path d="M5 4h14v16H5zM8 8h8M8 12h5M8 16h3" /></svg>;
}

function FilterSelect({ label, value, onChange, options }: { label: string; value: string; onChange: (value: string) => void; options: string[][] }) {
  return <label><span className="sr-only">{label}</span><select value={value} onChange={(event) => onChange(event.target.value)} className="w-full rounded-xl border border-navy/10 bg-cream/25 px-3 py-2.5 text-sm text-navy outline-none focus:border-teal/50">{options.map(([id, text]) => <option key={id} value={id}>{text}</option>)}</select></label>;
}

function EditExpenseDialog({ row, categories, onClose, onSaved }: { row: Expense; categories: Category[]; onClose: () => void; onSaved: () => Promise<void> }) {
  const [description, setDescription] = useState(row.description);
  const [amount, setAmount] = useState(formatBrlFromCents(row.amount_cents));
  const [expenseDate, setExpenseDate] = useState(row.expense_date);
  const [dueDate, setDueDate] = useState(row.due_date || "");
  const [categoryId, setCategoryId] = useState(row.category_id);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  async function submit(event: FormEvent) {
    event.preventDefault();
    const cents = parseBrlToCents(amount);
    if (!description.trim() || cents <= 0) { setError("Informe descrição e valor válidos."); return; }
    setBusy(true); setError(null);
    try { await updateExpense(row.id, { description: description.trim(), amount_cents: cents, expense_date: expenseDate, due_date: dueDate || null, category_id: categoryId }); await onSaved(); }
    catch (err) { setError(err instanceof Error ? err.message : "Não foi possível editar."); setBusy(false); }
  }
  return <div className="fixed inset-0 z-50 grid place-items-center bg-navy-deep/65 p-4 backdrop-blur-sm" role="dialog" aria-modal="true" aria-labelledby="edit-expense-title" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}><form onSubmit={submit} className="wp-settings-card w-full max-w-2xl rounded-3xl border border-navy/10 bg-white p-5 shadow-2xl"><div className="flex items-start justify-between gap-3"><div><p className="text-[11px] font-semibold uppercase tracking-widest text-teal-deep">Lançamento</p><h2 id="edit-expense-title" className="mt-1 font-serif text-2xl text-navy-deep">Editar despesa</h2></div><button type="button" onClick={onClose} className="rounded-full px-3 py-2 text-muted hover:bg-cream">✕</button></div>{row.installment_group_id || row.recurring_series_id ? <p className="mt-3 rounded-xl bg-gold/10 px-3 py-2 text-xs text-gold-pressed">Esta linha pertence a um plano. A edição altera esta ocorrência; use a exclusão para remover o plano completo.</p> : null}{error ? <p className="mt-3 text-sm text-red-700">{error}</p> : null}<div className="mt-5 grid gap-3 sm:grid-cols-2"><DialogField label="Descrição" value={description} onChange={setDescription} wide /><DialogField label="Valor (R$)" value={amount} onChange={setAmount} /><DialogField label="Data" type="date" value={expenseDate} onChange={setExpenseDate} /><DialogField label="Vencimento" type="date" value={dueDate} onChange={setDueDate} /><label><span className="mb-1 block text-xs font-semibold uppercase tracking-wide text-muted">Categoria</span><select className="w-full rounded-xl border border-navy/10 bg-white px-3 py-2.5" value={categoryId} onChange={(event) => setCategoryId(event.target.value)}>{categories.map((cat) => <option key={cat.id} value={cat.id}>{cat.name}</option>)}</select></label></div><div className="mt-6 flex justify-end gap-2"><button type="button" onClick={onClose} className="rounded-xl px-4 py-2.5 text-sm font-semibold text-muted hover:bg-cream">Cancelar</button><button disabled={busy} className="rounded-xl bg-teal px-5 py-2.5 text-sm font-semibold text-white disabled:opacity-50">{busy ? "Salvando…" : "Salvar alterações"}</button></div></form></div>;
}

function DialogField({ label, value, onChange, type = "text", wide }: { label: string; value: string; onChange: (value: string) => void; type?: string; wide?: boolean }) {
  const money = label.startsWith("Valor");
  return <label className={wide ? "sm:col-span-2" : ""}><span className="mb-1 block text-xs font-semibold uppercase tracking-wide text-muted">{label}</span><input required={label !== "Vencimento"} type={type} inputMode={money ? "numeric" : undefined} value={value} onChange={(event) => onChange(money ? maskBrlInput(event.target.value) : event.target.value)} className="w-full rounded-xl border border-navy/10 bg-white px-3 py-2.5 outline-none focus:border-teal/50" /></label>;
}

function canManage(row: Expense) { return row.is_mine !== false && !row.is_projected; }
function freqLabel(freq: string) { if (freq === "weekly") return "Semanal"; if (freq === "yearly") return "Anual"; return "Mensal"; }
