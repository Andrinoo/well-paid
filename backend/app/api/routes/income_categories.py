from __future__ import annotations

import uuid as uuid_lib
from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import func, or_, select
from sqlalchemy.orm import Session

from app.api.deps import get_current_user
from app.core.database import get_db
from app.models.income_category import IncomeCategory
from app.models.user import User
from app.schemas.income_category import IncomeCategoryCreate, IncomeCategoryResponse

router = APIRouter(prefix="/income-categories", tags=["income-categories"])


@router.get("", response_model=list[IncomeCategoryResponse])
def list_income_categories(
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> list[IncomeCategory]:
    stmt = (
        select(IncomeCategory)
        .where(or_(IncomeCategory.user_id.is_(None), IncomeCategory.user_id == user.id))
        .order_by(IncomeCategory.sort_order.asc(), IncomeCategory.name.asc())
    )
    return list(db.scalars(stmt).all())


@router.post("", response_model=IncomeCategoryResponse, status_code=status.HTTP_201_CREATED)
def create_income_category(
    body: IncomeCategoryCreate,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> IncomeCategory:
    name = body.name.strip()
    if not name:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Name is required",
        )
    max_so = (
        db.scalar(select(func.coalesce(func.max(IncomeCategory.sort_order), 0))) or 0
    )
    key: str | None = None
    for _ in range(8):
        candidate = f"iu{user.id.hex}_{uuid_lib.uuid4().hex}"[:64]
        exists = db.scalar(select(IncomeCategory.id).where(IncomeCategory.key == candidate))
        if exists is None:
            key = candidate
            break
    if key is None:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Could not allocate income category key",
        )
    row = IncomeCategory(
        key=key,
        name=name[:100],
        sort_order=int(max_so) + 1,
        user_id=user.id,
    )
    db.add(row)
    db.commit()
    db.refresh(row)
    return row
