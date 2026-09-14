from __future__ import annotations

from fastapi import APIRouter, HTTPException, Query, Request, status
from fastapi.responses import Response

from app.core.limiter import limiter
from app.services.thumbnail_proxy import fetch_thumbnail_bytes, is_allowed_thumbnail_url

router = APIRouter(prefix="/media", tags=["media"])


@router.get("/thumbnail")
@limiter.limit("60/minute")
def thumbnail_proxy(
    request: Request,
    u: str = Query(..., min_length=8, max_length=2048),
) -> Response:
    _ = request
    if not is_allowed_thumbnail_url(u):
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Imagem indisponível.")
    fetched = fetch_thumbnail_bytes(u)
    if fetched is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Imagem indisponível.")
    body, ctype = fetched
    return Response(
        content=body,
        media_type=ctype,
        headers={
            "Cache-Control": "public, max-age=86400",
            "X-Content-Type-Options": "nosniff",
        },
    )
