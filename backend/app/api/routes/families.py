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
from app.core.database import get_db
from app.core.security import hash_password_reset_token
from app.models.family import Family, FamilyInvite, FamilyMember
from app.models.user import User
from app.schemas.family import (
    FamilyCreate,
    FamilyInviteCreateResponse,
    FamilyJoinRequest,
    FamilyMeResponse,
    FamilyMemberOut,
    FamilyOut,
    FamilyUpdate,
)
from app.services.family_limits import family_has_room

router = APIRouter(prefix="/families", tags=["families"])

INVITE_VALID_DAYS = 7
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
    exp = datetime.now(UTC) + timedelta(days=INVITE_VALID_DAYS)
    inv = FamilyInvite(family_id=row.family_id, token_hash=th, expires_at=exp)
    db.add(inv)
    db.commit()
    return FamilyInviteCreateResponse(
        token=raw,
        expires_at=exp,
        invite_url=f"{INVITE_URL_PREFIX}{raw}",
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
