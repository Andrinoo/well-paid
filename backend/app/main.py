import logging

from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from slowapi.errors import RateLimitExceeded
from slowapi.middleware import SlowAPIMiddleware

from app.api.routes import (
    auth,
    categories,
    dashboard,
    emergency_reserve,
    expenses,
    families,
    goals,
    health,
    income_categories,
    incomes,
    shopping_lists,
)
from app.core.config import get_settings
from app.core.limiter import limiter

logger = logging.getLogger(__name__)
settings = get_settings()

app = FastAPI(title="Well Paid API", version="0.1.0")
app.state.limiter = limiter


@app.get("/")
def root() -> dict[str, str]:
    """Raiz: evita 404 ao abrir a URL do deploy no browser; a API vive em /health, /auth, …"""
    return {
        "service": "Well Paid API",
        "health": "/health",
        "docs": "/docs",
    }


@app.on_event("startup")
def _log_smtp_status() -> None:
    s = get_settings()
    host = (s.smtp_host or "").strip()
    if not host:
        logger.warning(
            "SMTP não configurado (SMTP_HOST vazio): POST /auth/forgot-password não envia e-mail. "
            "Preenche SMTP_* e MAIL_FROM no .env, reinicia o Uvicorn. "
            "Para testar sem mail: APP_ENV=development (dev_reset_token na resposta) ou regista primeiro o utilizador."
        )
        return
    mf = (s.mail_from or s.smtp_user or "").strip()
    if not mf:
        logger.warning(
            "SMTP_HOST definido mas falta MAIL_FROM (ou SMTP_USER): o envio de recuperação de senha será ignorado."
        )
    else:
        logger.info("SMTP ativo: host=%s port=%s mail_from=%s", host, s.smtp_port, mf)


def _rate_limit_handler(request: Request, exc: RateLimitExceeded) -> JSONResponse:
    return JSONResponse(
        status_code=429,
        content={"detail": "Muitas requisições; tente novamente em instantes."},
    )


app.add_exception_handler(RateLimitExceeded, _rate_limit_handler)
app.add_middleware(SlowAPIMiddleware)

_origins = settings.cors_origins_list
app.add_middleware(
    CORSMiddleware,
    allow_origins=_origins,
    allow_credentials=_origins != ["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(health.router)
app.include_router(auth.router)
app.include_router(categories.router)
app.include_router(income_categories.router)
app.include_router(incomes.router)
app.include_router(dashboard.router)
app.include_router(expenses.router)
app.include_router(goals.router)
app.include_router(families.router)
app.include_router(emergency_reserve.router)
app.include_router(shopping_lists.router)
