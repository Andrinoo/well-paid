from typing import Annotated
import logging

from fastapi import APIRouter, Depends, HTTPException, Query, Request, Response, status
from sqlalchemy.orm import Session

from app.api.deps import get_current_user
from app.core.database import get_db
from app.core.limiter import limiter
from app.models.user import User
from app.schemas.investments import (
    InvestmentEvolutionPointOut,
    InvestmentOverviewOut,
    InvestmentPositionCreate,
    InvestmentPositionOut,
)
from app.services.investments import (
    create_position_for_user,
    delete_position_for_user,
    get_investment_evolution_for_user,
    get_investment_overview_for_user,
    list_positions_for_user,
)

router = APIRouter(prefix="/investments", tags=["investments"])
logger = logging.getLogger(__name__)


@router.get("/overview", response_model=InvestmentOverviewOut)
@limiter.limit("60/minute")
def read_investments_overview(
    request: Request,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> InvestmentOverviewOut:
    snapshot = get_investment_overview_for_user(db, user.id)
    logger.info(
        "investments_overview served user_id=%s source=%s fallback=%s",
        user.id,
        snapshot.rates_source,
        snapshot.rates_fallback_used,
    )
    return snapshot


@router.get("/evolution", response_model=list[InvestmentEvolutionPointOut])
@limiter.limit("60/minute")
def read_investments_evolution(
    request: Request,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
    months: Annotated[int, Query(ge=1, le=24)] = 6,
) -> list[InvestmentEvolutionPointOut]:
    points = get_investment_evolution_for_user(db, user.id, months=months)
    logger.info(
        "investments_evolution served user_id=%s months=%s points=%s",
        user.id,
        months,
        len(points),
    )
    return points


@router.get("/positions", response_model=list[InvestmentPositionOut])
@limiter.limit("60/minute")
def read_investment_positions(
    request: Request,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> list[InvestmentPositionOut]:
    return list_positions_for_user(db, user.id)


@router.post("/positions", response_model=InvestmentPositionOut, status_code=status.HTTP_201_CREATED)
@limiter.limit("30/minute")
def create_investment_position(
    request: Request,
    body: InvestmentPositionCreate,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> InvestmentPositionOut:
    try:
        return create_position_for_user(db, user.id, body)
    except ValueError as exc:
        if str(exc) == "investments_positions_unavailable":
            raise HTTPException(
                status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                detail=(
                    "Posições de investimento indisponíveis: execute "
                    "python -m alembic upgrade head"
                ),
            ) from exc
        raise


@router.delete("/positions/{position_id}", status_code=status.HTTP_204_NO_CONTENT)
@limiter.limit("30/minute")
def delete_investment_position(
    request: Request,
    position_id: str,
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> Response:
    if not delete_position_for_user(db, user.id, position_id):
        raise HTTPException(status.HTTP_404_NOT_FOUND, detail="Posição não encontrada")
    return Response(status_code=status.HTTP_204_NO_CONTENT)
