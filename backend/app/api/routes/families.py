"""Família e convites — núcleo Etapa 12."""

from __future__ import annotations

import secrets
import uuid
from datetime import UTC, datetime, timedelta
from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.orm import Session, joinedload

from app.api.deps import get_current_user
from app.core.mail import send_family_invite_email
from app.core.database import get_db
from app.core.security import hash_password_reset_token
from app.models.announcement import Announcement
from app.models.family import Family, FamilyInvite, FamilyMember
from app.models.user import User
from app.schemas.family import (
    FamilyCreate,
    FamilyInviteCreateRequest,
    FamilyInviteCreateResponse,
    FamilyJoinRequest,
    FamilyMeResponse,
    FamilyMemberOut,
    FamilyOut,
    FamilyPendingInviteOut,
    FamilyUpdate,
)
from app.services.family_limits import family_has_room

router = APIRouter(prefix="/families", tags=["families"])

INVITE_VALID_HOURS = 24
INVITE_URL_PREFIX = "wellpaid://join?token="


def _member_row(db: Session, user_id: uuid.UUID) -> FamilyMember | None:
    return db.scalar(
        select(FamilyMember)
        .options(
            joinedload(FamilyMember.family).joinedload(Family.members).joinedload(
                FamilyMember.user
            )
        )
        .where(FamilyMember.user_id == user_id)
    )


def _build_family_out(fam: Family, current_user_id: uuid.UUID | None = None) -> FamilyOut:
    members_out: list[FamilyMemberOut] = []
    for m in sorted(fam.members, key=lambda x: (x.role != "owner", x.created_at)):
        u = m.user
        members_out.append(
            FamilyMemberOut(
                user_id=m.user_id,
                email=u.email,
                full_name=u.full_name,
                role=m.role,
                is_self=current_user_id is not None and m.user_id == current_user_id,
            )
        )
    return FamilyOut(id=fam.id, name=fam.name, members=members_out)


@router.get("/me", response_model=FamilyMeResponse)
def get_my_family(
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> FamilyMeResponse:
    row = _member_row(db, user.id)
    if row is None or row.family is None:
        return FamilyMeResponse(family=None)
    return FamilyMeResponse(
        family=_build_family_out(row.family, current_user_id=user.id),
    )


@router.post("/me", response_model=FamilyOut, status_code=status.HTTP_201_CREATED)
def create_family(
    body: FamilyCreate,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> FamilyOut:
    if _member_row(db, user.id) is not None:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Já pertences a uma família",
        )
    name = (body.name or "").strip() or "Família"
    fam = Family(name=name[:200], created_by_user_id=user.id)
    db.add(fam)
    db.flush()
    db.add(
        FamilyMember(
            family_id=fam.id,
            user_id=user.id,
            role="owner",
        )
    )
    db.commit()
    db.refresh(fam)
    fam = db.scalar(
        select(Family)
        .options(
            joinedload(Family.members).joinedload(FamilyMember.user),
        )
        .where(Family.id == fam.id)
    )
    assert fam is not None
    return _build_family_out(fam, current_user_id=user.id)


@router.patch("/me", response_model=FamilyOut)
def update_my_family(
    body: FamilyUpdate,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> FamilyOut:
    row = _member_row(db, user.id)
    if row is None or row.family is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Sem família",
        )
    if row.role != "owner":
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Apenas o titular pode alterar o nome",
        )
    row.family.name = body.name.strip()[:200]
    db.commit()
    db.refresh(row.family)
    fam = db.scalar(
        select(Family)
        .options(joinedload(Family.members).joinedload(FamilyMember.user))
        .where(Family.id == row.family_id)
    )
    assert fam is not None
    return _build_family_out(fam, current_user_id=user.id)


@router.post("/me/invites", response_model=FamilyInviteCreateResponse)
def create_invite(
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
    body: FamilyInviteCreateRequest | None = None,
) -> FamilyInviteCreateResponse:
    row = _member_row(db, user.id)
    if row is None or row.family is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Sem família",
        )
    if row.role != "owner":
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Apenas o titular pode convidar",
        )
    if not family_has_room(len(row.family.members)):
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Família cheia",
        )
    raw = secrets.token_urlsafe(32)
    th = hash_password_reset_token(raw)
    req = body or FamilyInviteCreateRequest()
    invite_email = (req.invite_email or "").strip().lower() or None
    exp = datetime.now(UTC) + timedelta(hours=INVITE_VALID_HOURS)
    inv = FamilyInvite(
        family_id=row.family_id,
        token_hash=th,
        expires_at=exp,
        invite_email=invite_email,
    )
    db.add(inv)
    db.commit()
    invite_url = f"{INVITE_URL_PREFIX}{raw}"
    sent = False
    invite_target_user = None
    if invite_email is not None:
        invite_target_user = db.scalar(select(User).where(User.email == invite_email))
    if invite_target_user is not None:
        db.add(
            Announcement(
                title="Convite para família recebido",
                body=(
                    f"Recebeste um convite para entrar na família \"{row.family.name}\". "
                    "Para concluir: ativa o Modo Família em Configurações, abre a área Família, "
                    f"usa o código {raw} e confirma o ingresso. Este convite expira em 24 horas."
                )[:2000],
                kind="warning",
                placement="announcements_tab",
                priority=90,
                cta_label="Abrir Família",
                cta_url=invite_url,
                is_active=True,
                starts_at=datetime.now(UTC),
                ends_at=exp,
                created_by_user_id=user.id,
                target_user_id=invite_target_user.id,
            )
        )
    if invite_email:
        sent = send_family_invite_email(
            invite_email,
            family_name=row.family.name,
            invite_token=raw,
            invite_url=invite_url,
            expires_hours=INVITE_VALID_HOURS,
        )
    db.commit()
    return FamilyInviteCreateResponse(
        token=raw,
        expires_at=exp,
        invite_url=invite_url,
        invite_sent_email=invite_email,
        invite_sent=sent,
    )


@router.post("/join", response_model=FamilyOut, status_code=status.HTTP_201_CREATED)
def join_family(
    body: FamilyJoinRequest,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> FamilyOut:
    if _member_row(db, user.id) is not None:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Já pertences a uma família",
        )
    token = body.token.strip()
    th = hash_password_reset_token(token)
    inv = db.scalar(
        select(FamilyInvite)
        .options(joinedload(FamilyInvite.family).joinedload(Family.members))
        .where(FamilyInvite.token_hash == th)
    )
    if inv is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Convite inválido",
        )
    if inv.used:
        raise HTTPException(
            status_code=status.HTTP_410_GONE,
            detail="Convite já utilizado",
        )
    if inv.expires_at < datetime.now(UTC):
        raise HTTPException(
            status_code=status.HTTP_410_GONE,
            detail="Convite expirado",
        )
    if inv.invite_email is not None and inv.invite_email.strip().lower() != user.email.strip().lower():
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Este convite foi emitido para outro e-mail",
        )
    fam = inv.family
    if fam is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Família não encontrada")
    if not family_has_room(len(fam.members)):
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Família cheia",
        )
    inv.used = True
    db.add(
        FamilyMember(
            family_id=fam.id,
            user_id=user.id,
            role="member",
        )
    )
    db.commit()
    fam = db.scalar(
        select(Family)
        .options(joinedload(Family.members).joinedload(FamilyMember.user))
        .where(Family.id == fam.id)
    )
    assert fam is not None
    return _build_family_out(fam, current_user_id=user.id)


@router.get("/invites/pending", response_model=list[FamilyPendingInviteOut])
def list_pending_invites(
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> list[FamilyPendingInviteOut]:
    now = datetime.now(UTC)
    rows = db.scalars(
        select(FamilyInvite)
        .options(joinedload(FamilyInvite.family))
        .where(
            FamilyInvite.used.is_(False),
            FamilyInvite.expires_at >= now,
            FamilyInvite.invite_email == user.email.strip().lower(),
        )
        .order_by(FamilyInvite.expires_at.asc())
    ).all()
    out: list[FamilyPendingInviteOut] = []
    for row in rows:
        if row.family is None:
            continue
        secs = int((row.expires_at - now).total_seconds())
        hours_remaining = max(1, secs // 3600)
        out.append(
            FamilyPendingInviteOut(
                invite_id=row.id,
                family_id=row.family_id,
                family_name=row.family.name,
                invite_email=row.invite_email,
                expires_at=row.expires_at,
                hours_remaining=hours_remaining,
            )
        )
    return out


@router.delete("/me/members/{member_user_id}", status_code=status.HTTP_204_NO_CONTENT)
def remove_member(
    member_user_id: uuid.UUID,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> None:
    actor = _member_row(db, user.id)
    if actor is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Sem família")
    if actor.role != "owner":
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Apenas o titular pode remover membros",
        )
    if member_user_id == user.id:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Usa sair da família para te removeres a ti",
        )
    target = db.scalar(
        select(FamilyMember).where(
            FamilyMember.family_id == actor.family_id,
            FamilyMember.user_id == member_user_id,
        )
    )
    if target is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Membro não encontrado")
    db.delete(target)
    db.commit()


@router.delete("/me", status_code=status.HTTP_204_NO_CONTENT)
def leave_or_dissolve_family(
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> None:
    row = _member_row(db, user.id)
    if row is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Sem família")
    fam_id = row.family_id
    others = db.scalars(
        select(FamilyMember).where(
            FamilyMember.family_id == fam_id,
            FamilyMember.user_id != user.id,
        )
    ).all()
    if row.role == "owner":
        if not others:
            fam = db.get(Family, fam_id)
            if fam:
                db.delete(fam)
        else:
            successor = min(others, key=lambda m: m.created_at)
            successor.role = "owner"
            db.delete(row)
    else:
        db.delete(row)
    db.commit()
