import uuid
from typing import Annotated, Callable

from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy.orm import Session

from app.core.database import get_db
from app.core.security import decode_token_safe
from app.models.user import User
from app.services.entitlements import assert_module, assert_plan_access

security = HTTPBearer(auto_error=False)

_NOT_FOUND = HTTPException(
    status_code=status.HTTP_404_NOT_FOUND,
    detail="Not found",
)


def _user_from_bearer(
    creds: HTTPAuthorizationCredentials | None,
    db: Session,
    *,
    hide: bool,
) -> User:
    missing = _NOT_FOUND if hide else HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Credenciais ausentes",
    )
    invalid = _NOT_FOUND if hide else HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Token inválido",
    )
    if creds is None or creds.scheme.lower() != "bearer":
        raise missing
    payload = decode_token_safe(creds.credentials)
    if not payload or payload.get("type") != "access":
        raise invalid
    sub = payload.get("sub")
    if not sub:
        raise invalid
    try:
        uid = uuid.UUID(str(sub))
    except ValueError as exc:
        raise invalid from exc
    user = db.get(User, uid)
    if user is None or not user.is_active:
        raise invalid
    return user


def get_current_user(
    creds: Annotated[HTTPAuthorizationCredentials | None, Depends(security)],
    db: Annotated[Session, Depends(get_db)],
) -> User:
    user = _user_from_bearer(creds, db, hide=False)
    if user.email_verified_at is None:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Confirme o seu e-mail antes de continuar. Use o código enviado ou solicite um novo e-mail.",
        )
    assert_plan_access(db, user)
    return user


def get_current_admin_user(
    creds: Annotated[HTTPAuthorizationCredentials | None, Depends(security)],
    db: Annotated[Session, Depends(get_db)],
) -> User:
    """Rotas internas que ainda usam is_admin (ex. anúncios)."""
    user = _user_from_bearer(creds, db, hide=False)
    if not user.is_admin:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Acesso reservado a administradores",
        )
    return user


def get_current_superuser(
    creds: Annotated[HTTPAuthorizationCredentials | None, Depends(security)],
    db: Annotated[Session, Depends(get_db)],
) -> User:
    """Painel oculto: qualquer falha parece 404."""
    user = _user_from_bearer(creds, db, hide=True)
    if not user.is_superuser:
        raise _NOT_FOUND
    return user


def require_module(module_id: str) -> Callable[..., User]:
    def _dep(
        user: Annotated[User, Depends(get_current_user)],
        db: Annotated[Session, Depends(get_db)],
    ) -> User:
        assert_module(db, user, module_id)
        return user

    return _dep
