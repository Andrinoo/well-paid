package com.wellpaid.core.model.investment

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull

/**
 * Some backend responses can exceed Int range for *_cents fields.
 * This serializer is lenient and always coerces to Long.
 */
object CentsLongSerializer : KSerializer<Long> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("CentsLong", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder): Long {
        if (decoder is JsonDecoder) {
            val primitive = decoder.decodeJsonElement() as? JsonPrimitive
                ?: return decoder.decodeLong()
            primitive.longOrNull?.let { return it }
            val content = primitive.content.trim()
            if (content.isNotEmpty()) {
                content.toLongOrNull()?.let { return it }
            }
            throw IllegalArgumentException("Invalid cents value: '$content'")
        }
        return decoder.decodeLong()
    }

    override fun serialize(encoder: Encoder, value: Long) {
        encoder.encodeLong(value)
    }
}

@Serializable
data class InvestmentBucketDto(
    val key: String,
    val label: String,
    @Serializable(with = CentsLongSerializer::class)
    @SerialName("allocated_cents") val allocatedCents: Long,
    @Serializable(with = CentsLongSerializer::class)
    @SerialName("yield_cents") val yieldCents: Long,
    @SerialName("yield_pct_month") val yieldPctMonth: Float,
)

@Serializable
data class InvestmentOverviewDto(
    @Serializable(with = CentsLongSerializer::class)
    @SerialName("total_allocated_cents") val totalAllocatedCents: Long,
    @Serializable(with = CentsLongSerializer::class)
    @SerialName("total_yield_cents") val totalYieldCents: Long,
    @Serializable(with = CentsLongSerializer::class)
    @SerialName("estimated_monthly_yield_cents") val estimatedMonthlyYieldCents: Long,
    @SerialName("rates_source") val ratesSource: String = "fallback_default",
    @SerialName("rates_fallback_used") val ratesFallbackUsed: Boolean = true,
    val buckets: List<InvestmentBucketDto> = emptyList(),
)

@Serializable
data class InvestmentEvolutionPointDto(
    val year: Int,
    val month: Int,
    @Serializable(with = CentsLongSerializer::class)
    @SerialName("projected_total_cents") val projectedTotalCents: Long,
    @Serializable(with = CentsLongSerializer::class)
    @SerialName("cumulative_yield_cents") val cumulativeYieldCents: Long,
    @SerialName("is_estimated") val isEstimated: Boolean = false,
)

@Serializable
data class InvestmentPositionDto(
    val id: String,
    @SerialName("instrument_type") val instrumentType: String,
    val name: String,
    val description: String? = null,
    @Serializable(with = CentsLongSerializer::class)
    @SerialName("principal_cents") val principalCents: Long,
    @SerialName("annual_rate_bps") val annualRateBps: Int,
    @SerialName("maturity_date") val maturityDate: String? = null,
    @SerialName("is_liquid") val isLiquid: Boolean = true,
)

@Serializable
data class InvestmentPositionCreateDto(
    @SerialName("instrument_type") val instrumentType: String,
    val name: String,
    val description: String? = null,
    @Serializable(with = CentsLongSerializer::class)
    @SerialName("principal_cents") val principalCents: Long,
    @SerialName("annual_rate_bps") val annualRateBps: Int,
    @SerialName("maturity_date") val maturityDate: String? = null,
    @SerialName("is_liquid") val isLiquid: Boolean = true,
)

@Serializable
data class InvestmentPositionAddPrincipalDto(
    @Serializable(with = CentsLongSerializer::class)
    @SerialName("add_principal_cents") val addPrincipalCents: Long,
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
    @SerialName("fallback_used") val fallbackUsed: Boolean = false,
    @SerialName("provider_strategy") val providerStrategy: String = "single",
    val stale: Boolean = false,
    @SerialName("change_24h") val change24h: Double? = null,
    @SerialName("change_24h_percent") val change24hPercent: Double? = null,
    @SerialName("day_high") val dayHigh: Double? = null,
    @SerialName("day_low") val dayLow: Double? = null,
    @SerialName("volume_24h") val volume24h: Double? = null,
    val error: String? = null,
)

@Serializable
data class TickerSearchItemDto(
    val symbol: String,
    val name: String,
    @SerialName("instrument_type") val instrumentType: String = "stock",
    val source: String = "unknown",
    val confidence: Double? = null,
    @SerialName("last_price") val lastPrice: Double? = null,
    val currency: String? = null,
    @SerialName("change_24h_percent") val change24hPercent: Double? = null,
    @SerialName("day_high") val dayHigh: Double? = null,
    @SerialName("day_low") val dayLow: Double? = null,
    @SerialName("volume_24h") val volume24h: Double? = null,
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
    @SerialName("fallback_used") val fallbackUsed: Boolean = false,
    @SerialName("provider_strategy") val providerStrategy: String = "single",
    val stale: Boolean = false,
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
    @SerialName("daily_liquidity") val dailyLiquidity: String? = null,
    @SerialName("dividend_yield") val dividendYield: String? = null,
    @SerialName("dividend_yield_12m") val dividendYield12m: String? = null,
    @SerialName("vacancy_financial") val vacancyFinancial: String? = null,
    @SerialName("contract_term_wault") val contractTermWault: String? = null,
    @SerialName("atypical_contracts_ratio") val atypicalContractsRatio: String? = null,
    @SerialName("top5_tenants_concentration") val top5TenantsConcentration: String? = null,
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
    @SerialName("fallback_used") val fallbackUsed: Boolean = false,
    @SerialName("provider_strategy") val providerStrategy: String = "single",
)
