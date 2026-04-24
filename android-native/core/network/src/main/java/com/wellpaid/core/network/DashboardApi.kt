package com.wellpaid.core.network

import com.wellpaid.core.model.dashboard.DashboardCashflowDto
import com.wellpaid.core.model.dashboard.DashboardOverviewDto
import retrofit2.http.GET
import retrofit2.http.Query

interface DashboardApi {
    /**
     * Futuro: endpoint agregado (overview + user + anúncios) reduz round-trips na Home.
     * Hoje: [com.wellpaid.data.HomeDashboardCacheRepository] + requisições em paralelo no [com.wellpaid.ui.home.HomeViewModel].
     */
    @GET("dashboard/overview")
    suspend fun overview(
        @Query("year") year: Int,
        @Query("month") month: Int,
    ): DashboardOverviewDto

    @GET("dashboard/cashflow")
    suspend fun cashflow(
        @Query("dynamic") dynamic: Boolean,
        @Query("forecast_months") forecastMonths: Int,
        @Query("start_year") startYear: Int? = null,
        @Query("start_month") startMonth: Int? = null,
        @Query("end_year") endYear: Int? = null,
        @Query("end_month") endMonth: Int? = null,
    ): DashboardCashflowDto
}
