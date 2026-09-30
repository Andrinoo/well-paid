import { useEffect, useState, type FormEvent } from "react";
import {
  addShoppingItem,
  createShoppingList,
  deleteShoppingList,
  fetchShoppingDetail,
  fetchShoppingLists,
  patchShoppingItem,
  type ShoppingItem,
  type ShoppingList,
} from "../api";
import { formatBrlFromCents } from "../format";
import { ApiError, ErrorNote, PageTitle } from "./common";

export function ShoppingPage() {
  const [rows, setRows] = useState<ShoppingList[]>([]);
  const [openId, setOpenId] = useState<string | null>(null);
  const [items, setItems] = useState<ShoppingItem[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [title, setTitle] = useState("");
  const [item, setItem] = useState("");

  async function load() {
    setError(null);
    try {
      setRows(await fetchShoppingLists());
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Falha ao carregar.");
    }
  }

  useEffect(() => {
    void load();
  }, []);

  async function openList(id: string) {
    setOpenId(id);
    const detail = await fetchShoppingDetail(id);
    setItems(detail.items ?? []);
  }

  async function onCreate(e: FormEvent) {
    e.preventDefault();
    if (!title.trim()) return;
    setBusy(true);
    try {
      await createShoppingList(title.trim());
      setTitle("");
      await load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Não foi possível criar.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="-mx-4 -my-5 min-h-full bg-gradient-to-br from-paper via-paper to-sage/45 px-4 py-5 sm:-mx-6 sm:px-6">
      <div className="flex flex-wrap items-end justify-between gap-4"><div><PageTitle kicker="Organização" title="Listas de compras" /><p className="mt-2 text-sm text-muted">Planeje suas compras e acompanhe o total sem perder nenhum item.</p></div><form onSubmit={onCreate} className="flex gap-2"><input className="field min-w-56" value={title} onChange={(e) => setTitle(e.target.value)} placeholder="Nome da nova lista" required /><button disabled={busy} className="rounded-xl bg-gold px-4 text-sm font-bold text-navy-deep shadow-sm">{busy ? "…" : "+ Criar lista"}</button></form></div>
      <div className="mt-4"><ErrorNote message={error} /></div>
      <section className="mt-4 grid gap-3 sm:grid-cols-3"><ShoppingMetric label="Listas" value={String(rows.length)} detail="organizadas" /><ShoppingMetric label="Itens" value={String(rows.reduce((sum, row) => sum + row.items_count, 0))} detail="no total" /><ShoppingMetric label="Valor planejado" value={formatBrlFromCents(rows.reduce((sum, row) => sum + (row.total_cents ?? 0), 0))} detail="em todas as listas" /></section>
      <ul className="mt-4 grid gap-4 lg:grid-cols-2">
        {rows.length === 0 ? (
          <li className="rounded-3xl border border-dashed border-navy/15 bg-white/70 px-4 py-14 text-center text-sm text-muted lg:col-span-2">
            Sem listas.
          </li>
        ) : (
          rows.map((list) => (
            <li key={list.id} className="expense-list-row self-start overflow-hidden rounded-3xl border border-navy/8 bg-white/90 shadow-[0_12px_38px_rgba(20,28,42,0.06)] transition hover:-translate-y-0.5 hover:shadow-xl">
              <div className="h-1 bg-gradient-to-r from-gold to-teal" />
              <div className="p-5">
              <div className="flex items-start justify-between gap-3">
                <button type="button" className="min-w-0 flex-1 text-left" onClick={() => void openList(list.id)}>
                  <div className="flex justify-between text-sm">
                    <span className="font-serif text-xl font-semibold text-navy-deep">{list.title || "Lista"}</span>
                    <span className="text-muted">
                      {list.items_count} itens
                      {list.total_cents != null
                        ? ` · ${formatBrlFromCents(list.total_cents)}`
                        : ""}
                    </span>
                  </div>
                </button>
                <button
                  type="button"
                  className="text-xs text-muted hover:text-red-700"
                  onClick={() =>
                    void deleteShoppingList(list.id).then(() => {
                      if (openId === list.id) {
                        setOpenId(null);
                        setItems([]);
                      }
                      return load();
                    })
                  }
                >
                  Apagar
                </button>
              </div>
              {openId === list.id ? (
                <div className="mt-4 border-t border-navy/8 pt-3">
                  <div className="mb-3 h-1.5 overflow-hidden rounded-full bg-cream-muted"><div className="h-full rounded-full bg-teal" style={{ width: `${items.length ? Math.round(items.filter((i) => i.is_picked).length / items.length * 100) : 0}%` }} /></div>
                  <ul className="max-h-64 space-y-1 overflow-auto text-sm">
                    {items.map((it) => (
                      <li key={it.id} className="flex items-center gap-2 rounded-xl px-2 py-2 hover:bg-cream/50">
                        <input
                          type="checkbox"
                          checked={it.is_picked}
                          onChange={() =>
                            void patchShoppingItem(list.id, it.id, {
                              is_picked: !it.is_picked,
                            }).then(() => openList(list.id))
                          }
                        />
                        <span className={it.is_picked ? "text-muted line-through" : ""}>
                          {it.quantity}× {it.label}
                        </span>
                      </li>
                    ))}
                  </ul>
                  <form
                    className="mt-3 flex gap-2"
                    onSubmit={(e) => {
                      e.preventDefault();
                      if (!item.trim()) return;
                      void addShoppingItem(list.id, item.trim()).then(() => {
                        setItem("");
                        return openList(list.id);
                      });
                    }}
                  >
                    <input
                      className="min-w-0 flex-1 rounded-lg border border-navy/10 px-3 py-2 text-sm"
                      placeholder="Novo item"
                      value={item}
                      onChange={(e) => setItem(e.target.value)}
                    />
                    <button type="submit" className="rounded-lg bg-teal px-3 py-2 text-xs font-bold text-white">
                      Adicionar
                    </button>
                  </form>
                </div>
              ) : null}
              </div>
            </li>
          ))
        )}
      </ul>
    </div>
  );
}

function ShoppingMetric({ label, value, detail }: { label: string; value: string; detail: string }) { return <article className="rounded-2xl border border-navy/8 border-l-4 border-l-teal bg-white/90 px-4 py-3.5 shadow-sm"><p className="text-[10px] font-bold uppercase tracking-wider text-muted">{label}</p><p className="mt-1 font-display text-2xl font-semibold text-navy-deep">{value}</p><p className="text-xs text-muted">{detail}</p></article>; }
