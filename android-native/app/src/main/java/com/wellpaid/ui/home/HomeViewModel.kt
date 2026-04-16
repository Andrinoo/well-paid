package com.wellpaid.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wellpaid.R
import com.wellpaid.core.model.announcement.AnnouncementDto
import com.wellpaid.core.model.announcement.AnnouncementListDto
import com.wellpaid.core.model.auth.UserMeDto
import com.wellpaid.core.model.dashboard.DashboardCashflowDto
import com.wellpaid.core.model.dashboard.DashboardOverviewDto
import com.wellpaid.core.model.auth.TokenStorage
import com.wellpaid.core.network.DashboardApi
import com.wellpaid.core.network.AnnouncementsApi
import com.wellpaid.core.network.UserApi
import com.wellpaid.util.FastApiErrorMapper
import com.wellpaid.util.greetingFirstNameFromAccessToken
import com.wellpaid.util.looksLikeUuid
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.util.Locale
import javax.inject.Inject

data class HomeUiState(
    val period: YearMonth = YearMonth.now(),
    val isLoading: Boolean = true,
    val overview: DashboardOverviewDto? = null,
    val cashflow: DashboardCashflowDto? = null,
    /** Primeiro nome (ou parte local do e-mail) para saudação; null se não autenticado ou falha silenciosa. */
    val userFirstName: String? = null,
    val errorMessage: String? = null,
    val cashflowError: String? = null,
    /** Janela deslizante (true) vs eixo fixo (false) — query `dynamic` na API. */
    val cashflowDynamic: Boolean = true,
    /** Meses de previsão incluídos na série (1–12) — query `forecast_months`. */
    val cashflowForecastMonths: Int = 3,
    val announcements: List<AnnouncementDto> = emptyList(),
    val announcementsError: String? = null,
    /** Total de recados ativos (todos os placements agregados), para badge no header. */
    val recadosBadgeCount: Int = 0,
    /** `warning` | `info` | `tip` | `material` — cor do badge pela prioridade visual. */
    val recadosBadgeKind: String = "info",
)

/** Com `dynamic=false`, a API exige janela explícita: 6 meses civis terminando no mês do dashboard. */
private fun fixedCashflowStart(period: YearMonth): YearMonth = period.minusMonths(5)

private fun cashflowStartYear(s: HomeUiState): Int? =
    if (s.cashflowDynamic) null else fixedCashflowStart(s.period).year

private fun cashflowStartMonth(s: HomeUiState): Int? =
    if (s.cashflowDynamic) null else fixedCashflowStart(s.period).monthValue

private fun cashflowEndYear(s: HomeUiState): Int? =
    if (s.cashflowDynamic) null else s.period.year

private fun cashflowEndMonth(s: HomeUiState): Int? =
    if (s.cashflowDynamic) null else s.period.monthValue

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val dashboardApi: DashboardApi,
    private val announcementsApi: AnnouncementsApi,
    private val userApi: UserApi,
    private val tokenStorage: TokenStorage,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val token = tokenStorage.getAccessToken()
        if (token.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    overview = null,
                    cashflow = null,
                    announcements = emptyList(),
                    userFirstName = null,
                    errorMessage = appContext.getString(R.string.home_need_login),
                    cashflowError = null,
                    announcementsError = null,
                    recadosBadgeCount = 0,
                    recadosBadgeKind = "info",
                )
            }
            return
        }

        val period = _uiState.value.period
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null, cashflowError = null)
            }
            // Perfil primeiro: mesmo token do dashboard; garante nome/full_name antes dos agregados.
            val fromApi = runCatching { userApi.getCurrentUser() }
                .getOrNull()
                ?.let { dto -> greetingFirstName(dto) }
            val s = _uiState.value
            val overviewDeferred = async {
                runCatching {
                    dashboardApi.overview(year = period.year, month = period.monthValue)
                }
            }
            val cashflowDeferred = async {
                runCatching {
                    dashboardApi.cashflow(
                        dynamic = s.cashflowDynamic,
                        forecastMonths = s.cashflowForecastMonths,
                        startYear = cashflowStartYear(s),
                        startMonth = cashflowStartMonth(s),
                        endYear = cashflowEndYear(s),
                        endMonth = cashflowEndMonth(s),
                    )
                }
            }
            val bannerPlacements = async {
                runCatching { announcementsApi.listActive(placement = "home_banner", limit = 50) }
            }
            val feedPlacements = async {
                runCatching { announcementsApi.listActive(placement = "home_feed", limit = 50) }
            }
            val financePlacements = async {
                runCatching { announcementsApi.listActive(placement = "finance_tab", limit = 50) }
            }
            val announcementsTabPlacements = async {
                runCatching { announcementsApi.listActive(placement = "announcements_tab", limit = 50) }
            }
            val overviewResult = overviewDeferred.await()
            val cashflowResult = cashflowDeferred.await()
            val placementResults = listOf(
                bannerPlacements.await(),
                feedPlacements.await(),
                financePlacements.await(),
                announcementsTabPlacements.await(),
            )
            val mergedRecados = mergeAnnouncementLists(placementResults.mapNotNull { it.getOrNull() })
            val bannerForHome = mergedRecados
                .filter { it.placement == "home_banner" }
                .sortedWith(
                    compareByDescending<AnnouncementDto> { it.priority }
                        .thenByDescending { it.createdAt.orEmpty() },
                )
                .take(5)
            val unreadRecados = mergedRecados.filter { it.userReadAt.isNullOrBlank() }
            val recadosBadgeCount = unreadRecados.size
            val recadosBadgeKind = worstRecadosKindForBadge(
                if (unreadRecados.isNotEmpty()) unreadRecados else mergedRecados,
            )
            val fromJwt =
                if (fromApi.isNullOrBlank()) greetingFirstNameFromAccessToken(token) else null
            val firstName = fromApi?.takeIf { it.isNotBlank() }
                ?: fromJwt?.takeIf { it.isNotBlank() }
                ?: _uiState.value.userFirstName?.takeUnless { it.looksLikeUuid() }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    overview = overviewResult.getOrNull(),
                    cashflow = cashflowResult.getOrNull(),
                    userFirstName = firstName,
                    errorMessage = overviewResult.exceptionOrNull()?.let { e ->
                        FastApiErrorMapper.message(appContext, e)
                    },
                    cashflowError = cashflowResult.exceptionOrNull()?.let { e ->
                        FastApiErrorMapper.message(appContext, e)
                    },
                    announcements = bannerForHome,
                    announcementsError = placementAnnouncementsError(placementResults),
                    recadosBadgeCount = recadosBadgeCount,
                    recadosBadgeKind = recadosBadgeKind,
                )
            }
        }
    }

    private fun mergeAnnouncementLists(lists: List<AnnouncementListDto>): List<AnnouncementDto> {
        val merged = LinkedHashMap<String, AnnouncementDto>()
        lists.forEach { dto ->
            dto.items.forEach { row -> merged[row.id] = row }
        }
        return merged.values.sortedWith(
            compareByDescending<AnnouncementDto> { it.priority }
                .thenByDescending { it.createdAt.orEmpty() },
        )
    }

    private fun worstRecadosKindForBadge(items: List<AnnouncementDto>): String {
        if (items.isEmpty()) return "info"
        val kinds = items.map { it.kind.lowercase() }.toSet()
        return when {
            "warning" in kinds -> "warning"
            "info" in kinds -> "info"
            "tip" in kinds -> "tip"
            "material" in kinds -> "material"
            else -> items.first().kind.lowercase()
        }
    }

    private fun placementAnnouncementsError(results: List<Result<AnnouncementListDto>>): String? {
        val merged = mergeAnnouncementLists(results.mapNotNull { it.getOrNull() })
        if (merged.isNotEmpty()) return null
        val firstErr = results.mapNotNull { it.exceptionOrNull() }.firstOrNull() ?: return null
        return FastApiErrorMapper.message(appContext, firstErr)
    }

    private fun greetingFirstName(dto: UserMeDto): String? {
        val custom = dto.displayName?.trim().orEmpty()
        if (custom.isNotEmpty() && !custom.looksLikeUuid()) {
            return custom
        }
        val fromFull = dto.fullName?.trim().orEmpty()
        if (fromFull.isNotEmpty()) {
            val first = fromFull.split(Regex("\\s+")).first()
            if (!first.looksLikeUuid()) {
                return first.replaceFirstChar { ch ->
                    if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
                }
            }
        }
        val local = dto.email.substringBefore("@").trim()
        if (local.isEmpty() || local.looksLikeUuid()) return null
        return local.replaceFirstChar { ch ->
            if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
        }
    }

    fun setCashflowDynamic(dynamic: Boolean) {
        _uiState.update { it.copy(cashflowDynamic = dynamic) }
        refreshCashflowOnly()
    }

    fun setForecastMonths(months: Int) {
        val v = months.coerceIn(1, 12)
        _uiState.update { it.copy(cashflowForecastMonths = v) }
        refreshCashflowOnly()
    }

    fun shiftForecastMonths(delta: Int) {
        val next = (_uiState.value.cashflowForecastMonths + delta).coerceIn(1, 12)
        setForecastMonths(next)
    }

    private fun refreshCashflowOnly() {
        val token = tokenStorage.getAccessToken()
        if (token.isNullOrBlank()) return
        viewModelScope.launch {
            val s = _uiState.value
            _uiState.update { it.copy(cashflowError = null) }
            val cashflowResult = runCatching {
                dashboardApi.cashflow(
                    dynamic = s.cashflowDynamic,
                    forecastMonths = s.cashflowForecastMonths,
                    startYear = cashflowStartYear(s),
                    startMonth = cashflowStartMonth(s),
                    endYear = cashflowEndYear(s),
                    endMonth = cashflowEndMonth(s),
                )
            }
            _uiState.update {
                it.copy(
                    cashflow = cashflowResult.getOrNull() ?: it.cashflow,
                    cashflowError = cashflowResult.exceptionOrNull()?.let { e ->
                        FastApiErrorMapper.message(appContext, e)
                    },
                )
            }
        }
    }

    fun previousMonth() {
        _uiState.update { it.copy(period = it.period.minusMonths(1)) }
        refresh()
    }

    fun nextMonth() {
        _uiState.update { it.copy(period = it.period.plusMonths(1)) }
        refresh()
    }
}
