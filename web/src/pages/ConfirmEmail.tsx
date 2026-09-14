import { useEffect, useState, type FormEvent } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { ApiError, resendVerification, verifyEmail } from "../api";
import { useLocale } from "../i18n/LocaleProvider";
import { AuthCard, ErrorText, Field, PrimaryButton } from "../ui";

export function ConfirmEmailPage() {
  const navigate = useNavigate();
  const { auth: a } = useLocale();
  const [params] = useSearchParams();
  const tokenFromLink = params.get("token")?.trim() ?? "";
  const [email, setEmail] = useState(params.get("email")?.trim() ?? "");
  const [code, setCode] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(
    tokenFromLink ? a.confirm.checking : null,
  );
  const [busy, setBusy] = useState(Boolean(tokenFromLink));

  useEffect(() => {
    if (!tokenFromLink) return;
    let cancelled = false;
    void (async () => {
      try {
        await verifyEmail({ token: tokenFromLink });
        if (!cancelled) navigate("/app", { replace: true });
      } catch (err) {
        if (!cancelled) {
          setBusy(false);
          setInfo(null);
          setError(
            err instanceof ApiError ? err.message : a.confirm.invalidLink,
          );
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [tokenFromLink, navigate, a.confirm.invalidLink]);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      await verifyEmail({ email: email.trim(), code: code.trim() });
      navigate("/app", { replace: true });
    } catch (err) {
      setError(err instanceof ApiError ? err.message : a.confirm.fallback);
    } finally {
      setBusy(false);
    }
  }

  async function onResend() {
    if (!email.trim()) {
      setError(a.confirm.needEmail);
      return;
    }
    setError(null);
    setBusy(true);
    try {
      const message = await resendVerification(email.trim());
      setInfo(message);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : a.confirm.resendFail);
    } finally {
      setBusy(false);
    }
  }

  return (
    <AuthCard
      title={a.confirm.title}
      subtitle={a.confirm.subtitle}
      footer={
        <Link to="/login" className="font-semibold text-teal-deep hover:underline">
          {a.confirm.back}
        </Link>
      }
    >
      <form className="space-y-4" onSubmit={onSubmit}>
        <ErrorText message={error} />
        {info ? <p className="text-sm text-navy/70">{info}</p> : null}
        <Field
          label={a.confirm.email}
          type="email"
          value={email}
          autoComplete="email"
          required
          onChange={setEmail}
        />
        <Field
          label={a.confirm.code}
          value={code}
          autoComplete="one-time-code"
          required
          onChange={setCode}
        />
        <PrimaryButton disabled={busy}>
          {busy ? a.confirm.busy : a.confirm.submit}
        </PrimaryButton>
        <button
          type="button"
          className="w-full text-sm font-medium text-teal-deep hover:underline"
          onClick={() => void onResend()}
          disabled={busy}
        >
          {a.confirm.resend}
        </button>
      </form>
    </AuthCard>
  );
}
