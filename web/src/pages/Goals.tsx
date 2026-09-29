import { useEffect, useState, type FormEvent, type ReactNode } from "react";
import {
  contributeGoal,
  createGoal,
  deleteGoal,
  fetchGoalContributions,
  fetchGoals,
  fetchMe,
  refreshGoalPrice,
  searchGoalProducts,
  updateGoal,
  type Goal,
  type GoalContribution,
  type GoalProductHit,
} from "../api";
import {
  buildGoalPayload,
  canDeleteGoal,
  dateInputValue,
  goalProgress,
  type GoalDraft,
} from "../features/goals/model";
import { formatBrlFromCents, parseBrlToCents } from "../format";
import { GoalThumb } from "./home/GoalThumb";
import { ApiError, ErrorNote, InField, PageTitle, SwitchRow } from "./common";

const emptyDraft: GoalDraft = {
  title: "",
  target: "",
  initial: "",
  description: "",
  dueDate: "",
  isActive: true,
  isFamily: false,
  trackingEnabled: true,
  targetUrl: "",
  picked: null,
};

export function GoalsPage() {
  const [rows, setRows] = useState<Goal[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [familyMode, setFamilyMode] = useState(false);
  const [draft, setDraft] = useState<GoalDraft>(emptyDraft);
  const [editing, setEditing] = useState<Goal | null>(null);
  const [query, setQuery] = useState("");
  const [hits, setHits] = useState<GoalProductHit[]>([]);
  const [searching, setSearching] = useState(false);
  const [showSearch, setShowSearch] = useState(false);
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [contributions, setContributions] = useState<Record<string, GoalContribution[]>>({});
  const [contributionAmount, setContributionAmount] = useState<Record<string, string>>({});
  const [contributionNote, setContributionNote] = useState<Record<string, string>>({});
  const [actionId, setActionId] = useState<string | null>(null);
  const [deleteCandidate, setDeleteCandidate] = useState<Goal | null>(null);

  async function load() {
    setError(null);
    try {
      setRows(await fetchGoals());
    } catch (err) {
      setError(messageOf(err, "Falha ao carregar metas."));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load();
    void fetchMe().then((me) => setFamilyMode(Boolean(me.family_mode_enabled))).catch(() => undefined);
  }, []);

  function patchDraft(patch: Partial<GoalDraft>) {
    setDraft((current) => ({ ...current, ...patch }));
  }

  async function onSearch() {
    const term = (query.trim() || draft.title.trim());
    if (term.length < 2) {
      setError("Digite pelo menos dois caracteres para pesquisar um produto.");
      return;
    }
    setSearching(true);
    setError(null);
    try {
      setHits((await searchGoalProducts(term)).slice(0, 12));
    } catch (err) {
      setHits([]);
      setError(messageOf(err, "Não foi possível pesquisar produtos."));
    } finally {
      setSearching(false);
    }
  }

  function applyHit(hit: GoalProductHit) {
    patchDraft({
      picked: hit,
      title: hit.title.slice(0, 200),
      target: (hit.price_cents / 100).toFixed(2).replace(".", ","),
      targetUrl: hit.url,
    });
    setHits([]);
    setShowSearch(false);
  }

  async function saveGoal(e: FormEvent) {
    e.preventDefault();
    setError(null);
    const result = buildGoalPayload(draft, editing ?? undefined);
    if (!result.ok) {
      setError(result.message);
      return;
    }
    setBusy(true);
    try {
      if (editing) await updateGoal(editing.id, result.payload);
      else await createGoal(result.payload);
      resetForm();
      await load();
    } catch (err) {
      setError(messageOf(err, editing ? "Não foi possível atualizar a meta." : "Não foi possível criar a meta."));
    } finally {
      setBusy(false);
    }
  }

  function beginEdit(goal: Goal) {
    setEditing(goal);
    setDraft({
      title: goal.title,
      target: centsInput(goal.target_cents),
      initial: "",
      description: goal.description ?? "",
      dueDate: dateInputValue(goal.due_at),
      isActive: goal.is_active,
      isFamily: Boolean(goal.is_family),
      trackingEnabled: goal.tracking_enabled !== false,
      targetUrl: goal.target_url ?? "",
      picked: null,
    });
    setQuery("");
    setHits([]);
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function resetForm() {
    setEditing(null);
    setDraft(emptyDraft);
    setQuery("");
    setHits([]);
    setShowSearch(false);
  }

  async function toggleDetails(goal: Goal) {
    if (expandedId === goal.id) {
      setExpandedId(null);
      return;
    }
    setExpandedId(goal.id);
    if (goal.is_mine === false || contributions[goal.id]) return;
    try {
      const items = await fetchGoalContributions(goal.id);
      setContributions((current) => ({ ...current, [goal.id]: items }));
    } catch (err) {
      setError(messageOf(err, "Não foi possível carregar o histórico de aportes."));
    }
  }

  async function addContribution(goal: Goal, e: FormEvent) {
    e.preventDefault();
    const cents = parseBrlToCents(contributionAmount[goal.id] ?? "");
    if (!cents) {
      setError("Indique um valor de aporte maior que zero.");
      return;
    }
    setActionId(goal.id);
    setError(null);
    try {
      await contributeGoal(goal.id, cents, contributionNote[goal.id]);
      const items = await fetchGoalContributions(goal.id);
      setContributions((current) => ({ ...current, [goal.id]: items }));
      setContributionAmount((current) => ({ ...current, [goal.id]: "" }));
      setContributionNote((current) => ({ ...current, [goal.id]: "" }));
      await load();
    } catch (err) {
      setError(messageOf(err, "Não foi possível registrar o aporte."));
    } finally {
      setActionId(null);
    }
  }

  async function refreshPrice(goal: Goal) {
    setActionId(goal.id);
    setError(null);
    try {
      await refreshGoalPrice(goal.id);
      await load();
    } catch (err) {
      setError(messageOf(err, "Não foi possível atualizar o preço de referência."));
    } finally {
      setActionId(null);
    }
  }

  async function archiveGoal(goal: Goal) {
    setActionId(goal.id);
    try {
      await updateGoal(goal.id, { is_active: false });
      await load();
    } catch (err) {
      setError(messageOf(err, "Não foi possível arquivar a meta."));
    } finally {
      setActionId(null);
    }
  }

  async function confirmDelete() {
    const goal = deleteCandidate;
    if (!goal) return;
    setActionId(goal.id);
    try {
      await deleteGoal(goal.id);
      setDeleteCandidate(null);
      if (editing?.id === goal.id) resetForm();
      await load();
    } catch (err) {
      setError(messageOf(err, "Não foi possível excluir a meta."));
    } finally {
      setActionId(null);
    }
  }

  return (
    <div className="mx-auto max-w-7xl space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <PageTitle kicker="Planejamento" title="Metas" />
        <p className="max-w-lg text-sm text-muted">
          Transforme objetivos em planos claros, acompanhe aportes e preços de referência.
        </p>
      </div>
      <ErrorNote message={error} />

      <div className="grid items-start gap-6 xl:grid-cols-[minmax(0,0.92fr)_minmax(0,1.08fr)]">
      <form className="min-w-0 space-y-4 rounded-3xl border border-navy/8 bg-white p-4 shadow-[0_18px_55px_rgba(20,28,42,0.08)] sm:p-5" onSubmit={saveGoal}>
        <div className="flex flex-wrap items-start justify-between gap-3 border-b border-navy/8 pb-4">
          <div>
            <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-gold-pressed">
              {editing ? "Editando" : "Novo objetivo"}
            </p>
            <h2 className="mt-1 font-serif text-2xl text-navy-deep">
              {editing ? editing.title : "Criar uma meta"}
            </h2>
          </div>
          {editing ? <SecondaryButton onClick={resetForm}>Cancelar edição</SecondaryButton> : null}
        </div>

        <section className="grid gap-3 rounded-2xl bg-cream/35 p-3.5 sm:grid-cols-2 sm:p-4">
          <InField label="Título" value={draft.title} required onChange={(title) => patchDraft({ title: title.slice(0, 200) })} hint={`${draft.title.length}/200`} />
          <InField label="Valor-alvo (R$)" value={draft.target} required placeholder="Ex.: 5.000,00" onChange={(target) => patchDraft({ target })} />
          {!editing ? <InField label="Valor inicial (R$)" value={draft.initial} placeholder="Opcional" onChange={(initial) => patchDraft({ initial })} /> : null}
          <InField label="Data-alvo" type="date" value={draft.dueDate} onChange={(dueDate) => patchDraft({ dueDate })} hint="Opcional" />
          <label className="block md:col-span-2">
            <span className="mb-1 block text-xs uppercase tracking-wide text-muted">Descrição</span>
            <textarea className="min-h-24 w-full resize-y rounded-xl border border-navy/10 bg-white px-3 py-2.5 text-sm text-navy outline-none ring-gold/40 focus:ring-2" value={draft.description} maxLength={1000} onChange={(e) => patchDraft({ description: e.target.value })} placeholder="Por que esta meta é importante?" />
            <span className="mt-1 block text-[11px] text-muted">{draft.description.length}/1000</span>
          </label>
        </section>

        <section className="space-y-3 rounded-2xl border border-navy/8 p-3.5 sm:p-4">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h3 className="font-serif text-lg text-navy-deep">Produto e preço de referência</h3>
              <p className="text-xs leading-5 text-muted">Opcional. A busca preenche nome, preço, link e miniatura.</p>
            </div>
            <SecondaryButton onClick={() => setShowSearch((value) => !value)}>{showSearch ? "Fechar busca" : "Pesquisar produto"}</SecondaryButton>
          </div>
          {showSearch ? (
            <div className="space-y-3 rounded-2xl bg-sage/45 p-3">
              <div className="flex flex-col gap-2 sm:flex-row">
                <div className="min-w-0 flex-1"><InField label="Nome do produto" value={query} onChange={setQuery} placeholder="Ex.: notebook para trabalho" /></div>
                <button type="button" disabled={searching} onClick={() => void onSearch()} className="self-end rounded-xl bg-teal px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-teal-deep disabled:opacity-60">
                  {searching ? "Pesquisando…" : "Pesquisar"}
                </button>
              </div>
              {hits.length > 0 ? (
                <ul className="grid max-h-96 gap-2 overflow-y-auto pr-1 sm:grid-cols-2">
                  {hits.map((hit) => (
                    <li key={`${hit.url}-${hit.price_cents}`}>
                      <button type="button" className="flex h-full w-full items-center gap-3 rounded-2xl border border-navy/8 bg-white p-3 text-left transition hover:-translate-y-0.5 hover:border-teal/40 hover:shadow-md" onClick={() => applyHit(hit)}>
                        <GoalThumb url={hit.thumbnail} alt={hit.title} className="h-14 w-14" />
                        <span className="min-w-0 flex-1"><span className="line-clamp-2 block text-sm font-medium text-navy">{hit.title}</span><span className="mt-1 block text-xs text-muted">{hit.source}</span></span>
                        <span className="shrink-0 text-sm font-bold tabular-nums text-navy-deep">{formatBrlFromCents(hit.price_cents)}</span>
                      </button>
                    </li>
                  ))}
                </ul>
              ) : null}
            </div>
          ) : null}
          {draft.picked || draft.targetUrl ? (
            <div className="flex items-center gap-3 rounded-2xl border border-gold/25 bg-gold/8 p-3">
              <GoalThumb url={draft.picked?.thumbnail ?? editing?.reference_thumbnail_url} alt={draft.title} className="h-16 w-16" />
              <div className="min-w-0 flex-1"><p className="truncate text-sm font-semibold text-navy">{draft.picked?.title ?? editing?.reference_product_name ?? draft.title}</p><p className="mt-1 text-xs text-muted">Referência selecionada; título e valor ainda podem ser ajustados.</p></div>
              {draft.picked ? <SecondaryButton onClick={() => patchDraft({ picked: null, targetUrl: "" })}>Remover</SecondaryButton> : null}
            </div>
          ) : null}
          <InField label="Link do produto" value={draft.targetUrl} onChange={(targetUrl) => patchDraft({ targetUrl })} placeholder="https://… (opcional)" />
        </section>

        <section className="grid gap-3 rounded-2xl border border-navy/8 p-3.5 sm:grid-cols-2 sm:p-4">
          <SwitchRow label="Meta ativa" sub="Metas arquivadas continuam no histórico." checked={draft.isActive} onChange={(isActive) => patchDraft({ isActive })} />
          <SwitchRow label="Atualizar preço automaticamente" sub="Acompanha mudanças do produto de referência." checked={draft.trackingEnabled} onChange={(trackingEnabled) => patchDraft({ trackingEnabled })} />
          {familyMode ? <SwitchRow label="Meta da família" sub="Fica visível para os membros convidados." checked={draft.isFamily} onChange={(isFamily) => patchDraft({ isFamily })} /> : null}
        </section>

        <div className="flex justify-end border-t border-navy/8 pt-5">
          <button type="submit" disabled={busy} className="min-h-12 w-full rounded-xl bg-gold px-8 py-3 text-sm font-bold text-navy-deep shadow-[0_8px_22px_rgba(201,169,78,0.28)] transition hover:-translate-y-0.5 hover:bg-gold/90 disabled:opacity-60 sm:w-auto">
            {busy ? "Salvando…" : editing ? "Salvar alterações" : "Criar meta"}
          </button>
        </div>
      </form>

      <section className="min-w-0 space-y-3 xl:sticky xl:top-6">
        <div className="flex items-center justify-between"><h2 className="font-serif text-xl text-navy-deep">Suas metas</h2><span className="text-xs text-muted">{rows.length} no total</span></div>
        {loading ? <div className="rounded-2xl border border-navy/8 bg-white p-8 text-center text-sm text-muted">Carregando metas…</div> : null}
        {!loading && rows.length === 0 ? <div className="rounded-2xl border border-dashed border-navy/15 bg-white/70 px-4 py-10 text-center text-sm text-muted">Nenhuma meta criada ainda.</div> : null}
        <ul className="space-y-4 xl:max-h-[calc(100vh-11rem)] xl:overflow-y-auto xl:pr-1">
          {rows.map((goal) => (
            <GoalCard
              key={goal.id}
              goal={goal}
              expanded={expandedId === goal.id}
              contributions={contributions[goal.id] ?? []}
              amount={contributionAmount[goal.id] ?? ""}
              note={contributionNote[goal.id] ?? ""}
              busy={actionId === goal.id}
              onToggle={() => void toggleDetails(goal)}
              onEdit={() => beginEdit(goal)}
              onDelete={() => setDeleteCandidate(goal)}
              onArchive={() => void archiveGoal(goal)}
              onRefresh={() => void refreshPrice(goal)}
              onAmount={(value) => setContributionAmount((current) => ({ ...current, [goal.id]: value }))}
              onNote={(value) => setContributionNote((current) => ({ ...current, [goal.id]: value }))}
              onContribute={(e) => void addContribution(goal, e)}
            />
          ))}
        </ul>
      </section>
      </div>

      {deleteCandidate ? <ConfirmDelete goal={deleteCandidate} busy={actionId === deleteCandidate.id} onCancel={() => setDeleteCandidate(null)} onConfirm={() => void confirmDelete()} /> : null}
    </div>
  );
}

function GoalCard({ goal, expanded, contributions, amount, note, busy, onToggle, onEdit, onDelete, onArchive, onRefresh, onAmount, onNote, onContribute }: {
  goal: Goal; expanded: boolean; contributions: GoalContribution[]; amount: string; note: string; busy: boolean;
  onToggle: () => void; onEdit: () => void; onDelete: () => void; onArchive: () => void; onRefresh: () => void;
  onAmount: (value: string) => void; onNote: (value: string) => void; onContribute: (e: FormEvent) => void;
}) {
  const pct = goalProgress(goal);
  const mine = goal.is_mine !== false;
  return (
    <li className="overflow-hidden rounded-3xl border border-navy/8 bg-white shadow-[0_10px_35px_rgba(20,28,42,0.05)]">
      <div className="p-4 sm:p-5">
        <div className="flex items-start gap-4">
          <GoalThumb url={goal.reference_thumbnail_url} alt={goal.title} className="h-20 w-20" />
          <div className="min-w-0 flex-1">
            <div className="flex items-start justify-between gap-2"><div><h3 className="line-clamp-2 font-serif text-xl text-navy-deep">{goal.title}</h3><div className="mt-1 flex flex-wrap gap-1.5"><Badge>{goal.is_active ? "Ativa" : "Arquivada"}</Badge>{goal.is_family ? <Badge>Família</Badge> : null}{!mine ? <Badge>Compartilhada</Badge> : null}</div></div><span className="rounded-full bg-gold/15 px-2.5 py-1 text-xs font-bold text-navy">{pct}%</span></div>
            <p className="mt-3 text-sm font-semibold tabular-nums text-navy">{formatBrlFromCents(goal.current_cents)} <span className="font-normal text-muted">de {formatBrlFromCents(goal.target_cents)}</span></p>
            <div className="mt-2 h-2.5 overflow-hidden rounded-full bg-cream-muted"><div className="h-full rounded-full bg-gradient-to-r from-gold to-teal transition-[width]" style={{ width: `${pct}%` }} /></div>
          </div>
        </div>
        {goal.description ? <p className="mt-4 line-clamp-2 text-sm leading-6 text-muted">{goal.description}</p> : null}
        <div className="mt-4 flex flex-wrap gap-2">
          <SecondaryButton onClick={onToggle}>{expanded ? "Fechar detalhes" : "Ver detalhes"}</SecondaryButton>
          {mine ? <SecondaryButton onClick={onEdit}>Editar</SecondaryButton> : null}
          {mine && (goal.target_url || goal.reference_product_name) ? <SecondaryButton onClick={onRefresh} disabled={busy}>{busy ? "Atualizando…" : "Atualizar preço"}</SecondaryButton> : null}
          {mine && canDeleteGoal(goal) ? <DangerButton onClick={onDelete}>Excluir</DangerButton> : null}
          {mine && !canDeleteGoal(goal) && goal.is_active ? <SecondaryButton onClick={onArchive} disabled={busy}>Arquivar</SecondaryButton> : null}
        </div>
      </div>
      {expanded ? (
        <div className="space-y-4 border-t border-navy/8 bg-cream/25 p-4 sm:p-5">
          <div className="grid gap-2 text-xs text-muted sm:grid-cols-2">
            <p>Data-alvo: <strong className="text-navy">{formatDate(goal.due_at) || "Não definida"}</strong></p>
            <p>Preço de referência: <strong className="text-navy">{goal.reference_price_cents ? formatBrlFromCents(goal.reference_price_cents) : "Não definido"}</strong></p>
          </div>
          {goal.target_url ? <a href={goal.target_url} target="_blank" rel="noreferrer" className="inline-flex text-xs font-semibold text-teal-deep underline decoration-teal/30 underline-offset-4">Abrir produto de referência</a> : null}
          {mine ? (
            <form className="grid gap-2 rounded-2xl border border-navy/8 bg-white p-3 sm:grid-cols-[1fr_1fr_auto]" onSubmit={onContribute}>
              <input className="rounded-xl border border-navy/10 px-3 py-2.5 text-sm outline-none ring-gold/40 focus:ring-2" placeholder="Valor do aporte" value={amount} onChange={(e) => onAmount(e.target.value)} />
              <input className="rounded-xl border border-navy/10 px-3 py-2.5 text-sm outline-none ring-gold/40 focus:ring-2" placeholder="Nota opcional" maxLength={500} value={note} onChange={(e) => onNote(e.target.value)} />
              <button disabled={busy} className="rounded-xl bg-navy-deep px-4 py-2.5 text-sm font-semibold text-cream disabled:opacity-60">{busy ? "Salvando…" : "Aportar"}</button>
            </form>
          ) : null}
          {mine ? <div><p className="mb-2 text-xs font-semibold uppercase tracking-wide text-muted">Histórico de aportes</p>{contributions.length ? <ul className="space-y-1.5">{contributions.slice(0, 8).map((item) => <li key={item.id} className="flex items-start justify-between gap-3 rounded-xl bg-white px-3 py-2 text-sm"><span><strong className="text-navy">{formatBrlFromCents(item.amount_cents)}</strong>{item.note ? <span className="ml-2 text-muted">{item.note}</span> : null}</span><time className="shrink-0 text-xs text-muted">{formatDate(item.recorded_at)}</time></li>)}</ul> : <p className="text-sm text-muted">Nenhum aporte registrado.</p>}</div> : null}
        </div>
      ) : null}
    </li>
  );
}

function ConfirmDelete({ goal, busy, onCancel, onConfirm }: { goal: Goal; busy: boolean; onCancel: () => void; onConfirm: () => void }) {
  return <div className="fixed inset-0 z-50 grid place-items-center bg-navy-deep/55 p-4" role="presentation" onMouseDown={(e) => { if (e.target === e.currentTarget) onCancel(); }}><div role="dialog" aria-modal="true" aria-labelledby="delete-goal-title" className="w-full max-w-md rounded-3xl bg-white p-6 shadow-2xl"><h2 id="delete-goal-title" className="font-serif text-2xl text-navy-deep">Excluir meta?</h2><p className="mt-2 text-sm leading-6 text-muted">“{goal.title}” será removida definitivamente. Esta ação só está disponível porque a meta não possui saldo.</p><div className="mt-6 flex justify-end gap-2"><SecondaryButton onClick={onCancel}>Cancelar</SecondaryButton><button type="button" disabled={busy} onClick={onConfirm} className="rounded-xl bg-red-700 px-4 py-2.5 text-sm font-semibold text-white hover:bg-red-800 disabled:opacity-60">{busy ? "Excluindo…" : "Excluir definitivamente"}</button></div></div></div>;
}

function SecondaryButton({ children, onClick, disabled = false }: { children: ReactNode; onClick: () => void; disabled?: boolean }) {
  return <button type="button" disabled={disabled} onClick={onClick} className="rounded-xl border border-navy/10 bg-white px-3 py-2 text-xs font-semibold text-navy transition hover:-translate-y-0.5 hover:border-gold/50 hover:bg-gold/8 disabled:opacity-60">{children}</button>;
}

function DangerButton({ children, onClick }: { children: ReactNode; onClick: () => void }) {
  return <button type="button" onClick={onClick} className="rounded-xl border border-red-200 bg-red-50 px-3 py-2 text-xs font-semibold text-red-700 transition hover:bg-red-100">{children}</button>;
}

function Badge({ children }: { children: ReactNode }) {
  return <span className="rounded-full bg-sage px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide text-teal-deep">{children}</span>;
}

function centsInput(cents: number): string {
  return (cents / 100).toFixed(2).replace(".", ",");
}

function formatDate(raw: string | null | undefined): string {
  if (!raw) return "";
  const date = new Date(raw);
  return Number.isNaN(date.getTime()) ? raw.slice(0, 10) : date.toLocaleDateString("pt-BR");
}

function messageOf(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : error instanceof Error ? error.message : fallback;
}
