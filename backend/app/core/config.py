import os
from functools import lru_cache
from pathlib import Path
from urllib.parse import parse_qs, urlencode, urlparse, urlunparse

from dotenv import load_dotenv
from pydantic import AliasChoices, Field, ValidationError, field_validator, model_validator
from pydantic_settings import BaseSettings, SettingsConfigDict

_ENV_ENCODING = "utf-8-sig"


def _repo_paths() -> tuple[Path, Path]:
    """(backend_dir, repo_root) a partir de app/core/config.py."""
    _core = Path(__file__).resolve().parent
    _backend = _core.parent.parent
    _root = _backend.parent
    return _backend, _root


def _load_dotenv_files() -> None:
    """Carrega .env com caminhos absolutos a partir deste ficheiro (não depende do cwd).

    Não carregamos cwd/.env nem cwd.parent/.env: com cwd = Well Paid/backend,
    cwd.parent/.env seria a raiz do repo — mas com cwd = Well Paid, cwd.parent apontaria
    para D:\\Projects (etc.) e um .env errado aí poderia sobrescrever DATABASE_URL.
    """
    _backend, _root = _repo_paths()

    def _load(path: Path, *, override: bool) -> None:
        path = path.resolve()
        if path.is_file():
            load_dotenv(path, override=override, encoding=_ENV_ENCODING)

    _load(_backend / ".env", override=False)
    _load(_root / ".env", override=True)

    custom = os.getenv("WELL_PAID_DOTENV", "").strip() or os.getenv(
        "ONE_PAY_DOTENV", ""
    ).strip()
    if custom:
        _load(Path(custom), override=True)


_load_dotenv_files()


def _env_files() -> tuple[str, ...]:
    """Ficheiros .env para o Pydantic (caminhos absolutos). WELL_PAID_DOTENV sobrepõe o resto."""
    _backend, _root = _repo_paths()
    files: list[str] = [
        str((_backend / ".env").resolve()),
        str((_root / ".env").resolve()),
    ]
    custom = os.getenv("WELL_PAID_DOTENV", "").strip() or os.getenv(
        "ONE_PAY_DOTENV", ""
    ).strip()
    if custom:
        files.append(str(Path(custom).resolve()))
    return tuple(files)


def _normalize_psycopg_driver(url: str) -> str:
    u = url.strip()
    # Cópias parciais (só "//user:pass@host/...") ou sem esquema — o Neon envia postgresql:// completo.
    if "://" not in u:
        if u.startswith("//"):
            u = "postgresql:" + u
        elif "@" in u and ":" in u.split("@", 1)[0]:
            u = "postgresql://" + u
    parts = u.split("://", 1)
    if len(parts) != 2:
        raise ValueError(
            "URL inválida: falta o esquema com '://' (ex.: postgresql://user:pass@host/db). "
            "Cola a connection string completa do Neon, não só o host ou um placeholder."
        )
    scheme, rest = parts
    if scheme == "postgresql":
        return f"postgresql+psycopg://{rest}"
    return u


def _ensure_tls_query(url: str) -> str:
    parsed = urlparse(url)
    host = (parsed.hostname or "").lower()
    if not host or host in ("localhost", "127.0.0.1"):
        return url
    qs = parse_qs(parsed.query, keep_blank_values=True)
    mode = (qs.get("sslmode") or [""])[0].lower()
    if mode not in ("require", "verify-ca", "verify-full"):
        qs["sslmode"] = ["require"]
        new_query = urlencode(qs, doseq=True)
        return urlunparse(parsed._replace(query=new_query))
    return url


def _build_settings_config() -> SettingsConfigDict:
    """Em Vercel não há .env no bundle: usar só `os.environ` evita interacções estranhas com `env_file`."""
    if os.getenv("VERCEL"):
        return SettingsConfigDict(
            env_file=None,
            env_file_encoding=_ENV_ENCODING,
            extra="ignore",
        )
    return SettingsConfigDict(
        env_file=_env_files(),
        env_file_encoding=_ENV_ENCODING,
        extra="ignore",
    )


class Settings(BaseSettings):
    model_config = _build_settings_config()

    database_url: str
    secret_key: str
    algorithm: str = "HS256"
    access_token_expire_minutes: int = 60
    refresh_token_expire_days: int = 30
    cors_origins: str = "*"
    # Ambiente: em development/local o endpoint forgot-password pode devolver o token (apenas para testes).
    app_env: str = "production"
    # E-mail opcional para recuperação de senha (SMTP).
    smtp_host: str | None = None
    smtp_port: int = 587
    smtp_user: str | None = None
    smtp_password: str | None = None
    mail_from: str | None = None
    # Quando true e SMTP não configurado, regista o token de reset nos logs (só para desenvolvimento).
    password_reset_log_token: bool = False
    # SerpAPI (Google Shopping): necessária para POST /goals/product-search e /shopping-lists/price-suggestions.
    serpapi_key: str | None = Field(
        default=None,
        validation_alias=AliasChoices("serpapi_key", "SERPAPI_KEY"),
    )
    # Opcional: sufixo de local na query de mercearia (ex.: "Blumenau SC") para POST /shopping-lists/price-suggestions.
    grocery_search_location_hint: str | None = Field(
        default=None,
        validation_alias=AliasChoices(
            "grocery_search_location_hint",
            "GROCERY_SEARCH_LOCATION_HINT",
        ),
    )
    # Link na mensagem de confirmação de e-mail (deep link ou URL HTTPS). Vazio = não incluir link no e-mail.
    email_verification_link_base: str | None = None
    # Só desenvolvimento: regista código e token de verificação nos logs se o e-mail não for enviado.
    email_verification_log_token: bool = False
    # Investimentos: fonte pública para CDI (BACEN SGS) e fator default para CDB.
    investments_cdi_sgs_series: int = 12
    investments_rates_cache_ttl_seconds: int = 900
    investments_cdb_pct_of_cdi: float = 1.02
    investments_fallback_stable_window_months: int = 2
    # Opcional: cotação de ações B3 (mesmo padrão que docs/stock.py — brapi.dev).
    brapi_api_key: str = Field(
        default="",
        validation_alias=AliasChoices("brapi_api_key", "BRAPI_API_KEY"),
    )

    @model_validator(mode="before")
    @classmethod
    def _smtp_empty_strings_to_none(cls, data: object) -> object:
        """Evita SMTP_HOST= no .env ser lido como '' e parecer 'definido' em logs."""
        if not isinstance(data, dict):
            return data
        for key in (
            "smtp_host",
            "smtp_user",
            "smtp_password",
            "mail_from",
            "email_verification_link_base",
            "serpapi_key",
            "grocery_search_location_hint",
        ):
            v = data.get(key)
            if isinstance(v, str) and not v.strip():
                data[key] = None
        return data

    @field_validator("database_url")
    @classmethod
    def database_url_tls_and_driver(cls, v: str) -> str:
        u = _normalize_psycopg_driver(v.strip())
        return _ensure_tls_query(u)

    @property
    def cors_origins_list(self) -> list[str]:
        if self.cors_origins.strip() == "*":
            return ["*"]
        return [o.strip() for o in self.cors_origins.split(",") if o.strip()]


def _settings_hint() -> str:
    _b, _r = _repo_paths()
    cwd = Path.cwd()
    custom = os.getenv("WELL_PAID_DOTENV", "").strip() or os.getenv(
        "ONE_PAY_DOTENV", ""
    ).strip()
    on_vercel = bool(os.getenv("VERCEL"))
    lines = [
        "DATABASE_URL e SECRET_KEY são obrigatórios (valores reais, não placeholders do .env.example).",
    ]
    if on_vercel:
        v_url = os.getenv("VERCEL_URL", "(não definida)")
        v_pid = os.getenv("VERCEL_PROJECT_ID", "(não definida)")
        v_env = os.getenv("VERCEL_ENV", "(não definida)")
        lines.extend(
            [
                "Ambiente Vercel detetado: não há .env no servidor. Define no painel do projeto:",
                "  Vercel → Settings → Environment Variables → DATABASE_URL e SECRET_KEY (Production).",
                "Redeploy só envia código; não cria estas variáveis. Ver Ordems 1.md §2.5.",
                f"  (diagnóstico) VERCEL_URL={v_url} VERCEL_PROJECT_ID={v_pid} VERCEL_ENV={v_env}",
                "  Confirma no dashboard que estas variáveis existem neste mesmo projeto (ex.: well-paid-psi).",
            ]
        )
    else:
        lines.extend(
            [
                "Local: cria ou edita o .env com essas chaves (sem aspas em volta da linha inteira).",
                "Ficheiros .env (só estes são carregados pelo backend; não usamos cwd):",
                f"  WELL_PAID_DOTENV -> {custom or '(não definida)'}",
                f"  {_b / '.env'} -> {(_b / '.env').resolve().is_file()}",
                f"  {_r / '.env'} -> {(_r / '.env').resolve().is_file()}",
                f"cwd (só informativo): {cwd}",
                "Nota: no Explorador, .env.txt nao conta - o nome tem de ser exatamente .env",
            ]
        )
    return "\n".join(lines)


def _format_validation_error(e: ValidationError) -> str:
    lines = [_settings_hint(), "", "Detalhe da validação:"]
    for err in e.errors():
        loc = ".".join(str(x) for x in err.get("loc", ()))
        lines.append(f"  - {loc}: {err.get('msg')}")
    lines.append(
        "Se DATABASE_URL for texto de exemplo (ex.: COLE_AQUI...), substitui pela connection string completa do Neon."
    )
    return "\n".join(lines)


@lru_cache
def get_settings() -> Settings:
    try:
        return Settings()
    except ValidationError as e:
        raise RuntimeError(_format_validation_error(e)) from e
