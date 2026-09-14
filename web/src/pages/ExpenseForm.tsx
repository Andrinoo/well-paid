import { useEffect, useState, type FormEvent } from "react";
import {
  createExpense,
  fetchFamilyMe,
  fetchMe,
  type Category,
} from "../api";
import { parseBrlToCents, parsePercentToBps, todayIso } from "../format";
import { ApiError, CategorySelect, ChipRow, InField, SwitchRow } from "./common";

type Kind = "single" | "installments" | "recurring";
type Freq = "monthly" | "weekly" | "yearly";

type Peer = { user_id: string; label: string };

export function ExpenseCreateForm({
  categories,
  onCreated,
}: {
  categories: Category[];
  onCreated: () => Promise<void> | void;
}) {
  const [kind, setKind] = useState<Kind>("single");
  const [description, setDescription] = useState("");
  const [amount, setAmount] = useState("");
  const [paid, setPaid] = useState(false);
  const [hasDue, setHasDue] = useState(false);
  const [expenseDate, setExpenseDate] = useState(todayIso());
  const [dueDate, setDueDate] = useState(todayIso());
  const [interest, setInterest] = useState("");
  const [installments, setInstallments] = useState("2");
  const [freq, setFreq] = useState<Freq>("monthly");
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
      }
    })();
  }, []);

  function onKind(next: Kind) {
    setKind(next);
    if (next !== "single") setHasDue(true);
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    const desc = description.trim();
    if (!desc) {
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

    let installmentTotal = 1;
    let recurring: string | null = null;
    let interestBps: number | null = null;
    let due: string | null = null;
    let start: string | null = expenseDate;
    let expDate = expenseDate;

    if (kind === "installments") {
      const n = Number(installments);
      if (!Number.isInteger(n) || n < 2 || n > 999) {
        setError("Indique um número de prestações entre 2 e 999.");
        return;
      }
      const bps = parsePercentToBps(interest);
      if (bps == null) {
        setError("Parcelamento exige juro mensal (%).");
        return;
      }
      installmentTotal = n;
      interestBps = bps;
      due = dueDate;
      expDate = dueDate;
      start = null;
    } else if (kind === "recurring") {
      recurring = freq;
      due = dueDate;
      start = expenseDate;
    } else if (hasDue) {
      due = dueDate;
    }

    const body: Parameters<typeof createExpense>[0] = {
      description: desc,
      amount_cents: cents,
      expense_date: expDate,
      due_date: due,
      category_id: categoryId,
      status: paid ? "paid" : "pending",
      installment_total: installmentTotal,
      recurring_frequency: recurring,
      monthly_interest_bps: interestBps,
      start_date: start,
      is_family: familyMode && isFamily,
      is_shared: false,
    };

    if (familyMode && share) {
      if (!peerId) {
        setError("Escolha um membro da família para dividir.");
        return;
      }
      body.is_shared = true;
      body.shared_with_user_id = peerId;
      if (splitPercent) {
        const ownerBps = parsePercentToBps(ownerPart || "50");
        if (ownerBps == null || ownerBps <= 0 || ownerBps >= 10000) {
          setError("Indique uma percentagem entre 0 e 100 (exclusivo).");
          return;
        }
        body.split_mode = "percent";
        body.owner_percent_bps = ownerBps;
        body.peer_percent_bps = 10000 - ownerBps;
      } else {
        const ownerCents = ownerPart ? parseBrlToCents(ownerPart) : Math.ceil(cents / 2);
        if (!ownerCents || ownerCents >= cents) {
          setError("A tua parte tem de fechar com o valor total da despesa.");
          return;
        }
        body.split_mode = "amount";
        body.owner_share_cents = ownerCents;
        body.peer_share_cents = cents - ownerCents;
      }
    }

    setBusy(true);
    try {
      await createExpense(body);
      setDescription("");
      setAmount("");
      setPaid(false);
      setInterest("");
      setInstallments("2");
      setOwnerPart("");
      await onCreated();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Não foi possível criar.");
    } finally {
      setBusy(false);
    }
  }

  const canShare = familyMode && peers.length >= 1;

  return (
    <form
      className="space-y-4 rounded-2xl border border-navy/8 bg-white p-4 shadow-[0_8px_24px_rgba(20,28,42,0.04)]"
      onSubmit={onSubmit}
    >
      <div className="flex flex-wrap items-center justify-between gap-2">
        <p className="font-serif text-xl text-navy-deep">Nova despesa</p>
        <ChipRow
          value={kind}
          onChange={onKind}
          options={[
            { id: "single", label: "Única" },
            { id: "installments", label: "Parcelas" },
            { id: "recurring", label: "Recorrente" },
          ]}
        />
      </div>
      {error ? (
        <p className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800">
          {error}
        </p>
      ) : null}

      <InField
        label="Descrição"
        value={description}
        required
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

      {kind === "single" ? (
        <>
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
        </>
      ) : null}

      {kind === "installments" ? (
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
            label="Número de prestações"
            value={installments}
            required
            placeholder="2–999"
            hint="Indique quantas prestações tem o plano (2–999)."
            onChange={setInstallments}
          />
        </div>
      ) : null}

      {kind === "recurring" ? (
        <>
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
        </>
      ) : null}

      <CategorySelect
        label="Categoria"
        value={categoryId}
        categories={categories}
        onChange={setCategoryId}
      />

      {familyMode ? (
        <div className="space-y-3 border-t border-navy/8 pt-3">
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
                onChange={setShare}
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
                    onChange={setSplitPercent}
                  />
                  <InField
                    label={splitPercent ? "Tua %" : "Tu (R$)"}
                    value={ownerPart}
                    placeholder={splitPercent ? "50" : "metade"}
                    onChange={setOwnerPart}
                  />
                </div>
              ) : null}
            </>
          ) : (
            <p className="text-xs text-muted">
              Adicione mais membros ao agregado para partilhar a despesa com alguém em concreto.
            </p>
          )}
        </div>
      ) : null}

      <button
        type="submit"
        disabled={busy}
        className="w-full rounded-lg bg-gold py-3 text-sm font-semibold text-navy-deep disabled:opacity-60 sm:w-auto sm:px-8"
      >
        {busy ? "A guardar…" : "Salvar"}
      </button>
    </form>
  );
}
