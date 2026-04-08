import hashlib
import hmac
import re
import secrets
import uuid
from datetime import UTC, datetime, timedelta

import bcrypt
from jose import JWTError, jwt

from app.core.config import get_settings

settings = get_settings()

_PASSWORD_RULE = re.compile(
    r"^(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9\s]).{8,}$"
)


def validate_password_strength(password: str) -> None:
    if not _PASSWORD_RULE.match(password):
        raise ValueError(
            "Senha: mínimo 8 caracteres, 1 maiúscula, 1 número e 1 caractere especial"
        )


def hash_password(password: str) -> str:
    validate_password_strength(password)
    raw = password.encode("utf-8")
    if len(raw) > 72:
        raw = raw[:72]
    salt = bcrypt.gensalt(rounds=12)
    return bcrypt.hashpw(raw, salt).decode("utf-8")


def verify_password(password: str, password_hash: str) -> bool:
    raw = password.encode("utf-8")
    if len(raw) > 72:
        raw = raw[:72]
    try:
        return bcrypt.checkpw(raw, password_hash.encode("utf-8"))
    except ValueError:
        return False


def create_access_token(subject: str) -> str:
    expire = datetime.now(UTC) + timedelta(minutes=settings.access_token_expire_minutes)
    return jwt.encode(
        {"sub": subject, "type": "access", "exp": expire},
        settings.secret_key,
        algorithm=settings.algorithm,
    )


def create_refresh_token(subject: str, jti: str) -> str:
    expire = datetime.now(UTC) + timedelta(days=settings.refresh_token_expire_days)
    return jwt.encode(
        {"sub": subject, "type": "refresh", "jti": jti, "exp": expire},
        settings.secret_key,
        algorithm=settings.algorithm,
    )


def decode_token(token: str) -> dict:
    return jwt.decode(
        token,
        settings.secret_key,
        algorithms=[settings.algorithm],
    )


def decode_token_safe(token: str) -> dict | None:
    try:
        return decode_token(token)
    except JWTError:
        return None


def new_jti() -> str:
    return str(uuid.uuid4())


def new_refresh_secret() -> str:
    return secrets.token_urlsafe(32)


def hash_password_reset_token(raw_token: str) -> str:
    key = settings.secret_key.encode("utf-8")
    return hmac.new(key, raw_token.encode("utf-8"), hashlib.sha256).hexdigest()
