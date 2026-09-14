from datetime import UTC, datetime, timedelta

from fastapi import HTTPException, Request, status
from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.core.security import hash_password_reset_token
from app.models.signup_attempt import SignupAttempt
from app.models.user import User

SIGNUPS_PER_WINDOW = 3
SIGNUP_WINDOW = timedelta(hours=6)
MAX_UNVERIFIED_PER_IP = 2
UNVERIFIED_TTL = timedelta(hours=24)

SIGNUP_LOCKED_DETAIL = "Não foi possível criar a conta. Tente mais tarde."
REGISTER_OK_DETAIL = (
    "Se este e-mail for novo, enviámos um código para confirmar a conta."
)

_DISPOSABLE_DOMAINS = frozenset(
    {
        "mailinator.com",
        "guerrillamail.com",
        "guerrillamailblock.com",
        "sharklasers.com",
        "grr.la",
        "10minutemail.com",
        "10minutemail.net",
        "tempmail.com",
        "temp-mail.org",
        "trashmail.com",
        "yopmail.com",
        "discard.email",
        "getnada.com",
        "emailondeck.com",
    }
)


def _now() -> datetime:
    return datetime.now(UTC)


def client_ip(request: Request) -> str:
    forwarded = (request.headers.get("x-forwarded-for") or "").strip()
    if forwarded:
        return forwarded.split(",")[0].strip() or "unknown"
    cf = (request.headers.get("cf-connecting-ip") or "").strip()
    if cf:
        return cf
    if request.client and request.client.host:
        return request.client.host
    return "unknown"


def hash_client_ip(ip: str) -> str:
    return hash_password_reset_token(f"signup-ip:{ip.strip() or 'unknown'}")


def email_domain(email: str) -> str:
    part = email.rsplit("@", 1)
    if len(part) != 2:
        return ""
    return part[1].strip().lower()


def is_disposable_email(email: str) -> bool:
    return email_domain(email) in _DISPOSABLE_DOMAINS


def _row_for(db: Session, ip_hash: str) -> SignupAttempt | None:
    return db.scalar(select(SignupAttempt).where(SignupAttempt.ip_hash == ip_hash))


def _unverified_count(db: Session, ip_hash: str) -> int:
    return int(
        db.scalar(
            select(func.count()).select_from(User).where(
                User.signup_ip_hash == ip_hash,
                User.email_verified_at.is_(None),
                User.is_active.is_(True),
                User.is_superuser.is_(False),
            )
        )
        or 0
    )


def raise_if_signup_blocked(db: Session, ip_hash: str) -> None:
    now = _now()
    row = _row_for(db, ip_hash)
    if row is not None and row.locked_until is not None and row.locked_until > now:
        raise HTTPException(
            status_code=status.HTTP_429_TOO_MANY_REQUESTS,
            detail=SIGNUP_LOCKED_DETAIL,
        )
    if row is not None and row.locked_until is not None and row.locked_until <= now:
        row.success_count = 0
        row.locked_until = None
        row.window_started_at = None
        db.add(row)
        db.commit()
    if _unverified_count(db, ip_hash) >= MAX_UNVERIFIED_PER_IP:
        raise HTTPException(
            status_code=status.HTTP_429_TOO_MANY_REQUESTS,
            detail=SIGNUP_LOCKED_DETAIL,
        )


def record_signup_attempt(db: Session, ip_hash: str) -> None:
    now = _now()
    row = _row_for(db, ip_hash)
    if row is None:
        row = SignupAttempt(
            ip_hash=ip_hash,
            success_count=0,
            window_started_at=now,
            locked_until=None,
        )
        db.add(row)
        db.flush()
    if row.window_started_at is None or now - row.window_started_at >= SIGNUP_WINDOW:
        row.window_started_at = now
        row.success_count = 0
        row.locked_until = None
    row.success_count += 1
    row.last_at = now
    if row.success_count >= SIGNUPS_PER_WINDOW:
        row.locked_until = now + SIGNUP_WINDOW
    db.add(row)
    db.commit()


def expire_stale_unverified(db: Session) -> int:
    cutoff = _now() - UNVERIFIED_TTL
    users = list(
        db.scalars(
            select(User).where(
                User.is_active.is_(True),
                User.is_superuser.is_(False),
                User.email_verified_at.is_(None),
                User.created_at < cutoff,
            )
        ).all()
    )
    n = 0
    for user in users:
        user.is_active = False
        db.add(user)
        n += 1
    if n:
        db.commit()
    return n
