import { useEffect, useState, type FormEvent } from "react";
import {
  contributeGoal,
  createGoal,
  fetchGoals,
  searchGoalProducts,
  type Goal,
  type GoalProductHit,
} from "../api";
import { formatBrlFromCents, parseBrlToCents } from "../format";
import { GoalThumb } from "./home/GoalThumb";
import {
  ApiError,
  ErrorNote,
  InField,
  MoneyForm,
  PageTitle,
} from "./common";

export function GoalsPage() {
  const [rows, setRows] = useState<Goal[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [title, setTitle] = useState("");
  const [target, setTarget] = useState("");
  const [query, setQuery] = useState("");
  const [hits, setHits] = useState<GoalProductHit[]>([]);
  const [searching, setSearching] = useState(false);
  const [picked, setPicked] = useState<GoalProductHit | null>(null);
  const [amounts, setAmounts] = useState<Record<string, string>>({});

  async function load() {
    setError(null);
    try {
      setRows(await fetchGoals());
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Falha ao carregar.");
    }
  }

  useEffect(() => {
    void load();
  }, []);

  async function onSearch(e: FormEvent) {
    e.preventDefault();
    const q = query.trim();
    if (q.length < 2) return;
    setSearching(true);
    setError(null);
    try {
      setHits(await searchGoalProducts(q));
    } catch (err) {
      setHits([]);
      setError(err instanceof ApiError ? err.message : "Não foi possível pesquisar.");
    } finally {
      setSearching(false);
    }
  }

  function applyHit(hit: GoalProductHit) {
    setPicked(hit);
    setTitle(hit.title);
    setTarget((hit.price_cents / 100).toFixed(2).replace(".", ","));
    setHits([]);
  }

  async function onCreate(e: FormEvent) {
    e.preventDefault();
    const cents = parseBrlToCents(target);
    if (!cents) return;
    setBusy(true);
    try {
      await createGoal({
        title: title.trim(),
        target_cents: cents,
        target_url: picked?.url ?? null,
        reference_product_name: picked?.title ?? null,
        reference_price_cents: picked?.price_cents ?? null,
        reference_thumbnail_url: picked?.thumbnail ?? null,
        price_source: picked?.source ?? null,
      });
      setTitle("");
      setTarget("");
      setQuery("");
      setPicked(null);
      setHits([]);
      await load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Não foi possível criar.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="mx-auto max-w-5xl space-y-5">
      <PageTitle kicker="Módulo" title="Metas" />
      <ErrorNote message={error} />

      <form
        className="flex flex-wrap gap-2 rounded-2xl border border-navy/8 bg-white/80 p-4"
        onSubmit={(e) => void onSearch(e)}
      >
        <InField
          label="Pesquisar item"
          value={query}
          onChange={setQuery}
          placeholder="ex.: fone bluetooth"
        />
        <button
          type="submit"
          disabled={searching}
          className="self-end rounded-lg bg-teal px-4 py-2.5 text-sm font-medium text-white disabled:opacity-60"
        >
          {searching ? "A procurar…" : "Procurar"}
        </button>
      </form>

      {hits.length > 0 ? (
        <ul className="space-y-2">
          {hits.map((hit) => (
            <li key={`${hit.url}-${hit.price_cents}`}>
              <button
                type="button"
                className="flex w-full items-center gap-3 rounded-2xl border border-navy/8 bg-white px-3 py-2 text-left hover:border-teal/40"
                onClick={() => applyHit(hit)}
              >
                <GoalThumb url={hit.thumbnail} className="h-12 w-12" />
                <span className="min-w-0 flex-1">
                  <span className="block truncate text-sm font-medium text-navy">{hit.title}</span>
                  <span className="text-xs text-muted">{hit.source}</span>
                </span>
                <span className="shrink-0 text-sm font-semibold tabular-nums">
                  {formatBrlFromCents(hit.price_cents)}
                </span>
              </button>
            </li>
          ))}
        </ul>
      ) : null}

      {picked ? (
        <p className="flex items-center gap-3 text-sm text-navy/80">
          <GoalThumb url={picked.thumbnail} className="h-10 w-10" />
          Item escolhido. Pode ajustar o título e o alvo antes de criar.
        </p>
      ) : null}

      <MoneyForm
        onSubmit={onCreate}
        submitLabel={busy ? "…" : "Nova meta"}
        busy={busy}
        extra={
          <>
            <InField label="Título" value={title} required onChange={setTitle} />
            <InField label="Alvo" value={target} required onChange={setTarget} />
          </>
        }
      />
      <ul className="space-y-3">
        {rows.length === 0 ? (
          <li className="rounded-2xl border border-navy/8 bg-white/80 px-4 py-8 text-center text-sm text-muted">
            Sem metas.
          </li>
        ) : (
          rows.map((goal) => {
            const pct = Math.min(
              100,
              Math.round((goal.current_cents / Math.max(1, goal.target_cents)) * 100),
            );
            return (
              <li key={goal.id} className="rounded-2xl border border-navy/8 bg-white/80 p-4">
                <div className="flex items-start gap-3">
                  <GoalThumb url={goal.reference_thumbnail_url} />
                  <div className="min-w-0 flex-1">
                    <div className="flex justify-between gap-3 text-sm">
                      <span className="font-medium">{goal.title}</span>
                      <span className="shrink-0 text-muted">
                        {formatBrlFromCents(goal.current_cents)} /{" "}
                        {formatBrlFromCents(goal.target_cents)}
                      </span>
                    </div>
                    <div className="mt-2 h-2 overflow-hidden rounded-full bg-cream-muted">
                      <div className="h-full rounded-full bg-gold" style={{ width: `${pct}%` }} />
                    </div>
                  </div>
                </div>
                <form
                  className="mt-3 flex gap-2"
                  onSubmit={(e) => {
                    e.preventDefault();
                    const cents = parseBrlToCents(amounts[goal.id] ?? "");
                    if (!cents) return;
                    void contributeGoal(goal.id, cents).then(() => {
                      setAmounts((a) => ({ ...a, [goal.id]: "" }));
                      return load();
                    });
                  }}
                >
                  <input
                    className="min-w-0 flex-1 rounded-lg border border-navy/10 px-3 py-2 text-sm"
                    placeholder="Aportar"
                    value={amounts[goal.id] ?? ""}
                    onChange={(e) =>
                      setAmounts((a) => ({ ...a, [goal.id]: e.target.value }))
                    }
                  />
                  <button
                    type="submit"
                    className="rounded-lg bg-navy-deep px-3 py-2 text-xs text-cream"
                  >
                    Aportar
                  </button>
                </form>
              </li>
            );
          })
        )}
      </ul>
    </div>
  );
}
