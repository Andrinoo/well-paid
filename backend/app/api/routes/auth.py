import logging
import secrets
from datetime import UTC, datetime, timedelta

from fastapi import APIRouter, Depends, HTTPException, Request, status
from sqlalchemy import delete, select, update
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.core.database import get_db
from app.core.limiter import limiter
from app.core.mail import send_password_reset_email
from app.core.security import (
    create_access_token,
    create_refresh_token,
    decode_token_safe,
    hash_password,
    hash_password_reset_token,
    new_jti,
    verify_password,
)
from app.models.password_reset_token import PasswordResetToken
from app.models.refresh_token import RefreshToken
from app.models.user import User
from app.schemas.auth import (
    ForgotPasswordRequest,
    ForgotPasswordResponse,
    LoginRequest,
    LogoutRequest,
    MessageResponse,
    RefreshRequest,
    RegisterRequest,
    ResetPasswordRequest,
    TokenPairResponse,
)

router = APIRouter(prefix="/auth", tags=["auth"])
settings = get_settings()
logger = logging.getLogger(__name__)

_DEV_ENVS = frozenset({"development", "dev", "local"})


def _is_dev_env() -> bool:
    return settings.app_env.strip().lower() in _DEV_ENVS


def _issue_tokens(db: Session, user: User) -> TokenPairResponse:
    jti = new_jti()
    refresh = create_refresh_token(str(user.id), jti)
    exp = datetime.now(UTC) + timedelta(days=settings.refresh_token_expire_days)
    db.add(
        RefreshToken(
            user_id=user.id,
            jti=jti,
            revoked=False,
            expires_at=exp,
        )
    )
    db.commit()
    access = create_access_token(str(user.id))
    return TokenPairResponse(access_token=access, refresh_token=refresh)


@router.post("/register", response_model=TokenPairResponse)
@limiter.limit("10/minute")
def register(
    request: Request,
    body: RegisterRequest,
    db: Session = Depends(get_db),
) -> TokenPairResponse:
    email = body.email.lower().strip()
    try:
        hashed = hash_password(body.password)
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail=str(e),
        ) from e
    user = User(
        email=email,
        hashed_password=hashed,
        full_name=body.full_name,
        phone=body.phone,
    )
    db.add(user)
    try:
        db.commit()
    except IntegrityError as e:
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="E-mail já cadastrado",
        ) from e
    db.refresh(user)
    return _issue_tokens(db, user)


@router.post("/login", response_model=TokenPairResponse)
@limiter.limit("5/minute")
def login(
    request: Request,
    body: LoginRequest,
    db: Session = Depends(get_db),
) -> TokenPairResponse:
    email = body.email.lower().strip()
    user = db.scalar(select(User).where(User.email == email))
    if user is None or not verify_password(body.password, user.hashed_password):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="E-mail ou senha incorretos",
        )
    if not user.is_active:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Conta inativa",
        )
    return _issue_tokens(db, user)


@router.post("/refresh", response_model=TokenPairResponse)
@limiter.limit("30/minute")
def refresh_token(
    request: Request,
    body: RefreshRequest,
    db: Session = Depends(get_db),
) -> TokenPairResponse:
    payload = decode_token_safe(body.refresh_token)
    if not payload or payload.get("type") != "refresh":
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Refresh token inválido",
        )
    jti = payload.get("jti")
    sub = payload.get("sub")
    if not jti or not sub:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Refresh token inválido",
        )
    row = db.scalar(
        select(RefreshToken).where(
            RefreshToken.jti == str(jti),
            RefreshToken.revoked.is_(False),
        )
    )
    if row is None or row.expires_at < datetime.now(UTC):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Refresh token inválido ou expirado",
        )
    if str(row.user_id) != str(sub):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Refresh token inválido",
        )
    row.revoked = True
    db.add(row)
    db.commit()
    user = db.get(User, row.user_id)
    if user is None or not user.is_active:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Usuário inválido",
        )
    return _issue_tokens(db, user)


@router.post("/logout", response_model=MessageResponse)
def logout(body: LogoutRequest, db: Session = Depends(get_db)) -> MessageResponse:
    payload = decode_token_safe(body.refresh_token)
    if payload and payload.get("type") == "refresh":
        jti = payload.get("jti")
        if jti:
            row = db.scalar(select(RefreshToken).where(RefreshToken.jti == str(jti)))
            if row is not None:
                row.revoked = True
                db.add(row)
                db.commit()
    return MessageResponse(message="ok")


_RESET_MSG = (
    "Se o e-mail estiver cadastrado, enviaremos instruções em breve. "
    "Verifique também a pasta de spam."
)


@router.post("/forgot-password", response_model=ForgotPasswordResponse)
@limiter.limit("5/minute")
def forgot_password(
    request: Request,
    body: ForgotPasswordRequest,
    db: Session = Depends(get_db),
) -> ForgotPasswordResponse:
    email = body.email.lower().strip()
    user = db.scalar(select(User).where(User.email == email))
    dev_token: str | None = None

    if user is not None and user.is_active:
        db.execute(
            delete(PasswordResetToken).where(
                PasswordResetToken.user_id == user.id,
                PasswordResetToken.used.is_(False),
            )
        )
        raw = secrets.token_urlsafe(32)
        token_hash = hash_password_reset_token(raw)
        exp = datetime.now(UTC) + timedelta(hours=1)
        db.add(
            PasswordResetToken(
                user_id=user.id,
                token_hash=token_hash,
                expires_at=exp,
                used=False,
            )
        )
        db.commit()

        sent = send_password_reset_email(user.email, raw)
        if not sent:
            if settings.password_reset_log_token:
                logger.warning(
                    "Recuperação de senha (token nos logs — desativar PASSWORD_RESET_LOG_TOKEN em produção): "
                    "email=%s token=%s",
                    user.email,
                    raw,
                )
            else:
                logger.warning(
                    "Recuperação de senha: e-mail não enviado para utilizador registado. "
                    "Configure SMTP (SMTP_HOST, MAIL_FROM, SMTP_USER/SMTP_PASSWORD conforme o teu provedor) "
                    "ou, só em desenvolvimento: APP_ENV=development (dev_reset_token na resposta JSON) "
                    "ou PASSWORD_RESET_LOG_TOKEN=true (token nos logs do servidor)."
                )
        if _is_dev_env():
            dev_token = raw

    return ForgotPasswordResponse(message=_RESET_MSG, dev_reset_token=dev_token)


@router.post("/reset-password", response_model=MessageResponse)
@limiter.limit("10/minute")
def reset_password(
    request: Request,
    body: ResetPasswordRequest,
    db: Session = Depends(get_db),
) -> MessageResponse:
    raw = body.token.strip()
    token_hash = hash_password_reset_token(raw)
    row = db.scalar(
        select(PasswordResetToken).where(
            PasswordResetToken.token_hash == token_hash,
            PasswordResetToken.used.is_(False),
        )
    )
    now = datetime.now(UTC)
    if row is None or row.expires_at < now:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Token inválido ou expirado. Peça um novo link ou código.",
        )

    user = db.get(User, row.user_id)
    if user is None or not user.is_active:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Conta inválida",
        )

    try:
        user.hashed_password = hash_password(body.new_password)
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail=str(e),
        ) from e

    row.used = True
    db.add(row)
    db.execute(
        update(RefreshToken)
        .where(RefreshToken.user_id == user.id)
        .values(revoked=True)
    )
    db.add(user)
    db.commit()

    return MessageResponse(message="Senha atualizada. Pode entrar com a nova senha.")
