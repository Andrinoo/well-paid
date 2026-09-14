export type LocaleCode = "pt-BR" | "en-US";

export const PLAN_PRICE_CENTS = 1499;

export const landingCopy = {
  "pt-BR": {
    metaTitle: "Well Paid — a sua vida financeira, bem gerida",
    metaDescription:
      "Well Paid é a plataforma para gerir o mês com leveza: o que entra, o que sai, o que ainda falta pagar — e a folga que sobra.",
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
      eyebrow: "Gestão da vida financeira",
      title: "Saber que o mês está nas suas mãos.",
      body: "Entradas, contas e a folga que sobra — à vista. Assim a gestão deixa de ser peso e passa a ser clareza, e um bocadinho de alívio.",
      cta: "Quero essa leveza",
      secondary: "Já tenho conta",
    },
    preview: {
      kicker: "O seu mês",
      status: "Tudo em ordem",
      in: "Entrou",
      out: "Saiu",
      due: "A pagar",
      left: "Sobra",
      caption: "Folga visível, contas no sítio. É isto uma gestão legal da vida.",
    },
    pillars: {
      kicker: "O que você passa a ver",
      title: "Três gestos para viver mais leve com o dinheiro.",
      items: [
        {
          title: "O mês",
          body: "O que entrou, o que saiu e o que sobrou. Uma leitura do período que deixa a cabeça mais leve — sem folha de cálculo.",
        },
        {
          title: "A pagar",
          body: "Contas com data à vista, para chegar ao fim do mês sem susto e com a vida em ordem.",
        },
        {
          title: "Proventos",
          body: "O que entra, organizado. Saber de onde vem o dinheiro também é cuidar de si.",
        },
      ],
    },
    trust: {
      kicker: "Feito para respirar",
      title: "Gestão da vida, não só das contas.",
      body: "A mesma conta no celular e no browser. Os seus números, no ritmo de quem cuida da vida — não de quem vive ansioso com o extrato.",
      points: ["Folga à vista", "Contas em ordem", "Português e English"],
    },
    plan: {
      kicker: "Assinatura",
      name: "Well Paid",
      title: "Um plano. A gestão completa do seu mês.",
      period: "/mês",
      billed: "Cobrado mensalmente. Cancele quando quiser.",
      cta: "Começar a gerir melhor",
      includes: "Inclui",
      items: [
        "O mês — entradas, saídas e o que sobra",
        "A pagar — contas com vencimento à vista",
        "Proventos — o que entra, organizado",
        "A mesma conta no celular e no browser",
      ],
    },
    footer: {
      tag: "A sua vida financeira, bem acompanhada.",
      copyright: "© 2026 WellPaid Dev Andrino Cabral. Todos os direitos reservados.",
    },
  },
  "en-US": {
    metaTitle: "Well Paid — your money, happily in order",
    metaDescription:
      "Well Paid helps you run the month with ease: what came in, what went out, what is still due — and the room you have left.",
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
      eyebrow: "Everyday money, well looked after",
      title: "Feel like the month is in your hands.",
      body: "Inflows, bills, and the room you have left — in plain sight. Money management stops being a weight and becomes clarity, plus a little relief.",
      cta: "I want that ease",
      secondary: "I already have an account",
    },
    preview: {
      kicker: "Your month",
      status: "All in order",
      in: "In",
      out: "Out",
      due: "Due",
      left: "Left",
      caption: "Room to breathe, bills in place. That’s a well-run financial life.",
    },
    pillars: {
      kicker: "What you get to see",
      title: "Three habits that make money feel lighter.",
      items: [
        {
          title: "The month",
          body: "What came in, what went out, and what’s left. A reading of the period that eases your mind — no spreadsheet.",
        },
        {
          title: "Due",
          body: "Bills with dates you can see, so month-end arrives without a jolt, and life stays in order.",
        },
        {
          title: "Income",
          body: "What comes in, organized. Knowing where the money comes from is part of looking after yourself.",
        },
      ],
    },
    trust: {
      kicker: "Built to breathe",
      title: "Life management, not just accounts.",
      body: "The same account on your phone and in the browser. Your numbers, at the pace of someone who looks after their life — not someone anxious about the statement.",
      points: ["Room to breathe", "Bills in order", "English and Portuguese"],
    },
    plan: {
      kicker: "Subscription",
      name: "Well Paid",
      title: "One plan. Your whole month, looked after.",
      period: "/month",
      billed: "Billed monthly. Cancel anytime.",
      cta: "Start managing better",
      includes: "Includes",
      items: [
        "The month — inflows, outflows, and what’s left",
        "Due — bills with dates you can see",
        "Income — what comes in, organized",
        "The same account on phone and browser",
      ],
    },
    footer: {
      tag: "Your financial life, well looked after.",
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
