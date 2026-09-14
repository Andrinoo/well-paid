import { useEffect, useState, type FormEvent } from "react";
import { fetchMe, patchDisplayName } from "../api";
import { ApiError, ErrorNote, InField, MoneyForm, PageTitle } from "./common";

export function SettingsPage() {
  const [email, setEmail] = useState("");
  const [publicId, setPublicId] = useState("");
  const [display, setDisplay] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    void fetchMe()
      .then((me) => {
        setEmail(me.email);
        setPublicId(me.public_id || "");
        setDisplay(me.display_name || me.full_name || "");
      })
      .catch((err) =>
        setError(err instanceof ApiError ? err.message : "Falha ao carregar."),
      );
  }, []);

  async function onSave(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const me = await patchDisplayName(display.trim());
      setDisplay(me.display_name || me.full_name || "");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Não foi possível guardar.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="mx-auto max-w-xl space-y-5">
      <PageTitle kicker="Conta" title="Definições" />
      <ErrorNote message={error} />
      <p className="text-sm text-muted">{email}</p>
      {publicId ? (
        <p className="font-mono text-sm tracking-wide text-navy-deep">ID {publicId}</p>
      ) : null}
      <MoneyForm onSubmit={onSave} submitLabel={busy ? "…" : "Guardar nome"} busy={busy} extra={
        <InField label="Nome visível" value={display} onChange={setDisplay} />
      } />
    </div>
  );
}
