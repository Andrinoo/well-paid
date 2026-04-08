import html
import logging
import smtplib
import ssl
from email.message import EmailMessage
from email.utils import formataddr, parseaddr

from app.core.config import get_settings

logger = logging.getLogger(__name__)

# Nome visível nos e-mails transacionais (nunca usar nome pessoal no remetente).
_BRAND_MAIL_NAME = "Equipe Well Paid"


def _brand_from_header(mail_from: str) -> str:
    """Usa sempre o nome da marca no From; extrai só o endereço de MAIL_FROM/SMTP_USER."""
    stripped = mail_from.strip()
    _, addr = parseaddr(stripped)
    if addr:
        return formataddr((_BRAND_MAIL_NAME, addr))
    return stripped


def _password_reset_plain_text(raw_token: str) -> str:
    return (
        "Olá,\n\n"
        "Recebemos um pedido para redefinir a palavra-passe da sua conta Well Paid.\n\n"
        "CÓDIGO DE RECUPERAÇÃO (copie e cole na app, ecrã «Redefinir senha»):\n\n"
        f"{raw_token}\n\n"
        "Este código é válido por 1 hora. Se não pediu esta alteração, pode ignorar este e-mail — "
        "a sua conta permanece segura.\n\n"
        "Obrigado por confiar no Well Paid para organizar as suas finanças.\n\n"
        f"— {_BRAND_MAIL_NAME}\n"
    )


def _password_reset_html(raw_token: str) -> str:
    safe = html.escape(raw_token, quote=True)
    # Destaque visual: metades do token com cores da marca (legível em clientes HTML).
    mid = len(raw_token) // 2
    if mid > 0:
        part_a = html.escape(raw_token[:mid], quote=True)
        part_b = html.escape(raw_token[mid:], quote=True)
        token_html = (
            f'<span style="color:#38bdf8;font-weight:700;">{part_a}</span>'
            f'<span style="color:#c084fc;font-weight:700;">{part_b}</span>'
        )
    else:
        token_html = f'<span style="color:#38bdf8;font-weight:700;">{safe}</span>'

    return f"""\
<!DOCTYPE html>
<html lang="pt">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Recuperação de senha — Well Paid</title>
</head>
<body style="margin:0;padding:0;background-color:#040301;font-family:Segoe UI,Roboto,Helvetica,Arial,sans-serif;">
<table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="background-color:#040301;padding:24px 12px;">
<tr>
<td align="center">
<table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="max-width:560px;background-color:#0e0e14;border-radius:16px;border:1px solid rgba(30,144,255,0.35);overflow:hidden;">
<tr>
<td style="padding:28px 24px 8px 24px;text-align:center;">
<p style="margin:0;font-size:13px;letter-spacing:0.25em;text-transform:uppercase;color:#94a3b8;">Well Paid</p>
<h1 style="margin:12px 0 0 0;font-size:22px;font-weight:700;color:#f1f5f9;line-height:1.3;">
Recuperação de palavra-passe
</h1>
</td>
</tr>
<tr>
<td style="padding:16px 24px 8px 24px;">
<p style="margin:0 0 14px 0;font-size:15px;line-height:1.55;color:#cbd5e1;">
Recebemos um pedido para redefinir a palavra-passe da sua conta. Utilize o código abaixo
<strong style="color:#e2e8f0;">na aplicação Well Paid</strong>, no ecrã <strong style="color:#e2e8f0;">«Redefinir senha»</strong>.
</p>
<p style="margin:0 0 18px 0;font-size:14px;line-height:1.5;color:#94a3b8;">
O código é de uso único neste fluxo e <strong style="color:#fcd34d;">expira ao fim de 1 hora</strong>.
Se não foi você a pedir, pode ignorar este e-mail — a sua conta mantém-se protegida.
</p>
</td>
</tr>
<tr>
<td style="padding:0 24px 20px 24px;">
<table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="background:linear-gradient(135deg,rgba(30,144,255,0.15),rgba(147,51,234,0.12));border-radius:12px;border:1px solid rgba(56,189,248,0.4);">
<tr>
<td style="padding:20px 18px;text-align:center;">
<p style="margin:0 0 10px 0;font-size:12px;text-transform:uppercase;letter-spacing:0.12em;color:#94a3b8;">
O seu código de recuperação
</p>
<p style="margin:0;font-size:19px;line-height:1.4;word-break:break-all;font-family:Consolas,Monaco,'Courier New',monospace;letter-spacing:0.04em;">
{token_html}
</p>
</td>
</tr>
</table>
</td>
</tr>
<tr>
<td style="padding:8px 24px 24px 24px;border-top:1px solid rgba(148,163,184,0.15);">
<p style="margin:16px 0 10px 0;font-size:14px;line-height:1.55;color:#cbd5e1;">
<strong style="color:#f8fafc;">Dica:</strong> copie o código completo (incluindo todos os caracteres) e cole no campo indicado na app.
Se o teclado sugerir espaços, remova-os — o código deve ficar numa só linha.
</p>
<p style="margin:0 0 18px 0;font-size:14px;line-height:1.55;color:#94a3b8;">
Obrigado por utilizar o <strong style="color:#38bdf8;">Well Paid</strong> para acompanhar as suas finanças.
</p>
<p style="margin:0;font-size:13px;line-height:1.5;color:#64748b;">
Com os melhores cumprimentos,<br>
<strong style="color:#94a3b8;">{_BRAND_MAIL_NAME}</strong><br>
<span style="font-size:12px;color:#64748b;">Este e-mail foi enviado automaticamente; por favor não responda a esta mensagem.</span>
</p>
</td>
</tr>
</table>
<p style="max-width:560px;margin:16px auto 0 auto;font-size:11px;color:#52525b;text-align:center;line-height:1.4;">
Well Paid — gestão financeira pensada para si.
</p>
</td>
</tr>
</table>
</body>
</html>
"""


def send_password_reset_email(to_email: str, raw_token: str) -> bool:
    """Envia e-mail HTML + texto simples com o token de recuperação."""
    settings = get_settings()
    host = (settings.smtp_host or "").strip()
    if not host:
        return False
    mail_from = (settings.mail_from or settings.smtp_user or "").strip()
    if not mail_from:
        logger.warning("SMTP configurado mas mail_from/smtp_user em falta; e-mail não enviado.")
        return False

    msg = EmailMessage()
    msg["Subject"] = "Well Paid — recuperação de palavra-passe"
    msg["From"] = _brand_from_header(mail_from)
    msg["To"] = to_email
    msg.set_content(_password_reset_plain_text(raw_token))
    msg.add_alternative(_password_reset_html(raw_token), subtype="html")

    try:
        context = ssl.create_default_context()
        port = int(settings.smtp_port)
        user = (settings.smtp_user or "").strip()
        password = (settings.smtp_password or "").strip()
        if port == 465:
            with smtplib.SMTP_SSL(host, port, context=context, timeout=30) as server:
                if user and password:
                    server.login(user, password)
                server.send_message(msg)
        else:
            with smtplib.SMTP(host, port, timeout=30) as server:
                server.ehlo()
                if server.has_extn("STARTTLS"):
                    server.starttls(context=context)
                    server.ehlo()
                if user and password:
                    server.login(user, password)
                server.send_message(msg)
        return True
    except Exception as e:
        logger.exception("Falha ao enviar e-mail de recuperação: %s", e)
        return False
