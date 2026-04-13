from typing import Self

from pydantic import BaseModel, EmailStr, Field, model_validator


class RegisterRequest(BaseModel):
    email: EmailStr
    password: str = Field(min_length=8)
    full_name: str | None = Field(default=None, max_length=200)
    phone: str | None = Field(default=None, max_length=32)


class LoginRequest(BaseModel):
    email: EmailStr
    password: str


class TokenPairResponse(BaseModel):
    access_token: str
    refresh_token: str
    token_type: str = "bearer"


class RegisterResponse(BaseModel):
    message: str
    email: str
    dev_verification_token: str | None = None
    dev_verification_code: str | None = None


class VerifyEmailRequest(BaseModel):
    email: EmailStr | None = None
    token: str | None = Field(default=None, max_length=512)
    code: str | None = Field(default=None, min_length=6, max_length=6)

    @model_validator(mode="after")
    def token_or_code_with_email(self) -> Self:
        tok = (self.token or "").strip()
        cod = (self.code or "").strip()
        if tok:
            self.token = tok
            return self
        if cod and self.email is not None:
            self.code = cod
            return self
        raise ValueError(
            "Informe o token do link de confirmação ou o e-mail juntamente com o código de 6 dígitos."
        )


class ResendVerificationRequest(BaseModel):
    email: EmailStr


class ResendVerificationResponse(BaseModel):
    message: str
    dev_verification_token: str | None = None
    dev_verification_code: str | None = None


class RefreshRequest(BaseModel):
    refresh_token: str


class LogoutRequest(BaseModel):
    refresh_token: str


class MessageResponse(BaseModel):
    message: str


class UserMeResponse(BaseModel):
    """Perfil mínimo do utilizador autenticado (após login)."""

    email: str
    full_name: str | None = None
    display_name: str | None = None


class UserProfilePatch(BaseModel):
    """Atualização parcial do perfil (nome mostrado no ecrã inicial)."""

    display_name: str | None = Field(default=None, max_length=200)


class DisplayNameUpdateBody(BaseModel):
    """Corpo POST para alterar o nome de saudação (compatível com clientes sem PATCH)."""

    display_name: str = Field("", max_length=200)


class ForgotPasswordRequest(BaseModel):
    email: EmailStr


class ForgotPasswordResponse(BaseModel):
    message: str
    # Só preenchido em ambiente de desenvolvimento (APP_ENV), para testar sem SMTP.
    dev_reset_token: str | None = None


class ResetPasswordRequest(BaseModel):
    token: str = Field(min_length=10, max_length=256)
    new_password: str = Field(min_length=8)
