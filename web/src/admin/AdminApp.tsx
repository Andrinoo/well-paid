import { useEffect, useState, type FormEvent, type ReactNode } from "react";
import {
  BrowserRouter,
  Link,
  Navigate,
  Route,
  Routes,
  useNavigate,
  useParams,
} from "react-router-dom";
import { ApiError } from "../api";
import { useLocale } from "../i18n/LocaleProvider";
import { AuthCard, ErrorText, Field, PrimaryButton, Wordmark } from "../ui";
import {
  clearSaTokens,
  createPix,
  fetchBilling,
  fetchSaMe,
  fetchUsers,
  getSaAccess,
  patchFree,
  patchModules,
  releasePix,
  saBoot,
  saLogin,
  type AdminUserRow,
  type BillingDetail,
} from "./api";

const PLAN_LABEL: Record<string, { "pt-BR": string; "en-US": string }> = {
  super: { "pt-BR": "Super", "en-US": "Super" },
  free: { "pt-BR": "Free", "en-US": "Free" },
  trial: { "pt-BR": "Trial", "en-US": "Trial" },
  paid: { "pt-BR": "Pago", "en-US": "Paid" },
  expired: { "pt-BR": "Expirado", "en-US": "Expired" },
};

const MODULE_LABEL: Record<string, { "pt-BR": string; "en-US": string }> = {
  dashboard: { "pt-BR": "Dashboard", "en-US": "Dashboard" },
  payables: { "pt-BR": "A pagar", "en-US": "Payables" },
  incomes: { "pt-BR": "Proventos", "en-US": "Income" },
  settings: { "pt-BR": "Configurações", "en-US": "Settings" },
};

function locLabel(
  map: Record<string, { "pt-BR": string; "en-US": string }>,
  id: string,
  locale: "pt-BR" | "en-US",
): string {
  return map[id]?.[locale] ?? id;
}

function NotFound() {
  useEffect(() => {
    document.title = "Well Paid";
  }, []);
  return (
    <div className="flex min-h-dvh items-center justify-center bg-paper font-ui text-navy/70">
      Não encontrado
    </div>
  );
}

function RequireSa({ children }: { children: ReactNode }) {
  const [state, setState] = useState<"loading" | "ok" | "gone">("loading");

  useEffect(() => {
    if (!getSaAccess()) {
      setState("gone");
      return;
    }
    fetchSaMe()
      .then(() => setState("ok"))
      .catch((err) => {
        clearSaTokens();
        if (err instanceof ApiError && (err.status === 404 || err.status === 401)) {
          setState("gone");
          return;
        }
        setState("gone");
      });
  }, []);

  if (state === "loading") {
    return (
      <div className="flex min-h-dvh items-center justify-center bg-paper font-ui text-muted">
        …
      </div>
    );
  }
  if (state === "gone") return <Navigate to="/" replace />;
  return children;
}

function LoginScreen() {
  const navigate = useNavigate();
  const { auth: a } = useLocale();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [hidden, setHidden] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      await saLogin(email.trim(), password);
      await fetchSaMe();
      navigate("/users", { replace: true });
    } catch (err) {
      clearSaTokens();
      if (err instanceof ApiError && err.status === 404) {
        setHidden(true);
      } else if (err instanceof ApiError && err.status === 429) {
        setError(a.login.locked);
      } else {
        setError(a.login.fallback);
      }
    } finally {
      setBusy(false);
    }
  }

  if (hidden) return <NotFound />;

  return (
    <AuthCard title={a.login.title} subtitle={a.login.subtitle}>
      <form className="space-y-4" onSubmit={onSubmit}>
        <ErrorText message={error} />
        <Field
          label={a.login.email}
          type="email"
          value={email}
          onChange={setEmail}
          autoComplete="username"
          required
        />
        <Field
          label={a.login.password}
          type="password"
          value={password}
          onChange={setPassword}
          autoComplete="current-password"
          required
        />
        <PrimaryButton disabled={busy}>{busy ? a.login.busy : a.login.submit}</PrimaryButton>
      </form>
    </AuthCard>
  );
}

function planOf(row: AdminUserRow): string {
  if (row.is_superuser) return "super";
  if (row.is_free_plan) return "free";
  if (row.trial_ends_at && new Date(row.trial_ends_at).getTime() > Date.now()) return "trial";
  if (!row.is_active) return "expired";
  return "paid";
}

function UsersScreen() {
  const { locale } = useLocale();
  const navigate = useNavigate();
  const [q, setQ] = useState("");
  const [plan, setPlan] = useState("");
  const [items, setItems] = useState<AdminUserRow[]>([]);
  const [total, setTotal] = useState(0);
  const [error, setError] = useState<string | null>(null);

  async function load(nextPlan = plan, nextQ = q) {
    setError(null);
    try {
      const data = await fetchUsers({ q: nextQ, plan: nextPlan });
      setItems(data.items);
      setTotal(data.total);
    } catch (err) {
      if (err instanceof ApiError && err.status === 404) {
        clearSaTokens();
        navigate("/", { replace: true });
        return;
      }
      setError("Não foi possível carregar a lista.");
    }
  }

  useEffect(() => {
    void load();
  }, []);

  const filters = [
    { id: "", label: locale === "en-US" ? "All" : "Todos" },
    { id: "trial", label: "Trial" },
    { id: "free", label: "Free" },
    { id: "paid", label: locale === "en-US" ? "Paid" : "Pago" },
    { id: "expired", label: locale === "en-US" ? "Expired" : "Expirado" },
    { id: "pending_pix", label: "PIX" },
  ];

  return (
    <Shell
      title={locale === "en-US" ? "Accounts" : "Contas"}
      onSignOut={() => {
        clearSaTokens();
        navigate("/", { replace: true });
      }}
    >
      <form
        className="mb-5 flex flex-wrap gap-2"
        onSubmit={(e) => {
          e.preventDefault();
          void load(plan, q);
        }}
      >
        <input
          className="min-w-[12rem] flex-1 rounded-xl border border-navy/12 bg-white px-3 py-2 text-sm outline-none focus:border-teal/40 focus:ring-2 focus:ring-teal/30"
          placeholder="e-mail"
          value={q}
          onChange={(e) => setQ(e.target.value)}
        />
        <button
          type="submit"
          className="rounded-full bg-teal px-4 py-2 text-sm font-semibold text-white"
        >
          {locale === "en-US" ? "Search" : "Pesquisar"}
        </button>
      </form>
      <div className="mb-4 flex flex-wrap gap-2">
        {filters.map((f) => (
          <button
            key={f.id || "all"}
            type="button"
            onClick={() => {
              setPlan(f.id);
              void load(f.id, q);
            }}
            className={`rounded-full px-3 py-1 text-xs font-semibold ${
              plan === f.id ? "bg-navy-deep text-cream" : "bg-sage text-teal-deep"
            }`}
          >
            {f.label}
          </button>
        ))}
      </div>
      <ErrorText message={error} />
      <p className="mb-3 text-xs text-muted">{total} contas</p>
      <ul className="divide-y divide-navy/8 rounded-[22px] border border-navy/8 bg-white">
        {items.map((row) => (
          <li key={row.id}>
            <Link
              to={`/users/${row.id}`}
              className="flex items-center justify-between gap-3 px-4 py-3 text-sm hover:bg-sage/50"
            >
              <span>
                <span className="block font-medium text-navy-deep">{row.email}</span>
                <span className="text-xs text-muted">
                  {row.display_name || row.full_name || "—"}
                </span>
              </span>
              <span className="shrink-0 rounded-full bg-sage px-2.5 py-1 text-[11px] font-semibold uppercase tracking-wide text-teal-deep">
                {locLabel(PLAN_LABEL, planOf(row), locale)}
              </span>
            </Link>
          </li>
        ))}
      </ul>
    </Shell>
  );
}

function UserScreen() {
  const { userId } = useParams();
  const { locale } = useLocale();
  const navigate = useNavigate();
  const [data, setData] = useState<BillingDetail | null>(null);
  const [payer, setPayer] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function load() {
    if (!userId) return;
    const row = await fetchBilling(userId);
    setData(row);
    setPayer(row.full_name || row.display_name || row.email);
  }

  useEffect(() => {
    load().catch((err) => {
      if (err instanceof ApiError && err.status === 404) {
        setData(null);
        setError("gone");
        return;
      }
      setError("fail");
    });
  }, [userId]);

  if (error === "gone") return <NotFound />;
  if (error === "fail") {
    return (
      <Shell title="Well Paid" onSignOut={() => navigate("/", { replace: true })}>
        <ErrorText message="Não foi possível carregar a conta." />
      </Shell>
    );
  }
  if (!data) {
    return (
      <div className="flex min-h-dvh items-center justify-center bg-paper font-ui text-muted">
        …
      </div>
    );
  }

  const latestPending = data.payments.find((p) => p.status === "pending");

  async function run(fn: () => Promise<BillingDetail>) {
    setBusy(true);
    setError(null);
    try {
      setData(await fn());
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Falha");
    } finally {
      setBusy(false);
    }
  }

  return (
    <Shell
      title={data.email}
      backTo="/users"
      onSignOut={() => {
        clearSaTokens();
        navigate("/", { replace: true });
      }}
    >
      <ErrorText message={error && error !== "fail" && error !== "gone" ? error : null} />
      <section className="mb-6 rounded-[22px] border border-navy/8 bg-white p-5">
        <p className="text-xs font-semibold uppercase tracking-[0.2em] text-teal-deep">
          {locLabel(PLAN_LABEL, data.plan, locale)}
        </p>
        <p className="mt-2 text-sm text-navy/70">
          {locale === "en-US" ? "Trial until" : "Trial até"}{" "}
          {data.trial_ends_at ? new Date(data.trial_ends_at).toLocaleString(locale) : "—"}
        </p>
        <p className="text-sm text-navy/70">
          {locale === "en-US" ? "Paid until" : "Pago até"}{" "}
          {data.paid_until ? new Date(data.paid_until).toLocaleString(locale) : "—"}
        </p>
        <label className="mt-4 flex items-center gap-2 text-sm">
          <input
            type="checkbox"
            checked={data.is_free_plan}
            disabled={busy || data.is_superuser}
            onChange={(e) => void run(() => patchFree(data.id, e.target.checked))}
          />
          Free
        </label>
      </section>

      <section className="mb-6 rounded-[22px] border border-navy/8 bg-white p-5">
        <h2 className="font-display text-xl text-navy-deep">
          {locale === "en-US" ? "Modules" : "Módulos"}
        </h2>
        <ul className="mt-3 space-y-2">
          {data.catalog.map((mod) => (
            <li key={mod.id}>
              <label className="flex items-center gap-2 text-sm">
                <input
                  type="checkbox"
                  checked={data.modules.includes(mod.id)}
                  disabled={busy || data.is_superuser}
                  onChange={(e) =>
                    void run(() =>
                      patchModules(data.id, [{ module_id: mod.id, enabled: e.target.checked }]),
                    )
                  }
                />
                {locLabel(MODULE_LABEL, mod.id, locale)}
              </label>
            </li>
          ))}
        </ul>
      </section>

      <section className="mb-6 rounded-[22px] border border-navy/8 bg-white p-5">
        <h2 className="font-display text-xl text-navy-deep">PIX</h2>
        <Field
          label={locale === "en-US" ? "Payer name" : "Nome de quem pagou"}
          value={payer}
          onChange={setPayer}
          required
        />
        <div className="mt-4 flex flex-wrap gap-2">
          <button
            type="button"
            disabled={busy}
            onClick={() => void run(() => createPix(data.id))}
            className="rounded-full border border-navy/15 px-4 py-2 text-sm font-semibold"
          >
            {locale === "en-US" ? "Generate PIX" : "Gerar PIX"}
          </button>
          <button
            type="button"
            disabled={busy || payer.trim().length < 2}
            onClick={() =>
              void run(() => releasePix(data.id, payer.trim(), latestPending?.id))
            }
            className="rounded-full bg-teal px-4 py-2 text-sm font-semibold text-white"
          >
            {locale === "en-US" ? "Release access" : "Liberar"}
          </button>
        </div>
        {latestPending?.pix_copy ? (
          <p className="mt-3 break-all rounded-xl bg-sage px-3 py-2 text-xs text-navy-deep">
            {latestPending.pix_copy}
          </p>
        ) : null}
        <ul className="mt-4 space-y-2 text-xs text-navy/70">
          {data.payments.map((p) => (
            <li key={p.id} className="rounded-lg border border-navy/8 px-3 py-2">
              {p.status} · {p.payer_name || "—"} ·{" "}
              {p.due_at ? new Date(p.due_at).toLocaleDateString(locale) : p.created_at}
            </li>
          ))}
        </ul>
      </section>
    </Shell>
  );
}

function Shell({
  title,
  backTo,
  onSignOut,
  children,
}: {
  title: string;
  backTo?: string;
  onSignOut: () => void;
  children: ReactNode;
}) {
  const { locale } = useLocale();
  useEffect(() => {
    document.title = "Well Paid";
  }, [title]);
  return (
    <div className="site-root min-h-dvh bg-paper font-ui text-navy-deep">
      <header className="mx-auto flex w-full max-w-3xl items-center justify-between gap-3 px-4 py-5">
        <Wordmark accent="teal" size="md" withMark to="/" />
        <button
          type="button"
          onClick={onSignOut}
          className="text-xs font-semibold text-teal-deep hover:underline"
        >
          {locale === "en-US" ? "Sign out" : "Sair"}
        </button>
      </header>
      <main className="mx-auto w-full max-w-3xl px-4 pb-16">
        {backTo ? (
          <Link to={backTo} className="mb-3 inline-block text-sm text-teal-deep hover:underline">
            ←
          </Link>
        ) : null}
        <h1 className="mb-5 font-display text-3xl font-semibold">{title}</h1>
        {children}
      </main>
    </div>
  );
}

export function AdminApp() {
  const boot = saBoot();
  if (!boot.path) return <NotFound />;

  return (
    <BrowserRouter basename={boot.path}>
      <Routes>
        <Route path="/" element={<LoginScreen />} />
        <Route
          path="/users"
          element={
            <RequireSa>
              <UsersScreen />
            </RequireSa>
          }
        />
        <Route
          path="/users/:userId"
          element={
            <RequireSa>
              <UserScreen />
            </RequireSa>
          }
        />
        <Route path="*" element={<NotFound />} />
      </Routes>
    </BrowserRouter>
  );
}
