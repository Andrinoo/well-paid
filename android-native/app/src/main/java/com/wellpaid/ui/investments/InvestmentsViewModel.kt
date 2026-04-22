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

data class InvestmentsUiState(
    val isLoading: Boolean = true,
    val overview: InvestmentOverviewDto? = null,
    val evolution: List<InvestmentEvolutionPointDto> = emptyList(),
    val positions: List<InvestmentPositionDto> = emptyList(),
    val showCreatePositionForm: Boolean = false,
    val newPositionType: String = "cdi",
    val newPositionName: String = "",
    val globalSearchText: String = "",
    val quantityText: String = "",
    val averagePriceText: String = "",
    val targetPriceText: String = "",
    val selectedFundamentals: FundamentalPreviewUi? = null,
    val newPositionPrincipalText: String = "",
    val newPositionAnnualRateText: String = "",
    val tickerSuggestions: List<TickerSuggestionUi> = emptyList(),
    val globalTickerSuggestions: List<TickerSuggestionUi> = emptyList(),
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
                    runCatching { api.searchTickers(query = query, limit = 12) }
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
                    if (query.length < 2) {
                        _uiState.update {
                            it.copy(
                                globalTickerSuggestions = emptyList(),
                                isSearchingGlobalTickers = false,
                            )
                        }
                        return@collectLatest
                    }
                    _uiState.update { it.copy(isSearchingGlobalTickers = true) }
                    runCatching { api.searchTickers(query = query, limit = 14) }
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
                        it.copy(
                            isFetchingQuote = false,
                            quoteInfoMessage = line,
                            quoteSourceLabel = q.source,
                            quoteConfidence = q.confidence,
                        )
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
        _uiState.update { it.copy(globalSearchText = value) }
        globalTickerQueryFlow.tryEmit(value.trim())
    }

    fun clearGlobalSearch() {
        _uiState.update { it.copy(globalSearchText = "", globalTickerSuggestions = emptyList()) }
    }

    fun selectTickerSuggestion(symbol: String, fromGlobalSearch: Boolean = false) {
        _uiState.update {
            it.copy(
                newPositionName = symbol.uppercase(Locale.ROOT),
                tickerSuggestions = emptyList(),
                globalSearchText = if (fromGlobalSearch) symbol.uppercase(Locale.ROOT) else it.globalSearchText,
                globalTickerSuggestions = if (fromGlobalSearch) emptyList() else it.globalTickerSuggestions,
                newPositionType = "stocks",
                showCreatePositionForm = true,
            )
        }
        fetchB3StockQuoteAndFundamentals()
    }

    fun setNewPositionPrincipalText(value: String) {
        _uiState.update { it.copy(newPositionPrincipalText = value) }
    }

    fun setNewPositionAnnualRateText(value: String) {
        _uiState.update { it.copy(newPositionAnnualRateText = value.filter { c -> c.isDigit() || c == '.' || c == ',' }) }
    }

    fun setQuantityText(value: String) {
        _uiState.update { it.copy(quantityText = value.filter { c -> c.isDigit() || c == '.' || c == ',' }) }
        recalculatePrincipalFromStocks()
    }

    fun setAveragePriceText(value: String) {
        _uiState.update { it.copy(averagePriceText = value.filter { c -> c.isDigit() || c == '.' || c == ',' }) }
        recalculatePrincipalFromStocks()
    }

    fun setTargetPriceText(value: String) {
        _uiState.update { it.copy(targetPriceText = value.filter { c -> c.isDigit() || c == '.' || c == ',' }) }
    }

    fun createPosition() {
        val s = _uiState.value
        val name = s.newPositionName.trim()
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
        val inferredType = inferInstrumentType(name)
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
                        principalCents = principal,
                        annualRateBps = rateBps,
                    )
                )
            }.onSuccess {
                closeCreatePositionForm()
                refresh()
            }.onFailure { t ->
                _uiState.update {
                    it.copy(
                        isSavingPosition = false,
                        errorMessage = FastApiErrorMapper.message(appContext, t),
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
