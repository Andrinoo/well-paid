import { Link } from "react-router-dom";
import { isSignedIn } from "../session";
import { Wordmark } from "../ui";

export function LandingPage() {
  const inApp = isSignedIn();
  return (
    <div className="min-h-dvh bg-cream">
      <header className="mx-auto flex max-w-5xl items-center justify-between px-5 py-5">
        <Wordmark />
        <nav className="flex items-center gap-3 text-sm">
          {inApp ? (
            <Link
              to="/app"
              className="rounded-lg bg-navy-deep px-4 py-2 font-medium text-cream"
            >
              Abrir dashboard
            </Link>
          ) : (
            <>
              <Link to="/login" className="text-navy hover:text-gold-pressed">
                Entrar
              </Link>
              <Link
                to="/registar"
                className="rounded-lg bg-navy-deep px-4 py-2 font-medium text-cream"
              >
                Criar conta
              </Link>
            </>
          )}
        </nav>
      </header>

      <main className="mx-auto max-w-5xl px-5 pb-20 pt-10 sm:pt-16">
        <p className="text-[11px] uppercase tracking-[0.32em] text-muted">
          Finanças pessoais
        </p>
        <h1 className="mt-4 max-w-xl font-serif text-4xl leading-tight text-navy-deep sm:text-6xl">
          O mês, de relance.
        </h1>
        <p className="mt-5 max-w-lg text-base leading-relaxed text-navy/80 sm:text-lg">
          Well Paid mostra quanto entrou, quanto saiu, o que ainda falta pagar e
          como vão as metas — no telemóvel e agora também no browser.
        </p>
        <div className="mt-8 flex flex-wrap gap-3">
          <Link
            to={inApp ? "/app" : "/registar"}
            className="rounded-lg bg-gold px-5 py-3 text-sm font-semibold text-navy-deep hover:bg-gold-pressed"
          >
            {inApp ? "Ir ao dashboard" : "Começar"}
          </Link>
          <Link
            to="/login"
            className="rounded-lg border border-navy/15 px-5 py-3 text-sm font-medium text-navy"
          >
            Já tenho conta
          </Link>
        </div>

        <ul className="mt-16 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {[
            ["Categorias", "Onde o dinheiro foi neste mês."],
            ["Fluxo", "Receitas contra despesas, mês a mês."],
            ["A pagar", "Pendentes com vencimento à vista."],
            ["Metas", "Progresso sem abrir a folha toda."],
          ].map(([title, body]) => (
            <li
              key={title}
              className="rounded-2xl border border-navy/8 bg-white/50 p-5"
            >
              <h2 className="font-serif text-xl text-navy-deep">{title}</h2>
              <p className="mt-2 text-sm leading-relaxed text-muted">{body}</p>
            </li>
          ))}
        </ul>
      </main>
    </div>
  );
}
