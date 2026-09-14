from datetime import UTC, datetime, timedelta
import uuid
from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, Request, status
from pydantic import BaseModel, Field
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.api.deps import get_current_superuser
from app.core.database import get_db
from app.core.limiter import limiter
from app.models.admin_audit_event import AdminAuditEvent
from app.models.entitlements import Payment, UserModule
from app.models.user import User
from app.services.entitlements import (
    enable_basic_modules,
    enabled_module_ids,
    paid_until,
    plan_status,
)
from app.services.module_catalog import (
    BASIC_PLAN_CENTS,
    MODULE_CATALOG,
    PAID_PERIOD_DAYS,
    catalog_public,
)
from app.services.pix import generate_pix

router = APIRouter(tags=["admin"])


class PaymentOut(BaseModel):
    id: uuid.UUID
    amount_cents: int
    currency: str
    method: str
    status: str
    payer_name: str | None = None
    pix_copy: str | None = None
    pix_qr: str | None = None
    paid_at: datetime | None = None
    due_at: datetime | None = None
    pix_received_at: datetime | None = None
    created_at: datetime

    model_config = {"from_attributes": True}


class ModuleToggle(BaseModel):
    module_id: str
    enabled: bool


class SuperUserDetail(BaseModel):
    id: uuid.UUID
    email: str
    public_id: str | None = None
    full_name: str | None = None
    display_name: str | None = None
    is_active: bool
    is_free_plan: bool
    is_superuser: bool
    plan: str
    trial_ends_at: datetime | None = None
    paid_until: datetime | None = None
    modules: list[str]
    catalog: list[dict]
    payments: list[PaymentOut]


class FreePatch(BaseModel):
    is_free_plan: bool


class ModulesPatch(BaseModel):
    modules: list[ModuleToggle]


class PixReleaseBody(BaseModel):
    payer_name: str = Field(min_length=2, max_length=200)
    payment_id: uuid.UUID | None = None


def _audit(db: Session, actor: User, action: str, target: User, details: dict) -> None:
    db.add(
        AdminAuditEvent(
            actor_user_id=actor.id,
            actor_email=actor.email,
            action=action,
            target_user_id=target.id,
            target_email=target.email,
            details=details,
        )
    )


def _detail(db: Session, target: User) -> SuperUserDetail:
    pays = list(
        db.scalars(
            select(Payment)
            .where(Payment.user_id == target.id)
            .order_by(Payment.created_at.desc())
        ).all()
    )
    return SuperUserDetail(
        id=target.id,
        email=target.email,
        public_id=target.public_id,
        full_name=target.full_name,
        display_name=target.display_name,
        is_active=target.is_active,
        is_free_plan=target.is_free_plan,
        is_superuser=target.is_superuser,
        plan=plan_status(db, target),
        trial_ends_at=target.trial_ends_at,
        paid_until=paid_until(db, target),
        modules=enabled_module_ids(db, target),
        catalog=catalog_public(),
        payments=[PaymentOut.model_validate(p) for p in pays],
    )


@router.get("/catalog")
def module_catalog(
    _admin: Annotated[User, Depends(get_current_superuser)],
) -> dict:
    return {"items": catalog_public(), "basic_cents": BASIC_PLAN_CENTS}


@router.get("/users/{user_id}/billing", response_model=SuperUserDetail)
@limiter.limit("60/minute")
def user_billing(
    request: Request,
    user_id: uuid.UUID,
    _admin: Annotated[User, Depends(get_current_superuser)],
    db: Annotated[Session, Depends(get_db)],
) -> SuperUserDetail:
    target = db.get(User, user_id)
    if target is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Not found")
    return _detail(db, target)


@router.patch("/users/{user_id}/free", response_model=SuperUserDetail)
@limiter.limit("30/minute")
def patch_free(
    request: Request,
    user_id: uuid.UUID,
    body: FreePatch,
    admin_user: Annotated[User, Depends(get_current_superuser)],
    db: Annotated[Session, Depends(get_db)],
) -> SuperUserDetail:
    target = db.get(User, user_id)
    if target is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Not found")
    target.is_free_plan = body.is_free_plan
    if body.is_free_plan and not target.is_active:
        target.is_active = True
    enable_basic_modules(db, target.id)
    _audit(db, admin_user, "user.free", target, {"is_free_plan": body.is_free_plan})
    db.add(target)
    db.commit()
    db.refresh(target)
    return _detail(db, target)


@router.patch("/users/{user_id}/modules", response_model=SuperUserDetail)
@limiter.limit("30/minute")
def patch_modules(
    request: Request,
    user_id: uuid.UUID,
    body: ModulesPatch,
    admin_user: Annotated[User, Depends(get_current_superuser)],
    db: Annotated[Session, Depends(get_db)],
) -> SuperUserDetail:
    target = db.get(User, user_id)
    if target is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Not found")
    known = {str(item["id"]) for item in MODULE_CATALOG}
    for item in body.modules:
        if item.module_id not in known:
            raise HTTPException(status.HTTP_422_UNPROCESSABLE_ENTITY, "Módulo desconhecido")
        row = db.scalar(
            select(UserModule).where(
                UserModule.user_id == target.id,
                UserModule.module_id == item.module_id,
            )
        )
        if row is None:
            db.add(
                UserModule(
                    user_id=target.id,
                    module_id=item.module_id,
                    enabled=item.enabled,
                )
            )
        else:
            row.enabled = item.enabled
            db.add(row)
    _audit(
        db,
        admin_user,
        "user.modules",
        target,
        {"modules": [m.model_dump() for m in body.modules]},
    )
    db.commit()
    return _detail(db, target)


@router.post("/users/{user_id}/pix", response_model=SuperUserDetail)
@limiter.limit("20/minute")
def create_pix(
    request: Request,
    user_id: uuid.UUID,
    admin_user: Annotated[User, Depends(get_current_superuser)],
    db: Annotated[Session, Depends(get_db)],
) -> SuperUserDetail:
    target = db.get(User, user_id)
    if target is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Not found")
    payer = (target.full_name or target.display_name or target.email).strip()
    try:
        pix = generate_pix(user_id=target.id, email=target.email, payer_name=payer)
    except Exception as exc:
        raise HTTPException(
            status.HTTP_502_BAD_GATEWAY,
            "Não foi possível gerar o PIX. Use libertação manual.",
        ) from exc
    pay = Payment(
        user_id=target.id,
        amount_cents=BASIC_PLAN_CENTS,
        currency="BRL",
        method="pix",
        status="pending",
        payer_name=payer,
        pix_id=pix.get("pix_id"),
        pix_copy=pix.get("pix_copy"),
        pix_qr=pix.get("pix_qr"),
    )
    db.add(pay)
    _audit(db, admin_user, "user.pix.create", target, {"payer_name": payer})
    db.commit()
    return _detail(db, target)


@router.post("/users/{user_id}/pix/release", response_model=SuperUserDetail)
@limiter.limit("20/minute")
def release_pix(
    request: Request,
    user_id: uuid.UUID,
    body: PixReleaseBody,
    admin_user: Annotated[User, Depends(get_current_superuser)],
    db: Annotated[Session, Depends(get_db)],
) -> SuperUserDetail:
    target = db.get(User, user_id)
    if target is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Not found")
    now = datetime.now(UTC)
    pay: Payment | None = None
    if body.payment_id is not None:
        pay = db.get(Payment, body.payment_id)
        if pay is None or pay.user_id != target.id:
            raise HTTPException(status.HTTP_404_NOT_FOUND, "Not found")
    else:
        pay = db.scalar(
            select(Payment)
            .where(Payment.user_id == target.id, Payment.status == "pending")
            .order_by(Payment.created_at.desc())
        )
    if pay is None:
        pay = Payment(
            user_id=target.id,
            amount_cents=BASIC_PLAN_CENTS,
            currency="BRL",
            method="pix",
            status="pending",
            payer_name=body.payer_name.strip(),
        )
        db.add(pay)
        db.flush()
    pay.status = "paid"
    pay.method = "pix"
    pay.payer_name = body.payer_name.strip()
    pay.paid_at = now
    pay.due_at = now + timedelta(days=PAID_PERIOD_DAYS)
    target.is_active = True
    enable_basic_modules(db, target.id)
    _audit(
        db,
        admin_user,
        "user.pix.release",
        target,
        {"payer_name": pay.payer_name, "due_at": pay.due_at.isoformat()},
    )
    db.add(pay)
    db.add(target)
    db.commit()
    db.refresh(target)
    return _detail(db, target)
