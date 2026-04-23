package com.wellpaid.core.model.investment

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InvestmentBucketDto(
    val key: String,
    val label: String,
    @SerialName("allocated_cents") val allocatedCents: Int,
    @SerialName("yield_cents") val yieldCents: Int,
    @SerialName("yield_pct_month") val yieldPctMonth: Float,
)

@Serializable
data class InvestmentOverviewDto(
    @SerialName("total_allocated_cents") val totalAllocatedCents: Int,
    @SerialName("total_yield_cents") val totalYieldCents: Int,
    @SerialName("estimated_monthly_yield_cents") val estimatedMonthlyYieldCents: Int,
    @SerialName("rates_source") val ratesSource: String = "fallback_default",
    @SerialName("rates_fallback_used") val ratesFallbackUsed: Boolean = true,
    val buckets: List<InvestmentBucketDto> = emptyList(),
)

@Serializable
data class InvestmentEvolutionPointDto(
    val year: Int,
    val month: Int,
    @SerialName("projected_total_cents") val projectedTotalCents: Int,
    @SerialName("cumulative_yield_cents") val cumulativeYieldCents: Int,
    @SerialName("is_estimated") val isEstimated: Boolean = false,
)

@Serializable
data class InvestmentPositionDto(
    val id: String,
    @SerialName("instrument_type") val instrumentType: String,
    val name: String,
    val description: String? = null,
    @SerialName("principal_cents") val principalCents: Int,
    @SerialName("annual_rate_bps") val annualRateBps: Int,
    @SerialName("maturity_date") val maturityDate: String? = null,
    @SerialName("is_liquid") val isLiquid: Boolean = true,
)

@Serializable
data class InvestmentPositionCreateDto(
    @SerialName("instrument_type") val instrumentType: String,
    val name: String,
    val description: String? = null,
    @SerialName("principal_cents") val principalCents: Int,
    @SerialName("annual_rate_bps") val annualRateBps: Int,
    @SerialName("maturity_date") val maturityDate: String? = null,
    @SerialName("is_liquid") val isLiquid: Boolean = true,
)

@Serializable
data class InvestmentPositionAddPrincipalDto(
    @SerialName("add_principal_cents") val addPrincipalCents: Int,
)

@Serializable
data class InvestmentSuggestedRatesDto(
    @SerialName("cdi_annual_percent") val cdiAnnualPercent: Double,
    @SerialName("cdb_annual_percent") val cdbAnnualPercent: Double,
    @SerialName("fixed_income_annual_percent") val fixedIncomeAnnualPercent: Double,
    @SerialName("source") val source: String = "fallback_default",
    @SerialName("rates_fallback_used") val ratesFallbackUsed: Boolean = true,
)

@Serializable
data class StockQuoteDto(
    val symbol: String,
    @SerialName("last_price") val lastPrice: Double = 0.0,
    val currency: String = "BRL",
    @SerialName("as_of") val asOf: String? = null,
    val source: String = "brapi",
    val confidence: Double? = null,
    val error: String? = null,
)

@Serializable
data class TickerSearchItemDto(
    val symbol: String,
    val name: String,
    @SerialName("instrument_type") val instrumentType: String = "stocks",
    val source: String = "unknown",
    val confidence: Double? = null,
)

@Serializable
data class StockHistoryPointDto(
    val close: Double = 0.0,
    @SerialName("as_of") val asOf: String? = null,
)

@Serializable
data class StockHistoryDto(
    val symbol: String,
    val range: String,
    val points: List<StockHistoryPointDto> = emptyList(),
    val source: String = "brapi",
    val confidence: Double? = null,
    val error: String? = null,
)

@Serializable
data class MacroSnapshotDto(
    val cdi: Double? = null,
    val selic: Double? = null,
    val ipca: Double? = null,
    val source: String = "sgs",
    val confidence: Double? = null,
)

@Serializable
data class EquityFundamentalsDto(
    val symbol: String,
    @SerialName("company_name") val companyName: String? = null,
    val pl: String? = null,
    val pvp: String? = null,
    @SerialName("dividend_yield") val dividendYield: String? = null,
    val roe: String? = null,
    @SerialName("ev_ebitda") val evEbitda: String? = null,
    @SerialName("net_margin") val netMargin: String? = null,
    @SerialName("net_debt_ebitda") val netDebtEbitda: String? = null,
    val eps: String? = null,
    val source: String = "fundamentus",
    val confidence: Double? = null,
)

@Serializable
data class TopMoverItemDto(
    val symbol: String,
    val name: String,
    @SerialName("change_percent") val changePercent: Double = 0.0,
    val volume: Double = 0.0,
    val window: String,
    val source: String = "brapi",
    val confidence: Double? = null,
)
