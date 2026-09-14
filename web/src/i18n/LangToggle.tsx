import { useLocale } from "./LocaleProvider";

export function LangToggle({ light = false }: { light?: boolean }) {
  const { locale, setLocale, t } = useLocale();
  const shell = light
    ? "border-cream/20 bg-white/10"
    : "border-navy/10 bg-white/70";
  return (
    <div
      className={`flex rounded-full border p-0.5 text-[11px] font-semibold tracking-wide ${shell}`}
      role="group"
      aria-label={t.lang.switchTo}
    >
      <button
        type="button"
        aria-pressed={locale === "pt-BR"}
        className="site-lang-btn rounded-full px-2.5 py-1 outline-none transition hover:text-navy-deep"
        onClick={() => setLocale("pt-BR")}
      >
        {t.lang.pt}
      </button>
      <button
        type="button"
        aria-pressed={locale === "en-US"}
        className="site-lang-btn rounded-full px-2.5 py-1 outline-none transition hover:text-navy-deep"
        onClick={() => setLocale("en-US")}
      >
        {t.lang.en}
      </button>
    </div>
  );
}
