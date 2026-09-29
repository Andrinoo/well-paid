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
import { ApiError, CategorySelect, ChipRow, InField, SwitchRow } from "./common";

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
  const [showHelp, setShowHelp] = useState(false);
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
      className="space-y-5 overflow-hidden rounded-3xl border border-navy/8 bg-white p-4 shadow-[0_18px_55px_rgba(20,28,42,0.08)] sm:p-6"
      onSubmit={onSubmit}
    >
      <div className="flex flex-wrap items-start justify-between gap-3 border-b border-navy/8 pb-4">
        <div>
          <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-gold-pressed">
            Lançamento
          </p>
          <p className="mt-1 font-serif text-2xl text-navy-deep">Nova despesa</p>
          <p className="mt-1 max-w-xl text-sm text-muted">
            Registre uma compra única, um parcelamento ou uma conta recorrente.
          </p>
        </div>
        <button
          type="button"
          onClick={() => setShowHelp((value) => !value)}
          aria-expanded={showHelp}
          className="rounded-full border border-navy/10 bg-cream/60 px-3 py-2 text-xs font-semibold text-navy transition hover:border-gold/60 hover:bg-gold/10"
        >
          {showHelp ? "Fechar ajuda" : "Como funciona?"}
        </button>
      </div>
      {showHelp ? (
        <div className="rounded-2xl border border-gold/25 bg-gold/8 p-4 text-sm leading-6 text-navy">
          <strong>Única</strong> cria somente um lançamento. <strong>Parcelas</strong> cria o plano
          completo a partir do primeiro vencimento. <strong>Recorrente</strong> projeta novos
          lançamentos na frequência escolhida. Em uma despesa compartilhada, sua parte e a da
          outra pessoa sempre fecham exatamente com o total.
        </div>
      ) : null}
      {error ? (
        <p role="alert" className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800">
          {error}
        </p>
      ) : null}

      <section className="space-y-4 rounded-2xl bg-cream/35 p-4">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.18em] text-muted">Tipo da despesa</p>
          <div className="mt-2">
            <ChipRow
              value={kind}
              onChange={onKind}
              options={[
                { id: "single", label: "Única" },
                { id: "installments", label: "Parcelada" },
                { id: "recurring", label: "Recorrente" },
              ]}
            />
          </div>
        </div>
        <InField
          label="Descrição"
          value={description}
          required
          placeholder="Ex.: conta de energia"
          onChange={(v) => setDescription(v.slice(0, 500))}
          hint={`${description.length}/500`}
        />

        <div className="grid gap-3 sm:grid-cols-2">
        <InField
          label="Valor (R$)"
          value={amount}
          required
          placeholder="Ex.: 12,50 ou 1234,56"
          onChange={setAmount}
        />
        {kind === "installments" ? (
          <InField
            label="Juro mensal (%)"
            value={interest}
            required
            placeholder="Ex.: 4,5"
            onChange={setInterest}
          />
        ) : (
          <SwitchRow
            label="Já está paga"
            checked={paid}
            onChange={setPaid}
          />
        )}
        </div>
      {kind === "installments" ? (
        <SwitchRow label="Já está paga" checked={paid} onChange={setPaid} />
      ) : null}
      </section>

      {kind === "single" ? (
        <section className="space-y-3 rounded-2xl border border-navy/8 p-4">
          <SectionTitle title="Datas" description="Quando a despesa aconteceu e, se aplicável, quando vence." />
          <SwitchRow
            label="Tem data de vencimento"
            sub="Contas a pagar / alertas no dashboard"
            checked={hasDue}
            onChange={setHasDue}
          />
          <div className={`grid gap-3 ${hasDue ? "sm:grid-cols-2" : ""}`}>
            <InField
              label="Data da despesa"
              type="date"
              value={expenseDate}
              required
              onChange={setExpenseDate}
            />
            {hasDue ? (
              <InField
                label="Data de vencimento"
                type="date"
                value={dueDate}
                required
                hint="Opcional no tipo única, obrigatório se ligado"
                onChange={setDueDate}
              />
            ) : null}
          </div>
        </section>
      ) : null}

      {kind === "installments" ? (
        <section className="space-y-3 rounded-2xl border border-navy/8 p-4">
          <SectionTitle title="Plano de parcelas" description="O primeiro vencimento ancora todo o plano." />
          <div className="grid gap-3 sm:grid-cols-2">
          <InField
            label="Primeiro vencimento"
            type="date"
            value={dueDate}
            required
            hint="Esta data define o vencimento de cada parcela."
            onChange={setDueDate}
          />
          <InField
            label="Número de parcelas"
            type="number"
            value={installments}
            required
            placeholder="2–999"
            hint="Indique quantas parcelas tem o plano (2–999)."
            onChange={setInstallments}
          />
          </div>
        </section>
      ) : null}

      {kind === "recurring" ? (
        <section className="space-y-3 rounded-2xl border border-navy/8 p-4">
          <SectionTitle title="Recorrência" description="Defina o início, o primeiro vencimento e a frequência." />
          <div className="grid gap-3 sm:grid-cols-2">
            <InField
              label="Primeiro vencimento"
              type="date"
              value={dueDate}
              required
              hint="Obrigatório para parcelas e recorrentes."
              onChange={setDueDate}
            />
            <InField
              label="Data da despesa"
              type="date"
              value={expenseDate}
              required
              onChange={setExpenseDate}
            />
          </div>
          <div>
            <p className="mb-1 text-xs uppercase tracking-wide text-muted">Frequência</p>
            <ChipRow
              value={freq}
              onChange={setFreq}
              options={[
                { id: "monthly", label: "mensal" },
                { id: "weekly", label: "semanal" },
                { id: "yearly", label: "anual" },
              ]}
            />
          </div>
        </section>
      ) : null}

      <section className="space-y-3 rounded-2xl border border-navy/8 p-4">
        <SectionTitle title="Classificação" description="Use a mesma categoria que organiza seus relatórios." />
        <CategorySelect
          label="Categoria"
          value={categoryId}
          categories={categories}
          onChange={setCategoryId}
        />
      </section>

      {familyMode ? (
        <section className="space-y-3 rounded-2xl border border-navy/8 bg-navy/[0.025] p-4">
          <SectionTitle title="Família e divisão" description="Compartilhe a visibilidade ou divida o pagamento com uma pessoa." />
          <SwitchRow
            label="Conta família"
            sub="Itens marcados aparecem no Modo Família para membros convidados."
            checked={isFamily}
            onChange={setIsFamily}
          />
          {canShare ? (
            <>
              <SwitchRow
                label="Partilhar na família"
                sub="Junta-te a uma família (convite) para usar partilha."
                checked={share}
                onChange={onShare}
              />
              {share ? (
                <div className="grid gap-3 sm:grid-cols-2">
                  <label className="block">
                    <span className="mb-1 block text-xs uppercase tracking-wide text-muted">
                      Partilhar com
                    </span>
                    <select
                      className="w-full rounded-lg border border-navy/10 bg-white px-3 py-2.5 text-sm"
                      value={peerId}
                      onChange={(e) => setPeerId(e.target.value)}
                      required
                    >
                      <option value="">Escolher</option>
                      {peers.map((p) => (
                        <option key={p.user_id} value={p.user_id}>
                          {p.label}
                        </option>
                      ))}
                    </select>
                  </label>
                  <SwitchRow
                    label="Dividir em %"
                    sub="Predef.: valor; ligue para %."
                    checked={splitPercent}
                    onChange={onSplitMode}
                  />
                  <InField
                    label={splitPercent ? "Sua parte (%)" : "Sua parte (R$)"}
                    value={ownerPart}
                    placeholder={splitPercent ? "50" : "metade"}
                    onChange={setOwnerPart}
                  />
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
                    <div className="grid gap-2 sm:col-span-2 sm:grid-cols-2">
                      <SplitPreview label="Sua parte estimada" cents={percentageSplit.ownerCents} />
                      <SplitPreview label="Parte da outra pessoa" cents={percentageSplit.peerCents} />
                    </div>
                  ) : null}
                </div>
              ) : null}
            </>
          ) : (
            <p className="text-xs text-muted">
              Adicione mais membros ao agregado para partilhar a despesa com alguém em concreto.
            </p>
          )}
        </section>
      ) : familyLoading ? (
        <div className="rounded-2xl border border-navy/8 px-4 py-3 text-sm text-muted">
          Verificando recursos da família…
        </div>
      ) : null}

      <div className="flex flex-col-reverse gap-3 border-t border-navy/8 pt-5 sm:flex-row sm:items-center sm:justify-end">
        <p className="text-center text-xs text-muted sm:mr-auto sm:text-left">
          Os valores são gravados com precisão de centavos.
        </p>
        <button
          type="submit"
          disabled={busy || categories.length === 0}
          className="min-h-12 w-full rounded-xl bg-gold px-8 py-3 text-sm font-bold text-navy-deep shadow-[0_8px_22px_rgba(201,169,78,0.28)] transition hover:-translate-y-0.5 hover:bg-gold/90 hover:shadow-[0_12px_28px_rgba(201,169,78,0.36)] disabled:cursor-not-allowed disabled:opacity-60 disabled:hover:translate-y-0 sm:w-auto"
        >
          {busy ? "Salvando…" : "Salvar despesa"}
        </button>
      </div>
    </form>
  );
}

function SectionTitle({ title, description }: { title: string; description: string }) {
  return (
    <div>
      <h3 className="font-serif text-lg text-navy-deep">{title}</h3>
      <p className="mt-0.5 text-xs leading-5 text-muted">{description}</p>
    </div>
  );
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
