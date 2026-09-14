import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ApiError, forgotPassword, resetPassword } from "../api";
import { useLocale } from "../i18n/LocaleProvider";
import { AuthCard, ErrorText, Field, PrimaryButton } from "../ui";

export function RecoverPage() {
  const navigate = useNavigate();
  const { auth: a } = useLocale();
  const [step, setStep] = useState<"ask" | "reset">("ask");
  const [email, setEmail] = useState("");
  const [token, setToken] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function onAsk(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      const message = await forgotPassword(email.trim());
      setInfo(message);
      setStep("reset");
    } catch {
      setError(a.recover.sendFail);
    } finally {
      setBusy(false);
    }
  }

  async function onReset(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      await resetPassword(token.trim(), password);
      navigate("/login", { replace: true });
    } catch (err) {
      setError(err instanceof ApiError ? err.message : a.recover.resetFail);
    } finally {
      setBusy(false);
    }
  }

  return (
    <AuthCard
      title={step === "ask" ? a.recover.titleAsk : a.recover.titleReset}
      subtitle={step === "ask" ? a.recover.subtitleAsk : a.recover.subtitleReset}
      footer={
        <Link to="/login" className="font-semibold text-teal-deep hover:underline">
          {a.recover.back}
        </Link>
      }
    >
      {step === "ask" ? (
        <form className="space-y-4" onSubmit={onAsk}>
          <ErrorText message={error} />
          <Field
            label={a.recover.email}
            type="email"
            value={email}
            autoComplete="email"
            required
            onChange={setEmail}
          />
          <PrimaryButton disabled={busy}>
            {busy ? a.recover.sending : a.recover.send}
          </PrimaryButton>
        </form>
      ) : (
        <form className="space-y-4" onSubmit={onReset}>
          <ErrorText message={error} />
          {info ? <p className="text-sm text-navy/70">{info}</p> : null}
          <Field label={a.recover.code} value={token} required onChange={setToken} />
          <Field
            label={a.recover.newPassword}
            type="password"
            value={password}
            autoComplete="new-password"
            required
            onChange={setPassword}
          />
          <PrimaryButton disabled={busy}>
            {busy ? a.recover.saving : a.recover.save}
          </PrimaryButton>
        </form>
      )}
    </AuthCard>
  );
}
