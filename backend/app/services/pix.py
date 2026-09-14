import logging
import uuid

import httpx

from app.core.config import get_settings
from app.services.module_catalog import BASIC_PLAN_CENTS

logger = logging.getLogger(__name__)

_MP_PAYMENTS = "https://api.mercadopago.com/v1/payments"


def generate_pix(
    *,
    user_id: uuid.UUID,
    email: str,
    payer_name: str,
) -> dict[str, str | None]:
    """Returns pix_id, pix_copy, pix_qr. Empty strings if MP is not configured."""
    settings = get_settings()
    token = (settings.mercado_pago_access_token or "").strip()
    name = (payer_name or email).strip() or email
    if not token:
        return {"pix_id": None, "pix_copy": None, "pix_qr": None}

    payload = {
        "transaction_amount": BASIC_PLAN_CENTS / 100,
        "description": f"Well Paid — {name} ({email})",
        "payment_method_id": "pix",
        "payer": {
            "email": email,
            "first_name": name[:80],
        },
        "external_reference": str(user_id),
        "metadata": {"user_id": str(user_id), "email": email},
    }
    try:
        res = httpx.post(
            _MP_PAYMENTS,
            json=payload,
            headers={
                "Authorization": f"Bearer {token}",
                "X-Idempotency-Key": str(uuid.uuid4()),
            },
            timeout=20.0,
        )
        res.raise_for_status()
        data = res.json()
    except Exception:
        logger.exception("Mercado Pago PIX failed")
        raise

    tx = data.get("point_of_interaction", {}).get("transaction_data", {}) or {}
    return {
        "pix_id": str(data.get("id") or "") or None,
        "pix_copy": tx.get("qr_code"),
        "pix_qr": tx.get("qr_code_base64"),
    }
