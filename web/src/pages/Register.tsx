import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ApiError, registerAccount } from "../api";
import { AuthCard, ErrorText, Field, PrimaryButton } from "../ui";

export function RegisterPage() {
  const navigate = useNavigate();
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
      setError(
        err instanceof ApiError ? err.message : "Não foi possível criar a conta.",
      );
    } finally {
      setBusy(false);
    }
  }

  return (
    <AuthCard
      title="Criar conta"
      subtitle="Enviamos um código e um link para confirmar o e-mail."
      footer={
        <>
          Já tem conta?{" "}
          <Link to="/login" className="text-gold hover:underline">
            Entrar
          </Link>
        </>
      }
    >
      <form className="space-y-4" onSubmit={onSubmit}>
        <ErrorText message={error} />
        <Field
          label="Nome"
          value={fullName}
          autoComplete="name"
          onChange={setFullName}
        />
        <Field
          label="E-mail"
          type="email"
          value={email}
          autoComplete="email"
          required
          onChange={setEmail}
        />
        <Field
          label="Senha (mín. 8 caracteres)"
          type="password"
          value={password}
          autoComplete="new-password"
          required
          onChange={setPassword}
        />
        <PrimaryButton disabled={busy}>
          {busy ? "A criar…" : "Criar conta"}
        </PrimaryButton>
      </form>
    </AuthCard>
  );
}
