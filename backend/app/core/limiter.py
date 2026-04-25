import inspect

from slowapi import Limiter
import slowapi.extension as slowapi_extension
from slowapi.util import get_remote_address

slowapi_extension.asyncio.iscoroutinefunction = inspect.iscoroutinefunction

limiter = Limiter(key_func=get_remote_address)
