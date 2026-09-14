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
import { ApiError, ErrorNote, InField, MoneyForm, PageTitle } from "./common";

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
    <div className="mx-auto max-w-5xl space-y-5">
      <PageTitle kicker="Módulo" title="Listas de compras" />
      <ErrorNote message={error} />
      <MoneyForm onSubmit={onCreate} submitLabel={busy ? "…" : "Nova lista"} busy={busy} extra={
        <InField label="Título" value={title} required onChange={setTitle} />
      } />
      <ul className="space-y-3">
        {rows.length === 0 ? (
          <li className="rounded-2xl border border-navy/8 bg-white/80 px-4 py-8 text-center text-sm text-muted">
            Sem listas.
          </li>
        ) : (
          rows.map((list) => (
            <li key={list.id} className="rounded-2xl border border-navy/8 bg-white/80 p-4">
              <div className="flex items-start justify-between gap-3">
                <button type="button" className="min-w-0 flex-1 text-left" onClick={() => void openList(list.id)}>
                  <div className="flex justify-between text-sm">
                    <span className="font-medium">{list.title || "Lista"}</span>
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
                <div className="mt-3 border-t border-navy/8 pt-3">
                  <ul className="space-y-1 text-sm">
                    {items.map((it) => (
                      <li key={it.id} className="flex items-center gap-2">
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
                    <button type="submit" className="rounded-lg bg-navy-deep px-3 py-2 text-xs text-cream">
                      Adicionar
                    </button>
                  </form>
                </div>
              ) : null}
            </li>
          ))
        )}
      </ul>
    </div>
  );
}
