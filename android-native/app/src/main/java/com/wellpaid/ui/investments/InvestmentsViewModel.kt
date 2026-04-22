package com.wellpaid.ui.investments

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wellpaid.R
import com.wellpaid.core.model.investment.InvestmentEvolutionPointDto
import com.wellpaid.core.model.investment.EquityFundamentalsDto
import com.wellpaid.core.model.investment.InvestmentPositionCreateDto
import com.wellpaid.core.model.investment.InvestmentPositionDto
import com.wellpaid.core.model.investment.InvestmentOverviewDto
import com.wellpaid.core.model.investment.MacroSnapshotDto
import com.wellpaid.core.model.investment.StockHistoryPointDto
import com.wellpaid.core.model.investment.TopMoverItemDto
import com.wellpaid.core.network.InvestmentsApi
import com.wellpaid.util.FastApiErrorMapper
import com.wellpaid.util.parseBrlToCents
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.util.Locale
import java.util.regex.Pattern
import javax.inject.Inject

private val TickerPattern = Pattern.compile("([A-Za-z]{4}\\d{1,2})")
val InvestmentHistoryRanges = listOf("5m", "30m", "60m", "3h", "12h", "1d", "1w", "1m", "3m", "6m", "1y")

data class TickerSuggestionUi(
    val symbol: String,
    val name: String,
    val instrumentType: String = "stocks",
    val source: String = "unknown",
    val confidence: Double? = null,
)

data class FundamentalPreviewUi(
    val symbol: String,
    val dy: String? = null,
    val pl: String? = null,
    val pvp: String? = null,
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
    val errorMessage: String? = null,
)

@HiltViewModel
@OptIn(FlowPreview::class)
class InvestmentsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val api: InvestmentsApi,
) : ViewModel() {
    private val _uiState = MutableStateFlow(InvestmentsUiState())
    val uiState: StateFlow<InvestmentsUiState> = _uiState.asStateFlow()
    private val formTickerQueryFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)
    private val globalTickerQueryFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)

    init {
        observeFormTickerSearch()
        observeGlobalTickerSearch()
        refresh()
    }

    private fun observeFormTickerSearch() {
        viewModelScope.launch {
            formTickerQueryFlow
                .debounce(350)
                .distinctUntilChanged()
                .collectLatest { query ->
                    if (query.length < 2) {
                        _uiState.update {
                            it.copy(
                                tickerSuggestions = emptyList(),
                                isSearchingTickers = false,
                            )
                        }
                        return@collectLatest
                    }
                    _uiState.update { it.copy(isSearchingTickers = true) }
                    runCatching { api.searchTickers(query = query, limit = 5) }
                        .onSuccess { rows ->
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
                .debounce(300)
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
                    if (query.length < 2) {
                        _uiState.update {
                            it.copy(
                                globalTickerSuggestions = emptyList(),
                                isSearchingGlobalTickers = false,
                                showSearchResultsScreen = false,
                            )
                        }
                        return@collectLatest
                    }
                    _uiState.update { it.copy(isSearchingGlobalTickers = true, showSearchResultsScreen = true) }
                    runCatching { api.searchTickers(query = query, limit = 5) }
                        .onSuccess { rows ->
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
            val overviewResult = runCatching { api.getOverview() }
            val positionsResult = runCatching { api.listPositions() }
            val macroResult = runCatching { api.getMacroSnapshot() }
            overviewResult
                .onSuccess { payload ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            overview = payload,
                            positions = positionsResult.getOrElse { emptyList() },
                            macroSnapshot = macroResult.getOrNull(),
                            errorMessage = null,
                        )
                    }
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
        if (value == "stocks") {
            formTickerQueryFlow.tryEmit(_uiState.value.newPositionName.trim())
        }
    }

    fun setCompactListMode(enabled: Boolean) {
        _uiState.update { it.copy(compactListMode = enabled) }
    }

    fun openPositionDetails(positionId: String) {
        val selected = _uiState.value.positions.firstOrNull { it.id == positionId }
        _uiState.update {
            it.copy(
                selectedPositionId = positionId,
                historyErrorMessage = null,
            )
        }
        val symbol = extractTickerFromText(selected?.name.orEmpty())
        if (!symbol.isNullOrBlank()) {
            loadHistoryForSymbol(symbol = symbol, range = _uiState.value.selectedHistoryRange)
        } else {
            _uiState.update {
                it.copy(
                    selectedPositionHistory = emptyList(),
                    selectedPositionHistorySymbol = null,
                    historyErrorMessage = appContext.getString(R.string.investments_error_ticker),
                )
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
            )
        }
    }

    fun setHistoryRange(range: String) {
        if (range !in InvestmentHistoryRanges) return
        _uiState.update { it.copy(selectedHistoryRange = range) }
        val selectedSymbol = _uiState.value.selectedPositionHistorySymbol
            ?: extractTickerFromSelectedPosition()
        if (!selectedSymbol.isNullOrBlank()) {
            loadHistoryForSymbol(selectedSymbol, range)
        }
    }

    fun startTopUpFromPosition(positionId: String) {
        val p = _uiState.value.positions.firstOrNull { it.id == positionId } ?: return
        _uiState.update {
            it.copy(
                selectedPositionId = null,
                showCreatePositionForm = true,
                newPositionType = p.instrumentType,
                newPositionName = p.name,
                newPositionPrincipalText = "",
                newPositionAnnualRateText = String.format(
                    Locale.US,
                    "%.2f",
                    p.annualRateBps / 100.0,
                ).replace('.', ','),
                quoteInfoMessage = null,
                errorMessage = null,
            )
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
        if (sym.length < 3) {
            _uiState.update { it.copy(errorMessage = appContext.getString(R.string.investments_error_ticker)) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingQuote = true, errorMessage = null) }
            runCatching { api.getStockQuote(symbol = sym.uppercase(Locale.ROOT)) }
                .onSuccess { q ->
                    val line = if (!q.error.isNullOrBlank() || q.lastPrice <= 0) {
                        appContext.getString(
                            R.string.investments_quote_unavailable,
                            q.error.orEmpty().ifBlank { "—" },
                        )
                    } else {
                        val priceStr = String.format(Locale.US, "%.2f", q.lastPrice).replace('.', ',')
                        val asOf = q.asOf?.trim().orEmpty()
                        if (asOf.isNotEmpty()) {
                            appContext.getString(R.string.investments_stock_quote_ref, priceStr, asOf)
                        } else {
                            appContext.getString(R.string.investments_stock_quote, priceStr)
                        }
                    }
                    _uiState.update {
                        val shouldAutofillAverage = it.newPositionType == "stocks" &&
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
                            averagePriceText = if (shouldAutofillAverage && averageFromQuote != null) averageFromQuote else it.averagePriceText,
                        )
                    }
                    if (q.lastPrice > 0) {
                        recalculatePrincipalFromStocks()
                    }
                    if (state.newPositionType == "stocks") {
                        loadHistoryForSymbol(sym.uppercase(Locale.ROOT), state.selectedHistoryRange)
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
                selectedFundamentals = if (inferredType == "stocks") it.selectedFundamentals else null,
            )
        }
        formTickerQueryFlow.tryEmit(value.trim())
    }

    fun setGlobalSearchText(value: String) {
        val trimmed = value.trim()
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
        if (trimmed.length >= 2) {
            loadTopMoversIfNeeded()
        }
        if (!_uiState.value.familySearchEnabled) {
            val maybeTicker = extractTickerFromText(trimmed)
            if (!maybeTicker.isNullOrBlank() && maybeTicker.length >= 5) {
                openStockJoin(maybeTicker)
                return
            }
            val upper = trimmed.uppercase(Locale.ROOT)
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

    fun clearGlobalSearch() {
        _uiState.update {
            it.copy(
                globalSearchText = "",
                globalTickerSuggestions = emptyList(),
                showSearchResultsScreen = false,
            )
        }
    }

    fun closeSearchResults() {
        _uiState.update { it.copy(showSearchResultsScreen = false) }
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
            if (q.length >= 2) {
                globalTickerQueryFlow.tryEmit(q)
            }
        }
    }

    fun selectTickerSuggestion(symbol: String, fromGlobalSearch: Boolean = false) {
        val selected = _uiState.value.globalTickerSuggestions.firstOrNull { it.symbol == symbol }
            ?: _uiState.value.tickerSuggestions.firstOrNull { it.symbol == symbol }
        val instrument = selected?.instrumentType ?: "stocks"
        if (instrument != "stocks") {
            openFixedIncomeJoin(symbol = symbol, instrumentType = instrument)
            return
        }
        _uiState.update {
            it.copy(
                newPositionName = symbol.uppercase(Locale.ROOT),
                tickerSuggestions = emptyList(),
                globalSearchText = if (fromGlobalSearch) symbol.uppercase(Locale.ROOT) else it.globalSearchText,
                globalTickerSuggestions = if (fromGlobalSearch) emptyList() else it.globalTickerSuggestions,
                newPositionType = "stocks",
                showCreatePositionForm = true,
                showSearchResultsScreen = false,
                showStockJoinScreen = true,
                showFixedIncomeJoinScreen = false,
            )
        }
        fetchB3StockQuoteAndFundamentals()
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
        _uiState.update { it.copy(stockJoinModeByValue = enabled, stockJoinAdjustedAlert = null) }
    }

    fun setNewPositionAnnualRateText(value: String) {
        _uiState.update { it.copy(newPositionAnnualRateText = value.filter { c -> c.isDigit() || c == '.' || c == ',' }) }
    }

    fun setQuantityText(value: String) {
        val clean = value.filter { c -> c.isDigit() }
        _uiState.update { it.copy(quantityText = clean, stockJoinAdjustedAlert = null) }
        recalculatePrincipalFromStocks()
    }

    fun setAveragePriceText(value: String) {
        _uiState.update { it.copy(averagePriceText = value.filter { c -> c.isDigit() || c == '.' || c == ',' }) }
        recalculatePrincipalFromStocks()
    }

    fun setTargetPriceText(value: String) {
        _uiState.update { it.copy(targetPriceText = value.filter { c -> c.isDigit() || c == '.' || c == ',' }) }
    }

    fun setStockJoinValueText(value: String) {
        val clean = value.filter { c -> c.isDigit() || c == '.' || c == ',' }
        _uiState.update { it.copy(newPositionPrincipalText = clean) }
        applyWholeShareRuleFromValue()
    }

    private fun applyWholeShareRuleFromValue() {
        val s = _uiState.value
        if (!s.stockJoinModeByValue) return
        if (inferInstrumentType(s.newPositionName) != "stocks") return
        val price = s.averagePriceText.replace(",", ".").toDoubleOrNull()
            ?: s.quoteInfoMessage?.let { extractPriceFromQuoteMessage(it) }
        val value = s.newPositionPrincipalText.replace(",", ".").toDoubleOrNull()
        if (price == null || price <= 0.0 || value == null || value <= 0.0) return
        val shares = kotlin.math.floor(value / price).toInt()
        if (shares <= 0) return
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
            )
        }
    }

    fun createPosition() {
        val s = _uiState.value
        val inferredType = inferInstrumentType(s.newPositionName.trim())
        val name = if (inferredType == "stocks") {
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
        if (inferredType == "stocks") {
            val qty = s.quantityText.replace(",", ".").toDoubleOrNull()
            val avg = s.averagePriceText.replace(",", ".").toDoubleOrNull()
            if (qty == null || qty <= 0 || avg == null || avg <= 0) {
                _uiState.update { it.copy(errorMessage = appContext.getString(R.string.investments_error_stock_quantity_price)) }
                return
            }
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingPosition = true, errorMessage = null) }
            runCatching {
                api.createPosition(
                    InvestmentPositionCreateDto(
                        instrumentType = inferredType,
                        name = name,
                        description = if (inferredType == "stocks") s.stockJoinDescription.trim().ifBlank { null } else s.fixedIncomeDescription.trim().ifBlank { null },
                        principalCents = principal,
                        annualRateBps = rateBps,
                    )
                )
            }.onSuccess {
                closeCreatePositionForm()
                refresh()
            }.onFailure { t ->
                val mappedMessage = when ((t as? HttpException)?.code()) {
                    401, 403 -> appContext.getString(R.string.investments_error_session_expired)
                    else -> FastApiErrorMapper.message(appContext, t)
                }
                _uiState.update {
                    it.copy(
                        isSavingPosition = false,
                        errorMessage = mappedMessage,
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
        if (inferInstrumentType(s.newPositionName) != "stocks") return
        val qty = s.quantityText.replace(",", ".").toDoubleOrNull()
        val avg = s.averagePriceText.replace(",", ".").toDoubleOrNull()
        if (qty == null || avg == null || qty <= 0 || avg <= 0) return
        val total = qty * avg
        val brl = String.format(Locale.US, "%.2f", total).replace('.', ',')
        _uiState.update { it.copy(newPositionPrincipalText = brl) }
    }

    private fun extractPriceFromQuoteMessage(msg: String): Double? {
        val m = Regex("([0-9]+[\\.,][0-9]{2})").find(msg) ?: return null
        return m.groupValues[1].replace(",", ".").toDoubleOrNull()
    }

    private fun applyFundamentals(f: EquityFundamentalsDto) {
        _uiState.update {
            it.copy(
                selectedFundamentals = FundamentalPreviewUi(
                    symbol = f.symbol,
                    dy = f.dividendYield,
                    pl = f.pl,
                    pvp = f.pvp,
                    source = f.source,
                )
            )
        }
        val dyNumber = f.dividendYield
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
        fetchB3StockQuote()
        val symbol = extractTickerFromText(_uiState.value.newPositionName.trim()) ?: return
        viewModelScope.launch {
            runCatching { api.getEquityFundamentals(symbol.uppercase(Locale.ROOT)) }
                .onSuccess { payload ->
                    applyFundamentals(payload)
                }
                .onFailure {
                    _uiState.update { st -> st.copy(selectedFundamentals = null) }
                }
        }
    }

    fun openStockJoin(symbol: String) {
        _uiState.update {
            it.copy(
                newPositionName = symbol.uppercase(Locale.ROOT),
                newPositionType = "stocks",
                showStockJoinScreen = true,
                showFixedIncomeJoinScreen = false,
                showSearchResultsScreen = false,
                showCreatePositionForm = true,
            )
        }
        fetchB3StockQuoteAndFundamentals()
    }

    fun closeStockJoin() {
        _uiState.update { it.copy(showStockJoinScreen = false, stockJoinAdjustedAlert = null) }
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
            )
        }
        applyMarketRateToForm()
    }

    fun closeFixedIncomeJoin() {
        _uiState.update { it.copy(showFixedIncomeJoinScreen = false) }
    }

    fun setFixedIncomeDescription(value: String) {
        _uiState.update { it.copy(fixedIncomeDescription = value) }
    }

    private fun inferInstrumentType(text: String): String {
        val raw = text.trim().lowercase(Locale.ROOT)
        if (extractTickerFromText(text) != null) return "stocks"
        if ("cdb" in raw) return "cdb"
        if ("cdi" in raw) return "cdi"
        if ("tesouro" in raw || "ipca" in raw || "selic" in raw) return "tesouro"
        return "fixed_income"
    }

    private fun loadHistoryForSymbol(symbol: String, range: String) {
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
