from pydantic import BaseModel, Field
from datetime import date


class InvestmentBucketOut(BaseModel):
    key: str
    label: str
    allocated_cents: int = Field(ge=0)
    yield_cents: int = Field(ge=0)
    yield_pct_month: float = Field(ge=0)


class InvestmentOverviewOut(BaseModel):
    total_allocated_cents: int = Field(ge=0)
    total_yield_cents: int = Field(ge=0)
    estimated_monthly_yield_cents: int = Field(ge=0)
    rates_source: str = Field(default="fallback")
    rates_fallback_used: bool = Field(default=True)
    buckets: list[InvestmentBucketOut] = Field(default_factory=list)


class InvestmentEvolutionPointOut(BaseModel):
    year: int
    month: int
    projected_total_cents: int = Field(ge=0)
    cumulative_yield_cents: int = Field(ge=0)
    is_estimated: bool = Field(default=False)


class InvestmentPositionCreate(BaseModel):
    instrument_type: str = Field(min_length=2, max_length=32)
    name: str = Field(min_length=2, max_length=180)
    principal_cents: int = Field(gt=0)
    annual_rate_bps: int = Field(ge=0, le=100000)
    maturity_date: date | None = None
    is_liquid: bool = True


class InvestmentPositionOut(BaseModel):
    id: str
    instrument_type: str
    name: str
    principal_cents: int = Field(ge=0)
    annual_rate_bps: int = Field(ge=0)
    maturity_date: date | None = None
    is_liquid: bool = True
