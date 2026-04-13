"""Rotas do dashboard (Telas.txt §5.4).

Agregações a partir de `expenses`/`categories`; JWT obrigatório.
Listas completas: futuro GET /expenses (§6).
"""

from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session

from app.api.deps import get_current_user
from app.core.database import get_db
from app.models.user import User
from app.schemas.dashboard import DashboardCashflowResponse, DashboardOverviewResponse
from app.services.dashboard import get_dashboard_overview
from app.services.dashboard_cashflow import get_dashboard_cashflow

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


@router.get(
    "/cashflow",
    response_model=DashboardCashflowResponse,
    summary="Série mensal (histórico + previsão)",
    description=(
        "Proventos, despesas pagas e despesas pendentes por mês civil, para o gráfico "
        "Histórico mensal (Ordems §6.2.1). Com dynamic=true, o intervalo histórico é fixado no servidor."
    ),
)
def read_dashboard_cashflow(
    user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
    dynamic: Annotated[
        bool,
        Query(description="Se true, ignora start_* / end_* e usa janela móvel de 8 meses"),
    ] = False,
    start_year: Annotated[int | None, Query(ge=2000, le=2100)] = None,
    start_month: Annotated[int | None, Query(ge=1, le=12)] = None,
    end_year: Annotated[int | None, Query(ge=2000, le=2100)] = None,
    end_month: Annotated[int | None, Query(ge=1, le=12)] = None,
    forecast_months: Annotated[int, Query(ge=1, le=12)] = 3,
) -> DashboardCashflowResponse:
    try:
        return get_dashboard_cashflow(
            db,
            user,
            dynamic=dynamic,
            start_year=start_year,
            start_month=start_month,
            end_year=end_year,
            end_month=end_month,
            forecast_months=forecast_months,
        )
    except ValueError as exc:
        code = str(exc)
        if code == "cashflow_manual_range_incomplete":
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Com dynamic=false, indique start_year, start_month, end_year e end_month.",
            ) from exc
        if code == "cashflow_invalid_range":
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Intervalo inválido: o mês inicial não pode ser posterior ao mês final.",
            ) from exc
        raise
