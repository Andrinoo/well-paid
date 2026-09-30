import { useEffect, useState, type FormEvent } from "react";
import { createFamily, createFamilyInvite, fetchFamilyMe, joinFamily, type FamilyMe } from "../api";
import { ApiError, ErrorNote, PageTitle } from "./common";

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
    <div className="-mx-4 -my-5 min-h-full bg-gradient-to-br from-paper via-paper to-peach/35 px-4 py-5 sm:-mx-6 sm:px-6">
      <div><PageTitle kicker="Finanças compartilhadas" title="Família" /><p className="mt-2 max-w-2xl text-sm text-muted">Organize responsabilidades e acompanhe quem participa do seu espaço financeiro.</p></div>
      <div className="mt-4"><ErrorNote message={error} /></div>
      {family ? (
        <section className="mt-4 overflow-hidden rounded-3xl border border-navy/8 bg-white/90 shadow-[0_18px_55px_rgba(20,28,42,0.08)]"><header className="bg-gradient-to-r from-navy-deep to-navy p-6 text-white"><p className="text-[10px] font-bold uppercase tracking-[0.2em] text-gold">Seu grupo</p><h2 className="mt-1 font-serif text-3xl">{family.name}</h2><p className="mt-1 text-sm text-white/60">{family.members.length} {family.members.length === 1 ? "membro" : "membros"}</p></header>
          <ul className="grid gap-3 p-5 md:grid-cols-2">
            {family.members.map((m) => (
              <li key={m.user_id} className="flex items-center gap-3 rounded-2xl border border-navy/8 bg-cream/25 p-3"><span className="grid h-11 w-11 place-items-center rounded-full bg-sage font-bold text-teal-deep">{(m.full_name || m.email).slice(0, 1).toUpperCase()}</span><span className="min-w-0 flex-1"><b className="block truncate text-sm text-navy">{m.full_name || m.email.split("@")[0]}</b><small className="block truncate text-muted">{m.email}</small></span><span className="rounded-full bg-gold/15 px-2 py-1 text-[10px] font-bold uppercase text-gold-pressed">{m.is_self ? "Você" : m.role}</span></li>
            ))}
          </ul>
          <button
            type="button"
            className="mx-5 mb-5 rounded-xl bg-teal px-4 py-2.5 text-sm font-bold text-white"
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
            <p className="mx-5 mb-5 break-all rounded-xl border border-gold/25 bg-gold/10 px-4 py-3 text-sm text-navy">
              Código: {invite}
            </p>
          ) : null}
        </section>
      ) : (
        <>
          <div className="mt-4 grid gap-4 lg:grid-cols-2"><FamilyOption title="Criar uma família" description="Comece um novo espaço para organizar as finanças em conjunto." onSubmit={onCreate} value={name} onChange={setName} placeholder="Nome da família" button="Criar família" busy={busy} /><FamilyOption title="Entrar com convite" description="Use o código enviado por alguém da família." onSubmit={onJoin} value={token} onChange={setToken} placeholder="Cole o código do convite" button="Entrar na família" busy={busy} /></div>
        </>
      )}
    </div>
  );
}

function FamilyOption({ title, description, onSubmit, value, onChange, placeholder, button, busy }: { title: string; description: string; onSubmit: (e: FormEvent) => void; value: string; onChange: (v: string) => void; placeholder: string; button: string; busy: boolean }) { return <form onSubmit={onSubmit} className="rounded-3xl border border-navy/8 bg-white/90 p-6 shadow-sm"><span className="grid h-12 w-12 place-items-center rounded-2xl bg-sage text-xl text-teal-deep">♟</span><h2 className="mt-4 font-serif text-2xl text-navy-deep">{title}</h2><p className="mt-1 min-h-10 text-sm text-muted">{description}</p><input required value={value} onChange={(e) => onChange(e.target.value)} placeholder={placeholder} className="field mt-5" /><button disabled={busy} className="mt-3 h-10 w-full rounded-xl bg-navy-deep text-sm font-bold text-white disabled:opacity-50">{busy ? "Aguarde…" : button}</button></form>; }
