from __future__ import annotations

import uuid as uuid_lib
from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import func, or_, select
from sqlalchemy.orm import Session

from app.api.deps import get_current_user
from app.core.database import get_db
from app.models.category import Category
from app.models.user import User
from app.schemas.category_public import CategoryCreate, CategoryPublic

router = APIRouter(prefix="/categories", tags=["categories"])


@router.get("", response_model=list[CategoryPublic])
def list_categories(
    _user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> list[Category]:
    """Categorias do sistema (seed) + categorias personalizadas do utilizador."""
    stmt = (
        select(Category)
        .where(or_(Category.user_id.is_(None), Category.user_id == _user.id))
        .order_by(Category.sort_order.asc(), Category.name.asc())
    )
    return list(db.scalars(stmt).all())


@router.post("", response_model=CategoryPublic, status_code=status.HTTP_201_CREATED)
def create_category(
    body: CategoryCreate,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> Category:
    name = body.name.strip()
    if not name:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Name is required",
        )
    max_so = db.scalar(select(func.coalesce(func.max(Category.sort_order), 0))) or 0
    key: str | None = None
    for _ in range(8):
        candidate = f"u{user.id.hex}_{uuid_lib.uuid4().hex}"[:64]
        exists = db.scalar(select(Category.id).where(Category.key == candidate))
        if exists is None:
            key = candidate
            break
    if key is None:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Could not allocate category key",
        )
    row = Category(
        key=key,
        name=name[:100],
        sort_order=int(max_so) + 1,
        user_id=user.id,
    )
    db.add(row)
    db.commit()
    db.refresh(row)
    return row
