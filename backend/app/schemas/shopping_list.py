import uuid
from datetime import date, datetime
from typing import Self

from pydantic import BaseModel, ConfigDict, Field, field_validator, model_validator

from app.schemas.dashboard import ExpenseStatus


class ShoppingListCreate(BaseModel):
    title: str | None = Field(default=None, max_length=200)
    store_name: str | None = Field(default=None, max_length=200)
    is_family: bool = False

    @field_validator("title", "store_name", mode="before")
    @classmethod
    def empty_to_none(cls, v: str | None) -> str | None:
        if v is None:
            return None
        if isinstance(v, str):
            s = v.strip()
            return s if s else None
        return v


class ShoppingListPatch(BaseModel):
    title: str | None = Field(default=None, max_length=200)
    store_name: str | None = Field(default=None, max_length=200)
    is_family: bool | None = None
    sync_total_from_line_items: bool | None = Field(
        default=None,
        description="Só em listas concluídas: recalcula total_cents e amount_cents da despesa a partir das linhas com preço.",
    )

    @field_validator("title", "store_name", mode="before")
    @classmethod
    def empty_to_none(cls, v: str | None) -> str | None:
        if v is None:
            return None
        if isinstance(v, str):
            s = v.strip()
            return s if s else None
        return v


class ShoppingListItemCreate(BaseModel):
    label: str = Field(min_length=1, max_length=500)
    quantity: int = Field(default=1, ge=1, le=9999)
    line_amount_cents: int | None = Field(
        default=None,
        gt=0,
        description="Preço unitário em centavos (total da linha = unitário × quantidade)",
    )
    is_picked: bool = Field(
        default=True,
        description="Se o item já foi pego no mercado. Omitido em clientes antigos: assume true.",
    )

    @field_validator("label")
    @classmethod
    def strip_label(cls, v: str) -> str:
        return v.strip()


class ShoppingListItemPatch(BaseModel):
    label: str | None = Field(default=None, min_length=1, max_length=500)
    quantity: int | None = Field(default=None, ge=1, le=9999)
    line_amount_cents: int | None = Field(
        default=None,
        description="Preço unitário em centavos",
    )
    sort_order: int | None = Field(default=None, ge=0)
    is_picked: bool | None = None

    @field_validator("label")
    @classmethod
    def strip_label(cls, v: str | None) -> str | None:
        if v is None:
            return None
        return v.strip()

    @field_validator("line_amount_cents")
    @classmethod
    def cents_positive_when_set(cls, v: int | None) -> int | None:
        if v is not None and v <= 0:
            raise ValueError("line_amount_cents deve ser positivo")
        return v


class ShoppingListItemResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    sort_order: int
    label: str
    quantity: int
    line_amount_cents: int | None = Field(
        default=None,
        description="Preço unitário em centavos; total da linha = unitário × quantidade",
    )
    is_picked: bool = True


class ShoppingListSummaryResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    owner_user_id: uuid.UUID
    is_mine: bool
    title: str | None
    store_name: str | None
    status: str
    is_family: bool = False
    completed_at: datetime | None
    expense_id: uuid.UUID | None
    total_cents: int | None
    items_count: int
    created_at: datetime
    updated_at: datetime


class ShoppingListDetailResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    owner_user_id: uuid.UUID
    is_mine: bool
    title: str | None
    store_name: str | None
    status: str
    is_family: bool = False
    completed_at: datetime | None
    expense_id: uuid.UUID | None
    total_cents: int | None
    items: list[ShoppingListItemResponse]
    created_at: datetime
    updated_at: datetime


class ShoppingListComplete(BaseModel):
    """Fechar lista: a despesa é sempre **paga**, sem parcelas/recorrência.

    `status` e `description` no corpo são ignorados (retrocompat.); a descrição
    da despesa vem só do título da lista (ou texto padrão com loja/data).
    """

    category_id: uuid.UUID
    expense_date: date
    status: ExpenseStatus = ExpenseStatus.PAID
    description: str | None = Field(default=None, max_length=500)
    total_cents: int | None = Field(
        default=None,
        gt=0,
        description="Total pago na loja (opcional). Se definido, substitui a soma das linhas e ignora desconto.",
    )
    discount_cents: int | None = Field(
        default=None,
        ge=0,
        description="Desconto em centavos (opcional). Só com total_cents omitido; subtrai da soma das linhas.",
    )
    is_shared: bool = False
    is_family: bool = False
    shared_with_user_id: uuid.UUID | None = None

    @field_validator("description", mode="before")
    @classmethod
    def empty_desc_none(cls, v: str | None) -> str | None:
        if v is None:
            return None
        if isinstance(v, str):
            s = v.strip()
            return s if s else None
        return v

    @model_validator(mode="after")
    def share_consistency(self) -> Self:
        if not self.is_shared and self.shared_with_user_id is not None:
            raise ValueError("shared_with_user_id exige is_shared true")
        return self

    @model_validator(mode="after")
    def total_vs_discount(self) -> Self:
        if self.total_cents is not None and self.discount_cents is not None and self.discount_cents > 0:
            raise ValueError("total_cents e discount_cents não podem ser usados em conjunto")
        return self


class ShoppingListGroceryPriceBody(BaseModel):
    """Sugestões de preço para mercearia (Google Shopping via SerpAPI no backend)."""

    query: str = Field(min_length=2, max_length=200)
    unit: str | None = Field(default=None, max_length=64)
