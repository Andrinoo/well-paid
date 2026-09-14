import { createContext, useContext, useState, type ReactNode } from "react";
import { NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import { logout } from "./api";
import { Wordmark } from "./ui";

type NavItem = {
  to: string;
  label: string;
  end?: boolean;
  icon: ReactNode;
};

const PRIMARY: NavItem[] = [
  { to: "/app", label: "Início", end: true, icon: <IconGrid /> },
  { to: "/app/despesas", label: "Despesas", icon: <IconCoin /> },
  { to: "/app/receitas", label: "Proventos", icon: <IconWallet /> },
  { to: "/app/metas", label: "Metas", icon: <IconTrophy /> },
  { to: "/app/reserva", label: "Reserva", icon: <IconShield /> },
];

const MORE: NavItem[] = [
  { to: "/app/despesas?filtro=pagar", label: "A pagar", icon: <IconCoin /> },
  { to: "/app/listas", label: "Listas de compras", icon: <IconList /> },
  { to: "/app/investimentos", label: "Investimentos", icon: <IconChart /> },
  { to: "/app/familia", label: "Família", icon: <IconUsers /> },
  { to: "/app/definicoes", label: "Definições", icon: <IconCog /> },
];

const ShellMenuCtx = createContext<() => void>(() => {});
export function useToggleShellMenu() {
  return useContext(ShellMenuCtx);
}

function linkActive(pathname: string, search: string, item: NavItem): boolean {
  if (item.to.startsWith("/app/despesas?filtro=pagar")) {
    return pathname === "/app/despesas" && search.includes("filtro=pagar");
  }
  if (item.to === "/app/despesas") {
    return pathname === "/app/despesas" && !search.includes("filtro=pagar");
  }
  if (item.end) return pathname === item.to;
  return pathname === item.to || pathname.startsWith(`${item.to}/`);
}

export function AppShell() {
  const navigate = useNavigate();
  const location = useLocation();
  const [open, setOpen] = useState(false);
  const isHome = location.pathname === "/app";

  async function onLogout() {
    await logout();
    navigate("/", { replace: true });
  }

  function renderLinks(items: NavItem[]) {
    return items.map((item) => {
      const active = linkActive(location.pathname, location.search, item);
      return (
        <NavLink
          key={item.to}
          to={item.to}
          end={item.end}
          onClick={() => setOpen(false)}
          className={`flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm tracking-wide ${
            active
              ? "bg-gold/15 text-gold"
              : "text-cream/70 hover:bg-white/5 hover:text-cream"
          }`}
        >
          <span className="inline-flex h-5 w-5 shrink-0 items-center justify-center opacity-90">
            {item.icon}
          </span>
          {item.label}
        </NavLink>
      );
    });
  }

  const nav = (
    <nav className="flex flex-1 flex-col gap-1 px-3" aria-label="Navegação principal">
      {renderLinks(PRIMARY)}
      <p className="mt-4 px-3 text-[10px] uppercase tracking-[0.22em] text-cream/35">
        Atalhos
      </p>
      {renderLinks(MORE)}
    </nav>
  );

  return (
    <ShellMenuCtx.Provider value={() => setOpen((v) => !v)}>
    <div className="flex h-full min-h-dvh bg-cream">
      <aside className="hidden w-60 shrink-0 flex-col bg-navy-deep md:flex">
        <div className="px-5 py-6">
          <Wordmark light to="/app" />
          <p className="mt-1 text-[10px] uppercase tracking-[0.28em] text-cream/35">
            Carteira
          </p>
        </div>
        {nav}
        <div className="border-t border-white/10 p-3">
          <button
            type="button"
            className="flex w-full items-center gap-3 rounded-lg px-3 py-2 text-left text-sm text-cream/50 hover:bg-white/5 hover:text-gold"
            onClick={() => void onLogout()}
          >
            <span className="inline-flex h-5 w-5 items-center justify-center">
              <IconLeave />
            </span>
            Sair
          </button>
        </div>
      </aside>

      <div className="flex min-w-0 flex-1 flex-col">
        {!isHome ? (
          <header className="flex items-center justify-between bg-navy-deep px-4 py-3 md:hidden">
            <Wordmark light to="/app" />
            <button
              type="button"
              className="text-cream"
              onClick={() => setOpen((v) => !v)}
            >
              Menu
            </button>
          </header>
        ) : null}
        {open ? (
          <div className="bg-navy-deep pb-3 md:hidden">
            {nav}
            <button
              type="button"
              className="mx-3 mb-2 flex items-center gap-3 rounded-lg px-3 py-2 text-left text-sm text-cream/50"
              onClick={() => void onLogout()}
            >
              <IconLeave />
              Sair
            </button>
          </div>
        ) : null}
        <main className={isHome ? "flex min-h-0 flex-1 flex-col overflow-hidden pb-16 md:pb-0" : "flex-1 px-4 py-5 pb-20 sm:px-6 md:pb-5"}>
          <Outlet />
        </main>
        <nav
          className="sticky bottom-0 z-10 grid grid-cols-5 border-t border-navy/10 bg-cream-muted md:hidden"
          aria-label="Separadores"
        >
          {PRIMARY.map((item) => {
            const active = linkActive(location.pathname, location.search, item);
            return (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.end}
                className={`flex flex-col items-center gap-0.5 py-2 text-[10px] ${
                  active ? "text-navy" : "text-muted"
                }`}
              >
                <span className={active ? "text-gold" : ""}>{item.icon}</span>
                {item.label}
              </NavLink>
            );
          })}
        </nav>
      </div>
    </div>
    </ShellMenuCtx.Provider>
  );
}

function IconGrid() {
  return (
    <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="1.8">
      <rect x="3" y="3" width="7" height="7" rx="1.2" />
      <rect x="14" y="3" width="7" height="7" rx="1.2" />
      <rect x="3" y="14" width="7" height="7" rx="1.2" />
      <rect x="14" y="14" width="7" height="7" rx="1.2" />
    </svg>
  );
}
function IconCoin() {
  return (
    <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="1.8">
      <circle cx="12" cy="12" r="8" />
      <path d="M12 7v10M9.5 9.5c.6-1 1.6-1.5 2.5-1.5s1.9.5 2.5 1.5M9.5 14.5c.6 1 1.6 1.5 2.5 1.5s1.9-.5 2.5-1.5" />
    </svg>
  );
}
function IconWallet() {
  return (
    <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="1.8">
      <rect x="3" y="6" width="18" height="13" rx="2" />
      <path d="M3 10h18M16 14.5h.01" />
    </svg>
  );
}
function IconTrophy() {
  return (
    <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="1.8">
      <path d="M8 4h8v5a4 4 0 0 1-8 0V4Z" />
      <path d="M8 6H5a3 3 0 0 0 3 5M16 6h3a3 3 0 0 1-3 5M12 13v3M9 20h6" />
    </svg>
  );
}
function IconChart() {
  return (
    <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="1.8">
      <path d="M4 19V5M4 19h16" />
      <path d="M7 14l4-4 3 3 5-6" />
    </svg>
  );
}
function IconShield() {
  return (
    <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="1.8">
      <path d="M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6l8-3Z" />
    </svg>
  );
}
function IconList() {
  return (
    <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="1.8">
      <path d="M9 6h12M9 12h12M9 18h12M4 6h.01M4 12h.01M4 18h.01" />
    </svg>
  );
}
function IconUsers() {
  return (
    <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="1.8">
      <circle cx="9" cy="8" r="3" />
      <path d="M3 19c.6-3 3-5 6-5s5.4 2 6 5" />
      <circle cx="17" cy="9" r="2.2" />
      <path d="M16 19c.4-2 1.6-3.4 3.4-4" />
    </svg>
  );
}
function IconCog() {
  return (
    <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="1.8">
      <circle cx="12" cy="12" r="3" />
      <path d="M12 3v2M12 19v2M4.9 6.5l1.7 1M17.4 16.5l1.7 1M3 12h2M19 12h2M4.9 17.5l1.7-1M17.4 7.5l1.7-1" />
    </svg>
  );
}
function IconLeave() {
  return (
    <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="1.8">
      <path d="M10 6H6a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h4M14 16l4-4-4-4M10 12h8" />
    </svg>
  );
}
