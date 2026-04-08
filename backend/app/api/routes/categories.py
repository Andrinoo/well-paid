from typing import Annotated

from fastapi import APIRouter, Depends
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.api.deps import get_current_user
from app.core.database import get_db
from app.models.category import Category
from app.models.user import User
from app.schemas.category_public import CategoryPublic

router = APIRouter(prefix="/categories", tags=["categories"])


@router.get("", response_model=list[CategoryPublic])
def list_categories(
    _user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> list[Category]:
    """Categorias MVP (seed) para dropdown §5.6."""
    return list(
        db.scalars(select(Category).order_by(Category.sort_order.asc(), Category.name.asc()))
    )
