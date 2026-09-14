from datetime import UTC, datetime, timedelta

from fastapi import HTTPException, status
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models.login_attempt import LoginAttempt

LOCK_AFTER_FAILURES = 5
LOCK_FOR = timedelta(minutes=15)

LOGIN_FAIL_DETAIL = "E-mail ou senha incorretos"
LOGIN_LOCKED_DETAIL = "Muitas tentativas. Tente novamente em alguns minutos."


def _now() -> datetime:
    return datetime.now(UTC)


def _row_for(db: Session, email: str) -> LoginAttempt | None:
    return db.scalar(select(LoginAttempt).where(LoginAttempt.email == email))


def raise_if_locked(db: Session, email: str) -> None:
    row = _row_for(db, email)
    if row is None or row.locked_until is None:
        return
    if row.locked_until > _now():
        raise HTTPException(
            status_code=status.HTTP_429_TOO_MANY_REQUESTS,
            detail=LOGIN_LOCKED_DETAIL,
        )
    row.fail_count = 0
    row.locked_until = None
    db.add(row)
    db.commit()


def register_failure(db: Session, email: str) -> None:
    now = _now()
    row = _row_for(db, email)
    if row is None:
        row = LoginAttempt(email=email, fail_count=0, locked_until=None)
        db.add(row)
        db.flush()
    row.fail_count += 1
    row.last_failed_at = now
    if row.fail_count >= LOCK_AFTER_FAILURES:
        row.locked_until = now + LOCK_FOR
    db.add(row)
    db.commit()


def clear_failures(db: Session, email: str) -> None:
    row = _row_for(db, email)
    if row is None:
        return
    db.delete(row)
    db.commit()
