import { useEffect, useState, type FormEvent } from "react";
import {
  createExpense,
  fetchFamilyMe,
  fetchMe,
  type Category,
} from "../api";
import { formatBrlFromCents, parseBrlToCents, todayIso } from "../format";
import {
  amountSplit,
  amountToOwnerPercent,
  buildExpensePayload,
  defaultAmountSplit,
  formatBrlInput,
  formatPercentInput,
  percentSplit,
  type ExpenseFrequency,
  type ExpenseKind,
} from "../features/expenses/create-model";
import { ApiError } from "./common";

type Peer = { user_id: string; label: string };

export function ExpenseCreateForm({
  categories,
  onCreated,
}: {
  categories: Category[];
  onCreated: () => Promise<void> | void;
}) {
  const [kind, setKind] = useState<ExpenseKind>("single");
  const [description, setDescription] = useState("");
  const [amount, setAmount] = useState("");
  const [paid, setPaid] = useState(false);
  const [hasDue, setHasDue] = useState(false);
  const [expenseDate, setExpenseDate] = useState(todayIso());
  const [dueDate, setDueDate] = useState(todayIso());
  const [interest, setInterest] = useState("");
  const [installments, setInstallments] = useState("2");
  const [freq, setFreq] = useState<ExpenseFrequency>("monthly");
  const [categoryId, setCategoryId] = useState(categories[0]?.id ?? "");
  const [familyMode, setFamilyMode] = useState(false);
  const [isFamily, setIsFamily] = useState(false);
  const [share, setShare] = useState(false);
  const [peers, setPeers] = useState<Peer[]>([]);
  const [peerId, setPeerId] = useState("");
  const [splitPercent, setSplitPercent] = useState(false);
  const [ownerPart, setOwnerPart] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [showAdvanced, setShowAdvanced] = useState(false);
  const [familyLoading, setFamilyLoading] = useState(true);

  useEffect(() => {
    if (!categoryId && categories[0]) setCategoryId(categories[0].id);
  }, [categories, categoryId]);

  useEffect(() => {
    void (async () => {
      try {
        const [me, fam] = await Promise.all([fetchMe(), fetchFamilyMe()]);
        const enabled = Boolean(me.family_mode_enabled);
        setFamilyMode(enabled);
        const members = fam.family?.members ?? [];
        const others = members
          .filter((m) => !m.is_self)
          .map((m) => ({
            user_id: m.user_id,
            label: m.full_name || m.email,
          }));
        setPeers(others);
        if (others.length === 1) setPeerId(others[0].user_id);
      } catch {
        setFamilyMode(false);
      } finally {
        setFamilyLoading(false);
      }
    })();
  }, []);

  function onKind(next: ExpenseKind) {
    setKind(next);
    if (next !== "single") {
      setHasDue(true);
      if (!dueDate) setDueDate(todayIso());
    }
  }

  function onShare(next: boolean) {
    setShare(next);
    if (!next) {
      setPeerId(peers.length === 1 ? peers[0].user_id : "");
      setSplitPercent(false);
      setOwnerPart("");
      return;
    }
    if (!peerId && peers.length === 1) setPeerId(peers[0].user_id);
    const total = parseBrlToCents(amount);
    if (total > 0) setOwnerPart(formatBrlInput(defaultAmountSplit(total)[0]));
  }

  function onSplitMode(nextPercent: boolean) {
    const total = parseBrlToCents(amount);
    if (nextPercent) {
      setOwnerPart(amountToOwnerPercent(total, ownerPart));
    } else if (total > 0) {
      const split = percentSplit(total, ownerPart || "50");
      setOwnerPart(formatBrlInput(split?.ownerCents ?? defaultAmountSplit(total)[0]));
    } else {
      setOwnerPart("");
    }
    setSplitPercent(nextPercent);
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    const result = buildExpensePayload({
      kind,
      description,
      amount,
      paid,
      hasDue,
      expenseDate,
      dueDate,
      interest,
      installments,
      frequency: freq,
      categoryId,
      familyMode,
      isFamily,
      share,
      peerId,
      splitPercent,
      ownerPart,
    });
    if (!result.ok) {
      setError(result.message);
      return;
    }

    setBusy(true);
    try {
      await createExpense(result.payload);
      setDescription("");
      setAmount("");
      setPaid(false);
      setInterest("");
      setInstallments("2");
      setOwnerPart("");
      setShare(false);
      setSplitPercent(false);
      await onCreated();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Não foi possível criar.");
    } finally {
      setBusy(false);
    }
  }

  const canShare = familyMode && peers.length >= 1;
  const totalCents = parseBrlToCents(amount);
  const valueSplit = share && !splitPercent ? amountSplit(totalCents, ownerPart) : null;
  const percentageSplit = share && splitPercent
    ? percentSplit(totalCents, ownerPart || "50")
    : null;

  return (
    <form
      className="overflow-hidden rounded-2xl border border-navy/8 bg-white shadow-[0_12px_38px_rgba(20,28,42,0.06)]"
      onSubmit={onSubmit}
    >
      <header className="flex flex-wrap items-center justify-between gap-3 border-b border-navy/8 bg-cream/25 px-4 py-3">
        <div className="flex items-center gap-3">
          <span className="grid h-9 w-9 place-items-center rounded-xl bg-gold/15 text-gold-pressed" aria-hidden="true">
            <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="1.8"><path d="M12 5v14M5 12h14" /></svg>
          </span>
          <div><h2 className="font-serif text-lg text-navy-deep">Lançamento rápido</h2><p className="text-xs text-muted">Preencha o essencial e inclua.</p></div>
        </div>
        <div className="flex rounded-xl border border-navy/8 bg-white p-1" role="group" aria-label="Tipo da despesa">
          {([['single', 'Única'], ['installments', 'Parcelada'], ['recurring', 'Recorrente']] as [ExpenseKind, string][]).map(([id, label]) => (
            <button key={id} type="button" onClick={() => onKind(id)} className={`rounded-lg px-3 py-1.5 text-xs font-semibold transition ${kind === id ? "bg-navy-deep text-cream shadow-sm" : "text-muted hover:text-navy"}`}>{label}</button>
          ))}
        </div>
      </header>
      {error ? (
        <p role="alert" className="m-3 rounded-xl border border-red-200 bg-red-50 px-4 py-2.5 text-sm text-red-800">
          {error}
        </p>
      ) : null}
      <div className="p-3 sm:p-4">
        <div className="grid items-end gap-2 md:grid-cols-2 xl:grid-cols-[145px_minmax(240px,1fr)_135px_180px_auto_auto]">
          <CompactField label={kind === "single" ? "Data" : "Primeiro vencimento"} type="date" value={kind === "single" ? expenseDate : dueDate} onChange={kind === "single" ? setExpenseDate : setDueDate} />
          <CompactField label="Descrição" value={description} placeholder="Ex.: conta de energia" onChange={(value) => setDescription(value.slice(0, 500))} />
          <CompactField label="Valor total" value={amount} placeholder="R$ 0,00" onChange={setAmount} />
          <label><span className="mb-1 block text-[10px] font-bold uppercase tracking-wide text-muted">Categoria</span><select required value={categoryId} onChange={(event) => setCategoryId(event.target.value)} className="h-10 w-full rounded-lg border border-navy/10 bg-white px-2.5 text-sm outline-none focus:border-teal/50"><option value="">Escolher</option>{categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}</select></label>
          <MiniToggle label="Paga" checked={paid} onChange={setPaid} />
          <button type="submit" disabled={busy || categories.length === 0} className="h-10 rounded-lg bg-gold px-5 text-sm font-bold text-navy-deep shadow-sm transition hover:-translate-y-0.5 hover:bg-gold/90 disabled:opacity-50">{busy ? "Incluindo…" : "Incluir"}</button>
        </div>

        <div className="mt-3 flex flex-wrap items-end gap-2 border-t border-navy/8 pt-3">
          {kind === "single" ? <><MiniToggle label="Vencimento" checked={hasDue} onChange={setHasDue} />{hasDue ? <div className="w-40"><CompactField label="Data de vencimento" type="date" value={dueDate} onChange={setDueDate} /></div> : null}</> : null}
          {kind === "installments" ? <><div className="w-24"><CompactField label="Parcelas" type="number" value={installments} onChange={setInstallments} /></div><div className="w-32"><CompactField label="Juro mensal %" value={interest} placeholder="0,00" onChange={setInterest} /></div></> : null}
          {kind === "recurring" ? <><div className="w-40"><CompactField label="Data da despesa" type="date" value={expenseDate} onChange={setExpenseDate} /></div><label><span className="mb-1 block text-[10px] font-bold uppercase tracking-wide text-muted">Frequência</span><select value={freq} onChange={(event) => setFreq(event.target.value as ExpenseFrequency)} className="h-10 rounded-lg border border-navy/10 bg-white px-3 text-sm"><option value="monthly">Mensal</option><option value="weekly">Semanal</option><option value="yearly">Anual</option></select></label></> : null}
          {familyMode ? <button type="button" aria-expanded={showAdvanced} onClick={() => setShowAdvanced((value) => !value)} className="ml-auto h-10 rounded-lg border border-navy/10 px-3 text-xs font-semibold text-navy transition hover:border-teal/40">{showAdvanced ? "Ocultar família" : "Família e divisão"}<span className="ml-2" aria-hidden="true">{showAdvanced ? "⌃" : "⌄"}</span></button> : familyLoading ? <span className="ml-auto text-xs text-muted">Verificando família…</span> : null}
        </div>
      </div>

      {familyMode && showAdvanced ? (
        <section className="border-t border-navy/8 bg-cream/25 p-3 sm:p-4">
          <div className="grid gap-2 md:grid-cols-2"><MiniToggle label="Conta família" checked={isFamily} onChange={setIsFamily} />
          {canShare ? (
            <>
              <MiniToggle label="Partilhar com membro" checked={share} onChange={onShare} />
              {share ? (
                <div className="grid gap-2 md:col-span-2 sm:grid-cols-2 lg:grid-cols-4">
                  <label className="block">
                    <span className="mb-1 block text-[10px] font-bold uppercase tracking-wide text-muted">Partilhar com</span>
                    <select className="h-10 w-full rounded-lg border border-navy/10 bg-white px-3 text-sm" value={peerId} onChange={(e) => setPeerId(e.target.value)} required><option value="">Escolher</option>{peers.map((p) => <option key={p.user_id} value={p.user_id}>{p.label}</option>)}</select>
                  </label>
                  <MiniToggle label="Dividir em %" checked={splitPercent} onChange={onSplitMode} />
                  <CompactField label={splitPercent ? "Sua parte (%)" : "Sua parte (R$)"} value={ownerPart} placeholder={splitPercent ? "50" : "metade"} onChange={setOwnerPart} />
                  <CalculatedValue
                    label={splitPercent ? "Outra pessoa (%)" : "Outra pessoa (R$)"}
                    value={
                      splitPercent
                        ? percentageSplit
                          ? formatPercentInput(percentageSplit.peerBps)
                          : "—"
                        : valueSplit
                          ? formatBrlInput(valueSplit[1])
                          : "—"
                    }
                  />
                  {splitPercent && percentageSplit ? (
                    <div className="grid gap-2 md:col-span-2 lg:col-span-4 sm:grid-cols-2">
                      <SplitPreview label="Sua parte estimada" cents={percentageSplit.ownerCents} />
                      <SplitPreview label="Parte da outra pessoa" cents={percentageSplit.peerCents} />
                    </div>
                  ) : null}
                </div>
              ) : null}
            </>
          ) : (
            <p className="text-xs text-muted">Adicione membros à família para dividir esta despesa.</p>
          )}
          </div>
        </section>
      ) : null}
    </form>
  );
}

function CompactField({ label, value, onChange, type = "text", placeholder }: { label: string; value: string; onChange: (value: string) => void; type?: string; placeholder?: string }) {
  return (
    <label className="block"><span className="mb-1 block text-[10px] font-bold uppercase tracking-wide text-muted">{label}</span><input required type={type} value={value} placeholder={placeholder} onChange={(event) => onChange(event.target.value)} className="h-10 w-full rounded-lg border border-navy/10 bg-white px-3 text-sm outline-none focus:border-teal/50 focus:ring-2 focus:ring-teal/10" /></label>
  );
}

function MiniToggle({ label, checked, onChange }: { label: string; checked: boolean; onChange: (value: boolean) => void }) {
  return <label className="flex h-10 cursor-pointer items-center gap-2 rounded-lg border border-navy/8 bg-cream/30 px-2.5"><input type="checkbox" role="switch" checked={checked} onChange={(event) => onChange(event.target.checked)} className="peer sr-only" /><span aria-hidden="true" className="relative h-5 w-9 rounded-full bg-navy/20 transition peer-checked:bg-teal after:absolute after:left-0.5 after:top-0.5 after:h-4 after:w-4 after:rounded-full after:bg-white after:shadow-sm after:transition-transform peer-checked:after:translate-x-4" /><span className="whitespace-nowrap text-xs font-semibold text-navy">{label}</span></label>;
}

function CalculatedValue({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <span className="mb-1 block text-xs uppercase tracking-wide text-muted">{label}</span>
      <div className="rounded-lg border border-navy/8 bg-cream/60 px-3 py-2.5 text-sm font-semibold text-navy">
        {value}
      </div>
      <span className="mt-1 block text-[11px] text-muted">Calculado automaticamente</span>
    </div>
  );
}

function SplitPreview({ label, cents }: { label: string; cents: number }) {
  return (
    <div className="rounded-xl border border-gold/25 bg-gold/8 px-3 py-2.5">
      <p className="text-[11px] uppercase tracking-wide text-muted">{label}</p>
      <p className="mt-1 font-semibold text-navy-deep">{formatBrlFromCents(cents)}</p>
    </div>
  );
}
