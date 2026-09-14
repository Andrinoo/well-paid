import { useEffect } from "react";
import { Link } from "react-router-dom";
import {
  formatLandingMoney,
  formatLandingMonth,
  PLAN_PRICE_CENTS,
} from "../i18n/landing";
import { LangToggle } from "../i18n/LangToggle";
import { useLocale } from "../i18n/LocaleProvider";
import { isSignedIn } from "../session";
import { Wordmark } from "../ui";

const PREVIEW = {
  year: 2026,
  month: 9,
  inCents: 842000,
  outCents: 611050,
  dueCents: 128900,
};

export function LandingPage() {
  const inApp = isSignedIn();
  const { locale, t } = useLocale();
  const spentRatio = Math.min(PREVIEW.outCents / PREVIEW.inCents, 1);

  useEffect(() => {
    const previousTitle = document.title;
    const meta = document.querySelector('meta[name="description"]');
    const previousDescription = meta?.getAttribute("content") ?? "";
    document.title = t.metaTitle;
    if (meta) meta.setAttribute("content", t.metaDescription);
    return () => {
      document.title = previousTitle;
      if (meta) meta.setAttribute("content", previousDescription);
    };
  }, [t]);

  return (
    <div className="site-root min-h-dvh bg-paper font-ui text-navy-deep">
      <div className="pointer-events-none absolute inset-x-0 top-0 h-[28rem] bg-[radial-gradient(ellipse_at_top,_#e8f4f0_0%,_transparent_58%)]" />

      <header className="relative mx-auto flex max-w-6xl items-center justify-between gap-4 px-5 py-5">
        <Wordmark accent="teal" size="md" withMark />
        <nav className="flex items-center gap-2 sm:gap-3">
          <LangToggle />
          {inApp ? (
            <Link
              to="/app"
              className="rounded-full bg-navy-deep px-4 py-2 text-sm font-semibold text-cream hover:bg-navy"
            >
              {t.nav.openApp}
            </Link>
          ) : (
            <>
              <Link
                to="/login"
                className="hidden text-sm font-medium text-navy/80 hover:text-teal-deep sm:inline"
              >
                {t.nav.signIn}
              </Link>
              <Link
                to="/registar"
                className="rounded-full bg-navy-deep px-4 py-2 text-sm font-semibold text-cream hover:bg-navy"
              >
                {t.nav.signUp}
              </Link>
            </>
          )}
        </nav>
      </header>

      <main className="relative mx-auto max-w-6xl px-5 pb-20 pt-8 sm:pt-14">
        <div className="grid items-center gap-12 lg:grid-cols-[1.05fr_0.95fr] lg:gap-16">
          <section>
            <p className="text-[11px] font-semibold uppercase tracking-[0.28em] text-teal-deep">
              {t.hero.eyebrow}
            </p>
            <h1 className="mt-4 max-w-xl">
              <span className="block font-display text-5xl font-semibold leading-[0.95] tracking-tight text-navy-deep sm:text-7xl">
                Well <span className="text-teal">Paid</span>
              </span>
              <span className="mt-5 block font-display text-[1.65rem] font-medium leading-tight text-navy-deep/90 sm:text-4xl">
                {t.hero.title}
              </span>
            </h1>
            <p className="mt-5 max-w-lg text-base leading-relaxed text-navy/75 sm:text-lg">
              {t.hero.body}
            </p>
            <div className="mt-8 flex flex-wrap gap-3">
              <Link
                to={inApp ? "/app" : "/registar"}
                className="rounded-full bg-teal px-6 py-3 text-sm font-semibold text-white shadow-[0_10px_24px_-12px_rgba(26,143,122,0.9)] hover:bg-teal-deep"
              >
                {inApp ? t.nav.openApp : t.hero.cta}
              </Link>
              {!inApp && (
                <Link
                  to="/login"
                  className="rounded-full border border-navy/12 bg-white/70 px-6 py-3 text-sm font-medium text-navy hover:border-teal/40"
                >
                  {t.hero.secondary}
                </Link>
              )}
            </div>
          </section>

          <aside className="rounded-[28px] border border-navy/8 bg-white/80 p-6 shadow-[0_24px_60px_-36px_rgba(20,28,42,0.45)] backdrop-blur">
            <div className="flex items-start justify-between gap-3">
              <div>
                <p className="text-[11px] font-semibold uppercase tracking-[0.22em] text-muted">
                  {t.preview.kicker}
                </p>
                <p className="mt-1 font-display text-2xl text-navy-deep">
                  {formatLandingMonth(PREVIEW.year, PREVIEW.month, locale)}
                </p>
              </div>
              <span className="rounded-full bg-sage px-3 py-1 text-xs font-semibold text-teal-deep">
                {t.preview.status}
              </span>
            </div>

            <div className="mt-6 h-2 overflow-hidden rounded-full bg-sage">
              <div
                className="h-full rounded-full bg-teal"
                style={{ width: `${Math.round(spentRatio * 100)}%` }}
              />
            </div>

            <dl className="mt-6 grid grid-cols-3 gap-3">
              {[
                [t.preview.in, PREVIEW.inCents, "text-teal-deep"],
                [t.preview.out, PREVIEW.outCents, "text-navy-deep"],
                [t.preview.due, PREVIEW.dueCents, "text-expense-line"],
              ].map(([label, cents, color]) => (
                <div
                  key={String(label)}
                  className="rounded-2xl bg-paper px-3 py-3"
                >
                  <dt className="text-[11px] font-medium uppercase tracking-wide text-muted">
                    {label}
                  </dt>
                  <dd
                    className={`mt-1 text-[0.95rem] font-semibold tabular-nums sm:text-base ${color}`}
                  >
                    {formatLandingMoney(Number(cents), locale)}
                  </dd>
                </div>
              ))}
            </dl>
            <p className="mt-5 text-sm leading-relaxed text-muted">
              {t.preview.caption}
            </p>
          </aside>
        </div>

        <section className="mt-20 sm:mt-24">
          <p className="text-[11px] font-semibold uppercase tracking-[0.28em] text-teal-deep">
            {t.pillars.kicker}
          </p>
          <h2 className="mt-3 max-w-xl font-display text-3xl font-semibold tracking-tight text-navy-deep sm:text-4xl">
            {t.pillars.title}
          </h2>
          <ul className="mt-10 grid gap-4 md:grid-cols-3">
            {t.pillars.items.map((item) => (
              <li
                key={item.title}
                className="rounded-[24px] border border-navy/8 bg-white/70 p-6"
              >
                <h3 className="font-display text-2xl text-navy-deep">
                  {item.title}
                </h3>
                <p className="mt-3 text-sm leading-relaxed text-navy/70">
                  {item.body}
                </p>
              </li>
            ))}
          </ul>
        </section>

        <section className="mt-16 rounded-[28px] border border-navy/8 bg-white/80 p-6 shadow-[0_24px_60px_-36px_rgba(20,28,42,0.35)] sm:p-10 lg:grid lg:grid-cols-[1.1fr_0.9fr] lg:items-center lg:gap-12">
          <div>
            <p className="text-[11px] font-semibold uppercase tracking-[0.28em] text-teal-deep">
              {t.plan.kicker}
            </p>
            <h2 className="mt-3 font-display text-3xl font-semibold tracking-tight text-navy-deep sm:text-4xl">
              {t.plan.title}
            </h2>
            <p className="mt-2 font-display text-lg text-teal-deep">{t.plan.name}</p>
            <ul className="mt-6 space-y-3">
              {t.plan.items.map((item) => (
                <li key={item} className="flex gap-3 text-sm leading-relaxed text-navy/75">
                  <span className="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full bg-teal" />
                  {item}
                </li>
              ))}
            </ul>
          </div>
          <div className="mt-8 rounded-[24px] bg-navy-deep px-6 py-8 text-cream sm:px-8 lg:mt-0">
            <p className="text-[11px] font-semibold uppercase tracking-[0.22em] text-teal-bright">
              {t.plan.kicker}
            </p>
            <p className="mt-3 font-display text-2xl font-semibold">Well Paid</p>
            <p className="mt-4 flex items-end gap-1">
              <span className="font-display text-5xl font-semibold tabular-nums tracking-tight">
                {formatLandingMoney(PLAN_PRICE_CENTS, locale)}
              </span>
              <span className="mb-1.5 text-sm text-cream/70">{t.plan.period}</span>
            </p>
            <p className="mt-2 text-sm text-cream/65">{t.plan.billed}</p>
            <Link
              to={inApp ? "/app" : "/registar"}
              className="mt-8 inline-flex rounded-full bg-teal px-6 py-3 text-sm font-semibold text-white hover:bg-teal-bright"
            >
              {inApp ? t.nav.openApp : t.plan.cta}
            </Link>
          </div>
        </section>

        <section className="mt-16 rounded-[28px] bg-navy-deep px-6 py-10 text-cream sm:px-10">
          <p className="text-[11px] font-semibold uppercase tracking-[0.28em] text-teal-bright">
            {t.trust.kicker}
          </p>
          <h2 className="mt-3 max-w-lg font-display text-3xl font-semibold tracking-tight sm:text-4xl">
            {t.trust.title}
          </h2>
          <p className="mt-4 max-w-xl text-sm leading-relaxed text-cream/75 sm:text-base">
            {t.trust.body}
          </p>
          <ul className="mt-8 flex flex-wrap gap-2">
            {t.trust.points.map((point) => (
              <li
                key={point}
                className="rounded-full border border-cream/15 bg-white/5 px-4 py-2 text-sm text-cream/90"
              >
                {point}
              </li>
            ))}
          </ul>
        </section>
      </main>

      <footer className="relative mx-auto flex max-w-6xl flex-col gap-3 px-5 pb-10 pt-2 text-sm text-muted sm:flex-row sm:items-end sm:justify-between">
        <div>
          <Wordmark accent="teal" size="md" withMark />
          <p className="mt-2">{t.footer.tag}</p>
        </div>
        <p className="max-w-sm sm:text-right">{t.footer.copyright}</p>
      </footer>
    </div>
  );
}
