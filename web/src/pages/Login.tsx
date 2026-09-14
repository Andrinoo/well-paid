import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ApiError, login } from "../api";
import { AuthCard, ErrorText, Field, PrimaryButton } from "../ui";

export function LoginPage() {
  const navigate = useNavigate();
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
      setError(err instanceof ApiError ? err.message : "Não foi possível entrar.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <AuthCard
      title="Entrar"
      subtitle="A mesma conta do aplicativo Android."
      footer={
        <>
          Ainda não tem conta?{" "}
          <Link to="/registar" className="text-gold hover:underline">
            Criar conta
          </Link>
        </>
      }
    >
      <form className="space-y-4" onSubmit={onSubmit}>
        <ErrorText message={error} />
        <Field
          label="E-mail"
          type="email"
          value={email}
          autoComplete="email"
          required
          onChange={setEmail}
        />
        <Field
          label="Senha"
          type="password"
          value={password}
          autoComplete="current-password"
          required
          onChange={setPassword}
        />
        <div className="text-right">
          <Link to="/recuperar" className="text-sm text-gold/90 hover:underline">
            Esqueci a senha
          </Link>
        </div>
        <PrimaryButton disabled={busy}>{busy ? "A entrar…" : "Entrar"}</PrimaryButton>
      </form>
    </AuthCard>
  );
}
