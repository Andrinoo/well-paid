"""Ponto de entrada para plataformas que esperam `index.py` na raiz do deploy (ex.: Vercel).

O servidor local continua a usar: `uvicorn app.main:app --reload`
"""

import sys
import traceback

try:
    from app.main import app
except Exception:
    traceback.print_exc(file=sys.stderr)
    sys.stderr.write(
        "well-paid: falha ao importar app.main (ver traceback acima nos logs da Vercel).\n"
    )
    raise

__all__ = ["app"]
