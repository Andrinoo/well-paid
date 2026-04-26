package com.wellpaid.ui.investments

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wellpaid.R
import com.wellpaid.core.model.investment.InvestmentEvolutionPointDto
import com.wellpaid.core.model.investment.EquityFundamentalsDto
import com.wellpaid.core.model.investment.InvestmentAssetType
import com.wellpaid.core.model.investment.InvestmentPositionAddPrincipalDto
import com.wellpaid.core.model.investment.InvestmentPositionCreateDto
import com.wellpaid.core.model.investment.InvestmentPositionDto
import com.wellpaid.core.model.investment.InvestmentOverviewDto
import com.wellpaid.core.model.investment.MacroSnapshotDto
import com.wellpaid.core.model.investment.StockHistoryPointDto
import com.wellpaid.core.model.investment.StockQuoteDto
import com.wellpaid.core.model.investment.TopMoverItemDto
import com.wellpaid.core.network.InvestmentsApi
import com.wellpaid.util.FastApiErrorMapper
import com.wellpaid.util.parseBrlToCents
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern
import javax.inject.Inject

private val TickerPattern = Pattern.compile("([A-Za-z]{4}\\d{1,2}|BTC|ETH|SOL|BNB|XRP|ADA|DOGE|LTC|USDT|USDC)")
private val StrictTickerPattern = Pattern.compile("^[A-Za-z]{4}\\d{1,2}$")
private val SearchShortCrypto = Pattern.compile("^(BTC|ETH|SOL|BNB|XRP|ADA|DOGE|LTC|USDT|USDC)$", Pattern.CASE_INSENSITIVE)
/** Single-word queries that look like tickers but are not equities/crypto (B3/RF shortcuts). */
private val NonEquityUpperTokens = setOf("CDB", "CDI", "LCI", "LCA", "LIG", "LF", "CRI", "CRA", "FIDC")
private const val StaleQuoteTtlMs = 45_000L
val InvestmentHistoryRanges = listOf("5m", "30m", "60m", "3h", "12h", "1d", "1w", "1m", "3m", "6m", "1y", "2y", "3y")

private data class StockQuoteCacheEntry(
    val dto: StockQuoteDto,
    val receivedAt: Long = System.currentTimeMillis(),
) {
    fun isFresh() = (System.currentTimeMillis() - receivedAt) < StaleQuoteTtlMs
}

private fun normalizeAssetTypeKey(raw: String?): String {
    return InvestmentAssetType.fromRaw(raw).key
}

private fun isEquityLikeAsset(raw: String?): Boolean {
    return when (normalizeAssetTypeKey(raw)) {
        "stock", "fii", "bdr", "etf", "crypto" -> true
        else -> false
    }
}

data class TickerSuggestionUi(
    val symbol: String,
    val name: String,
    val instrumentType: String = "stock",
    val source: String = "unknown",
    val confidence: Double? = null,
)

data class FundamentalPreviewUi(
    val symbol: String,
    val companyName: String? = null,
    val dy: String? = null,
    val dy12m: String? = null,
    val pl: String? = null,
    val pvp: String? = null,
    val dailyLiquidity: String? = null,
    val vacancyFinancial: String? = null,
    val contractTermWault: String? = null,
    val atypicalContractsRatio: String? = null,
    val top5TenantsConcentration: String? = null,
    val roe: String? = null,
    val evEbitda: String? = null,
    val netMargin: String? = null,
    val netDebtEbitda: String? = null,
    val eps: String? = null,
    val source: String = "fundamentus",
)

data class TopMoverUi(
    val symbol: String,
    val name: String,
    val changePercent: Double,
    val volume: Double,
    val window: String,
    val source: String,
    val confidence: Double? = null,
)

data class InvestmentsUiState(
    val isLoading: Boolean = true,
    val overview: InvestmentOverviewDto? = null,
    val evolution: List<InvestmentEvolutionPointDto> = emptyList(),
    val positions: List<InvestmentPositionDto> = emptyList(),
    val showCreatePositionForm: Boolean = false,
    val newPositionType: String = "cdi",
    val newPositionName: String = "",
    val globalSearchText: String = "",
    val familySearchEnabled: Boolean = false,
    val showSearchResultsScreen: Boolean = false,
    val quantityText: String = "",
    val averagePriceText: String = "",
    val targetPriceText: String = "",
    val selectedFundamentals: FundamentalPreviewUi? = null,
    val stockJoinDescription: String = "",
    val stockJoinModeByValue: Boolean = true,
    val stockJoinAdjustedAlert: String? = null,
    val stockJoinNeedsSaveConfirmation: Boolean = false,
    val fixedIncomeDescription: String = "",
    val fixedIncomeType: String = "fixed_income",
    val showStockJoinScreen: Boolean = false,
    val showFixedIncomeJoinScreen: Boolean = false,
    val newPositionPrincipalText: String = "",
    val newPositionAnnualRateText: String = "",
    val tickerSuggestions: List<TickerSuggestionUi> = emptyList(),
    val globalTickerSuggestions: List<TickerSuggestionUi> = emptyList(),
    val topMoversHour: List<TopMoverUi> = emptyList(),
    val topMoversDay: List<TopMoverUi> = emptyList(),
    val topMoversWeek: List<TopMoverUi> = emptyList(),
    val isLoadingTopMovers: Boolean = false,
    val isSearchingGlobalTickers: Boolean = false,
    val isSearchingTickers: Boolean = false,
    val isSavingPosition: Boolean = false,
    val compactListMode: Boolean = true,
    val selectedPositionId: String? = null,
    val selectedHistoryRange: String = "1m",
    val selectedPositionHistory: List<StockHistoryPointDto> = emptyList(),
    val selectedPositionHistorySymbol: String? = null,
    val selectedPositionHistorySource: String? = null,
    val selectedPositionHistoryConfidence: Double? = null,
    val isLoadingHistory: Boolean = false,
    val historyErrorMessage: String? = null,
    val macroSnapshot: MacroSnapshotDto? = null,
    val isLoadingSuggestedRates: Boolean = false,
    val isFetchingQuote: Boolean = false,
    val quoteInfoMessage: String? = null,
    val quoteSourceLabel: String? = null,
    val quoteConfidence: Double? = null,
    val quoteLastPrice: Double? = null,
    val infoMessage: String? = null,
    val errorMessage: String? = null,
    val positionDetailsFundamentals: FundamentalPreviewUi? = null,
    val positionCardFundamentals: Map<String, FundamentalPreviewUi> = emptyMap(),
    val isLoadingPositionDetailsFundamentals: Boolean = false,
    val aporteAmountText: String = "",
    val aporteFundamentals: FundamentalPreviewUi? = null,
    val isLoadingAporteFundamentals: Boolean = false,
    val isSubmittingAporte: Boolean = false,
    val aporteErrorMessage: String? = null,
)

@HiltViewModel
@OptIn(FlowPreview::class)
class InvestmentsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val api: InvestmentsApi,
) : ViewModel() {
    private val searchMinLengthB3 = 3
    private val searchMinLengthCrypto = 2
    private val autoOpenDelayMs = 700L
    private var formSearchRequestSeq: Long = 0
    private var globalSearchRequestSeq: Long = 0
    private var globalAutoOpenJob: Job? = null
    private val stockQuoteCache = ConcurrentHashMap<String, StockQuoteCacheEntry>(32)
    private val _uiState = MutableStateFlow(InvestmentsUiState())
    val uiState: StateFlow<InvestmentsUiState> = _uiState.asStateFlow()
    private val formTickerQueryFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)
    private val globalTickerQueryFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)

    private fun isKnownShortCryptoSymbol(upper: String): Boolean = SearchShortCrypto.matcher(upper).matches()

    private fun isLikelyLetterTickerToken(upper: String): Boolean {
        if (upper.isEmpty() || upper.length > 4) return false
        if (!upper.all { it.isLetter() }) return false
        if (upper in NonEquityUpperTokens) return false
        if (upper == "BDR" || upper == "FII" || upper == "ETF" || upper == "RF") return false
        return true
    }

    private fun isCryptoTickerSymbol(upper: String): Boolean {
        if (isKnownShortCryptoSymbol(upper)) return true
        if (StrictTickerPattern.matcher(upper).matches()) return false
        return isLikelyLetterTickerToken(upper)
    }

    private fun isCryptoByInstrumentOrName(
        rawInstrumentType: String?,
        nameOrQuery: String,
    ): Boolean {
        val t = normalizeAssetTypeKey(rawInstrumentType)
        if (t == "crypto") return true
        val u = (extractTickerFromText(nameOrQuery.trim()) ?: nameOrQuery.trim())
            .uppercase(Locale.ROOT)
        if (u.isNotBlank() && isCryptoTickerSymbol(u)) return true
        return false
    }

    private fun minTickerSearchLengthForQuery(query: String): Int {
        val u = query.trim().uppercase(Locale.ROOT)
        if (u.isEmpty()) return searchMinLengthB3
        if (isKnownShortCryptoSymbol(u) || isLikelyLetterTickerToken(u)) {
            return searchMinLengthCrypto
        }
        if (u.length < 3) {
            return searchMinLengthCrypto
        }
        if (StrictTickerPattern.matcher(u).matches() || (u.length == 3 && TickerPattern.matcher(u).find())) {
            return searchMinLengthB3
        }
        return searchMinLengthCrypto
    }

    private fun quoteInfoLineFromDto(q: StockQuoteDto): String {
        val err = q.error
        if (!err.isNullOrBlank() && isLikelyProviderRateLimit(err)) {
            return appContext.getString(
                R.string.investments_quote_rate_limited,
                q.source,
            )
        }
        if (!err.isNullOrBlank() || q.lastPrice <= 0) {
            return appContext.getString(
                R.string.investments_quote_unavailable,
                err.orEmpty().ifBlank { "—" },
            )
        }
        val priceStr = String.format(Locale.US, "%.2f", q.lastPrice).replace('.', ',')
        val asOf = q.asOf?.trim().orEmpty()
        val ccy = q.currency.uppercase(Locale.ROOT)
        val withAsOf = { base: String ->
            if (asOf.isNotEmpty()) appContext.getString(R.string.investments_quote_line_ref, base, asOf)
            else base
        }
        val line = when (ccy) {
            "BRL" -> withAsOf(
                appContext.getString(
                    R.string.investments_quote_line_brl,
                    priceStr,
                ),
            )
            "USD" -> withAsOf(
                appContext.getString(
                    R.string.investments_quote_line_usd,
                    priceStr,
                ),
            )
            else -> withAsOf(
                appContext.getString(
                    R.string.investments_quote_line_generic,
                    ccy,
                    priceStr,
                ),
            )
        }
        return if (q.fallbackUsed) {
            appContext.getString(R.string.investments_quote_with_fallback, line, q.source)
        } else {
            line
        }
    }

    private fun isLikelyProviderRateLimit(err: String): Boolean {
        val s = err.lowercase(Locale.ROOT)
        return "429" in s || "rate limit" in s || "too many" in s || "limite" in s
    }

    init {
        observeFormTickerSearch()
        observeGlobalTickerSearch()
        refresh()
    }

    private fun observeFormTickerSearch() {
        viewModelScope.launch {
            formTickerQueryFlow
                .debounce(600)
                .distinctUntilChanged()
                .collectLatest { query ->
                    if (query.length < minTickerSearchLengthForQuery(query)) {
                        _uiState.update {
                            it.copy(
                                tickerSuggestions = emptyList(),
                                isSearchingTickers = false,
                            )
                        }
                        return@collectLatest
                    }
                    val requestId = ++formSearchRequestSeq
                    _uiState.update { it.copy(isSearchingTickers = true) }
                    runCatching { api.searchTickers(query = query, limit = 5) }
                        .onSuccess { rows ->
                            if (requestId != formSearchRequestSeq) return@onSuccess
                            _uiState.update {
                                it.copy(
                                    isSearchingTickers = false,
                                    tickerSuggestions = rows.map { row ->
                                        TickerSuggestionUi(
                                            symbol = row.symbol,
                                            name = row.name,
                                            instrumentType = row.instrumentType,
                                            source = row.source,
                                            confidence = row.confidence,
                                        )
                                    },
                                )
                            }
                        }
                        .onFailure {
                            if (requestId != formSearchRequestSeq) return@onFailure
                            _uiState.update {
                                it.copy(
                                    isSearchingTickers = false,
                                    tickerSuggestions = emptyList(),
                                )
                            }
                        }
                }
        }
    }

    private fun observeGlobalTickerSearch() {
        viewModelScope.launch {
            globalTickerQueryFlow
                .debounce(600)
                .distinctUntilChanged()
                .collectLatest { query ->
                    if (!_uiState.value.familySearchEnabled) {
                        _uiState.update {
                            it.copy(
                                globalTickerSuggestions = emptyList(),
                                isSearchingGlobalTickers = false,
                                showSearchResultsScreen = false,
                            )
                        }
                        return@collectLatest
                    }
                    if (query.length < minTickerSearchLengthForQuery(query)) {
                        _uiState.update {
                            it.copy(
                                globalTickerSuggestions = emptyList(),
                                isSearchingGlobalTickers = false,
                                showSearchResultsScreen = false,
                            )
                        }
                        return@collectLatest
                    }
                    val requestId = ++globalSearchRequestSeq
                    _uiState.update { it.copy(isSearchingGlobalTickers = true, showSearchResultsScreen = true) }
                    runCatching { api.searchTickers(query = query, limit = 5) }
                        .onSuccess { rows ->
                            if (requestId != globalSearchRequestSeq) return@onSuccess
                            _uiState.update {
                                it.copy(
                                    isSearchingGlobalTickers = false,
                                    globalTickerSuggestions = rows.map { row ->
                                        TickerSuggestionUi(
                                            symbol = row.symbol,
                                            name = row.name,
                                            instrumentType = row.instrumentType,
                                            source = row.source,
                                            confidence = row.confidence,
                                        )
                                    },
                                )
                            }
                        }
                        .onFailure {
                            if (requestId != globalSearchRequestSeq) return@onFailure
                            _uiState.update {
                                it.copy(
                                    isSearchingGlobalTickers = false,
                                    globalTickerSuggestions = emptyList(),
                                )
                            }
                        }
                }
        }
    }

    private fun TopMoverItemDto.toUi(): TopMoverUi = TopMoverUi(
        symbol = symbol,
        name = name,
        changePercent = changePercent,
        volume = volume,
        window = window,
        source = source,
        confidence = confidence,
    )

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val (overviewResult, positionsResult, macroResult) = coroutineScope {
                val overviewDeferred = async { runCatching { api.getOverview() } }
                val positionsDeferred = async { runCatching { api.listPositions() } }
                val macroDeferred = async { runCatching { api.getMacroSnapshot() } }
                Triple(overviewDeferred.await(), positionsDeferred.await(), macroDeferred.await())
            }
            overviewResult
                .onSuccess { payload ->
                    val positions = positionsResult.getOrElse { emptyList() }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            overview = payload,
                            positions = positions,
                            macroSnapshot = macroResult.getOrNull(),
                            positionCardFundamentals = it.positionCardFundamentals.filterKeys { id ->
                                positions.any { p -> p.id == id }
                            },
                            errorMessage = null,
                        )
                    }
                    preloadCardFundamentals(positions)
                }
                .onFailure { t ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = FastApiErrorMapper.message(appContext, t),
                        )
                    }
                }
        }
    }

    private fun preloadCardFundamentals(positions: List<InvestmentPositionDto>) {
        val stockPositions = positions
            .filter {
                isEquityLikeAsset(it.instrumentType) &&
                    !isCryptoByInstrumentOrName(it.instrumentType, it.name)
            }
            .mapNotNull { p ->
                val symbol = extractTickerFromText(p.name)?.uppercase(Locale.ROOT) ?: return@mapNotNull null
                p.id to symbol
            }
            .filter { (positionId, _) -> !_uiState.value.positionCardFundamentals.containsKey(positionId) }
            .take(6)
        if (stockPositions.isEmpty()) return
        stockPositions.forEach { (positionId, symbol) ->
            viewModelScope.launch {
                runCatching { api.getEquityFundamentals(symbol) }
                    .onSuccess { dto ->
                        _uiState.update { st ->
                            st.copy(
                                positionCardFundamentals = st.positionCardFundamentals + (positionId to fundamentalPreviewFromDto(dto)),
                            )
                        }
                    }
            }
        }
    }

    fun openCreatePositionForm() {
        _uiState.update {
            it.copy(
                showCreatePositionForm = true,
                quoteInfoMessage = null,
                tickerSuggestions = emptyList(),
                globalTickerSuggestions = emptyList(),
                newPositionType = "fixed_income",
            )
        }
    }

    fun closeCreatePositionForm() {
        _uiState.update {
            it.copy(
                showCreatePositionForm = false,
                showStockJoinScreen = false,
                showFixedIncomeJoinScreen = false,
                newPositionType = "fixed_income",
                newPositionName = "",
                quantityText = "",
                averagePriceText = "",
                targetPriceText = "",
                selectedFundamentals = null,
                newPositionPrincipalText = "",
                newPositionAnnualRateText = "",
                isSavingPosition = false,
                quoteInfoMessage = null,
                tickerSuggestions = emptyList(),
            )
        }
    }

    fun setNewPositionType(value: String) {
        _uiState.update {
            it.copy(
                newPositionType = value,
                quoteInfoMessage = null,
                tickerSuggestions = emptyList(),
            )
        }
        if (isEquityLikeAsset(value)) {
            formTickerQueryFlow.tryEmit(_uiState.value.newPositionName.trim())
        }
    }

    fun setCompactListMode(enabled: Boolean) {
        _uiState.update { it.copy(compactListMode = enabled) }
    }

    fun openPositionDetails(positionId: String) {
        val selected = _uiState.value.positions.firstOrNull { it.id == positionId }
        val isCrypto = selected != null && isCryptoByInstrumentOrName(selected.instrumentType, selected.name)
        _uiState.update {
            it.copy(
                selectedPositionId = positionId,
                historyErrorMessage = null,
                positionDetailsFundamentals = null,
                isLoadingPositionDetailsFundamentals = false,
            )
        }
        val symbol = extractTickerFromText(selected?.name.orEmpty())
        if (!symbol.isNullOrBlank()) {
            if (isCrypto) {
                _uiState.update {
                    it.copy(
                        selectedPositionHistory = emptyList(),
                        selectedPositionHistorySymbol = symbol,
                        isLoadingHistory = false,
                        historyErrorMessage = appContext.getString(R.string.investments_history_crypto_unavailable),
                    )
                }
            } else {
                loadHistoryForSymbol(symbol = symbol, range = _uiState.value.selectedHistoryRange)
            }
        } else {
            _uiState.update {
                it.copy(
                    selectedPositionHistory = emptyList(),
                    selectedPositionHistorySymbol = null,
                    historyErrorMessage = appContext.getString(R.string.investments_error_ticker),
                )
            }
        }
        if (selected != null && isEquityLikeAsset(selected.instrumentType) && !isCrypto) {
            val sym = extractTickerFromText(selected.name)
            if (!sym.isNullOrBlank()) {
                viewModelScope.launch {
                    _uiState.update { it.copy(isLoadingPositionDetailsFundamentals = true) }
                    runCatching { api.getEquityFundamentals(sym.uppercase(Locale.ROOT)) }
                        .onSuccess { f ->
                            _uiState.update {
                                it.copy(
                                    positionDetailsFundamentals = fundamentalPreviewFromDto(f),
                                    isLoadingPositionDetailsFundamentals = false,
                                )
                            }
                        }
                        .onFailure {
                            _uiState.update {
                                it.copy(
                                    positionDetailsFundamentals = null,
                                    isLoadingPositionDetailsFundamentals = false,
                                )
                            }
                        }
                }
            }
        }
    }

    fun closePositionDetails() {
        _uiState.update {
            it.copy(
                selectedPositionId = null,
                selectedPositionHistory = emptyList(),
                selectedPositionHistorySymbol = null,
                selectedPositionHistorySource = null,
                selectedPositionHistoryConfidence = null,
                isLoadingHistory = false,
                historyErrorMessage = null,
                positionDetailsFundamentals = null,
                isLoadingPositionDetailsFundamentals = false,
            )
        }
    }

    fun setHistoryRange(range: String) {
        if (range !in InvestmentHistoryRanges) return
        _uiState.update { it.copy(selectedHistoryRange = range) }
        val st = _uiState.value
        val pos = st.selectedPositionId?.let { id -> st.positions.firstOrNull { it.id == id } }
        if (pos != null && isCryptoByInstrumentOrName(pos.instrumentType, pos.name)) {
            _uiState.update {
                it.copy(
                    isLoadingHistory = false,
                    selectedPositionHistory = emptyList(),
                    historyErrorMessage = appContext.getString(R.string.investments_history_crypto_unavailable),
                )
            }
            return
        }
        val selectedSymbol = st.selectedPositionHistorySymbol
            ?: extractTickerFromSelectedPosition()
        if (!selectedSymbol.isNullOrBlank()) {
            loadHistoryForSymbol(selectedSymbol, range)
        }
    }

    fun setAporteAmountText(value: String) {
        _uiState.update { it.copy(aporteAmountText = value) }
    }

    fun initAporteForPosition(positionId: String) {
        val p = _uiState.value.positions.firstOrNull { it.id == positionId } ?: return
        _uiState.update {
            it.copy(
                aporteAmountText = "",
                aporteErrorMessage = null,
                aporteFundamentals = null,
                isSubmittingAporte = false,
                isLoadingAporteFundamentals = false,
            )
        }
        if (isCryptoByInstrumentOrName(p.instrumentType, p.name)) return
        if (normalizeAssetTypeKey(p.instrumentType) != "stock") return
        val sym = extractTickerFromText(p.name) ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAporteFundamentals = true) }
            runCatching { api.getEquityFundamentals(sym.uppercase(Locale.ROOT)) }
                .onSuccess { f ->
                    _uiState.update {
                        it.copy(
                            aporteFundamentals = fundamentalPreviewFromDto(f),
                            isLoadingAporteFundamentals = false,
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(aporteFundamentals = null, isLoadingAporteFundamentals = false) }
                }
        }
    }

    fun clearAporteState() {
        _uiState.update {
            it.copy(
                aporteAmountText = "",
                aporteFundamentals = null,
                aporteErrorMessage = null,
                isSubmittingAporte = false,
                isLoadingAporteFundamentals = false,
            )
        }
    }

    fun submitAporte(positionId: String, onSuccess: () -> Unit) {
        val principal = parseBrlToCents(_uiState.value.aporteAmountText) ?: run {
            _uiState.update { it.copy(aporteErrorMessage = appContext.getString(R.string.investments_error_principal)) }
            return
        }
        if (principal <= 0) {
            _uiState.update { it.copy(aporteErrorMessage = appContext.getString(R.string.investments_error_principal)) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingAporte = true, aporteErrorMessage = null) }
            runCatching {
                api.addPrincipalToPosition(
                    positionId,
                    InvestmentPositionAddPrincipalDto(addPrincipalCents = principal),
                )
            }.onSuccess {
                clearAporteState()
                refresh()
                _uiState.update { it.copy(isSubmittingAporte = false) }
                onSuccess()
            }.onFailure { t ->
                _uiState.update {
                    it.copy(
                        isSubmittingAporte = false,
                        aporteErrorMessage = when ((t as? HttpException)?.code()) {
                            401, 403 -> appContext.getString(R.string.investments_error_session_expired)
                            else -> FastApiErrorMapper.message(appContext, t)
                        },
                    )
                }
            }
        }
    }

    /**
     * Preenche a taxa anual a partir de BACEN (CDI) e fatores do servidor, como no resumo de investimentos.
     * CDB: % do CDI configurado no backend, não cotação de um título específico.
     */
    fun applyMarketRateToForm() {
        val s = _uiState.value
        val inferredType = inferInstrumentType(s.newPositionName)
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSuggestedRates = true, errorMessage = null) }
            runCatching { api.getSuggestedRates() }
                .onSuccess { rates ->
                    val pct = when (inferredType.lowercase(Locale.ROOT)) {
                        "cdi" -> rates.cdiAnnualPercent
                        "cdb" -> rates.cdbAnnualPercent
                        "tesouro", "fixed_income" -> rates.fixedIncomeAnnualPercent
                        else -> null
                    }
                    if (pct == null) {
                        _uiState.update {
                            it.copy(
                                isLoadingSuggestedRates = false,
                                errorMessage = appContext.getString(R.string.investments_market_rate_not_applicable),
                            )
                        }
                        return@onSuccess
                    }
                    val text = String.format(Locale.US, "%.2f", pct).replace('.', ',')
                    _uiState.update {
                        it.copy(
                            isLoadingSuggestedRates = false,
                            newPositionAnnualRateText = text,
                        )
                    }
                }
                .onFailure { t ->
                    _uiState.update {
                        it.copy(
                            isLoadingSuggestedRates = false,
                            errorMessage = FastApiErrorMapper.message(appContext, t),
                        )
                    }
                }
        }
    }

    /**
     * Cotação B3 (brapi), mesmo conceito de docs/stock.py. Usa o nome da posição como ticker (ex. PETR4).
     */
    fun fetchB3StockQuote() {
        val state = _uiState.value
        val sym = extractTickerFromText(state.newPositionName.trim()) ?: state.newPositionName.trim()
        val key = sym.uppercase(Locale.ROOT)
        val isCrypto = isCryptoByInstrumentOrName(state.newPositionType, state.newPositionName)
        val minLen = if (isCrypto) searchMinLengthCrypto else searchMinLengthB3
        if (sym.length < minLen) {
            val msg = if (isCrypto) {
                appContext.getString(R.string.investments_error_ticker_short_crypto)
            } else {
                appContext.getString(R.string.investments_error_ticker)
            }
            _uiState.update { it.copy(errorMessage = msg) }
            return
        }
        viewModelScope.launch {
            val cached = stockQuoteCache[key]?.takeIf { it.isFresh() }?.dto
            if (cached != null) {
                val line = quoteInfoLineFromDto(cached)
                _uiState.update {
                    val shouldAutofillAverage = isEquityLikeAsset(it.newPositionType) &&
                        (it.averagePriceText.isBlank() || (it.averagePriceText.replace(",", ".").toDoubleOrNull() ?: 0.0) <= 0.0)
                    val averageFromQuote = if (cached.lastPrice > 0) {
                        String.format(Locale.US, "%.2f", cached.lastPrice).replace('.', ',')
                    } else {
                        null
                    }
                    it.copy(
                        isFetchingQuote = false,
                        errorMessage = null,
                        quoteInfoMessage = line,
                        quoteSourceLabel = cached.source,
                        quoteConfidence = cached.confidence,
                        quoteLastPrice = cached.lastPrice.takeIf { p -> p > 0.0 },
                        averagePriceText = if (shouldAutofillAverage && averageFromQuote != null) averageFromQuote else it.averagePriceText,
                    )
                }
                if (cached.lastPrice > 0) {
                    recalculatePrincipalFromStocks()
                }
                if (isEquityLikeAsset(state.newPositionType) && !isCrypto) {
                    loadHistoryForSymbol(key, state.selectedHistoryRange)
                }
                return@launch
            }
            _uiState.update { it.copy(isFetchingQuote = true, errorMessage = null) }
            runCatching { api.getStockQuote(symbol = key) }
                .onSuccess { q ->
                    stockQuoteCache[key] = StockQuoteCacheEntry(dto = q)
                    val line = quoteInfoLineFromDto(q)
                    _uiState.update {
                        val shouldAutofillAverage = isEquityLikeAsset(it.newPositionType) &&
                            (it.averagePriceText.isBlank() || (it.averagePriceText.replace(",", ".").toDoubleOrNull() ?: 0.0) <= 0.0)
                        val averageFromQuote = if (q.lastPrice > 0) {
                            String.format(Locale.US, "%.2f", q.lastPrice).replace('.', ',')
                        } else {
                            null
                        }
                        it.copy(
                            isFetchingQuote = false,
                            quoteInfoMessage = line,
                            quoteSourceLabel = q.source,
                            quoteConfidence = q.confidence,
                            quoteLastPrice = q.lastPrice.takeIf { p -> p > 0.0 },
                            averagePriceText = if (shouldAutofillAverage && averageFromQuote != null) averageFromQuote else it.averagePriceText,
                        )
                    }
                    if (q.lastPrice > 0) {
                        recalculatePrincipalFromStocks()
                    }
                    if (isEquityLikeAsset(state.newPositionType) && !isCrypto) {
                        loadHistoryForSymbol(key, state.selectedHistoryRange)
                    }
                }
                .onFailure { t ->
                    _uiState.update {
                        it.copy(
                            isFetchingQuote = false,
                            errorMessage = FastApiErrorMapper.message(appContext, t),
                        )
                    }
                }
        }
    }

    fun setNewPositionName(value: String) {
        val inferredType = inferInstrumentType(value)
        _uiState.update {
            it.copy(
                newPositionName = value,
                newPositionType = inferredType,
                selectedFundamentals = if (isEquityLikeAsset(inferredType)) it.selectedFundamentals else null,
            )
        }
        formTickerQueryFlow.tryEmit(value.trim())
    }

    fun setGlobalSearchText(value: String) {
        val trimmed = value.trim()
        globalAutoOpenJob?.cancel()
        _uiState.update { it.copy(globalSearchText = value) }
        if (_uiState.value.familySearchEnabled) {
            globalTickerQueryFlow.tryEmit(trimmed)
        } else {
            _uiState.update {
                it.copy(
                    isSearchingGlobalTickers = false,
                    globalTickerSuggestions = emptyList(),
                    showSearchResultsScreen = false,
                )
            }
        }
        if (trimmed.length >= searchMinLengthCrypto) {
            loadTopMoversIfNeeded()
        }
        if (!_uiState.value.familySearchEnabled) {
            if (trimmed.length >= minTickerSearchLengthForQuery(trimmed)) {
                globalAutoOpenJob = viewModelScope.launch {
                    // Wait a short idle window to avoid opening on partial typing (e.g. FIIP1 before FIIP11).
                    delay(autoOpenDelayMs)
                    if (_uiState.value.globalSearchText.trim() != trimmed) return@launch
                    val upper = trimmed.uppercase(Locale.ROOT)
                    if (StrictTickerPattern.matcher(upper).matches()) {
                        val inferred = inferInstrumentType(upper)
                        if (isEquityLikeAsset(inferred)) {
                            openStockJoin(upper, inferred)
                            return@launch
                        }
                    } else if (isKnownShortCryptoSymbol(upper) || (upper.length in 2..3 && isCryptoTickerSymbol(upper))) {
                        openStockJoin(upper, "crypto")
                        return@launch
                    }
                    if (upper == "CDB" || upper == "CDI" || upper.contains("RENDA FIXA")) {
                        val type = when {
                            upper.contains("CDB") -> "cdb"
                            upper.contains("CDI") -> "cdi"
                            else -> "fixed_income"
                        }
                        openFixedIncomeJoin(symbol = upper, instrumentType = type)
                    }
                }
            }
        }
    }

    fun clearGlobalSearch() {
        globalAutoOpenJob?.cancel()
        _uiState.update {
            it.copy(
                globalSearchText = "",
                globalTickerSuggestions = emptyList(),
                showSearchResultsScreen = false,
            )
        }
    }

    fun closeSearchResults() {
        _uiState.update {
            it.copy(
                showSearchResultsScreen = false,
                showCreatePositionForm = false,
                errorMessage = null,
            )
        }
    }

    fun setFamilySearchEnabled(enabled: Boolean) {
        _uiState.update {
            it.copy(
                familySearchEnabled = enabled,
                showSearchResultsScreen = if (enabled) it.showSearchResultsScreen else false,
                globalTickerSuggestions = if (enabled) it.globalTickerSuggestions else emptyList(),
                isSearchingGlobalTickers = if (enabled) it.isSearchingGlobalTickers else false,
            )
        }
        if (enabled) {
            val q = _uiState.value.globalSearchText.trim()
            if (q.length >= minTickerSearchLengthForQuery(q)) {
                globalTickerQueryFlow.tryEmit(q)
            }
        }
    }

    fun selectTickerSuggestion(symbol: String, fromGlobalSearch: Boolean = false) {
        val selected = _uiState.value.globalTickerSuggestions.firstOrNull { it.symbol == symbol }
            ?: _uiState.value.tickerSuggestions.firstOrNull { it.symbol == symbol }
        val instrument = normalizeAssetTypeKey(selected?.instrumentType ?: inferInstrumentType(symbol))
        when (instrument) {
            "stock", "fii", "bdr", "etf", "crypto" -> openStockJoin(symbol = symbol, instrumentType = instrument)
            "treasury", "cdi", "cdb", "fixed_income" -> openFixedIncomeJoin(symbol = symbol, instrumentType = instrument)
            else -> openFixedIncomeJoin(symbol = symbol, instrumentType = "fixed_income")
        }
        _uiState.update {
            it.copy(
                newPositionName = symbol.uppercase(Locale.ROOT),
                tickerSuggestions = emptyList(),
                globalSearchText = if (fromGlobalSearch) symbol.uppercase(Locale.ROOT) else it.globalSearchText,
                globalTickerSuggestions = if (fromGlobalSearch) emptyList() else it.globalTickerSuggestions,
                newPositionType = instrument,
                showCreatePositionForm = true,
                showSearchResultsScreen = false,
            )
        }
    }

    private fun loadTopMoversIfNeeded() {
        val current = _uiState.value
        if (current.isLoadingTopMovers) return
        if (current.topMoversHour.isNotEmpty() && current.topMoversDay.isNotEmpty() && current.topMoversWeek.isNotEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingTopMovers = true) }
            val hour = runCatching { api.getTopMovers(window = "hour", limit = 10) }.getOrElse { emptyList() }
            val day = runCatching { api.getTopMovers(window = "day", limit = 10) }.getOrElse { emptyList() }
            val week = runCatching { api.getTopMovers(window = "week", limit = 10) }.getOrElse { emptyList() }
            _uiState.update {
                it.copy(
                    isLoadingTopMovers = false,
                    topMoversHour = hour.map { row -> row.toUi() },
                    topMoversDay = day.map { row -> row.toUi() },
                    topMoversWeek = week.map { row -> row.toUi() },
                )
            }
        }
    }

    fun setNewPositionPrincipalText(value: String) {
        _uiState.update { it.copy(newPositionPrincipalText = value) }
    }

    fun setStockJoinDescription(value: String) {
        _uiState.update { it.copy(stockJoinDescription = value) }
    }

    fun setStockJoinModeByValue(enabled: Boolean) {
        _uiState.update {
            it.copy(
                stockJoinModeByValue = enabled,
                stockJoinAdjustedAlert = null,
                stockJoinNeedsSaveConfirmation = false,
                errorMessage = null,
                infoMessage = null,
            )
        }
        if (enabled) {
            applyWholeShareRuleFromValue()
        } else {
            recalculatePrincipalFromStocks()
        }
    }

    fun setNewPositionAnnualRateText(value: String) {
        _uiState.update { it.copy(newPositionAnnualRateText = value.filter { c -> c.isDigit() || c == '.' || c == ',' }) }
    }

    fun setQuantityText(value: String) {
        val clean = value.filter { c -> c.isDigit() }
        _uiState.update {
            it.copy(
                quantityText = clean,
                stockJoinAdjustedAlert = null,
                stockJoinNeedsSaveConfirmation = false,
                errorMessage = null,
                infoMessage = null,
            )
        }
        recalculatePrincipalFromStocks()
    }

    fun setAveragePriceText(value: String) {
        _uiState.update {
            it.copy(
                averagePriceText = value.filter { c -> c.isDigit() || c == '.' || c == ',' },
                stockJoinNeedsSaveConfirmation = false,
                errorMessage = null,
                infoMessage = null,
            )
        }
        recalculatePrincipalFromStocks()
    }

    fun setTargetPriceText(value: String) {
        _uiState.update { it.copy(targetPriceText = value.filter { c -> c.isDigit() || c == '.' || c == ',' }) }
    }

    fun setStockJoinValueText(value: String) {
        val clean = value.filter { c -> c.isDigit() || c == '.' || c == ',' }
        _uiState.update {
            it.copy(
                newPositionPrincipalText = clean,
                stockJoinNeedsSaveConfirmation = false,
                errorMessage = null,
                infoMessage = null,
            )
        }
        applyWholeShareRuleFromValue()
    }

    private fun applyWholeShareRuleFromValue() {
        val s = _uiState.value
        if (!s.stockJoinModeByValue) return
        if (!isEquityLikeAsset(inferInstrumentType(s.newPositionName))) return
        val price = s.quoteLastPrice
            ?: s.averagePriceText.replace(",", ".").toDoubleOrNull()
            ?: s.quoteInfoMessage?.let { extractPriceFromQuoteMessage(it) }
        val value = s.newPositionPrincipalText.replace(",", ".").toDoubleOrNull()
        if (price == null || price <= 0.0 || value == null || value <= 0.0) return
        val shares = kotlin.math.floor(value / price).toInt()
        if (shares <= 0) {
            _uiState.update {
                it.copy(
                    quantityText = "",
                    stockJoinAdjustedAlert = null,
                    stockJoinNeedsSaveConfirmation = false,
                )
            }
            return
        }
        val adjustedValue = shares * price
        val adjustedText = String.format(Locale.US, "%.2f", adjustedValue).replace('.', ',')
        val originalRounded = String.format(Locale.US, "%.2f", value)
        val adjustedRounded = String.format(Locale.US, "%.2f", adjustedValue)
        val alert = if (originalRounded != adjustedRounded) {
            appContext.getString(R.string.investments_whole_share_adjusted, shares, adjustedText)
        } else {
            null
        }
        _uiState.update {
            it.copy(
                quantityText = shares.toString(),
                newPositionPrincipalText = adjustedText,
                stockJoinAdjustedAlert = alert,
                stockJoinNeedsSaveConfirmation = false,
            )
        }
    }

    fun createPosition() {
        val s = _uiState.value
        val inferredType = inferInstrumentType(s.newPositionName.trim())
        val name = if (isEquityLikeAsset(inferredType)) {
            (extractTickerFromText(s.newPositionName) ?: s.newPositionName).trim().uppercase(Locale.ROOT)
        } else {
            s.newPositionName.trim()
        }
        if (name.length < 2) {
            _uiState.update { it.copy(errorMessage = appContext.getString(R.string.investments_error_name)) }
            return
        }
        val principal = parseBrlToCents(s.newPositionPrincipalText)?.takeIf { it > 0 }
        if (principal == null) {
            _uiState.update { it.copy(errorMessage = appContext.getString(R.string.investments_error_principal)) }
            return
        }
        val annualPct = s.newPositionAnnualRateText.replace(",", ".").toDoubleOrNull()
        if (annualPct == null || annualPct < 0.0 || annualPct > 1000.0) {
            _uiState.update { it.copy(errorMessage = appContext.getString(R.string.investments_error_rate)) }
            return
        }
        val rateBps = (annualPct * 100.0).toInt()
        if (isEquityLikeAsset(inferredType)) {
            val qty = s.quantityText.toIntOrNull()
            val avg = s.quoteLastPrice
                ?: s.averagePriceText.replace(",", ".").toDoubleOrNull()
            if (qty == null || qty <= 0 || avg == null || avg <= 0) {
                _uiState.update { it.copy(errorMessage = appContext.getString(R.string.investments_error_stock_quantity_price)) }
                return
            }
            val minLotCents = (avg * 100.0).toInt()
            if (principal < minLotCents) {
                _uiState.update {
                    it.copy(
                        errorMessage = appContext.getString(
                            R.string.investments_error_minimum_share_value,
                            String.format(Locale.US, "%.2f", avg).replace('.', ','),
                        ),
                        infoMessage = null,
                    )
                }
                return
            }
            val maxWholeSharesByValue = kotlin.math.floor(principal.toDouble() / minLotCents.toDouble()).toInt().coerceAtLeast(0)
            if (maxWholeSharesByValue <= 0) {
                _uiState.update {
                    it.copy(errorMessage = appContext.getString(R.string.investments_error_stock_quantity_price), infoMessage = null)
                }
                return
            }
            if (qty > maxWholeSharesByValue) {
                val adjustedPrincipalCents = maxWholeSharesByValue * minLotCents
                val adjustedText = String.format(Locale.US, "%.2f", adjustedPrincipalCents / 100.0).replace('.', ',')
                _uiState.update {
                    it.copy(
                        quantityText = maxWholeSharesByValue.toString(),
                        newPositionPrincipalText = adjustedText,
                        stockJoinNeedsSaveConfirmation = true,
                        stockJoinAdjustedAlert = appContext.getString(
                            R.string.investments_whole_share_adjusted_confirm,
                            maxWholeSharesByValue,
                            adjustedText,
                        ),
                        errorMessage = null,
                        infoMessage = appContext.getString(R.string.investments_info_tap_save_again),
                    )
                }
                return
            }
            if (s.stockJoinNeedsSaveConfirmation) {
                _uiState.update { it.copy(stockJoinNeedsSaveConfirmation = false, infoMessage = null) }
            }
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingPosition = true, errorMessage = null, infoMessage = null) }
            runCatching {
                api.createPosition(
                    InvestmentPositionCreateDto(
                        instrumentType = inferredType,
                        name = name,
                        description = if (isEquityLikeAsset(inferredType)) s.stockJoinDescription.trim().ifBlank { null } else s.fixedIncomeDescription.trim().ifBlank { null },
                        principalCents = principal,
                        annualRateBps = rateBps,
                    )
                )
            }.onSuccess {
                closeCreatePositionForm()
                refresh()
                _uiState.update {
                    it.copy(
                        infoMessage = appContext.getString(R.string.investments_success_position_saved),
                        errorMessage = null,
                    )
                }
            }.onFailure { t ->
                val mappedMessage = when ((t as? HttpException)?.code()) {
                    401, 403 -> appContext.getString(R.string.investments_error_session_expired)
                    else -> FastApiErrorMapper.message(appContext, t)
                }
                _uiState.update {
                    it.copy(
                        isSavingPosition = false,
                        errorMessage = mappedMessage,
                        infoMessage = null,
                    )
                }
            }
        }
    }

    fun deletePosition(positionId: String) {
        viewModelScope.launch {
            runCatching { api.deletePosition(positionId) }
                .onSuccess { refresh() }
                .onFailure { t ->
                    _uiState.update { it.copy(errorMessage = FastApiErrorMapper.message(appContext, t)) }
                }
        }
    }

    private fun extractTickerFromSelectedPosition(): String? {
        val state = _uiState.value
        val selectedId = state.selectedPositionId ?: return null
        val selected = state.positions.firstOrNull { it.id == selectedId } ?: return null
        return extractTickerFromText(selected.name)
    }

    private fun extractTickerFromText(text: String): String? {
        val matcher = TickerPattern.matcher(text.uppercase(Locale.ROOT))
        return if (matcher.find()) matcher.group(1) else null
    }

    private fun recalculatePrincipalFromStocks() {
        val s = _uiState.value
        if (!isEquityLikeAsset(inferInstrumentType(s.newPositionName))) return
        val qty = s.quantityText.replace(",", ".").toDoubleOrNull()
        val avg = s.quoteLastPrice
            ?: s.averagePriceText.replace(",", ".").toDoubleOrNull()
        if (qty == null || avg == null || qty <= 0 || avg <= 0) return
        val total = qty * avg
        val brl = String.format(Locale.US, "%.2f", total).replace('.', ',')
        _uiState.update { it.copy(newPositionPrincipalText = brl) }
    }

    private fun extractPriceFromQuoteMessage(msg: String): Double? {
        val m = Regex("([0-9]+[\\.,][0-9]{2})").find(msg) ?: return null
        return m.groupValues[1].replace(",", ".").toDoubleOrNull()
    }

    private fun fundamentalPreviewFromDto(f: EquityFundamentalsDto) = FundamentalPreviewUi(
        symbol = f.symbol,
        companyName = f.companyName,
        dy = f.dividendYield,
        dy12m = f.dividendYield12m,
        pl = f.pl,
        pvp = f.pvp,
        dailyLiquidity = f.dailyLiquidity,
        vacancyFinancial = f.vacancyFinancial,
        contractTermWault = f.contractTermWault,
        atypicalContractsRatio = f.atypicalContractsRatio,
        top5TenantsConcentration = f.top5TenantsConcentration,
        roe = f.roe,
        evEbitda = f.evEbitda,
        netMargin = f.netMargin,
        netDebtEbitda = f.netDebtEbitda,
        eps = f.eps,
        source = f.source,
    )

    private fun applyFundamentals(f: EquityFundamentalsDto) {
        _uiState.update {
            it.copy(selectedFundamentals = fundamentalPreviewFromDto(f))
        }
        val dyNumber = (f.dividendYield12m ?: f.dividendYield)
            ?.replace("%", "")
            ?.replace(",", ".")
            ?.trim()
            ?.toDoubleOrNull()
        if (dyNumber != null && dyNumber > 0.0) {
            _uiState.update {
                it.copy(
                    newPositionAnnualRateText = String.format(Locale.US, "%.2f", dyNumber).replace('.', ',')
                )
            }
        }
    }

    private fun fetchB3StockQuoteAndFundamentals() {
        val st = _uiState.value
        if (isCryptoByInstrumentOrName(st.newPositionType, st.newPositionName)) {
            fetchB3StockQuote()
            _uiState.update { it.copy(selectedFundamentals = null) }
            return
        }
        fetchB3StockQuote()
        val symbol = extractTickerFromText(_uiState.value.newPositionName.trim()) ?: return
        viewModelScope.launch {
            runCatching { api.getEquityFundamentals(symbol.uppercase(Locale.ROOT)) }
                .onSuccess { payload ->
                    applyFundamentals(payload)
                }
                .onFailure {
                    _uiState.update { s -> s.copy(selectedFundamentals = null) }
                }
        }
    }

    fun openStockJoin(symbol: String, instrumentType: String = "stock") {
        _uiState.update {
            it.copy(
                newPositionName = symbol.uppercase(Locale.ROOT),
                newPositionType = normalizeAssetTypeKey(instrumentType),
                showStockJoinScreen = true,
                showFixedIncomeJoinScreen = false,
                showSearchResultsScreen = false,
                showCreatePositionForm = true,
                infoMessage = null,
                errorMessage = null,
            )
        }
        fetchB3StockQuoteAndFundamentals()
    }

    fun closeStockJoin() {
        _uiState.update {
            it.copy(
                showStockJoinScreen = false,
                showCreatePositionForm = false,
                stockJoinAdjustedAlert = null,
                stockJoinNeedsSaveConfirmation = false,
                quoteLastPrice = null,
                errorMessage = null,
                infoMessage = null,
            )
        }
    }

    fun openFixedIncomeJoin(symbol: String, instrumentType: String) {
        _uiState.update {
            it.copy(
                fixedIncomeType = instrumentType,
                newPositionType = instrumentType,
                newPositionName = symbol,
                showFixedIncomeJoinScreen = true,
                showStockJoinScreen = false,
                showSearchResultsScreen = false,
                showCreatePositionForm = true,
                infoMessage = null,
                errorMessage = null,
            )
        }
        applyMarketRateToForm()
    }

    fun closeFixedIncomeJoin() {
        _uiState.update {
            it.copy(
                showFixedIncomeJoinScreen = false,
                showCreatePositionForm = false,
                errorMessage = null,
                infoMessage = null,
            )
        }
    }

    fun setFixedIncomeDescription(value: String) {
        _uiState.update { it.copy(fixedIncomeDescription = value) }
    }

    private fun inferInstrumentType(text: String): String {
        val raw = text.trim().lowercase(Locale.ROOT)
        if (raw in setOf("btc", "eth", "sol", "bnb", "xrp", "ada", "doge", "ltc", "usdt", "usdc")) return "crypto"
        if ("bitcoin" in raw || "ethereum" in raw || "cripto" in raw || "crypto" in raw) return "crypto"
        if (extractTickerFromText(text) != null) {
            val ticker = extractTickerFromText(text)?.uppercase(Locale.ROOT).orEmpty()
            if (ticker.endsWith("34") || ticker.endsWith("35") || ticker.endsWith("39")) return "bdr"
            if (ticker.endsWith("11")) return "fii"
            return "stock"
        }
        if ("cdb" in raw) return "cdb"
        if ("cdi" in raw) return "cdi"
        if ("tesouro" in raw || "ipca" in raw || "selic" in raw) return "treasury"
        return "fixed_income"
    }

    private fun loadHistoryForSymbol(symbol: String, range: String) {
        if (isCryptoTickerSymbol(symbol.uppercase(Locale.ROOT))) {
            _uiState.update {
                it.copy(
                    isLoadingHistory = false,
                    selectedPositionHistory = emptyList(),
                    selectedPositionHistorySymbol = symbol,
                    historyErrorMessage = appContext.getString(R.string.investments_history_crypto_unavailable),
                )
            }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingHistory = true,
                    historyErrorMessage = null,
                    selectedPositionHistorySymbol = symbol,
                    selectedPositionHistorySource = null,
                    selectedPositionHistoryConfidence = null,
                )
            }
            runCatching {
                api.getStockQuoteHistory(symbol = symbol.uppercase(Locale.ROOT), range = range)
            }
                .onSuccess { response ->
                    _uiState.update {
                        it.copy(
                            isLoadingHistory = false,
                            selectedPositionHistory = response.points,
                            selectedPositionHistorySymbol = response.symbol,
                            selectedPositionHistorySource = response.source,
                            selectedPositionHistoryConfidence = response.confidence,
                            historyErrorMessage = response.error?.takeIf { err -> err.isNotBlank() },
                        )
                    }
                }
                .onFailure { t ->
                    _uiState.update {
                        it.copy(
                            isLoadingHistory = false,
                            historyErrorMessage = FastApiErrorMapper.message(appContext, t),
                        )
                    }
                }
        }
    }
}
