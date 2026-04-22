from typing import Annotated
import logging

from fastapi import APIRouter, Depends, HTTPException, Query, Request, Response, status
from sqlalchemy.orm import Session

from app.api.deps import get_current_user
from app.core.database import get_db
from app.core.limiter import limiter
from app.models.user import User
from app.schemas.investments import (
    EquityFundamentalsOut,
    InvestmentEvolutionPointOut,
    InvestmentOverviewOut,
    InvestmentPositionCreate,
    InvestmentPositionOut,
    InvestmentSuggestedRatesOut,
    MacroSnapshotOut,
    StockHistoryOut,
    StockQuoteOut,
    TickerSearchItemOut,
    TopMoverItemOut,
)
from app.services.investment_market_rates import get_suggested_annual_rates
from app.services.market_data_router import market_data_router
from app.services.investments import (
    create_position_for_user,
    delete_position_for_user,
    get_investment_evolution_for_user,
    get_investment_overview_for_user,
    list_positions_for_user,
)
from app.services.ticker_cache import ticker_cache_service

router = APIRouter(prefix="/investments", tags=["investments"])
logger = logging.getLogger(__name__)


@router.get("/suggested-rates", response_model=InvestmentSuggestedRatesOut)
@limiter.limit("60/minute")
def read_suggested_annual_rates(
    request: Request,
    user: Annotated[User, Depends(get_current_user)],
) -> InvestmentSuggestedRatesOut:
    s = get_suggested_annual_rates()
    return InvestmentSuggestedRatesOut(
        cdi_annual_percent=float(s["cdi_annual_percent"]),
        cdb_annual_percent=float(s["cdb_annual_percent"]),
        fixed_income_annual_percent=float(s["fixed_income_annual_percent"]),
        source=str(s.get("source") or "fallback_default"),
        rates_fallback_used=bool(s.get("rates_fallback_used", True)),
    )


@router.get("/quote", response_model=StockQuoteOut)
@limiter.limit("30/minute")
def read_stock_quote(
    request: Request,
    user: Annotated[User, Depends(get_current_user)],
    symbol: Annotated[str, Query(min_length=1, max_length=12, description="Ticker B3, ex. PETR4")],
) -> StockQuoteOut:
    try:
        raw = market_data_router.quote(symbol)
    except ValueError:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Ticker inválido")
    if raw is None:
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail="Falha ao consultar cotação.",
        )
    if raw.get("error") is not None or raw.get("last_price") is None:
        return StockQuoteOut(
            symbol=symbol.strip().upper(),
            last_price=0.0,
            error=str(raw.get("error") or "quote_unavailable"),
            source=str(raw.get("source") or "brapi"),
            confidence=raw.get("confidence"),
        )
    return StockQuoteOut(
        symbol=symbol.strip().upper(),
        last_price=float(raw["last_price"]),
        as_of=raw.get("as_of"),
        source=str(raw.get("source") or "brapi"),
        confidence=raw.get("confidence"),
        error=None,
    )


@router.get("/quote/history", response_model=StockHistoryOut)
@limiter.limit("30/minute")
def read_stock_quote_history(
    request: Request,
    user: Annotated[User, Depends(get_current_user)],
    symbol: Annotated[str, Query(min_length=1, max_length=12, description="Ticker B3, ex. PETR4")],
    range: Annotated[str, Query(min_length=2, max_length=8, description="5m,30m,60m,3h,12h,1d,1w,1m,3m,6m,1y")] = "1m",
) -> StockHistoryOut:
    try:
        raw = market_data_router.history(symbol=symbol, range_key=range)
    except ValueError as exc:
        detail = "Ticker inválido" if str(exc) == "ticker_invalid" else "Range inválido"
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=detail)
    if raw is None:
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail="Falha ao consultar histórico.",
        )
    return StockHistoryOut(
        symbol=str(raw.get("symbol") or symbol.strip().upper()),
        range=range,
        points=list(raw.get("points") or []),
        source=str(raw.get("source") or "brapi"),
        confidence=raw.get("confidence"),
        error=raw.get("error"),
    )


@router.get("/tickers/search", response_model=list[TickerSearchItemOut])
@limiter.limit("60/minute")
def search_tickers(
    request: Request,
    user: Annotated[User, Depends(get_current_user)],
    q: Annotated[str, Query(min_length=2, max_length=24)],
    limit: Annotated[int, Query(ge=1, le=50)] = 12,
) -> list[TickerSearchItemOut]:
    rows = ticker_cache_service.search(q, limit=limit)
    return [
        TickerSearchItemOut(
            symbol=r["symbol"],
            name=r["name"],
            instrument_type=str(r.get("instrument_type") or "stocks"),
            source=str(r.get("source") or "unknown"),
            confidence=r.get("confidence"),
        )
        for r in rows
    ]


@router.get("/tickers/top-movers", response_model=list[TopMoverItemOut])
@limiter.limit("60/minute")
def read_top_movers(
    request: Request,
    user: Annotated[User, Depends(get_current_user)],
    window: Annotated[str, Query(min_length=3, max_length=8, description="hour|day|week")] = "day",
    limit: Annotated[int, Query(ge=1, le=30)] = 10,
) -> list[TopMoverItemOut]:
    cached = ticker_cache_service.get_top_movers(window=window, limit=limit)
    if cached is not None:
        rows = cached
    else:
        try:
            rows = market_data_router.top_movers(window=window, limit=limit)
        except ValueError:
            raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Janela inválida")
        ticker_cache_service.set_top_movers(window=window, rows=rows)
    return [
        TopMoverItemOut(
            symbol=str(r.get("symbol") or ""),
            name=str(r.get("name") or r.get("symbol") or ""),
            change_percent=float(r.get("change_percent") or 0.0),
            volume=float(r.get("volume") or 0.0),
            window=str(r.get("window") or window),
            source=str(r.get("source") or "unknown"),
            confidence=r.get("confidence"),
        )
        for r in rows
    ]


@router.get("/macro/snapshot", response_model=MacroSnapshotOut)
@limiter.limit("30/minute")
def read_macro_snapshot(
    request: Request,
    user: Annotated[User, Depends(get_current_user)],
) -> MacroSnapshotOut:
    data = market_data_router.macro_snapshot()
    return MacroSnapshotOut(
        cdi=data.get("cdi"),
        selic=data.get("selic"),
        ipca=data.get("ipca"),
        source=str(data.get("source") or "sgs"),
        confidence=data.get("confidence"),
    )


@router.get("/fundamentals", response_model=EquityFundamentalsOut)
@limiter.limit("30/minute")
def read_equity_fundamentals(
    request: Request,
    user: Annotated[User, Depends(get_current_user)],
    symbol: Annotated[str, Query(min_length=1, max_length=12, description="Ticker B3, ex. PETR4")],
) -> EquityFundamentalsOut:
    try:
        data = market_data_router.fundamentals(symbol)
    except ValueError:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Ticker inválido")
    if data is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Fundamentos indisponíveis")
    return EquityFundamentalsOut(
        symbol=str(data.get("symbol") or symbol.strip().upper()),
        pl=data.get("pl"),
        pvp=data.get("pvp"),
        dividend_yield=data.get("dividend_yield"),
        roe=data.get("roe"),
        source=str(data.get("source") or "fundamentus"),
        confidence=data.get("confidence"),
    )


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
