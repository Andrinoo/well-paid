import { useEffect, type ReactNode } from "react";
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

function IconSun({ className = "h-5 w-5" }: { className?: string }) {
  return (
    <svg viewBox="0 0 24 24" className={className} fill="none" aria-hidden>
      <circle cx="12" cy="12" r="4" fill="currentColor" />
      <path
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
        d="M12 2.5v2.2M12 19.3v2.2M2.5 12h2.2M19.3 12h2.2M5.2 5.2l1.6 1.6M17.2 17.2l1.6 1.6M18.8 5.2l-1.6 1.6M6.8 17.2l-1.6 1.6"
      />
    </svg>
  );
}

function IconCheck({ className = "h-5 w-5" }: { className?: string }) {
  return (
    <svg viewBox="0 0 24 24" className={className} fill="none" aria-hidden>
      <circle cx="12" cy="12" r="9" stroke="currentColor" strokeWidth="1.8" />
      <path
        d="M8 12.2 10.6 15 16 9.5"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function IconLeaf() {
  return (
    <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" aria-hidden>
      <path
        d="M5 19c7-1 13-7 14-14-7 1-13 7-14 14Z"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinejoin="round"
      />
      <path d="M8 16c2-2 5-5 8-8" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
    </svg>
  );
}

const PILLAR_STYLE: { tone: string; icon: ReactNode }[] = [
  { tone: "bg-sky text-teal-deep", icon: <IconSun /> },
  { tone: "bg-peach text-gold-pressed", icon: <IconCheck /> },
  { tone: "bg-sage text-teal-deep", icon: <IconLeaf /> },
];

export function LandingPage() {
  const inApp = isSignedIn();
  const { locale, t } = useLocale();
  const leftCents = PREVIEW.inCents - PREVIEW.outCents;
  const roomRatio = Math.min(Math.max(leftCents / PREVIEW.inCents, 0), 1);

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
    <div className="site-root relative min-h-dvh overflow-hidden bg-paper font-ui text-navy-deep">
      <div className="pointer-events-none absolute -left-24 -top-28 h-[28rem] w-[28rem] rounded-full bg-peach/80 blur-3xl" />
      <div className="pointer-events-none absolute -right-16 top-24 h-[26rem] w-[26rem] rounded-full bg-sky blur-3xl" />
      <div className="pointer-events-none absolute bottom-40 left-1/3 h-64 w-64 rounded-full bg-sun/25 blur-3xl" />

      <header className="relative mx-auto flex max-w-6xl items-center justify-between gap-4 px-5 py-5">
        <Wordmark accent="teal" size="md" withMark />
        <nav className="flex shrink-0 items-center gap-2 sm:gap-3">
          <LangToggle />
          {inApp ? (
            <Link
              to="/app"
              className="rounded-full bg-teal px-4 py-2 text-sm font-semibold text-white hover:bg-teal-deep"
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
                className="rounded-full bg-teal px-4 py-2 text-sm font-semibold text-white shadow-[0_8px_20px_-10px_rgba(18,168,136,0.9)] hover:bg-teal-deep"
              >
                {t.nav.signUp}
              </Link>
            </>
          )}
        </nav>
      </header>

      <main className="relative mx-auto max-w-6xl px-5 pb-20 pt-6 sm:pt-12">
        <div className="grid items-center gap-12 lg:grid-cols-[1.05fr_0.95fr] lg:gap-16">
          <section>
            <p className="inline-flex items-center gap-2 rounded-full bg-white/70 px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.22em] text-teal-deep shadow-sm">
              <span className="text-sun">
                <IconSun />
              </span>
              {t.hero.eyebrow}
            </p>
            <h1 className="mt-5 max-w-xl">
              <span className="block font-display text-5xl font-semibold leading-[0.95] tracking-tight text-navy-deep sm:text-7xl">
                Well <span className="text-teal">Paid</span>
              </span>
              <span className="mt-5 block font-display text-[1.7rem] font-medium leading-tight text-navy-deep/90 sm:text-4xl">
                {t.hero.title}
              </span>
            </h1>
            <p className="mt-5 max-w-lg text-base leading-relaxed text-navy/75 sm:text-lg">
              {t.hero.body}
            </p>
            <div className="mt-8 flex flex-wrap gap-3">
              <Link
                to={inApp ? "/app" : "/registar"}
                className="rounded-full bg-teal px-6 py-3 text-sm font-semibold text-white shadow-[0_12px_28px_-12px_rgba(18,168,136,0.95)] hover:bg-teal-deep"
              >
                {inApp ? t.nav.openApp : t.hero.cta}
              </Link>
              {!inApp && (
                <Link
                  to="/login"
                  className="rounded-full border border-navy/10 bg-white/80 px-6 py-3 text-sm font-medium text-navy hover:border-sun/70 hover:bg-peach/40"
                >
                  {t.hero.secondary}
                </Link>
              )}
            </div>
          </section>

          <aside className="rounded-[32px] border border-white/80 bg-white/85 p-6 shadow-[0_28px_70px_-40px_rgba(20,28,42,0.4)] backdrop-blur">
            <div className="flex items-start justify-between gap-3">
              <div>
                <p className="text-[11px] font-semibold uppercase tracking-[0.22em] text-muted">
                  {t.preview.kicker}
                </p>
                <p className="mt-1 font-display text-2xl text-navy-deep">
                  {formatLandingMonth(PREVIEW.year, PREVIEW.month, locale)}
                </p>
              </div>
              <span className="inline-flex items-center gap-1 rounded-full bg-sage px-3 py-1 text-xs font-semibold text-teal-deep">
                <IconCheck />
                {t.preview.status}
              </span>
            </div>

            <div className="mt-6 rounded-[24px] bg-gradient-to-br from-sky to-peach/70 px-5 py-5">
              <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-teal-deep">
                {t.preview.left}
              </p>
              <p className="mt-1 font-display text-4xl font-semibold tabular-nums tracking-tight text-navy-deep sm:text-5xl">
                {formatLandingMoney(leftCents, locale)}
              </p>
              <div className="mt-4 h-2 overflow-hidden rounded-full bg-white/70">
                <div
                  className="h-full rounded-full bg-teal"
                  style={{ width: `${Math.round(roomRatio * 100)}%` }}
                />
              </div>
            </div>

            <dl className="mt-5 grid grid-cols-3 gap-3">
              {[
                [t.preview.in, PREVIEW.inCents, "text-teal-deep"],
                [t.preview.out, PREVIEW.outCents, "text-navy-deep"],
                [t.preview.due, PREVIEW.dueCents, "text-gold-pressed"],
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
            <p className="mt-5 text-sm leading-relaxed text-navy/70">
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
            {t.pillars.items.map((item, index) => {
              const style = PILLAR_STYLE[index] ?? PILLAR_STYLE[0];
              return (
                <li
                  key={item.title}
                  className="rounded-[28px] border border-white/80 bg-white/80 p-6 shadow-[0_16px_40px_-32px_rgba(20,28,42,0.35)]"
                >
                  <span
                    className={`inline-flex h-11 w-11 items-center justify-center rounded-2xl ${style.tone}`}
                  >
                    {style.icon}
                  </span>
                  <h3 className="mt-4 font-display text-2xl text-navy-deep">
                    {item.title}
                  </h3>
                  <p className="mt-3 text-sm leading-relaxed text-navy/70">
                    {item.body}
                  </p>
                </li>
              );
            })}
          </ul>
        </section>

        <section className="mt-16 rounded-[32px] border border-white/80 bg-white/80 p-6 shadow-[0_24px_60px_-36px_rgba(20,28,42,0.3)] sm:p-10 lg:grid lg:grid-cols-[1.1fr_0.9fr] lg:items-center lg:gap-12">
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
                  <span className="mt-0.5 inline-flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-sage text-teal-deep">
                    <IconCheck className="h-3.5 w-3.5" />
                  </span>
                  {item}
                </li>
              ))}
            </ul>
          </div>
          <div className="relative mt-8 overflow-hidden rounded-[28px] bg-gradient-to-br from-teal to-teal-deep px-6 py-8 text-cream sm:px-8 lg:mt-0">
            <div className="pointer-events-none absolute -right-8 -top-10 h-32 w-32 rounded-full bg-sun/40 blur-2xl" />
            <p className="relative text-[11px] font-semibold uppercase tracking-[0.22em] text-sun">
              {t.plan.kicker}
            </p>
            <p className="relative mt-3 font-display text-2xl font-semibold">Well Paid</p>
            <p className="relative mt-4 flex items-end gap-1">
              <span className="font-display text-5xl font-semibold tabular-nums tracking-tight">
                {formatLandingMoney(PLAN_PRICE_CENTS, locale)}
              </span>
              <span className="mb-1.5 text-sm text-cream/80">{t.plan.period}</span>
            </p>
            <p className="relative mt-2 text-sm text-cream/75">{t.plan.billed}</p>
            <Link
              to={inApp ? "/app" : "/registar"}
              className="relative mt-8 inline-flex rounded-full bg-sun px-6 py-3 text-sm font-semibold text-navy-deep hover:bg-gold"
            >
              {inApp ? t.nav.openApp : t.plan.cta}
            </Link>
          </div>
        </section>

        <section className="mt-16 rounded-[32px] bg-gradient-to-br from-peach via-paper to-sky px-6 py-10 sm:px-10">
          <p className="text-[11px] font-semibold uppercase tracking-[0.28em] text-teal-deep">
            {t.trust.kicker}
          </p>
          <h2 className="mt-3 max-w-lg font-display text-3xl font-semibold tracking-tight text-navy-deep sm:text-4xl">
            {t.trust.title}
          </h2>
          <p className="mt-4 max-w-xl text-sm leading-relaxed text-navy/75 sm:text-base">
            {t.trust.body}
          </p>
          <ul className="mt-8 flex flex-wrap gap-2">
            {t.trust.points.map((point) => (
              <li
                key={point}
                className="rounded-full bg-white/80 px-4 py-2 text-sm font-medium text-navy-deep shadow-sm"
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
