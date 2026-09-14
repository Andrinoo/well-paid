import { useEffect, useState, type FormEvent } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { ApiError, resendVerification, verifyEmail } from "../api";
import { AuthCard, ErrorText, Field, PrimaryButton } from "../ui";

export function ConfirmEmailPage() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const tokenFromLink = params.get("token")?.trim() ?? "";
  const [email, setEmail] = useState(params.get("email")?.trim() ?? "");
  const [code, setCode] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(
    tokenFromLink ? "A confirmar o e-mail…" : null,
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
            err instanceof ApiError
              ? err.message
              : "Link inválido. Use o código de 6 dígitos.",
          );
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [tokenFromLink, navigate]);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      await verifyEmail({ email: email.trim(), code: code.trim() });
      navigate("/app", { replace: true });
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Não foi possível confirmar.");
    } finally {
      setBusy(false);
    }
  }

  async function onResend() {
    if (!email.trim()) {
      setError("Indique o e-mail para reenviar o código.");
      return;
    }
    setError(null);
    setBusy(true);
    try {
      const message = await resendVerification(email.trim());
      setInfo(message);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Falha ao reenviar.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <AuthCard
      title="Confirmar e-mail"
      subtitle="Abra o link do e-mail ou introduza o código de 6 dígitos."
      footer={
        <Link to="/login" className="text-gold hover:underline">
          Voltar ao login
        </Link>
      }
    >
      <form className="space-y-4" onSubmit={onSubmit}>
        <ErrorText message={error} />
        {info ? <p className="text-sm text-cream/70">{info}</p> : null}
        <Field
          label="E-mail"
          type="email"
          value={email}
          autoComplete="email"
          required
          onChange={setEmail}
        />
        <Field
          label="Código"
          value={code}
          autoComplete="one-time-code"
          required
          onChange={setCode}
        />
        <PrimaryButton disabled={busy}>
          {busy ? "A confirmar…" : "Confirmar"}
        </PrimaryButton>
        <button
          type="button"
          className="w-full text-sm text-gold/90 hover:underline"
          onClick={() => void onResend()}
          disabled={busy}
        >
          Reenviar e-mail
        </button>
      </form>
    </AuthCard>
  );
}
