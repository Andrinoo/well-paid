import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ApiError, registerAccount } from "../api";
import { useLocale } from "../i18n/LocaleProvider";
import { AuthCard, ErrorText, Field, PrimaryButton } from "../ui";

export function RegisterPage() {
  const navigate = useNavigate();
  const { auth: a } = useLocale();
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      await registerAccount(email.trim(), password, fullName);
      navigate(`/confirmar-email?email=${encodeURIComponent(email.trim())}`, {
        replace: true,
      });
    } catch (err) {
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
        <PrimaryButton disabled={busy}>
          {busy ? a.register.busy : a.register.submit}
        </PrimaryButton>
      </form>
    </AuthCard>
  );
}
