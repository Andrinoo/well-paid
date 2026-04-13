import logging
import uuid

from fastapi import FastAPI, HTTPException, Request, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from slowapi.errors import RateLimitExceeded
from slowapi.middleware import SlowAPIMiddleware
from starlette.middleware.base import BaseHTTPMiddleware

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

_DEV_ENVS = frozenset({"development", "dev", "local"})


def _is_dev_env() -> bool:
    return settings.app_env.strip().lower() in _DEV_ENVS


_expose_docs = settings.expose_openapi
app = FastAPI(
    title="Well Paid API",
    version="0.1.0",
    docs_url="/docs" if _expose_docs else None,
    redoc_url="/redoc" if _expose_docs else None,
    openapi_url="/openapi.json" if _expose_docs else None,
)
app.state.limiter = limiter


class _SecurityAndRequestIdMiddleware(BaseHTTPMiddleware):
    """Cabeçalhos de segurança mínimos, X-Request-ID e linha de log por pedido (sem corpo nem Authorization)."""

    async def dispatch(self, request: Request, call_next):  # type: ignore[override]
        rid = str(uuid.uuid4())
        request.state.request_id = rid
        response = await call_next(request)
        response.headers["X-Request-ID"] = rid
        response.headers["X-Content-Type-Options"] = "nosniff"
        response.headers["X-Frame-Options"] = "DENY"
        response.headers["Referrer-Policy"] = "strict-origin-when-cross-origin"
        response.headers["Permissions-Policy"] = "camera=(), microphone=(), geolocation=()"
        path = request.url.path
        if path == "/health" and request.method == "GET":
            logger.debug(
                "request_id=%s method=%s path=%s status=%s",
                rid,
                request.method,
                path,
                response.status_code,
            )
        else:
            log_fn = (
                logger.error
                if response.status_code >= 500
                else logger.warning
                if response.status_code >= 400
                else logger.info
            )
            log_fn(
                "request_id=%s method=%s path=%s status=%s",
                rid,
                request.method,
                path,
                response.status_code,
            )
        return response


@app.get("/")
def root() -> dict[str, str]:
    """Raiz: evita 404 ao abrir a URL do deploy no browser; a API vive em /health, /auth, …"""
    out: dict[str, str] = {
        "service": "Well Paid API",
        "health": "/health",
    }
    if _expose_docs:
        out["docs"] = "/docs"
    return out


@app.post("/")
def root_post_rejected() -> None:
    """Evita 405 genérico: clientes que fazem POST na origem (URL mal configurada) recebem pista clara."""
    raise HTTPException(
        status_code=status.HTTP_400_BAD_REQUEST,
        detail=(
            "Não use POST na raiz. Para login: POST /auth/login com JSON "
            '{"email","password"}. No app Flutter, API_BASE_URL deve ser só a origem '
            "(ex.: https://….vercel.app), sem path extra; rebuild com "
            "--dart-define=API_BASE_URL=…"
        ),
    )


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


@app.on_event("startup")
def _warn_insecure_flags_in_production() -> None:
    """A.4: em APP_ENV típico de produção, avisar se flags de log de tokens estiverem ativas."""
    s = get_settings()
    if _is_dev_env():
        return
    problems: list[str] = []
    if s.email_verification_log_token:
        problems.append("EMAIL_VERIFICATION_LOG_TOKEN=true")
    if s.password_reset_log_token:
        problems.append("PASSWORD_RESET_LOG_TOKEN=true")
    if not problems:
        return
    logger.warning(
        "Segurança: em ambiente não-development, desligue %s no painel (Vercel) para não registar tokens em logs. "
        "Ver docs/VERCEL_E_NEON_OPERACOES.md e docs/SEGURANCA_ROADMAP.md (A.4).",
        " e ".join(problems),
    )


def _rate_limit_handler(request: Request, exc: RateLimitExceeded) -> JSONResponse:
    return JSONResponse(
        status_code=429,
        content={"detail": "Muitas requisições; tente novamente em instantes."},
    )


async def _unhandled_exception_handler(request: Request, exc: Exception) -> JSONResponse:
    """B.3: erros não tratados — em produção não vazar detalhes; em dev incluir pista e request_id."""
    rid = getattr(request.state, "request_id", None)
    logger.exception(
        "Erro não tratado request_id=%s method=%s path=%s",
        rid,
        request.method,
        request.url.path,
        exc_info=exc,
    )
    if _is_dev_env():
        return JSONResponse(
            status_code=500,
            content={
                "detail": str(exc),
                "request_id": rid,
            },
        )
    return JSONResponse(
        status_code=500,
        content={
            "detail": "Erro interno do servidor.",
            "request_id": rid,
        },
    )


app.add_exception_handler(Exception, _unhandled_exception_handler)
app.add_exception_handler(RateLimitExceeded, _rate_limit_handler)
app.add_middleware(SlowAPIMiddleware)
app.add_middleware(_SecurityAndRequestIdMiddleware)

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
