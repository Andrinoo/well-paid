import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ApiError, login } from "../api";
import { useLocale } from "../i18n/LocaleProvider";
import { AuthCard, ErrorText, Field, PrimaryButton } from "../ui";

export function LoginPage() {
  const navigate = useNavigate();
  const { auth: a } = useLocale();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      await login(email.trim(), password);
      navigate("/app", { replace: true });
    } catch (err) {
      if (err instanceof ApiError && err.status === 429) {
        setError(a.login.locked);
      } else {
        setError(a.login.fallback);
      }
    } finally {
      setBusy(false);
    }
  }

  return (
    <AuthCard
      title={a.login.title}
      subtitle={a.login.subtitle}
      footer={
        <>
          {a.login.noAccount}{" "}
          <Link to="/registar" className="font-semibold text-teal-deep hover:underline">
            {a.login.create}
          </Link>
        </>
      }
    >
      <form className="space-y-4" onSubmit={onSubmit}>
        <ErrorText message={error} />
        <Field
          label={a.login.email}
          type="text"
          value={email}
          autoComplete="username"
          required
          onChange={setEmail}
        />
        <Field
          label={a.login.password}
          type="password"
          value={password}
          autoComplete="current-password"
          required
          onChange={setPassword}
        />
        <div className="text-right">
          <Link to="/recuperar" className="text-sm font-medium text-teal-deep hover:underline">
            {a.login.forgot}
          </Link>
        </div>
        <PrimaryButton disabled={busy}>
          {busy ? a.login.busy : a.login.submit}
        </PrimaryButton>
      </form>
    </AuthCard>
  );
}
