import logging
import secrets
import uuid
from datetime import UTC, datetime, timedelta

from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, Request, status
from sqlalchemy import delete, select, update
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session, joinedload

from app.core.config import get_settings
from app.core.database import get_db
from app.core.limiter import limiter
from app.core.mail import send_password_reset_email, send_verification_email
from app.core.security import (
    create_access_token,
    create_refresh_token,
    decode_token_safe,
    hash_password,
    hash_password_reset_token,
    new_jti,
    verify_password,
)
from app.models.email_verification_token import EmailVerificationToken
from app.models.password_reset_token import PasswordResetToken
from app.models.refresh_token import RefreshToken
from app.api.deps import get_current_user
from app.models.user import User
from app.schemas.auth import (
    ForgotPasswordRequest,
    ForgotPasswordResponse,
    LoginRequest,
    LogoutRequest,
    MessageResponse,
    RefreshRequest,
    RegisterRequest,
    RegisterResponse,
    ResendVerificationRequest,
    ResendVerificationResponse,
    ResetPasswordRequest,
    TokenPairResponse,
    UserMeResponse,
    UserProfilePatch,
    VerifyEmailRequest,
)
router = APIRouter(prefix="/auth", tags=["auth"])
settings = get_settings()
logger = logging.getLogger(__name__)

_DEV_ENVS = frozenset({"development", "dev", "local"})

_VERIFICATION_INVALID = (
    "Link ou código inválido ou expirado. Solicite um novo e-mail na app ou reenvie o código."
)
_RESEND_VERIFICATION_MSG = (
    "Se o e-mail estiver cadastrado e a conta ainda não estiver confirmada, "
    "enviámos instruções. Verifique também a pasta de spam."
)
_REGISTER_OK_MSG = (
    "Conta criada. Enviámos um código e um link para confirmar o seu e-mail."
)


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


def _add_email_verification_row(db: Session, user_id: uuid.UUID) -> tuple[str, str]:
    db.execute(
        delete(EmailVerificationToken).where(
            EmailVerificationToken.user_id == user_id,
            EmailVerificationToken.used.is_(False),
        )
    )
    raw_tok = secrets.token_urlsafe(32)
    code = f"{secrets.randbelow(900000) + 100000:06d}"
    exp = datetime.now(UTC) + timedelta(hours=48)
    db.add(
        EmailVerificationToken(
            user_id=user_id,
            token_hash=hash_password_reset_token(raw_tok),
            code_hash=hash_password_reset_token(code),
            expires_at=exp,
            used=False,
        )
    )
    return raw_tok, code


def _send_verification_email_helper(user: User, raw_tok: str, code: str) -> None:
    sent = send_verification_email(user.email, raw_tok, code)
    if not sent:
        if settings.email_verification_log_token:
            logger.warning(
                "Confirmação de e-mail (valores nos logs — desactivar "
                "EMAIL_VERIFICATION_LOG_TOKEN em produção): email=%s token=%s code=%s",
                user.email,
                raw_tok,
                code,
            )
        else:
            logger.warning(
                "Confirmação de e-mail: SMTP não enviou. Configure SMTP ou "
                "APP_ENV=development / EMAIL_VERIFICATION_LOG_TOKEN=true para testes."
            )


@router.post(
    "/register",
    response_model=RegisterResponse,
    status_code=status.HTTP_201_CREATED,
)
@limiter.limit("10/minute")
def register(
    request: Request,
    body: RegisterRequest,
    db: Session = Depends(get_db),
) -> RegisterResponse:
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
        email_verified_at=None,
    )
    db.add(user)
    try:
        db.flush()
    except IntegrityError:
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="E-mail já cadastrado",
        ) from None

    raw_tok, code = _add_email_verification_row(db, user.id)
    try:
        db.commit()
    except IntegrityError:
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="E-mail já cadastrado",
        ) from None

    db.refresh(user)
    _send_verification_email_helper(user, raw_tok, code)

    return RegisterResponse(
        message=_REGISTER_OK_MSG,
        email=user.email,
        dev_verification_token=raw_tok if _is_dev_env() else None,
        dev_verification_code=code if _is_dev_env() else None,
    )


@router.post("/verify-email", response_model=TokenPairResponse)
@limiter.limit("30/minute")
def verify_email(
    request: Request,
    body: VerifyEmailRequest,
    db: Session = Depends(get_db),
) -> TokenPairResponse:
    now = datetime.now(UTC)
    tok = (body.token or "").strip()
    if tok:
        th = hash_password_reset_token(tok)
        row = db.scalar(
            select(EmailVerificationToken)
            .options(joinedload(EmailVerificationToken.user))
            .where(
                EmailVerificationToken.token_hash == th,
                EmailVerificationToken.used.is_(False),
            )
        )
    else:
        if body.email is None:
            raise HTTPException(
                status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
                detail="E-mail é obrigatório para confirmar com o código.",
            )
        email = body.email.lower().strip()
        cod = (body.code or "").strip()
        ch = hash_password_reset_token(cod)
        target = db.scalar(select(User).where(User.email == email))
        if target is None:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=_VERIFICATION_INVALID,
            )
        row = db.scalar(
            select(EmailVerificationToken)
            .options(joinedload(EmailVerificationToken.user))
            .where(
                EmailVerificationToken.user_id == target.id,
                EmailVerificationToken.code_hash == ch,
                EmailVerificationToken.used.is_(False),
            )
        )

    if row is None or row.expires_at < now:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=_VERIFICATION_INVALID,
        )
    u = row.user
    if u is None or not u.is_active:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=_VERIFICATION_INVALID,
        )
    if u.email_verified_at is not None:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Este e-mail já foi confirmado. Entre com a sua senha.",
        )

    u.email_verified_at = now
    row.used = True
    db.add(u)
    db.add(row)
    db.execute(
        update(RefreshToken).where(RefreshToken.user_id == u.id).values(revoked=True)
    )
    db.commit()
    return _issue_tokens(db, u)


@router.post("/resend-verification", response_model=ResendVerificationResponse)
@limiter.limit("3/minute")
def resend_verification(
    request: Request,
    body: ResendVerificationRequest,
    db: Session = Depends(get_db),
) -> ResendVerificationResponse:
    email = body.email.lower().strip()
    user = db.scalar(select(User).where(User.email == email))
    dev_tok: str | None = None
    dev_code: str | None = None

    if user is not None and user.is_active and user.email_verified_at is None:
        raw_tok, code = _add_email_verification_row(db, user.id)
        db.commit()
        db.refresh(user)
        _send_verification_email_helper(user, raw_tok, code)
        if _is_dev_env():
            dev_tok = raw_tok
            dev_code = code

    return ResendVerificationResponse(
        message=_RESEND_VERIFICATION_MSG,
        dev_verification_token=dev_tok,
        dev_verification_code=dev_code,
    )


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
    if user.email_verified_at is None:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Confirme o seu e-mail antes de entrar. Verifique a caixa de entrada ou reenvie o código.",
        )
    return _issue_tokens(db, user)


@router.get("/me", response_model=UserMeResponse)
def read_current_user(
    current_user: Annotated[User, Depends(get_current_user)],
) -> UserMeResponse:
    """Dados do perfil para saudação na app (nome, e-mail)."""
    return UserMeResponse(
        email=current_user.email,
        full_name=current_user.full_name,
        display_name=current_user.display_name,
    )


@router.patch("/me", response_model=UserMeResponse)
def patch_current_user(
    body: UserProfilePatch,
    current_user: Annotated[User, Depends(get_current_user)],
    db: Session = Depends(get_db),
) -> UserMeResponse:
    """Define o nome mostrado na saudação (opcional; vazio volta ao nome derivado do cadastro)."""
    data = body.model_dump(exclude_unset=True)
    if "display_name" in data:
        raw = body.display_name
        if raw is None:
            current_user.display_name = None
        else:
            stripped = raw.strip()
            current_user.display_name = stripped[:200] if stripped else None
        db.add(current_user)
        db.commit()
        db.refresh(current_user)
    return UserMeResponse(
        email=current_user.email,
        full_name=current_user.full_name,
        display_name=current_user.display_name,
    )


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
    if user.email_verified_at is None:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Confirme o seu e-mail para continuar a sessão.",
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
