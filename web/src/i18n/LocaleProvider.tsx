import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import {
  detectLocale,
  landingCopy,
  persistLocale,
  type LocaleCode,
} from "./landing";
import { authCopy } from "./auth";

type LocaleContextValue = {
  locale: LocaleCode;
  setLocale: (locale: LocaleCode) => void;
  t: (typeof landingCopy)[LocaleCode];
  auth: (typeof authCopy)[LocaleCode];
};

const LocaleContext = createContext<LocaleContextValue | null>(null);

export function LocaleProvider({ children }: { children: ReactNode }) {
  const [locale, setLocaleState] = useState<LocaleCode>(detectLocale);

  const setLocale = useCallback((next: LocaleCode) => {
    setLocaleState(next);
    persistLocale(next);
  }, []);

  const t = landingCopy[locale];
  const auth = authCopy[locale];

  useEffect(() => {
    document.documentElement.lang = locale;
  }, [locale]);

  const value = useMemo(
    () => ({ locale, setLocale, t, auth }),
    [locale, setLocale, t, auth],
  );

  return (
    <LocaleContext.Provider value={value}>{children}</LocaleContext.Provider>
  );
}

export function useLocale(): LocaleContextValue {
  const ctx = useContext(LocaleContext);
  if (!ctx) {
    throw new Error("useLocale must be used within LocaleProvider");
  }
  return ctx;
}
