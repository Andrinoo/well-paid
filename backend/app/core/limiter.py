import inspect

from fastapi import Request
from slowapi import Limiter
import slowapi.extension as slowapi_extension

slowapi_extension.asyncio.iscoroutinefunction = inspect.iscoroutinefunction


def rate_limit_key(request: Request) -> str:
    forwarded = (request.headers.get("x-forwarded-for") or "").strip()
    if forwarded:
        return forwarded.split(",")[0].strip() or "unknown"
    cf = (request.headers.get("cf-connecting-ip") or "").strip()
    if cf:
        return cf
    if request.client and request.client.host:
        return request.client.host
    return "unknown"


limiter = Limiter(key_func=rate_limit_key)
