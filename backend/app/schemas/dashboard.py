"""Contrato REST do dashboard (Telas.txt §5.4, §6).

Valores monetários: int em centavos (Ordems 1.md §4.1). Datas: ISO date (YYYY-MM-DD).

Esboço mínimo de persistência (Fase 2 — alinhar a §5.5–5.6):
- **categories**: id (UUID), key (str único, ex. alimentacao), name (str), sort_order (int).
- **expenses**: id, owner_user_id (UUID FK users; família/partilha pode vir depois com family_id),
  description (str), amount_cents (int), expense_date (date competência), due_date (date | null),
  status (pending | paid), category_id (FK), created_at, updated_at, sync_status (int, offline).
Índices úteis: (owner_user_id, expense_date), (owner_user_id, status, due_date).
"""

from datetime import date
from enum import StrEnum
from uuid import UUID

from pydantic import BaseModel, Field


class ExpenseStatus(StrEnum):
    """Estado da despesa; alinhado a §5.5 (Pagas / Pendentes)."""

    PENDING = "pending"
    PAID = "paid"


class PeriodMonth(BaseModel):
    year: int
    month: int


class CategorySpend(BaseModel):
    """Fatia do donut: categoria com gasto > 0 no período."""

    category_key: str = Field(description="Chave estável, ex. alimentacao, outros")
    name: str = Field(description="Nome para UI")
    amount_cents: int = Field(ge=0, description="Total da categoria no período (centavos)")
    share_bps: int | None = Field(
        default=None,
        ge=0,
        le=10000,
        description="Opcional: parte do total de despesas do mês em basis points (10000 = 100%)",
    )


class PendingExpenseItem(BaseModel):
    """Item para lista curta 'A pagar' ou 'Próximos vencimentos'."""

    id: UUID
    description: str
    amount_cents: int = Field(ge=0)
    due_date: date | None = Field(
        default=None,
        description="Vencimento; null se não aplicável",
    )


class GoalSummaryItem(BaseModel):
    """Resumo de meta ativa para o bloco Metas (§5.4)."""

    id: UUID
    title: str
    current_cents: int = Field(ge=0)
    target_cents: int = Field(gt=0)


class DashboardOverviewResponse(BaseModel):
    """Resposta agregada principal: um round-trip para o ecrã Dashboard."""

    period: PeriodMonth
    month_income_cents: int = Field(
        default=0,
        description="Receitas do mês (MVP pode ser 0 até existir modelo de receitas)",
    )
    month_expense_total_cents: int = Field(
        ge=0,
        description="Soma das despesas do período (competência), centavos",
    )
    month_balance_cents: int = Field(
        description="month_income_cents - month_expense_total_cents",
    )
    spending_by_category: list[CategorySpend]
    pending_total_cents: int = Field(
        ge=0,
        description="Soma de despesas pendentes não quitadas (escopo do utilizador)",
    )
    pending_preview: list[PendingExpenseItem] = Field(
        default_factory=list,
        description="Até ~5 itens pendentes para o card 'A pagar'",
    )
    upcoming_due: list[PendingExpenseItem] = Field(
        default_factory=list,
        description="Pendentes com due_date >= hoje, mais próximas primeiro",
    )
    goals_preview: list[GoalSummaryItem] = Field(
        default_factory=list,
        description="Metas ativas (curto); vazio com TODO no cliente se API /goals ainda não existir",
    )
