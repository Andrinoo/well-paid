import { useEffect, useRef, useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ApiError, fetchCaptchaConfig, registerAccount } from "../api";
import { useLocale } from "../i18n/LocaleProvider";
import { AuthCard, ErrorText, Field, PrimaryButton } from "../ui";

type TurnstileApi = {
  render: (
    el: HTMLElement,
    opts: {
      sitekey: string;
      callback?: (token: string) => void;
      "expired-callback"?: () => void;
      "error-callback"?: () => void;
    },
  ) => string;
  reset: (id: string) => void;
  remove: (id: string) => void;
};

function turnstileApi(): TurnstileApi | undefined {
  return (window as unknown as { turnstile?: TurnstileApi }).turnstile;
}

export function RegisterPage() {
  const navigate = useNavigate();
  const { auth: a } = useLocale();
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [siteKey, setSiteKey] = useState<string | null>(null);
  const [captchaOn, setCaptchaOn] = useState(false);
  const [token, setToken] = useState("");
  const widgetHost = useRef<HTMLDivElement>(null);
  const widgetId = useRef<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    fetchCaptchaConfig()
      .then((cfg) => {
        if (cancelled) return;
        const key = cfg.enabled ? (cfg.site_key || "").trim() : "";
        setCaptchaOn(Boolean(key));
        setSiteKey(key || null);
      })
      .catch(() => {
        if (!cancelled) setCaptchaOn(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (!captchaOn || !siteKey || !widgetHost.current) return;
    let cancelled = false;

    function renderWidget() {
      const api = turnstileApi();
      const el = widgetHost.current;
      if (cancelled || !api || !el || widgetId.current) return;
      widgetId.current = api.render(el, {
        sitekey: siteKey as string,
        callback: (value) => setToken(value),
        "expired-callback": () => setToken(""),
        "error-callback": () => setToken(""),
      });
    }

    if (turnstileApi()) {
      renderWidget();
    } else {
      const existing = document.querySelector<HTMLScriptElement>(
        "script[data-wp-turnstile]",
      );
      if (existing) {
        existing.addEventListener("load", renderWidget);
      } else {
        const script = document.createElement("script");
        script.src =
          "https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit";
        script.async = true;
        script.dataset.wpTurnstile = "1";
        script.addEventListener("load", renderWidget);
        document.head.appendChild(script);
      }
    }

    return () => {
      cancelled = true;
      const api = turnstileApi();
      const id = widgetId.current;
      if (api && id) {
        api.remove(id);
      }
      widgetId.current = null;
    };
  }, [captchaOn, siteKey]);

  function resetCaptcha() {
    setToken("");
    const api = turnstileApi();
    const id = widgetId.current;
    if (api && id) api.reset(id);
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (captchaOn && !token) {
      setError(a.register.captchaNeed);
      return;
    }
    setBusy(true);
    try {
      await registerAccount(email.trim(), password, fullName, token || null);
      navigate(`/confirmar-email?email=${encodeURIComponent(email.trim())}`, {
        replace: true,
      });
    } catch (err) {
      resetCaptcha();
      if (err instanceof ApiError && err.status === 429) {
        setError(a.register.throttled);
      } else {
        setError(a.register.fallback);
      }
    } finally {
      setBusy(false);
    }
  }

  return (
    <AuthCard
      title={a.register.title}
      subtitle={a.register.subtitle}
      footer={
        <>
          {a.register.hasAccount}{" "}
          <Link to="/login" className="font-semibold text-teal-deep hover:underline">
            {a.register.signIn}
          </Link>
        </>
      }
    >
      <form className="space-y-4" onSubmit={onSubmit}>
        <ErrorText message={error} />
        <Field
          label={a.register.name}
          value={fullName}
          autoComplete="name"
          onChange={setFullName}
        />
        <Field
          label={a.register.email}
          type="email"
          value={email}
          autoComplete="email"
          required
          onChange={setEmail}
        />
        <Field
          label={a.register.password}
          type="password"
          value={password}
          autoComplete="new-password"
          required
          onChange={setPassword}
        />
        {captchaOn ? (
          <div>
            <span className="mb-1.5 block text-sm font-medium text-navy/70">
              {a.register.captcha}
            </span>
            <div ref={widgetHost} className="min-h-[65px]" />
            {!token ? (
              <p className="mt-1.5 text-xs text-muted">{a.register.captchaWait}</p>
            ) : null}
          </div>
        ) : null}
        <PrimaryButton disabled={busy || (captchaOn && !token)}>
          {busy ? a.register.busy : a.register.submit}
        </PrimaryButton>
      </form>
    </AuthCard>
  );
}
