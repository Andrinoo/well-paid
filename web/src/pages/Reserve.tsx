import { useEffect, useState, type FormEvent } from "react";
import { createReservePlan, deleteReservePlan, fetchReservePlans, moveReserveBalance, updateReservePlan, type ReservePlan } from "../api";
import { formatBrlFromCents, maskBrlInput, todayIso } from "../format";
import { ApiError, ErrorNote, PageTitle, parseBrlToCents } from "./common";

type Movement = { kind: "deposit" | "withdraw"; amount: string; note: string };

export function ReservePage() {
  const [rows, setRows] = useState<ReservePlan[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [showCreate, setShowCreate] = useState(false);
  const [editing, setEditing] = useState<ReservePlan | null>(null);
  const [title, setTitle] = useState("");
  const [target, setTarget] = useState("");
  const [monthly, setMonthly] = useState("");
  const [opening, setOpening] = useState("");
  const [details, setDetails] = useState("");
  const [movements, setMovements] = useState<Record<string, Movement>>({});
  const [actionId, setActionId] = useState<string | null>(null);

  async function load() {
    setLoading(true); setError(null);
    try { setRows(await fetchReservePlans()); }
    catch (err) { setError(messageOf(err, "Falha ao carregar os cofrinhos.")); }
    finally { setLoading(false); }
  }
  useEffect(() => { void load(); }, []);

  function resetForm() {
    setEditing(null); setShowCreate(false); setTitle(""); setTarget(""); setMonthly(""); setOpening(""); setDetails("");
  }
  function edit(plan: ReservePlan) {
    setEditing(plan); setShowCreate(true); setTitle(plan.title); setTarget(inputMoney(plan.target_cents)); setMonthly(inputMoney(plan.monthly_target_cents)); setOpening(""); setDetails(plan.details || "");
    window.scrollTo({ top: 0, behavior: "smooth" });
  }
  async function save(event: FormEvent) {
    event.preventDefault(); setError(null);
    if (!title.trim()) { setError("Dê um nome ao cofrinho."); return; }
    const body = { title: title.trim(), monthly_target_cents: parseBrlToCents(monthly), target_cents: parseBrlToCents(target) || null, details: details.trim() || null };
    setBusy(true);
    try {
      if (editing) await updateReservePlan(editing.id, body);
      else await createReservePlan({ ...body, opening_balance_cents: parseBrlToCents(opening) || null });
      resetForm(); await load();
    } catch (err) { setError(messageOf(err, "Não foi possível guardar o cofrinho.")); }
    finally { setBusy(false); }
  }
  function movementFor(id: string): Movement { return movements[id] ?? { kind: "deposit", amount: "", note: "" }; }
  function patchMovement(id: string, patch: Partial<Movement>) { const normalized = patch.amount == null ? patch : { ...patch, amount: maskBrlInput(patch.amount) }; setMovements((current) => ({ ...current, [id]: { ...movementFor(id), ...normalized } })); }
  async function move(plan: ReservePlan, event: FormEvent) {
    event.preventDefault(); const movement = movementFor(plan.id); const cents = parseBrlToCents(movement.amount);
    if (!cents) { setError("Informe um valor maior que zero."); return; }
    if (movement.kind === "withdraw" && cents > plan.balance_cents) { setError("A retirada não pode ser maior que o saldo do cofrinho."); return; }
    setActionId(plan.id); setError(null);
    try { await moveReserveBalance(plan.id, movement.kind === "withdraw" ? -cents : cents, todayIso(), movement.note); setMovements((current) => ({ ...current, [plan.id]: { ...movement, amount: "", note: "" } })); await load(); }
    catch (err) { setError(messageOf(err, "Não foi possível registrar a movimentação.")); }
    finally { setActionId(null); }
  }
  async function remove(plan: ReservePlan) {
    if (!window.confirm(`Apagar o cofrinho “${plan.title}” e todo o histórico?`)) return;
    setActionId(plan.id);
    try { await deleteReservePlan(plan.id); await load(); }
    catch (err) { setError(messageOf(err, "Não foi possível apagar o cofrinho.")); }
    finally { setActionId(null); }
  }

  const total = rows.filter((row) => row.status === "active").reduce((sum, row) => sum + row.balance_cents, 0);
  const totalTargets = rows.reduce((sum, row) => sum + (row.target_cents || 0), 0);
  return (
    <div className="-mx-4 -my-5 min-h-full bg-gradient-to-br from-paper via-paper to-sage/45 px-4 py-5 sm:-mx-6 sm:px-6">
      <div className="flex flex-wrap items-end justify-between gap-4"><div><PageTitle kicker="Suas economias" title="Cofrinho" /><p className="mt-2 max-w-2xl text-sm text-muted">Separe dinheiro para o que importa, no seu ritmo. Deposite ou retire quando precisar.</p></div><button type="button" onClick={() => { if (showCreate) resetForm(); else setShowCreate(true); }} className="rounded-xl bg-gold px-4 py-2.5 text-sm font-bold text-navy-deep shadow-sm transition hover:-translate-y-0.5">{showCreate ? "Fechar cadastro" : "+ Novo cofrinho"}</button></div>
      <div className="mt-4"><ErrorNote message={error} /></div>
      {showCreate ? <form onSubmit={save} className="wp-rise mt-4 overflow-hidden rounded-2xl border border-navy/8 bg-white shadow-lg"><header className="flex items-center justify-between border-b border-navy/8 bg-cream/25 px-4 py-3"><div><h2 className="font-serif text-lg text-navy-deep">{editing ? "Editar cofrinho" : "Criar cofrinho"}</h2><p className="text-xs text-muted">A meta e o valor mensal são opcionais.</p></div>{editing ? <button type="button" onClick={resetForm} className="text-xs font-semibold text-muted">Cancelar</button> : null}</header><div className="grid items-end gap-2 p-4 md:grid-cols-2 xl:grid-cols-[minmax(200px,1fr)_150px_150px_150px_minmax(220px,1fr)_auto]"><Field label="Nome" value={title} placeholder="Ex.: Viagem" onChange={setTitle} /><Field label="Objetivo total" value={target} placeholder="Opcional" onChange={setTarget} /><Field label="Guardar por mês" value={monthly} placeholder="Opcional" onChange={setMonthly} />{!editing ? <Field label="Já economizado" value={opening} placeholder="Opcional" onChange={setOpening} /> : <div className="hidden xl:block" />}<Field label="Descrição" value={details} placeholder="Opcional" onChange={setDetails} /><button disabled={busy} className="h-10 rounded-lg bg-teal px-5 text-sm font-bold text-white disabled:opacity-50">{busy ? "Guardando…" : editing ? "Salvar" : "Criar"}</button></div></form> : null}
      <section className="mt-4 grid gap-3 sm:grid-cols-3"><Metric label="Total guardado" value={total} detail={`${rows.length} cofrinhos`} tone="teal" /><Metric label="Objetivos somados" value={totalTargets} detail="Metas definidas" tone="gold" /><Metric label="Disponível" value={total} detail="Pode retirar quando precisar" tone="navy" /></section>
      <section className="mt-4 overflow-hidden rounded-[1.6rem] border border-navy/8 bg-white/90 shadow-[0_18px_55px_rgba(20,28,42,0.08)]"><header className="border-b border-navy/8 bg-gradient-to-r from-white via-white to-sage/45 p-5"><h2 className="font-serif text-xl text-navy-deep">Seus cofrinhos</h2><p className="mt-1 text-xs text-muted">Economias flexíveis, sem calendário obrigatório.</p></header>{loading ? <div className="p-12 text-center text-sm text-muted">Carregando cofrinhos…</div> : rows.length === 0 ? <div className="p-14 text-center"><PiggyIcon className="mx-auto h-12 w-12 text-teal" /><p className="mt-3 font-semibold text-navy">Seu primeiro cofrinho começa aqui</p><p className="mt-1 text-sm text-muted">Crie um objetivo ou apenas comece a guardar.</p></div> : <ul className="grid gap-4 p-4 lg:grid-cols-2 2xl:grid-cols-3 sm:p-5">{rows.map((plan, index) => <SavingsCard key={plan.id} plan={plan} movement={movementFor(plan.id)} busy={actionId === plan.id} index={index} onPatch={(patch) => patchMovement(plan.id, patch)} onMove={(event) => void move(plan, event)} onEdit={() => edit(plan)} onDelete={() => void remove(plan)} />)}</ul>}</section>
    </div>
  );
}

function SavingsCard({ plan, movement, busy, index, onPatch, onMove, onEdit, onDelete }: { plan: ReservePlan; movement: Movement; busy: boolean; index: number; onPatch: (patch: Partial<Movement>) => void; onMove: (event: FormEvent) => void; onEdit: () => void; onDelete: () => void }) {
  const target = plan.target_cents || 0; const pct = target ? Math.min(100, Math.round(plan.balance_cents / target * 100)) : null;
  return <li className="expense-list-row self-start overflow-hidden rounded-3xl border border-navy/8 bg-white shadow-sm transition duration-300 hover:-translate-y-1 hover:shadow-xl" style={{ ["--expense-delay" as string]: `${index * 40}ms` }}><div className="bg-gradient-to-br from-teal to-teal-deep p-5 text-white"><div className="flex items-start justify-between gap-3"><span className="grid h-11 w-11 place-items-center rounded-2xl bg-white/15"><PiggyIcon className="h-6 w-6" /></span><div className="flex gap-1"><SmallAction label="Editar" kind="edit" onClick={onEdit} /><SmallAction label="Apagar" kind="delete" onClick={onDelete} danger /></div></div><h3 className="mt-4 font-display text-xl font-semibold">{plan.title || "Meu cofrinho"}</h3><p className="mt-1 text-3xl font-bold tabular-nums">{formatBrlFromCents(plan.balance_cents)}</p><p className="mt-1 text-xs text-white/70">saldo disponível</p>{pct != null ? <><div className="mt-4 h-2 overflow-hidden rounded-full bg-white/20"><div className="h-full rounded-full bg-gold transition-[width] duration-700" style={{ width: `${pct}%` }} /></div><div className="mt-1 flex justify-between text-[11px] text-white/75"><span>{pct}% alcançado</span><span>meta {formatBrlFromCents(target)}</span></div></> : null}</div><div className="p-4">{plan.details ? <p className="mb-3 line-clamp-2 text-xs text-muted">{plan.details}</p> : null}{plan.monthly_target_cents > 0 ? <p className="mb-3 text-xs text-muted">Sugestão mensal: <strong className="text-navy">{formatBrlFromCents(plan.monthly_target_cents)}</strong></p> : null}<div className="mb-3 grid grid-cols-2 rounded-xl bg-cream/60 p-1"><button type="button" onClick={() => onPatch({ kind: "deposit" })} className={`rounded-lg py-2 text-xs font-bold transition ${movement.kind === "deposit" ? "bg-teal text-white shadow-sm" : "text-muted"}`}>Depositar</button><button type="button" onClick={() => onPatch({ kind: "withdraw" })} className={`rounded-lg py-2 text-xs font-bold transition ${movement.kind === "withdraw" ? "bg-gold text-navy-deep shadow-sm" : "text-muted"}`}>Retirar</button></div><form onSubmit={onMove} className="grid gap-2 sm:grid-cols-[1fr_auto]"><input value={movement.amount} onChange={(event) => onPatch({ amount: event.target.value })} placeholder="R$ 0,00" className="h-10 min-w-0 rounded-xl border border-navy/10 px-3 text-sm" /><button disabled={busy} className={`h-10 rounded-xl px-4 text-xs font-bold disabled:opacity-50 ${movement.kind === "deposit" ? "bg-teal text-white" : "bg-gold text-navy-deep"}`}>{busy ? "…" : movement.kind === "deposit" ? "Guardar" : "Retirar"}</button><input value={movement.note} onChange={(event) => onPatch({ note: event.target.value })} placeholder="Nota opcional" className="h-9 min-w-0 rounded-xl border border-navy/8 px-3 text-xs sm:col-span-2" /></form></div></li>;
}
function Field({ label, value, placeholder, onChange }: { label: string; value: string; placeholder?: string; onChange: (value: string) => void }) { const money = label !== "Nome" && label !== "Descrição"; return <label><span className="mb-1 block text-[10px] font-bold uppercase tracking-wide text-muted">{label}</span><input required={label === "Nome"} inputMode={money ? "numeric" : undefined} value={value} placeholder={money ? "R$ 0,00" : placeholder} onChange={(event) => onChange(money ? maskBrlInput(event.target.value) : event.target.value)} className="h-10 w-full rounded-lg border border-navy/10 bg-white px-3 text-sm outline-none focus:border-teal/50" /></label>; }
function Metric({ label, value, detail, tone }: { label: string; value: number; detail: string; tone: "teal" | "gold" | "navy" }) { const border = tone === "teal" ? "border-l-teal" : tone === "gold" ? "border-l-gold" : "border-l-navy"; return <article className={`rounded-2xl border border-navy/8 border-l-4 ${border} bg-white/90 px-4 py-3.5 shadow-sm`}><p className="text-[10px] font-bold uppercase tracking-[0.16em] text-muted">{label}</p><p className="mt-1 font-display text-2xl font-semibold text-navy-deep">{formatBrlFromCents(value)}</p><p className="mt-1 text-xs text-muted">{detail}</p></article>; }
function SmallAction({ label, kind, onClick, danger }: { label: string; kind: "edit" | "delete"; onClick: () => void; danger?: boolean }) { return <button type="button" title={label} aria-label={label} onClick={onClick} className={`grid h-8 w-8 place-items-center rounded-lg ${danger ? "bg-red-800/60 text-white hover:bg-red-800" : "bg-white/15 text-white hover:bg-white/25"}`}><svg viewBox="0 0 24 24" className="h-4 w-4" fill="none" stroke="currentColor" strokeWidth="1.8">{kind === "edit" ? <path d="M4 20h4l10.5-10.5a2.1 2.1 0 0 0-4-4L4 16v4Z" /> : <path d="M4 7h16M9 7V4h6v3M7 7l1 13h8l1-13" />}</svg></button>; }
function PiggyIcon({ className }: { className: string }) { return <svg viewBox="0 0 24 24" className={className} fill="none" stroke="currentColor" strokeWidth="1.7"><path d="M5 10a7 7 0 0 1 7-5h2a6 6 0 0 1 5.7 4H22v5h-2.2a7 7 0 0 1-2.8 3v3h-3v-2H9v2H6v-3.2A6 6 0 0 1 5 10Z"/><path d="M13 8h3M5 11H2v-2M17.5 11h.01"/></svg>; }
function inputMoney(value?: number | null) { return value ? formatBrlFromCents(value) : ""; }
function messageOf(error: unknown, fallback: string) { return error instanceof ApiError ? error.message : error instanceof Error ? error.message : fallback; }
