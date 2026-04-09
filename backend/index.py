"""Ponto de entrada para plataformas que esperam `index.py` na raiz do deploy (ex.: Vercel).

O servidor local continua a usar: `uvicorn app.main:app --reload`
"""

from app.main import app

__all__ = ["app"]
