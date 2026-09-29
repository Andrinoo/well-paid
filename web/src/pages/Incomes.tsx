import { useEffect, useMemo, useState, type FormEvent } from "react";
import { createIncome, deleteIncome, fetchIncomeCategories, fetchIncomes, type Category, type Income } from "../api";
import { formatBrlFromCents, formatDueDate } from "../format";
import { ApiError, ErrorNote, MonthBar, PageTitle, parseBrlToCents, todayIso, usePeriod } from "./common";

type IncomeSortKey = "date" | "description" | "category" | "amount";

export function IncomesPage() {
  const [period, setPeriod] = usePeriod();
  const [rows, setRows] = useState<Income[]>([]);
  const [cats, setCats] = useState<Category[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [showCreate, setShowCreate] = useState(false);
  const [description, setDescription] = useState("");
  const [amount, setAmount] = useState("");
  const [date, setDate] = useState(todayIso());
  const [notes, setNotes] = useState("");
  const [categoryId, setCategoryId] = useState("");
  const [search, setSearch] = useState("");
  const [category, setCategory] = useState("all");
  const [sort, setSort] = useState<IncomeSortKey>("date");
  const [descending, setDescending] = useState(true);

  async function load() {
    setLoading(true); setError(null);
    try {
      const [list, categories] = await Promise.all([fetchIncomes(period.year, period.month), fetchIncomeCategories()]);
      setRows(list); setCats(categories);
      if (!categoryId && categories[0]) setCategoryId(categories[0].id);
    } catch (err) { setError(err instanceof ApiError ? err.message : "Falha ao carregar."); }
    finally { setLoading(false); }
  }

  useEffect(() => { void load(); /* eslint-disable-next-line react-hooks/exhaustive-deps */ }, [period.year, period.month]);

  async function onCreate(event: FormEvent) {
    event.preventDefault(); setError(null);
    const cents = parseBrlToCents(amount);
    if (!description.trim() || !cents || !categoryId) { setError("Preencha descrição, valor e categoria."); return; }
    setBusy(true);
    try {
      await createIncome({ description: description.trim(), amount_cents: cents, income_date: date, income_category_id: categoryId, notes: notes.trim() || null });
      setDescription(""); setAmount(""); setNotes(""); setShowCreate(false); await load();
    } catch (err) { setError(err instanceof ApiError ? err.message : "Não foi possível criar."); }
    finally { setBusy(false); }
  }

  async function remove(row: Income) {
    if (!window.confirm(`Apagar o provento “${row.description}”?`)) return;
    try { await deleteIncome(row.id); await load(); }
    catch (err) { setError(err instanceof Error ? err.message : "Não foi possível apagar."); }
  }

  const visible = useMemo(() => {
    const term = search.trim().toLocaleLowerCase("pt-BR");
    return rows.filter((row) => category === "all" || row.income_category_id === category)
      .filter((row) => !term || `${row.description} ${row.category_name} ${row.notes || ""}`.toLocaleLowerCase("pt-BR").includes(term))
      .sort((a, b) => {
        let value = 0;
        if (sort === "amount") value = a.amount_cents - b.amount_cents;
        else if (sort === "description") value = a.description.localeCompare(b.description, "pt-BR");
        else if (sort === "category") value = a.category_name.localeCompare(b.category_name, "pt-BR");
        else value = a.income_date.localeCompare(b.income_date);
        return descending ? -value : value;
      });
  }, [rows, category, search, sort, descending]);
  const total = rows.reduce((sum, row) => sum + row.amount_cents, 0);
  const average = rows.length ? Math.round(total / rows.length) : 0;

  function exportCsv() {
    const lines = [
      ["Data", "Descrição", "Categoria", "Valor", "Nota"],
      ...visible.map((row) => [row.income_date, row.description, row.category_name, (row.amount_cents / 100).toFixed(2).replace(".", ","), row.notes || ""]),
    ];
    const csv = lines.map((line) => line.map((cell) => `"${String(cell).replaceAll('"', '""')}"`).join(";")).join("\n");
    const url = URL.createObjectURL(new Blob(["\ufeff", csv], { type: "text/csv;charset=utf-8" }));
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = `proventos-${period.year}-${String(period.month).padStart(2, "0")}.csv`;
    anchor.click();
    URL.revokeObjectURL(url);
  }

  return (
    <div className="-mx-4 -my-5 min-h-full bg-gradient-to-br from-paper via-paper to-sage/45 px-4 py-5 sm:-mx-6 sm:px-6">
      <div className="flex flex-wrap items-end justify-between gap-4"><PageTitle kicker="Controle mensal" title="Proventos" /><div className="flex flex-wrap items-center gap-2"><MonthBar year={period.year} month={period.month} onChange={setPeriod} /><button type="button" onClick={() => setShowCreate((value) => !value)} className="rounded-xl bg-teal px-4 py-2.5 text-sm font-bold text-white shadow-sm transition hover:-translate-y-0.5 hover:bg-teal-deep">{showCreate ? "Fechar cadastro" : "+ Novo provento"}</button></div></div>
      <div className="mt-4"><ErrorNote message={error} /></div>
      {showCreate ? <form onSubmit={onCreate} className="wp-rise mt-4 overflow-hidden rounded-2xl border border-navy/8 bg-white shadow-[0_12px_38px_rgba(20,28,42,0.06)]"><header className="flex items-center gap-3 border-b border-navy/8 bg-cream/25 px-4 py-3"><span className="grid h-9 w-9 place-items-center rounded-xl bg-teal/10 text-teal"><PlusIcon /></span><div><h2 className="font-serif text-lg text-navy-deep">Lançamento rápido</h2><p className="text-xs text-muted">Registre uma entrada sem sair da lista.</p></div></header><div className="grid items-end gap-2 p-3 md:grid-cols-2 xl:grid-cols-[145px_minmax(230px,1fr)_140px_190px_minmax(180px,0.7fr)_auto] sm:p-4"><CompactField label="Data" type="date" value={date} onChange={setDate} /><CompactField label="Descrição" value={description} placeholder="Ex.: salário" onChange={setDescription} /><CompactField label="Valor" value={amount} placeholder="R$ 0,00" onChange={setAmount} /><label><FieldLabel>Categoria</FieldLabel><select required value={categoryId} onChange={(event) => setCategoryId(event.target.value)} className="h-10 w-full rounded-lg border border-navy/10 bg-white px-3 text-sm"><option value="">Escolher</option>{cats.map((cat) => <option key={cat.id} value={cat.id}>{cat.name}</option>)}</select></label><CompactField label="Nota opcional" value={notes} onChange={setNotes} /><button disabled={busy} className="h-10 rounded-lg bg-gold px-5 text-sm font-bold text-navy-deep disabled:opacity-50">{busy ? "Incluindo…" : "Incluir"}</button></div></form> : null}

      <section className="mt-4 grid gap-3 sm:grid-cols-3"><IncomeMetric label="Total recebido" value={total} detail={`${rows.length} lançamentos`} tone="teal" /><IncomeMetric label="Média por entrada" value={average} detail="Valor médio no período" tone="navy" /><IncomeMetric label="Categorias usadas" value={null} detail={`${new Set(rows.map((row) => row.income_category_id)).size} categorias`} tone="gold" /></section>

      <section className="mt-4 overflow-hidden rounded-[1.6rem] border border-navy/8 bg-white/90 shadow-[0_18px_55px_rgba(20,28,42,0.08)] backdrop-blur-sm"><div className="border-b border-navy/8 bg-gradient-to-r from-white via-white to-sage/45 p-4 sm:p-5"><div className="flex flex-wrap items-center justify-between gap-3"><div><h2 className="font-serif text-xl text-navy-deep">Central de proventos</h2><p className="mt-0.5 text-xs text-muted">Filtre, ordene e consulte todas as entradas do mês.</p></div><button type="button" onClick={exportCsv} disabled={!visible.length} className="rounded-xl border border-navy/10 px-3 py-2 text-xs font-semibold text-navy transition hover:border-teal/40 hover:text-teal-deep disabled:opacity-40">Exportar CSV</button></div><div className="mt-4 grid gap-2 md:grid-cols-[minmax(220px,1fr)_220px_170px]"><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Buscar descrição, categoria ou nota…" className="h-10 rounded-xl border border-navy/10 bg-cream/25 px-3 text-sm outline-none focus:border-teal/50" /><select value={category} onChange={(event) => setCategory(event.target.value)} className="h-10 rounded-xl border border-navy/10 bg-cream/25 px-3 text-sm"><option value="all">Todas as categorias</option>{cats.map((cat) => <option key={cat.id} value={cat.id}>{cat.name}</option>)}</select><select value={sort} onChange={(event) => setSort(event.target.value as IncomeSortKey)} className="h-10 rounded-xl border border-navy/10 bg-cream/25 px-3 text-sm"><option value="date">Data</option><option value="description">Descrição</option><option value="category">Categoria</option><option value="amount">Valor</option></select></div><div className="mt-3"><button type="button" onClick={() => setDescending((value) => !value)} className="rounded-lg bg-cream/60 px-3 py-1.5 text-xs font-semibold text-muted">{descending ? "↓ Decrescente" : "↑ Crescente"}</button></div></div>
        <div className="hidden max-h-[calc(100vh-22rem)] min-h-72 overflow-auto md:block"><table className="w-full table-fixed text-left text-sm"><thead className="sticky top-0 z-10 bg-cream-muted/95 text-[10px] uppercase tracking-[0.14em] text-muted shadow-[0_1px_0_rgba(20,28,42,0.08)] backdrop-blur-md"><tr><th className="w-32 px-5 py-3">Data</th><th className="px-3 py-3">Descrição</th><th className="w-56 px-3 py-3">Categoria</th><th className="w-40 px-3 py-3 text-right">Valor</th><th className="w-20 px-5 py-3 text-right">Ações</th></tr></thead><tbody className="divide-y divide-navy/8">{loading ? <LoadingRows /> : visible.length === 0 ? <tr><td colSpan={5} className="px-4 py-16 text-center"><p className="font-semibold text-navy">Nenhum provento encontrado</p><p className="mt-1 text-xs text-muted">Ajuste os filtros ou inclua uma nova entrada.</p></td></tr> : visible.map((row, index) => <tr key={row.id} className="expense-list-row group border-l-2 border-l-teal/60" style={{ ["--expense-delay" as string]: `${Math.min(index, 10) * 28}ms` }}><td className="px-5 py-3"><p className="text-xs font-semibold text-navy">{formatDueDate(row.income_date)}</p><p className="mt-0.5 text-[10px] text-muted">Recebimento</p></td><td className="px-3 py-3"><p className="truncate font-semibold text-navy transition-colors group-hover:text-teal-deep">{row.description}</p>{row.notes ? <p className="mt-0.5 truncate text-[11px] text-muted">{row.notes}</p> : null}</td><td className="px-3 py-3"><span className="inline-flex max-w-full items-center gap-1.5 truncate rounded-full border border-navy/8 bg-cream/65 px-2.5 py-1 text-xs font-medium text-navy"><span className="h-1.5 w-1.5 shrink-0 rounded-full bg-teal/70" />{row.category_name}</span></td><td className="px-3 py-3 text-right font-display text-base font-semibold tabular-nums text-teal-deep">{formatBrlFromCents(row.amount_cents)}</td><td className="px-5 py-3"><div className="flex justify-end opacity-70 transition-opacity group-hover:opacity-100 group-focus-within:opacity-100"><IconButton label="Apagar" danger onClick={() => void remove(row)} /></div></td></tr>)}</tbody></table></div>
        <ul className="divide-y divide-navy/8 md:hidden">{visible.map((row, index) => <li key={row.id} className="expense-list-row border-l-2 border-l-teal/60 p-4" style={{ ["--expense-delay" as string]: `${Math.min(index, 10) * 28}ms` }}><div className="flex justify-between gap-3"><div className="min-w-0"><p className="truncate font-semibold text-navy">{row.description}</p><p className="mt-1 text-xs text-muted">{row.category_name} · {formatDueDate(row.income_date)}</p></div><p className="font-display font-semibold text-teal-deep">{formatBrlFromCents(row.amount_cents)}</p></div><div className="mt-2 flex justify-end"><IconButton label="Apagar" danger onClick={() => void remove(row)} /></div></li>)}</ul>
        <footer className="flex justify-between border-t border-navy/8 bg-cream/25 px-4 py-3 text-xs text-muted"><span>{visible.length} de {rows.length} lançamentos</span><strong className="text-navy">Total filtrado: {formatBrlFromCents(visible.reduce((sum, row) => sum + row.amount_cents, 0))}</strong></footer>
      </section>
    </div>
  );
}

function CompactField({ label, value, onChange, type = "text", placeholder }: { label: string; value: string; onChange: (value: string) => void; type?: string; placeholder?: string }) { return <label><FieldLabel>{label}</FieldLabel><input required={label !== "Nota opcional"} type={type} value={value} placeholder={placeholder} onChange={(event) => onChange(event.target.value)} className="h-10 w-full rounded-lg border border-navy/10 bg-white px-3 text-sm outline-none focus:border-teal/50 focus:ring-2 focus:ring-teal/10" /></label>; }
function FieldLabel({ children }: { children: string }) { return <span className="mb-1 block text-[10px] font-bold uppercase tracking-wide text-muted">{children}</span>; }
function IncomeMetric({ label, value, detail, tone }: { label: string; value: number | null; detail: string; tone: "teal" | "navy" | "gold" }) { const border = tone === "teal" ? "border-l-teal" : tone === "gold" ? "border-l-gold" : "border-l-navy"; return <article className={`rounded-2xl border border-navy/8 border-l-4 ${border} bg-white/90 px-4 py-3.5 shadow-sm transition duration-300 hover:-translate-y-0.5 hover:shadow-lg`}><p className="text-[10px] font-bold uppercase tracking-[0.16em] text-muted">{label}</p>{value != null ? <p className="mt-1 font-display text-2xl font-semibold text-navy-deep">{formatBrlFromCents(value)}</p> : <p className="mt-1 font-display text-2xl font-semibold text-navy-deep">{detail.split(" ")[0]}</p>}<p className="mt-1 text-xs text-muted">{detail}</p></article>; }
function IconButton({ label, onClick, danger }: { label: string; onClick: () => void; danger?: boolean }) { return <button type="button" title={label} aria-label={label} onClick={onClick} className={`grid h-8 w-8 place-items-center rounded-lg transition ${danger ? "text-red-700 hover:bg-red-50" : "text-navy hover:bg-cream"}`}><svg viewBox="0 0 24 24" className="h-4 w-4" fill="none" stroke="currentColor" strokeWidth="1.8"><path d="M4 7h16M9 7V4h6v3M7 7l1 13h8l1-13M10 11v5M14 11v5" /></svg></button>; }
function LoadingRows() { return <>{Array.from({ length: 5 }, (_, index) => <tr key={index}><td colSpan={5} className="px-5 py-4"><div className="h-5 rounded-lg bg-cream-muted wp-shimmer" style={{ width: `${80 - index * 5}%` }} /></td></tr>)}</>; }
function PlusIcon() { return <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="1.8"><path d="M12 5v14M5 12h14" /></svg>; }
