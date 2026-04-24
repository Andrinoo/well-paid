package com.wellpaid.data

import android.content.Context
import com.wellpaid.core.datastore.EncryptedSharedPreferencesFactory
import com.wellpaid.core.model.auth.TokenStorage
import com.wellpaid.core.model.dashboard.DashboardCashflowDto
import com.wellpaid.core.model.dashboard.DashboardOverviewDto
import com.wellpaid.core.network.DashboardApi
import com.wellpaid.util.greetingFirstNameFromAccessToken
import com.wellpaid.core.network.UserApi
import com.wellpaid.util.looksLikeUuid
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.time.YearMonth
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Último snapshot encriptado da home (mês em cache) para [stale-while-revalidate](HomeViewModel).
 */
@Singleton
class HomeDashboardCacheRepository @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val tokenStorage: TokenStorage,
) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val prefs by lazy {
        EncryptedSharedPreferencesFactory.create(appContext, PREFS_NAME)
    }

    private fun canReadCache(): Boolean = !tokenStorage.getAccessToken().isNullOrBlank()

    /**
     * Lê o snapshot guardado, só se o mês bater com [expectedPeriod] (evita mostrar outro mês após o arranque).
     */
    fun readIfMatchingPeriod(expectedPeriod: YearMonth): HomeDashboardCacheSnapshot? {
        if (!canReadCache()) return null
        val raw = prefs.getString(KEY_JSON, null)?.trim().orEmpty() ?: return null
        if (raw.isEmpty()) return null
        return runCatching { json.decodeFromString<HomeDashboardCacheSnapshot>(raw) }
            .getOrNull()
            ?.takeIf {
                it.periodYear == expectedPeriod.year && it.periodMonth == expectedPeriod.monthValue
            }
    }

    fun persist(
        period: YearMonth,
        overview: DashboardOverviewDto,
        cashflow: DashboardCashflowDto?,
        userFirstName: String?,
        cashflowDynamic: Boolean,
        cashflowForecastMonths: Int,
    ) {
        if (!canReadCache()) return
        val snap = HomeDashboardCacheSnapshot(
            periodYear = period.year,
            periodMonth = period.monthValue,
            overview = overview,
            cashflow = cashflow,
            userFirstName = userFirstName,
            cashflowDynamic = cashflowDynamic,
            cashflowForecastMonths = cashflowForecastMonths,
            savedAtMillis = System.currentTimeMillis(),
        )
        val encoded = runCatching { json.encodeToString(HomeDashboardCacheSnapshot.serializer(), snap) }
            .getOrNull() ?: return
        prefs.edit().putString(KEY_JSON, encoded).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_JSON).apply()
    }

    /**
     * Após login, obtém user + overview + cashflow (mês civil atual) e grava cache antes de [NavigateToMain].
     */
    suspend fun warmAfterAuth(
        dashboardApi: DashboardApi,
        userApi: UserApi,
    ): Unit = withContext(Dispatchers.IO) {
        if (tokenStorage.getAccessToken().isNullOrBlank()) return@withContext
        val period = YearMonth.now()
        val token = tokenStorage.getAccessToken() ?: return@withContext
        val fromUser = runCatching { userApi.getCurrentUser() }
            .getOrNull()
            ?.let { u ->
                val custom = u.displayName?.trim().orEmpty()
                if (custom.isNotEmpty() && !custom.looksLikeUuid()) {
                    return@let custom
                }
                val fromFull = u.fullName?.trim().orEmpty()
                if (fromFull.isNotEmpty()) {
                    val first = fromFull.split(Regex("\\s+")).first()
                    if (!first.looksLikeUuid()) {
                        return@let first.replaceFirstChar { ch ->
                            if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
                        }
                    }
                }
                val local = u.email.substringBefore("@").trim()
                if (local.isEmpty() || local.looksLikeUuid()) {
                    null
                } else {
                    local.replaceFirstChar { ch ->
                        if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
                    }
                }
            }
        val fromJwt = if (fromUser.isNullOrBlank()) {
            greetingFirstNameFromAccessToken(token)
        } else {
            null
        }
        val firstName = fromUser?.takeIf { it.isNotBlank() } ?: fromJwt
        val cashflowDynamic = true
        val cashflowForecastMonths = 3
        val overview = runCatching {
            dashboardApi.overview(year = period.year, month = period.monthValue)
        }.getOrNull() ?: return@withContext
        val cashflow = runCatching {
            dashboardApi.cashflow(
                dynamic = cashflowDynamic,
                forecastMonths = cashflowForecastMonths,
                startYear = null,
                startMonth = null,
                endYear = null,
                endMonth = null,
            )
        }.getOrNull()
        persist(period, overview, cashflow, firstName, cashflowDynamic, cashflowForecastMonths)
    }

    private companion object {
        const val PREFS_NAME = "well_paid_home_dashboard_cache"
        const val KEY_JSON = "snapshot"
    }
}
