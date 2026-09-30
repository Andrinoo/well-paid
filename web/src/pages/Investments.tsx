import { useEffect, useMemo, useState, type FormEvent } from "react";
import { addInvestmentPrincipal, createPosition, deleteInvestmentPosition, fetchInvestmentOverview, fetchInvestmentPreferences, fetchInvestmentRates, fetchPositions, saveInvestmentPreferences, searchInvestmentTickers, type InvestmentOverview, type InvestmentPosition, type InvestmentRates, type InvestmentTicker } from "../api";
import { formatBrlFromCents, maskBrlInput, parseBrlToCents } from "../format";
import { ApiError, ErrorNote, PageTitle } from "./common";

type ViewMode = "cards" | "list";
const TYPES = [["all", "Todos"], ["stock", "Ações"], ["fii", "FIIs"], ["etf", "ETFs"], ["crypto", "Cripto"], ["treasury", "Tesouro"], ["cdb", "CDB"], ["fixed_income", "Renda fixa"]] as const;

export function InvestmentsPage() {
  const [overview, setOverview] = useState<InvestmentOverview | null>(null);
  const [rows, setRows] = useState<InvestmentPosition[]>([]);
  const [rates, setRates] = useState<InvestmentRates | null>(null);
  const [view, setView] = useState<ViewMode>("cards");
  const [filter, setFilter] = useState("all");
  const [search, setSearch] = useState("");
  const [showForm, setShowForm] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [name, setName] = useState("");
  const [principal, setPrincipal] = useState("");
  const [rate, setRate] = useState("");
  const [type, setType] = useState("stock");
  const [description, setDescription] = useState("");
  const [maturity, setMaturity] = useState("");
  const [liquid, setLiquid] = useState(true);
  const [suggestions, setSuggestions] = useState<InvestmentTicker[]>([]);
  const [searching, setSearching] = useState(false);
  const [aporte, setAporte] = useState<Record<string, string>>({});

  async function load() {
    setError(null);
    try {
      const [ov, list, suggested, preference] = await Promise.all([fetchInvestmentOverview(), fetchPositions(), fetchInvestmentRates(), fetchInvestmentPreferences()]);
      setOverview(ov); setRows(list); setRates(suggested); setView(preference.view_mode);
    } catch (err) { setError(messageOf(err, "Falha ao carregar investimentos.")); }
  }
  useEffect(() => { void load(); }, []);
  useEffect(() => {
    const query = name.trim();
    if (!showForm || query.length < 3) { setSuggestions([]); return; }
    const timer = window.setTimeout(() => { setSearching(true); void searchInvestmentTickers(query).then((items) => { setSuggestions(items); const exact = items.find((item) => item.symbol.toUpperCase() === query.toUpperCase()); if (exact) setType(exact.instrument_type || inferAssetType(query) || "stock"); }).catch(() => setSuggestions([])).finally(() => setSearching(false)); }, 350);
    return () => window.clearTimeout(timer);
  }, [name, showForm]);

  const visible = useMemo(() => rows.filter((row) => filter === "all" || row.instrument_type === filter).filter((row) => `${row.name} ${row.description ?? ""}`.toLocaleLowerCase("pt-BR").includes(search.toLocaleLowerCase("pt-BR"))), [rows, filter, search]);
  const marketType = ["stock", "fii", "etf", "crypto"].includes(type);

  function changeName(value: string) { setName(value); const inferred = inferAssetType(value); if (inferred) setType(inferred); }
  function selectTicker(item: InvestmentTicker) { setName(item.symbol); setType(item.instrument_type || "stock"); setDescription(item.name); if (item.last_price) setPrincipal(maskBrlInput(String(Math.round(item.last_price * 100)))); setSuggestions([]); }
  function applySuggestedRate() { const value = type === "cdb" ? rates?.cdb_annual_percent : rates?.fixed_income_annual_percent; if (value != null) setRate(String(value).replace(".", ",")); }
  async function onCreate(event: FormEvent) {
    event.preventDefault(); const cents = parseBrlToCents(principal); const bps = marketType ? 0 : Math.round(Number(rate.replace(",", ".")) * 100);
    if (!name.trim() || !cents || !Number.isFinite(bps)) { setError("Preencha nome, valor e taxa válidos."); return; }
    setBusy(true); setError(null);
    try { await createPosition({ instrument_type: type, name: name.trim().toUpperCase(), description: description.trim() || null, principal_cents: cents, annual_rate_bps: bps, maturity_date: maturity || null, is_liquid: liquid }); setName(""); setPrincipal(""); setDescription(""); setMaturity(""); setShowForm(false); await load(); }
    catch (err) { setError(messageOf(err, "Não foi possível criar a posição.")); } finally { setBusy(false); }
  }
  async function changeView(next: ViewMode) { setView(next); try { await saveInvestmentPreferences(next); } catch (err) { setError(messageOf(err, "Não foi possível salvar a visualização.")); } }
  async function addPrincipal(row: InvestmentPosition) { const cents = parseBrlToCents(aporte[row.id] ?? ""); if (!cents) return; setBusy(true); try { await addInvestmentPrincipal(row.id, cents); setAporte((old) => ({ ...old, [row.id]: "" })); await load(); } catch (err) { setError(messageOf(err, "Não foi possível aportar.")); } finally { setBusy(false); } }
  async function remove(row: InvestmentPosition) { if (!window.confirm(`Excluir ${row.name}?`)) return; setBusy(true); try { await deleteInvestmentPosition(row.id); await load(); } catch (err) { setError(messageOf(err, "Não foi possível excluir.")); } finally { setBusy(false); } }

  return <div className="-mx-4 -my-5 min-h-full bg-gradient-to-br from-paper via-paper to-sky/45 px-4 py-5 sm:-mx-6 sm:px-6">
    <div className="flex flex-wrap items-end justify-between gap-4"><div><PageTitle kicker="Patrimônio" title="Investimentos" /><p className="mt-2 text-sm text-muted">Acompanhe posições, encontre ativos e planeje seus próximos aportes.</p></div><button type="button" onClick={() => setShowForm((v) => !v)} className="rounded-xl bg-gold px-4 py-2.5 text-sm font-bold text-navy-deep shadow-sm">{showForm ? "Fechar cadastro" : "+ Nova posição"}</button></div>
    <div className="mt-4"><ErrorNote message={error} /></div>
    {overview ? <section className="mt-4 grid gap-3 sm:grid-cols-3">{[["Total alocado", overview.total_allocated_cents, "navy"], ["Rendimento estimado", overview.total_yield_cents, "teal"], ["Estimado por mês", overview.estimated_monthly_yield_cents, "gold"]].map(([label, value, tone]) => <Metric key={String(label)} label={String(label)} value={Number(value)} tone={String(tone)} />)}</section> : null}
    {showForm ? <form onSubmit={onCreate} className="wp-rise mt-4 overflow-visible rounded-3xl border border-navy/8 bg-white/90 shadow-[0_18px_55px_rgba(20,28,42,0.08)]"><header className="border-b border-navy/8 bg-cream/30 px-5 py-3"><h2 className="font-serif text-xl text-navy-deep">Adicionar investimento</h2><p className="text-xs text-muted">Pesquise pelo nome ou ticker; o tipo e os dados disponíveis serão preenchidos automaticamente.</p></header><div className="grid gap-3 p-4 md:grid-cols-2 xl:grid-cols-4">
      <label className="relative"><FieldLabel>Ativo ou título</FieldLabel><input required value={name} onChange={(e) => changeName(e.target.value)} placeholder="Ex.: FIQE3, bitcoin ou CDB" autoComplete="off" className="field" />{searching ? <span className="absolute right-3 top-9 text-xs text-muted">Buscando…</span> : null}{suggestions.length ? <ul className="absolute z-30 mt-1 max-h-64 w-full overflow-auto rounded-xl border border-navy/10 bg-white p-1 shadow-xl">{suggestions.map((item) => <li key={`${item.instrument_type}-${item.symbol}`}><button type="button" onClick={() => selectTicker(item)} className="flex w-full items-center justify-between rounded-lg px-3 py-2 text-left hover:bg-sage/50"><span><b className="text-sm text-navy">{item.symbol}</b><small className="block truncate text-muted">{item.name} · {typeLabel(item.instrument_type)}</small></span>{item.last_price != null ? <span className="text-xs font-bold text-teal-deep">{(item.currency || "BRL") === "BRL" ? "R$ " : `${item.currency || "USD"} `}{item.last_price.toLocaleString("pt-BR", { minimumFractionDigits: 2 })}</span> : null}</button></li>)}</ul> : null}</label>
      <label><FieldLabel>Tipo</FieldLabel><select value={type} onChange={(e) => setType(e.target.value)} className="field">{TYPES.slice(1).map(([key, label]) => <option key={key} value={key}>{label}</option>)}</select></label>
      <label><FieldLabel>Valor aplicado</FieldLabel><input required inputMode="numeric" value={principal} onChange={(e) => setPrincipal(maskBrlInput(e.target.value))} placeholder="R$ 0,00" className="field" /></label>
      {!marketType ? <label><FieldLabel>Rentabilidade anual</FieldLabel><div className="flex gap-1"><input required inputMode="decimal" value={rate} onChange={(e) => setRate(e.target.value)} placeholder="% a.a." className="field min-w-0" /><button type="button" onClick={applySuggestedRate} className="rounded-lg bg-sage px-2 text-[10px] font-bold text-teal-deep">Usar mercado</button></div></label> : <label><FieldLabel>Descrição</FieldLabel><input value={description} onChange={(e) => setDescription(e.target.value)} className="field" /></label>}
      <label><FieldLabel>Vencimento</FieldLabel><input type="date" value={maturity} onChange={(e) => setMaturity(e.target.value)} className="field" /></label><label className="flex items-end"><span className="flex h-10 w-full items-center gap-2 rounded-lg bg-cream/50 px-3 text-sm"><input type="checkbox" checked={liquid} onChange={(e) => setLiquid(e.target.checked)} className="accent-teal" /> Liquidez disponível</span></label><button disabled={busy} className="h-10 self-end rounded-lg bg-teal px-5 text-sm font-bold text-white disabled:opacity-50">{busy ? "Salvando…" : "Adicionar posição"}</button>
    </div></form> : null}
    <section className="mt-4 overflow-hidden rounded-[1.6rem] border border-navy/8 bg-white/90 shadow-[0_18px_55px_rgba(20,28,42,0.08)]"><header className="border-b border-navy/8 bg-gradient-to-r from-white via-white to-sage/40 p-4 sm:p-5"><div className="flex flex-wrap items-center justify-between gap-3"><div><h2 className="font-serif text-xl text-navy-deep">Sua carteira</h2><p className="text-xs text-muted">{visible.length} de {rows.length} posições</p></div><div className="flex rounded-xl border border-navy/10 bg-white p-1"><ViewButton active={view === "cards"} onClick={() => void changeView("cards")}>▦ Cards</ViewButton><ViewButton active={view === "list"} onClick={() => void changeView("list")}>☷ Lista</ViewButton></div></div><div className="mt-4 grid gap-2 md:grid-cols-[1fr_220px]"><input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Buscar na carteira…" className="field bg-cream/25" /><select value={filter} onChange={(e) => setFilter(e.target.value)} className="field bg-cream/25">{TYPES.map(([key, label]) => <option key={key} value={key}>{label}</option>)}</select></div></header>
      {!visible.length ? <div className="p-14 text-center text-sm text-muted">Nenhuma posição encontrada.</div> : view === "cards" ? <div className="grid gap-4 p-4 md:grid-cols-2 xl:grid-cols-3 sm:p-5">{visible.map((row) => <PositionCard key={row.id} row={row} aporte={aporte[row.id] ?? ""} busy={busy} onAporte={(value) => setAporte((old) => ({ ...old, [row.id]: maskBrlInput(value) }))} onAdd={() => void addPrincipal(row)} onDelete={() => void remove(row)} />)}</div> : <div className="overflow-x-auto"><table className="w-full text-left text-sm"><thead className="bg-cream/60 text-[10px] uppercase tracking-wider text-muted"><tr><th className="px-5 py-3">Ativo</th><th className="px-3 py-3">Tipo</th><th className="px-3 py-3">Condições</th><th className="px-3 py-3 text-right">Aplicado</th><th className="px-5 py-3 text-right">Ações</th></tr></thead><tbody className="divide-y divide-navy/8">{visible.map((row) => <tr key={row.id} className="hover:bg-sage/20"><td className="px-5 py-3"><b>{row.name}</b><small className="block text-muted">{row.description}</small></td><td className="px-3 py-3">{typeLabel(row.instrument_type)}</td><td className="px-3 py-3 text-xs text-muted">{row.annual_rate_bps ? `${(row.annual_rate_bps / 100).toFixed(2)}% a.a.` : "Cotação de mercado"} · {row.is_liquid ? "líquido" : "vencimento"}</td><td className="px-3 py-3 text-right font-bold">{formatBrlFromCents(row.principal_cents)}</td><td className="px-5 py-3 text-right"><button type="button" onClick={() => void remove(row)} className="text-xs font-semibold text-red-700">Excluir</button></td></tr>)}</tbody></table></div>}
    </section>
  </div>;
}

function Metric({ label, value, tone }: { label: string; value: number; tone: string }) { return <article className={`rounded-2xl border border-navy/8 border-l-4 ${tone === "teal" ? "border-l-teal" : tone === "gold" ? "border-l-gold" : "border-l-navy"} bg-white/90 px-4 py-3.5 shadow-sm`}><p className="text-[10px] font-bold uppercase tracking-[0.16em] text-muted">{label}</p><p className="mt-1 font-display text-2xl font-semibold text-navy-deep">{formatBrlFromCents(value)}</p></article>; }
function PositionCard({ row, aporte, busy, onAporte, onAdd, onDelete }: { row: InvestmentPosition; aporte: string; busy: boolean; onAporte: (v: string) => void; onAdd: () => void; onDelete: () => void }) { const market = ["stock", "fii", "etf", "crypto"].includes(row.instrument_type); return <article className={`group overflow-hidden rounded-3xl border border-navy/8 shadow-sm transition hover:-translate-y-1 hover:shadow-xl ${market ? "bg-gradient-to-br from-navy-deep to-navy text-white" : "bg-gradient-to-br from-white to-sage/40 text-navy"}`}><div className="p-5"><div className="flex justify-between gap-3"><span className={`rounded-full px-2.5 py-1 text-[10px] font-bold uppercase ${market ? "bg-white/12 text-gold" : "bg-teal/10 text-teal-deep"}`}>{typeLabel(row.instrument_type)}</span><button type="button" onClick={onDelete} className="text-xs opacity-60 hover:text-red-500 hover:opacity-100">Excluir</button></div><h3 className="mt-5 font-display text-2xl font-semibold">{row.name}</h3><p className={`mt-1 line-clamp-1 text-xs ${market ? "text-white/60" : "text-muted"}`}>{row.description || (market ? "Ativo de mercado" : "Renda fixa")}</p><p className="mt-5 text-3xl font-bold tabular-nums">{formatBrlFromCents(row.principal_cents)}</p><div className={`mt-4 grid grid-cols-2 gap-2 rounded-2xl p-3 text-xs ${market ? "bg-white/8" : "bg-white/70"}`}><span><small className="block opacity-60">Rentabilidade</small><b>{row.annual_rate_bps ? `${(row.annual_rate_bps / 100).toFixed(2)}% a.a.` : "Mercado"}</b></span><span><small className="block opacity-60">Liquidez</small><b>{row.is_liquid ? "Disponível" : row.maturity_date || "No vencimento"}</b></span></div></div><div className={`flex gap-2 border-t p-3 ${market ? "border-white/10" : "border-navy/8"}`}><input inputMode="numeric" value={aporte} onChange={(e) => onAporte(e.target.value)} placeholder="Novo aporte" className={`min-w-0 flex-1 rounded-xl px-3 py-2 text-sm ${market ? "bg-white/10 text-white placeholder:text-white/40" : "border border-navy/10 bg-white"}`} /><button type="button" disabled={busy || !aporte} onClick={onAdd} className="rounded-xl bg-gold px-3 text-xs font-bold text-navy-deep disabled:opacity-40">Aportar</button></div></article>; }
function ViewButton({ active, onClick, children }: { active: boolean; onClick: () => void; children: string }) { return <button type="button" onClick={onClick} className={`rounded-lg px-3 py-1.5 text-xs font-bold ${active ? "bg-navy-deep text-white" : "text-muted"}`}>{children}</button>; }
function FieldLabel({ children }: { children: string }) { return <span className="mb-1 block text-[10px] font-bold uppercase tracking-wide text-muted">{children}</span>; }
function typeLabel(type: string) { return TYPES.find(([key]) => key === type)?.[1] ?? type.replaceAll("_", " "); }
export function inferAssetType(raw: string): string | null { const value = raw.trim().toLocaleLowerCase("pt-BR"); if (/\b(bitcoin|btc|ethereum|ether|eth|solana|sol|bnb|xrp|cardano|ada|dogecoin|doge|litecoin|ltc|usdt|usdc|cripto|crypto)\b/.test(value)) return "crypto"; if (/^[a-z]{4}\d{1,2}$/i.test(value)) return "stock"; if (/\b(cdb|renda fixa|lci|lca|deb[eê]nture)\b/.test(value)) return "cdb"; if (/\b(tesouro|selic|ipca\+)\b/.test(value)) return "treasury"; return null; }
function messageOf(error: unknown, fallback: string) { return error instanceof ApiError ? error.message : error instanceof Error ? error.message : fallback; }
