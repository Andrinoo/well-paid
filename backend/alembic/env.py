import logging
import os
from logging.config import fileConfig
from pathlib import Path

from dotenv import load_dotenv

# Carregar .env antes de qualquer import que chame get_settings() (subprocess do Alembic).
# Caminhos absolutos a partir de alembic/env.py — não dependem do cwd.
_ENV_ENCODING = "utf-8-sig"
_backend_dir = Path(__file__).resolve().parent.parent
_repo_root = _backend_dir.parent

load_dotenv(_backend_dir / ".env", override=False, encoding=_ENV_ENCODING)
load_dotenv(_repo_root / ".env", override=True, encoding=_ENV_ENCODING)
_custom = os.getenv("ONE_PAY_DOTENV", "").strip()
if _custom:
    load_dotenv(_custom, override=True, encoding=_ENV_ENCODING)

from alembic import context
from sqlalchemy import engine_from_config, pool

from app.core.config import get_settings
from app.models import Base

config = context.config
if config.config_file_name is not None:
    fileConfig(config.config_file_name)
logger = logging.getLogger("alembic.env")

target_metadata = Base.metadata


def get_url() -> str:
    return get_settings().database_url


def run_migrations_offline() -> None:
    url = get_url()
    context.configure(
        url=url,
        target_metadata=target_metadata,
        literal_binds=True,
        dialect_opts={"paramstyle": "named"},
        compare_type=True,
    )

    with context.begin_transaction():
        context.run_migrations()


def run_migrations_online() -> None:
    configuration = config.get_section(config.config_ini_section) or {}
    configuration["sqlalchemy.url"] = get_url()
    connectable = engine_from_config(
        configuration,
        prefix="sqlalchemy.",
        poolclass=pool.NullPool,
    )

    with connectable.connect() as connection:
        context.configure(
            connection=connection,
            target_metadata=target_metadata,
            compare_type=True,
        )

        with context.begin_transaction():
            context.run_migrations()


if context.is_offline_mode():
    run_migrations_offline()
else:
    run_migrations_online()
