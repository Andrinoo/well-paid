import secrets

# Crockford-ish: no I, L, O, U — easier to read aloud and type.
_ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
PUBLIC_ID_PREFIX = "WP"
PUBLIC_ID_BODY_LEN = 8
PUBLIC_ID_LEN = len(PUBLIC_ID_PREFIX) + PUBLIC_ID_BODY_LEN


def new_public_id() -> str:
    body = "".join(secrets.choice(_ALPHABET) for _ in range(PUBLIC_ID_BODY_LEN))
    return f"{PUBLIC_ID_PREFIX}{body}"


def normalize_public_id(raw: str) -> str:
    return raw.strip().upper().replace(" ", "")


def looks_like_public_id(raw: str) -> bool:
    value = normalize_public_id(raw)
    if len(value) != PUBLIC_ID_LEN or not value.startswith(PUBLIC_ID_PREFIX):
        return False
    return all(ch in _ALPHABET for ch in value[len(PUBLIC_ID_PREFIX) :])
