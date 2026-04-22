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


class InvestmentSuggestedRatesOut(BaseModel):
    """Taxas anuais sugeridas a partir do CDI (BACEN SGS) e fatores de configuração."""

    cdi_annual_percent: float = Field(ge=0, description="Ex.: 10.5 significa 10,5% ao ano (nominal, decomposto do CDI).")
    cdb_annual_percent: float = Field(ge=0)
    fixed_income_annual_percent: float = Field(ge=0)
    source: str = "fallback"
    rates_fallback_used: bool = True


class StockQuoteOut(BaseModel):
    symbol: str
    last_price: float = Field(ge=0)
    currency: str = "BRL"
    as_of: str | None = None
    source: str = "brapi"
    confidence: float | None = Field(default=None, ge=0, le=1)
    error: str | None = None


class TickerSearchItemOut(BaseModel):
    symbol: str
    name: str
    instrument_type: str = "stocks"
    source: str = "unknown"
    confidence: float | None = Field(default=None, ge=0, le=1)


class StockHistoryPointOut(BaseModel):
    close: float = Field(ge=0)
    as_of: str | None = None


class StockHistoryOut(BaseModel):
    symbol: str
    range: str
    points: list[StockHistoryPointOut] = Field(default_factory=list)
    source: str = "brapi"
    confidence: float | None = Field(default=None, ge=0, le=1)
    error: str | None = None


class MacroSnapshotOut(BaseModel):
    cdi: float | None = None
    selic: float | None = None
    ipca: float | None = None
    source: str = "sgs"
    confidence: float | None = Field(default=None, ge=0, le=1)


class EquityFundamentalsOut(BaseModel):
    symbol: str
    pl: str | None = None
    pvp: str | None = None
    dividend_yield: str | None = None
    roe: str | None = None
    source: str = "fundamentus"
    confidence: float | None = Field(default=None, ge=0, le=1)
