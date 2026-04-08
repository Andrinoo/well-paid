from typing import Annotated

from fastapi import APIRouter, Depends
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.api.deps import get_current_user
from app.core.database import get_db
from app.models.income_category import IncomeCategory
from app.models.user import User
from app.schemas.income_category import IncomeCategoryResponse

router = APIRouter(prefix="/income-categories", tags=["income-categories"])


@router.get("", response_model=list[IncomeCategoryResponse])
def list_income_categories(
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> list[IncomeCategory]:
    return list(
        db.scalars(
            select(IncomeCategory).order_by(
                IncomeCategory.sort_order.asc(), IncomeCategory.name.asc()
            )
        ).all()
    )
