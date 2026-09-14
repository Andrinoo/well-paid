import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ApiError, forgotPassword, resetPassword } from "../api";
import { AuthCard, ErrorText, Field, PrimaryButton } from "../ui";

export function RecoverPage() {
  const navigate = useNavigate();
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
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Não foi possível enviar.");
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
      setError(
        err instanceof ApiError ? err.message : "Não foi possível redefinir a senha.",
      );
    } finally {
      setBusy(false);
    }
  }

  return (
    <AuthCard
      title={step === "ask" ? "Recuperar senha" : "Nova senha"}
      subtitle={
        step === "ask"
          ? "Enviamos um código para o e-mail da conta."
          : "Cole o código do e-mail e escolha a nova senha."
      }
      footer={
        <Link to="/login" className="text-gold hover:underline">
          Voltar ao login
        </Link>
      }
    >
      {step === "ask" ? (
        <form className="space-y-4" onSubmit={onAsk}>
          <ErrorText message={error} />
          <Field
            label="E-mail"
            type="email"
            value={email}
            autoComplete="email"
            required
            onChange={setEmail}
          />
          <PrimaryButton disabled={busy}>
            {busy ? "A enviar…" : "Enviar código"}
          </PrimaryButton>
        </form>
      ) : (
        <form className="space-y-4" onSubmit={onReset}>
          <ErrorText message={error} />
          {info ? <p className="text-sm text-cream/70">{info}</p> : null}
          <Field label="Código" value={token} required onChange={setToken} />
          <Field
            label="Nova senha"
            type="password"
            value={password}
            autoComplete="new-password"
            required
            onChange={setPassword}
          />
          <PrimaryButton disabled={busy}>
            {busy ? "A guardar…" : "Guardar senha"}
          </PrimaryButton>
        </form>
      )}
    </AuthCard>
  );
}
