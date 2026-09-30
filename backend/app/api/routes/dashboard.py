"""Rotas do dashboard (Telas.txt §5.4).

Agregações a partir de `expenses`/`categories`; JWT obrigatório.
Listas completas: futuro GET /expenses (§6).
"""

from datetime import date
from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session

from app.api.deps import require_module
from app.core.database import get_db
from app.models.user import User
from app.schemas.dashboard import (
    DashboardAttentionItem,
    DashboardCashflowResponse,
    DashboardChange,
    DashboardOverviewResponse,
    DashboardSnapshotResponse,
)
from app.services.dashboard import get_dashboard_overview
from app.services.dashboard_cashflow import get_dashboard_cashflow

router = APIRouter(prefix="/dashboard", tags=["dashboard"])


def _previous_month(year: int, month: int) -> tuple[int, int]:
    return (year - 1, 12) if month == 1 else (year, month - 1)


def _change(current: int, previous: int) -> DashboardChange:
    delta = current - previous
    percent = round(delta / abs(previous) * 100, 1) if previous else None
    return DashboardChange(
        current_cents=current,
        previous_cents=previous,
        delta_cents=delta,
        delta_percent=percent,
    )


@router.get(
    "/snapshot",
    response_model=DashboardSnapshotResponse,
    summary="Snapshot completo do dashboard",
)
def read_dashboard_snapshot(
    user: Annotated[User, Depends(require_module("dashboard"))],
    db: Annotated[Session, Depends(get_db)],
    year: Annotated[int, Query(ge=2000, le=2100)],
    month: Annotated[int, Query(ge=1, le=12)],
) -> DashboardSnapshotResponse:
    previous_year, previous_month = _previous_month(year, month)
    overview = get_dashboard_overview(db, user, year, month)
    previous = get_dashboard_overview(db, user, previous_year, previous_month)
    cashflow = get_dashboard_cashflow(db, user, dynamic=True, forecast_months=3)
    attention: list[DashboardAttentionItem] = []
    today = date.today()
    overdue = [
        item
        for item in overview.pending_preview
        if item.due_date and item.due_date < today
    ]
    upcoming = [
        item
        for item in overview.upcoming_due
        if item.due_date and 0 <= (item.due_date - today).days <= 5
    ]
    if overview.month_balance_cents < 0:
        attention.append(DashboardAttentionItem(key="negative_balance", title="Saldo mensal negativo", detail="As despesas já ultrapassaram as receitas deste mês.", tone="danger", href="/app/despesas"))
    if overdue:
        attention.append(DashboardAttentionItem(key="overdue", title=f"{len(overdue)} conta(s) vencida(s)", detail="Revise os pagamentos pendentes para evitar encargos.", tone="danger", href="/app/despesas?filtro=pagar"))
    elif upcoming:
        attention.append(DashboardAttentionItem(key="upcoming", title=f"{len(upcoming)} vencimento(s) próximo(s)", detail="Há compromissos previstos para os próximos cinco dias.", tone="warning", href="/app/despesas?filtro=pagar"))
    expense_change = _change(overview.month_expense_total_cents, previous.month_expense_total_cents)
    if expense_change.delta_percent is not None and expense_change.delta_percent >= 15:
        attention.append(DashboardAttentionItem(key="expense_growth", title="Despesas em crescimento", detail=f"O total está {expense_change.delta_percent:.1f}% acima do mês anterior.", tone="warning", href="/app/despesas"))
    if not attention:
        attention.append(DashboardAttentionItem(key="healthy", title="Nenhum alerta importante", detail="Seu mês não apresenta situações críticas neste momento.", tone="positive"))
    return DashboardSnapshotResponse(
        overview=overview,
        previous_overview=previous,
        cashflow=cashflow,
        income_change=_change(overview.month_income_cents, previous.month_income_cents),
        expense_change=expense_change,
        balance_change=_change(overview.month_balance_cents, previous.month_balance_cents),
        attention=attention,
    )


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
    user: Annotated[User, Depends(require_module("dashboard"))],
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
    user: Annotated[User, Depends(require_module("dashboard"))],
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
