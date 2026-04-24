package com.wellpaid.data

import com.wellpaid.core.model.dashboard.DashboardCashflowDto
import com.wellpaid.core.model.dashboard.DashboardOverviewDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HomeDashboardCacheSnapshot(
    @SerialName("period_year") val periodYear: Int,
    @SerialName("period_month") val periodMonth: Int,
    val overview: DashboardOverviewDto,
    val cashflow: DashboardCashflowDto? = null,
    @SerialName("user_first_name") val userFirstName: String? = null,
    @SerialName("cashflow_dynamic") val cashflowDynamic: Boolean = true,
    @SerialName("cashflow_forecast_months") val cashflowForecastMonths: Int = 3,
    @SerialName("saved_at_millis") val savedAtMillis: Long = 0L,
)
