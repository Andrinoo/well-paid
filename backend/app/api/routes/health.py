from fastapi import APIRouter, HTTPException, status
from sqlalchemy import text

from app.core.config import get_settings
from app.core.database import engine

router = APIRouter(tags=["health"])

_DEV_APP_ENV = frozenset({"development", "dev", "local"})


@router.get("/health", status_code=status.HTTP_200_OK)
def health() -> dict[str, str]:
    return {"status": "ok"}


@router.get("/health/db", status_code=status.HTTP_200_OK)
def health_db() -> dict[str, str]:
    with engine.connect() as conn:
        conn.execute(text("SELECT 1"))
        row = conn.execute(text("SHOW ssl")).fetchone()
    db_ssl = str(row[0]) if row is not None else "unknown"
    return {"status": "ok", "db_ssl": db_ssl}


@router.get("/health/smtp-effective")
def health_smtp_effective() -> dict[str, str | int | None]:
    """Mostra o SMTP que o processo Uvicorn carregou (sem password). Só em APP_ENV=development."""
    s = get_settings()
    if s.app_env.strip().lower() not in _DEV_APP_ENV:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="not found")
    return {
        "app_env": s.app_env,
        "smtp_host": s.smtp_host,
        "smtp_port": s.smtp_port,
        "smtp_user": s.smtp_user,
        "mail_from": s.mail_from,
        "hint": "Se não bate com o .env, reinicia o Uvicorn (get_settings fica em cache por processo).",
    }
