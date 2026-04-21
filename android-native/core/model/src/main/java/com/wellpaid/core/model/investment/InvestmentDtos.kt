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
    @SerialName("principal_cents") val principalCents: Int,
    @SerialName("annual_rate_bps") val annualRateBps: Int,
    @SerialName("maturity_date") val maturityDate: String? = null,
    @SerialName("is_liquid") val isLiquid: Boolean = true,
)

@Serializable
data class InvestmentPositionCreateDto(
    @SerialName("instrument_type") val instrumentType: String,
    val name: String,
    @SerialName("principal_cents") val principalCents: Int,
    @SerialName("annual_rate_bps") val annualRateBps: Int,
    @SerialName("maturity_date") val maturityDate: String? = null,
    @SerialName("is_liquid") val isLiquid: Boolean = true,
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
    val error: String? = null,
)
