export type LocaleCode = "pt-BR" | "en-US";

export const PLAN_PRICE_CENTS = 1499;

export const landingCopy = {
  "pt-BR": {
    metaTitle: "Well Paid — saúde financeira com clareza",
    metaDescription:
      "Well Paid é a plataforma para acompanhar a saúde das suas finanças: o que entra, o que sai e o que ainda falta pagar.",
    nav: {
      signIn: "Entrar",
      signUp: "Criar conta",
      openApp: "Abrir a plataforma",
    },
    lang: {
      pt: "PT",
      en: "EN",
      switchTo: "Mudar idioma",
    },
    hero: {
      brand: "Well Paid",
      eyebrow: "Saúde financeira",
      title: "A saúde das suas finanças, à vista.",
      body: "Uma plataforma para ver com clareza o que entra, o que sai e o que ainda falta pagar — e decidir com calma, não no susto.",
      cta: "Começar agora",
      secondary: "Já tenho conta",
    },
    preview: {
      kicker: "Este mês",
      status: "Estável",
      in: "Entrou",
      out: "Saiu",
      due: "A pagar",
      caption: "Uma leitura honesta do período — o essencial, sem ruído.",
    },
    pillars: {
      kicker: "O essencial",
      title: "Três hábitos que sustentam a saúde financeira.",
      items: [
        {
          title: "O mês",
          body: "Entradas, saídas e o que realmente sobra. Uma leitura do período, sem folha de cálculo.",
        },
        {
          title: "A pagar",
          body: "Contas com vencimento à vista, para nada escapar no fim do mês.",
        },
        {
          title: "Proventos",
          body: "O que entra, organizado. Saúde financeira também é saber de onde vem o dinheiro.",
        },
      ],
    },
    trust: {
      kicker: "Feito para o dia a dia",
      title: "Controlo sem ruído.",
      body: "A mesma conta no telemóvel e no browser. Dados seus, no ritmo de quem cuida — não de quem persegue o mercado.",
      points: ["Contas à vista", "Proventos claros", "Português e English"],
    },
    plan: {
      kicker: "Assinatura",
      name: "Well Paid",
      title: "Um plano, a plataforma completa.",
      period: "/mês",
      billed: "Cobrado mensalmente. Cancele quando quiser.",
      cta: "Assinar Well Paid",
      includes: "Inclui",
      items: [
        "O mês — entradas, saídas e o que sobra",
        "A pagar — contas com vencimento à vista",
        "Proventos — o que entra, organizado",
        "A mesma conta no telemóvel e no browser",
      ],
    },
    footer: {
      tag: "Gestão financeira pensada para a sua saúde.",
      copyright: "© 2026 WellPaid Dev Andrino Cabral. Todos os direitos reservados.",
    },
  },
  "en-US": {
    metaTitle: "Well Paid — financial health, in plain sight",
    metaDescription:
      "Well Paid is the platform to track the health of your finances: what came in, what went out, and what is still due.",
    nav: {
      signIn: "Sign in",
      signUp: "Create account",
      openApp: "Open the platform",
    },
    lang: {
      pt: "PT",
      en: "EN",
      switchTo: "Change language",
    },
    hero: {
      brand: "Well Paid",
      eyebrow: "Financial health",
      title: "See the health of your money.",
      body: "A platform that makes it clear what came in, what went out, and what is still due — so you decide calmly, not in a rush.",
      cta: "Get started",
      secondary: "I already have an account",
    },
    preview: {
      kicker: "This month",
      status: "Steady",
      in: "In",
      out: "Out",
      due: "Due",
      caption: "An honest reading of the period — the essentials, without noise.",
    },
    pillars: {
      kicker: "The essentials",
      title: "Three habits that keep financial health on track.",
      items: [
        {
          title: "The month",
          body: "Inflows, outflows, and what is actually left. A reading of the period, without a spreadsheet.",
        },
        {
          title: "Due",
          body: "Bills with dates you can see, so nothing slips through at month-end.",
        },
        {
          title: "Income",
          body: "What comes in, organized. Financial health also means knowing where the money comes from.",
        },
      ],
    },
    trust: {
      kicker: "Built for everyday life",
      title: "Control, without the noise.",
      body: "The same account on your phone and in the browser. Your data, at the pace of someone who cares — not someone chasing the market.",
      points: ["Bills in view", "Clear income", "English and Portuguese"],
    },
    plan: {
      kicker: "Subscription",
      name: "Well Paid",
      title: "One plan. The full platform.",
      period: "/month",
      billed: "Billed monthly. Cancel anytime.",
      cta: "Subscribe to Well Paid",
      includes: "Includes",
      items: [
        "The month — inflows, outflows, and what is left",
        "Due — bills with dates you can see",
        "Income — what comes in, organized",
        "The same account on phone and browser",
      ],
    },
    footer: {
      tag: "Personal finance, built around your health.",
      copyright: "© 2026 WellPaid Dev Andrino Cabral. All rights reserved.",
    },
  },
} as const;

export function detectLocale(): LocaleCode {
  try {
    const stored = localStorage.getItem("wp_locale");
    if (stored === "pt-BR" || stored === "en-US") return stored;
  } catch {
    /* private mode */
  }
  if (typeof navigator !== "undefined" && navigator.language.toLowerCase().startsWith("en")) {
    return "en-US";
  }
  return "pt-BR";
}

export function persistLocale(locale: LocaleCode): void {
  try {
    localStorage.setItem("wp_locale", locale);
  } catch {
    /* private mode */
  }
}

export function formatLandingMoney(cents: number, locale: LocaleCode): string {
  const currency = locale === "en-US" ? "USD" : "BRL";
  return (cents / 100).toLocaleString(locale, {
    style: "currency",
    currency,
  });
}

export function formatLandingMonth(year: number, month: number, locale: LocaleCode): string {
  const raw = new Date(year, month - 1, 1).toLocaleDateString(locale, {
    month: "long",
    year: "numeric",
  });
  if (!raw) return raw;
  return raw.charAt(0).toLocaleUpperCase(locale) + raw.slice(1);
}
