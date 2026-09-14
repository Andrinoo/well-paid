from datetime import UTC, datetime, timedelta

from fastapi import HTTPException, status
from sqlalchemy import select, update
from sqlalchemy.orm import Session

from app.models.entitlements import Payment, UserModule
from app.models.refresh_token import RefreshToken
from app.models.user import User
from app.services.module_catalog import (
    BASIC_MODULE_IDS,
    MODULE_CATALOG,
    MODULE_SETTINGS,
    TRIAL_HOURS,
)

PLAN_SUPER = "super"
PLAN_FREE = "free"
PLAN_PAID = "paid"
PLAN_TRIAL = "trial"
PLAN_EXPIRED = "expired"

ACCESS_DENIED = "Acesso indisponível. Assine o plano ou contacte o suporte."
MODULE_DENIED = "Este módulo não está activo na sua conta."


def _now() -> datetime:
    return datetime.now(UTC)


def current_paid_payment(db: Session, user_id) -> Payment | None:
    now = _now()
    return db.scalar(
        select(Payment)
        .where(
            Payment.user_id == user_id,
            Payment.status == "paid",
            Payment.due_at.is_not(None),
            Payment.due_at > now,
        )
        .order_by(Payment.due_at.desc())
    )


def plan_status(db: Session, user: User) -> str:
    if user.is_superuser:
        return PLAN_SUPER
    if user.is_free_plan:
        return PLAN_FREE
    if current_paid_payment(db, user.id) is not None:
        return PLAN_PAID
    if user.trial_ends_at is not None and user.trial_ends_at > _now():
        return PLAN_TRIAL
    return PLAN_EXPIRED


def start_trial(user: User) -> None:
    if user.is_free_plan or user.is_superuser:
        return
    user.trial_ends_at = _now() + timedelta(hours=TRIAL_HOURS)


def enable_basic_modules(db: Session, user_id) -> None:
    existing = {
        row.module_id: row
        for row in db.scalars(
            select(UserModule).where(UserModule.user_id == user_id)
        ).all()
    }
    for module_id in BASIC_MODULE_IDS:
        row = existing.get(module_id)
        if row is None:
            db.add(UserModule(user_id=user_id, module_id=module_id, enabled=True))
        elif not row.enabled:
            row.enabled = True
            db.add(row)


def revoke_sessions(db: Session, user_id) -> int:
    count = len(
        list(
            db.scalars(
                select(RefreshToken).where(
                    RefreshToken.user_id == user_id,
                    RefreshToken.revoked.is_(False),
                )
            ).all()
        )
    )
    db.execute(
        update(RefreshToken)
        .where(
            RefreshToken.user_id == user_id,
            RefreshToken.revoked.is_(False),
        )
        .values(revoked=True)
    )
    return count


def expire_trials(db: Session) -> int:
    users = list(
        db.scalars(
            select(User).where(
                User.is_active.is_(True),
                User.is_superuser.is_(False),
                User.is_free_plan.is_(False),
            )
        ).all()
    )
    n = 0
    for user in users:
        if plan_status(db, user) != PLAN_EXPIRED:
            continue
        user.is_active = False
        db.add(user)
        revoke_sessions(db, user.id)
        n += 1
    if n:
        db.commit()
    return n


def deactivate_expired(db: Session, user: User) -> None:
    if user.is_superuser or user.is_free_plan:
        return
    if plan_status(db, user) != PLAN_EXPIRED:
        return
    if not user.is_active:
        return
    user.is_active = False
    db.add(user)
    revoke_sessions(db, user.id)
    db.commit()
    db.refresh(user)


def assert_plan_access(db: Session, user: User) -> str:
    status_id = plan_status(db, user)
    if status_id == PLAN_EXPIRED:
        deactivate_expired(db, user)
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail=ACCESS_DENIED,
        )
    return status_id


def module_enabled(db: Session, user: User, module_id: str) -> bool:
    if user.is_superuser:
        return True
    if module_id == MODULE_SETTINGS:
        return True
    row = db.scalar(
        select(UserModule).where(
            UserModule.user_id == user.id,
            UserModule.module_id == module_id,
        )
    )
    return bool(row and row.enabled)


def assert_module(db: Session, user: User, module_id: str) -> None:
    if not module_enabled(db, user, module_id):
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail=MODULE_DENIED,
        )


def enabled_module_ids(db: Session, user: User) -> list[str]:
    if user.is_superuser:
        return [str(item["id"]) for item in MODULE_CATALOG] + [MODULE_SETTINGS]
    rows = db.scalars(select(UserModule).where(UserModule.user_id == user.id)).all()
    ids = [row.module_id for row in rows if row.enabled]
    if MODULE_SETTINGS not in ids:
        ids.append(MODULE_SETTINGS)
    return ids


def paid_until(db: Session, user: User) -> datetime | None:
    pay = current_paid_payment(db, user.id)
    return pay.due_at if pay is not None else None
