"""Rotas do dashboard (Telas.txt §5.4).

Agregações a partir de `expenses`/`categories`; JWT obrigatório.
Listas completas: futuro GET /expenses (§6).
"""

from typing import Annotated

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.api.deps import get_current_user
from app.core.database import get_db
from app.models.user import User
from app.schemas.dashboard import DashboardOverviewResponse
from app.services.dashboard import get_dashboard_overview

router = APIRouter(prefix="/dashboard", tags=["dashboard"])


@router.get(
    "/overview",
    response_model=DashboardOverviewResponse,
    summary="Resumo do dashboard (mês)",
    description=(
        "Agrega despesas por categoria no mês, totais pendentes, pré-visualização de pendentes, "
        "próximos vencimentos e resumo de metas. Requer Bearer JWT."
    ),
)
def read_dashboard_overview(
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
    year: Annotated[int, Query(ge=2000, le=2100, description="Ano civil")],
    month: Annotated[int, Query(ge=1, le=12, description="Mês 1–12")],
) -> DashboardOverviewResponse:
    return get_dashboard_overview(db, user, year, month)
