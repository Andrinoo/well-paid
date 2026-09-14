import { useEffect, useState, type FormEvent } from "react";
import { createFamily, createFamilyInvite, fetchFamilyMe, joinFamily, type FamilyMe } from "../api";
import { ApiError, ErrorNote, InField, MoneyForm, PageTitle } from "./common";

export function FamilyPage() {
  const [me, setMe] = useState<FamilyMe | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [name, setName] = useState("");
  const [token, setToken] = useState("");
  const [invite, setInvite] = useState<string | null>(null);

  async function load() {
    setError(null);
    try {
      setMe(await fetchFamilyMe());
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Falha ao carregar.");
    }
  }

  useEffect(() => {
    void load();
  }, []);

  async function onCreate(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    try {
      await createFamily(name.trim() || "Família");
      setName("");
      await load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Não foi possível criar.");
    } finally {
      setBusy(false);
    }
  }

  async function onJoin(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    try {
      await joinFamily(token.trim());
      setToken("");
      await load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Convite inválido.");
    } finally {
      setBusy(false);
    }
  }

  const family = me?.family;
  return (
    <div className="mx-auto max-w-5xl space-y-5">
      <PageTitle kicker="Módulo" title="Família" />
      <ErrorNote message={error} />
      {family ? (
        <section className="rounded-2xl border border-navy/8 bg-white/80 p-5">
          <h2 className="font-serif text-xl">{family.name}</h2>
          <ul className="mt-3 space-y-1 text-sm">
            {family.members.map((m) => (
              <li key={m.user_id}>
                {m.email} · {m.role}
                {m.is_self ? " (você)" : ""}
              </li>
            ))}
          </ul>
          <button
            type="button"
            className="mt-4 rounded-lg bg-navy-deep px-3 py-2 text-xs text-cream"
            disabled={busy}
            onClick={() => {
              setBusy(true);
              void createFamilyInvite()
                .then((res) => setInvite(res.token))
                .catch((err) =>
                  setError(err instanceof ApiError ? err.message : "Não foi possível gerar convite."),
                )
                .finally(() => setBusy(false));
            }}
          >
            Gerar convite
          </button>
          {invite ? (
            <p className="mt-3 break-all rounded-lg bg-cream-muted px-3 py-2 text-xs text-navy">
              Código: {invite}
            </p>
          ) : null}
        </section>
      ) : (
        <>
          <MoneyForm onSubmit={onCreate} submitLabel={busy ? "…" : "Criar família"} busy={busy} extra={
            <InField label="Nome" value={name} onChange={setName} />
          } />
          <MoneyForm onSubmit={onJoin} submitLabel={busy ? "…" : "Entrar com convite"} busy={busy} extra={
            <InField label="Token do convite" value={token} required onChange={setToken} />
          } />
        </>
      )}
    </div>
  );
}
