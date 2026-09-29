import { useEffect, useState, type FormEvent, type ReactNode } from "react";
import { Link, useNavigate } from "react-router-dom";
import { fetchMe, logout, patchDisplayName, patchFamilyMode } from "../api";
import { useLocale } from "../i18n/LocaleProvider";
import { useTheme, type ThemePreference } from "../theme/ThemeProvider";
import { ApiError, ErrorNote, PageTitle } from "./common";

export function SettingsPage() {
  const navigate = useNavigate();
  const { locale, setLocale } = useLocale();
  const { preference, resolvedTheme, setPreference } = useTheme();
  const [email, setEmail] = useState("");
  const [publicId, setPublicId] = useState("");
  const [display, setDisplay] = useState("");
  const [familyMode, setFamilyMode] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [busy, setBusy] = useState<"profile" | "family" | "logout" | null>(null);

  useEffect(() => {
    void fetchMe()
      .then((me) => {
        setEmail(me.email);
        setPublicId(me.public_id || "");
        setDisplay(me.display_name || me.full_name || "");
        setFamilyMode(Boolean(me.family_mode_enabled));
      })
      .catch((err) => setError(messageFrom(err, "Falha ao carregar as configurações.")));
  }, []);

  async function saveProfile(event: FormEvent) {
    event.preventDefault();
    setBusy("profile");
    setError(null);
    setNotice(null);
    try {
      const me = await patchDisplayName(display.trim());
      setDisplay(me.display_name || me.full_name || "");
      setNotice("Nome de exibição atualizado.");
    } catch (err) {
      setError(messageFrom(err, "Não foi possível guardar o nome."));
    } finally {
      setBusy(null);
    }
  }

  async function changeFamilyMode(enabled: boolean) {
    const previous = familyMode;
    setFamilyMode(enabled);
    setBusy("family");
    setError(null);
    try {
      const me = await patchFamilyMode(enabled);
      setFamilyMode(Boolean(me.family_mode_enabled));
      setNotice(enabled ? "Modo família ativado." : "Modo família desativado.");
    } catch (err) {
      setFamilyMode(previous);
      setError(messageFrom(err, "Não foi possível alterar o modo família."));
    } finally {
      setBusy(null);
    }
  }

  async function leave() {
    setBusy("logout");
    try {
      await logout();
    } finally {
      navigate("/", { replace: true });
    }
  }

  const initial = (display || email || "?").trim().charAt(0).toUpperCase();

  return (
    <div className="mx-auto max-w-6xl space-y-5">
      <PageTitle kicker="Conta" title="Configurações" />
      <ErrorNote message={error} />
      {notice ? (
        <p role="status" className="rounded-xl border border-positive/20 bg-positive/10 px-4 py-3 text-sm text-positive">
          {notice}
        </p>
      ) : null}

      <div className="grid items-start gap-5 lg:grid-cols-[minmax(0,1.15fr)_minmax(320px,0.85fr)]">
        <div className="space-y-5">
          <SettingsCard title="Perfil" description="Como sua conta aparece no Well Paid.">
            <div className="flex flex-wrap items-center gap-4 border-b border-navy/8 px-5 py-4">
              <span className="grid h-12 w-12 place-items-center rounded-2xl bg-teal/12 text-lg font-bold text-teal">
                {initial}
              </span>
              <div className="min-w-0 flex-1">
                <p className="truncate font-semibold text-navy-deep">{display || "Sua conta"}</p>
                <p className="truncate text-sm text-muted">{email || "Carregando…"}</p>
                {publicId ? <p className="mt-1 font-mono text-[11px] text-muted">ID {publicId}</p> : null}
              </div>
            </div>
            <form className="flex flex-col gap-3 px-5 py-4 sm:flex-row sm:items-end" onSubmit={saveProfile}>
              <label className="min-w-0 flex-1">
                <span className="mb-1.5 block text-xs font-semibold uppercase tracking-wide text-muted">Nome na saudação</span>
                <input
                  className="w-full rounded-xl border border-navy/12 bg-white px-3 py-2.5 outline-none ring-teal/20 focus:border-teal/50 focus:ring-2"
                  value={display}
                  maxLength={200}
                  onChange={(event) => setDisplay(event.target.value)}
                />
              </label>
              <button className="rounded-xl bg-teal px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-teal-deep disabled:opacity-60" disabled={busy === "profile"}>
                {busy === "profile" ? "Guardando…" : "Guardar"}
              </button>
            </form>
          </SettingsCard>

          <SettingsCard title="Aparência" description="A preferência fica salva neste navegador.">
            <div className="px-5 py-4">
              <p className="mb-3 text-sm font-semibold text-navy-deep">Tema</p>
              <div className="grid grid-cols-3 gap-2" role="radiogroup" aria-label="Tema da interface">
                <ThemeChoice label="Sistema" value="system" selected={preference} detail="Automático" onSelect={setPreference} />
                <ThemeChoice label="Claro" value="light" selected={preference} detail="Sempre claro" onSelect={setPreference} />
                <ThemeChoice label="Escuro" value="dark" selected={preference} detail="Sempre escuro" onSelect={setPreference} />
              </div>
              <p className="mt-3 text-xs text-muted">Tema em uso: {resolvedTheme === "dark" ? "escuro" : "claro"}.</p>
            </div>
            <div className="flex items-center justify-between gap-4 border-t border-navy/8 px-5 py-4">
              <div>
                <p className="text-sm font-semibold text-navy-deep">Idioma da interface</p>
                <p className="text-xs text-muted">Mantém a preferência usada no acesso e cadastro.</p>
              </div>
              <div className="flex rounded-xl bg-cream-muted p-1" role="group" aria-label="Idioma">
                <ChoiceButton active={locale === "pt-BR"} onClick={() => setLocale("pt-BR")}>PT</ChoiceButton>
                <ChoiceButton active={locale === "en-US"} onClick={() => setLocale("en-US")}>EN</ChoiceButton>
              </div>
            </div>
          </SettingsCard>
        </div>

        <div className="space-y-5">
          <SettingsCard title="Família e organização" description="Acesso rápido aos recursos compartilhados.">
            <ToggleRow
              label="Modo família"
              description="Mostra seus dados e itens marcados como família, como no aplicativo."
              checked={familyMode}
              disabled={busy === "family"}
              onChange={changeFamilyMode}
            />
            <SettingsLink to="/app/familia" title="Família e convites" description="Gerencie o agregado e convide participantes." icon="users" />
            <SettingsLink to="/app/despesas" title="Categorias de despesas" description="Use e revise as categorias ao lançar despesas." icon="tag" />
            <SettingsLink to="/app/receitas" title="Categorias de proventos" description="Organize as entradas pelas categorias disponíveis." icon="tag" />
          </SettingsCard>

          <SettingsCard title="Segurança e sessão" description="Controles adequados ao acesso pelo navegador.">
            <div className="flex gap-3 px-5 py-4">
              <Icon name="shield" />
              <div>
                <p className="text-sm font-semibold text-navy-deep">Sessão protegida</p>
                <p className="mt-1 text-xs leading-5 text-muted">O acesso usa sessão autenticada. Bloqueio por PIN, biometria e captura de tela continuam exclusivos do aplicativo Android.</p>
              </div>
            </div>
            <button
              type="button"
              className="flex w-full items-center justify-between border-t border-navy/8 px-5 py-4 text-left text-sm font-semibold text-red-700 transition hover:bg-red-50/70 disabled:opacity-60"
              disabled={busy === "logout"}
              onClick={() => void leave()}
            >
              <span>{busy === "logout" ? "Saindo…" : "Sair desta conta"}</span><span aria-hidden="true">→</span>
            </button>
          </SettingsCard>
        </div>
      </div>
    </div>
  );
}

function SettingsCard({ title, description, children }: { title: string; description: string; children: ReactNode }) {
  return (
    <section className="wp-settings-card overflow-hidden rounded-2xl border border-navy/8 bg-white/80 shadow-sm shadow-navy/5">
      <header className="border-b border-navy/8 px-5 py-4">
        <h2 className="font-serif text-xl text-navy-deep">{title}</h2>
        <p className="mt-1 text-sm text-muted">{description}</p>
      </header>
      {children}
    </section>
  );
}

function ToggleRow({ label, description, checked, disabled, onChange }: { label: string; description: string; checked: boolean; disabled?: boolean; onChange: (value: boolean) => void }) {
  return (
    <label className="flex cursor-pointer items-center gap-4 px-5 py-4">
      <span className="min-w-0 flex-1"><span className="block text-sm font-semibold text-navy-deep">{label}</span><span className="mt-1 block text-xs leading-5 text-muted">{description}</span></span>
      <input className="peer sr-only" type="checkbox" role="switch" checked={checked} disabled={disabled} onChange={(event) => onChange(event.target.checked)} />
      <span aria-hidden="true" className="relative h-7 w-12 shrink-0 rounded-full bg-navy/20 transition peer-checked:bg-teal peer-focus-visible:outline peer-focus-visible:outline-2 peer-focus-visible:outline-offset-2 peer-focus-visible:outline-teal peer-disabled:opacity-50 after:absolute after:left-1 after:top-1 after:h-5 after:w-5 after:rounded-full after:bg-white after:shadow after:transition-transform peer-checked:after:translate-x-5" />
    </label>
  );
}

function ThemeChoice({ label, detail, value, selected, onSelect }: { label: string; detail: string; value: ThemePreference; selected: ThemePreference; onSelect: (value: ThemePreference) => void }) {
  const active = value === selected;
  return (
    <button type="button" role="radio" aria-checked={active} onClick={() => onSelect(value)} className={`rounded-xl border px-2 py-3 text-center transition ${active ? "border-teal bg-teal/10 text-teal-deep ring-1 ring-teal/20" : "border-navy/10 bg-cream/50 text-muted hover:border-teal/35"}`}>
      <span className="block text-sm font-semibold">{label}</span><span className="mt-0.5 block text-[10px]">{detail}</span>
    </button>
  );
}

function ChoiceButton({ active, onClick, children }: { active: boolean; onClick: () => void; children: ReactNode }) {
  return <button type="button" aria-pressed={active} onClick={onClick} className={`rounded-lg px-3 py-1.5 text-xs font-bold transition ${active ? "bg-teal text-white shadow-sm" : "text-muted hover:text-navy-deep"}`}>{children}</button>;
}

function SettingsLink({ to, title, description, icon }: { to: string; title: string; description: string; icon: "users" | "tag" }) {
  return (
    <Link to={to} className="flex items-center gap-3 border-t border-navy/8 px-5 py-4 transition hover:bg-teal/5">
      <Icon name={icon} /><span className="min-w-0 flex-1"><span className="block text-sm font-semibold text-navy-deep">{title}</span><span className="mt-0.5 block text-xs text-muted">{description}</span></span><span className="text-muted" aria-hidden="true">›</span>
    </Link>
  );
}

function Icon({ name }: { name: "users" | "tag" | "shield" }) {
  const path = name === "users" ? <><circle cx="9" cy="8" r="3"/><path d="M3 19c.7-3 3-5 6-5s5.3 2 6 5M16 7.5a2.5 2.5 0 0 1 0 5M16 15c2.3.3 4 1.8 4.5 4"/></> : name === "tag" ? <path d="M4 5v6l8 8 7-7-8-8H5a1 1 0 0 0-1 1Z M8 8h.01"/> : <path d="M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6l8-3Z M9 12l2 2 4-4"/>;
  return <span className="grid h-9 w-9 shrink-0 place-items-center rounded-xl bg-teal/10 text-teal"><svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="1.8">{path}</svg></span>;
}

function messageFrom(error: unknown, fallback: string) {
  return error instanceof ApiError ? error.message : fallback;
}
