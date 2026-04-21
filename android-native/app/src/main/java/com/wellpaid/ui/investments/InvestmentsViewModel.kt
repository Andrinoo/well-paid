package com.wellpaid.ui.investments

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wellpaid.R
import com.wellpaid.core.model.investment.InvestmentEvolutionPointDto
import com.wellpaid.core.model.investment.InvestmentPositionCreateDto
import com.wellpaid.core.model.investment.InvestmentPositionDto
import com.wellpaid.core.model.investment.InvestmentOverviewDto
import com.wellpaid.core.network.InvestmentsApi
import com.wellpaid.util.FastApiErrorMapper
import com.wellpaid.util.parseBrlToCents
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class InvestmentsUiState(
    val isLoading: Boolean = true,
    val overview: InvestmentOverviewDto? = null,
    val evolution: List<InvestmentEvolutionPointDto> = emptyList(),
    val positions: List<InvestmentPositionDto> = emptyList(),
    val showCreatePositionForm: Boolean = false,
    val newPositionType: String = "cdi",
    val newPositionName: String = "",
    val newPositionPrincipalText: String = "",
    val newPositionAnnualRateText: String = "",
    val isSavingPosition: Boolean = false,
    val compactListMode: Boolean = true,
    val selectedPositionId: String? = null,
    val isLoadingSuggestedRates: Boolean = false,
    val isFetchingQuote: Boolean = false,
    val quoteInfoMessage: String? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class InvestmentsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val api: InvestmentsApi,
) : ViewModel() {
    private val _uiState = MutableStateFlow(InvestmentsUiState())
    val uiState: StateFlow<InvestmentsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val overviewResult = runCatching { api.getOverview() }
            val evolutionResult = runCatching { api.getEvolution(months = 6) }
            val positionsResult = runCatching { api.listPositions() }
            overviewResult
                .onSuccess { payload ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            overview = payload,
                            evolution = evolutionResult.getOrElse { emptyList() },
                            positions = positionsResult.getOrElse { emptyList() },
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
        _uiState.update { it.copy(showCreatePositionForm = true, quoteInfoMessage = null) }
    }

    fun closeCreatePositionForm() {
        _uiState.update {
            it.copy(
                showCreatePositionForm = false,
                newPositionType = "cdi",
                newPositionName = "",
                newPositionPrincipalText = "",
                newPositionAnnualRateText = "",
                isSavingPosition = false,
                quoteInfoMessage = null,
            )
        }
    }

    fun setNewPositionType(value: String) {
        _uiState.update { it.copy(newPositionType = value, quoteInfoMessage = null) }
    }

    fun setCompactListMode(enabled: Boolean) {
        _uiState.update { it.copy(compactListMode = enabled) }
    }

    fun openPositionDetails(positionId: String) {
        _uiState.update { it.copy(selectedPositionId = positionId) }
    }

    fun closePositionDetails() {
        _uiState.update { it.copy(selectedPositionId = null) }
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
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSuggestedRates = true, errorMessage = null) }
            runCatching { api.getSuggestedRates() }
                .onSuccess { rates ->
                    val pct = when (s.newPositionType.lowercase(Locale.ROOT)) {
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
        val sym = _uiState.value.newPositionName.trim()
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
                    _uiState.update { it.copy(isFetchingQuote = false, quoteInfoMessage = line) }
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
        _uiState.update { it.copy(newPositionName = value) }
    }

    fun setNewPositionPrincipalText(value: String) {
        _uiState.update { it.copy(newPositionPrincipalText = value) }
    }

    fun setNewPositionAnnualRateText(value: String) {
        _uiState.update { it.copy(newPositionAnnualRateText = value.filter { c -> c.isDigit() || c == '.' || c == ',' }) }
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
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingPosition = true, errorMessage = null) }
            runCatching {
                api.createPosition(
                    InvestmentPositionCreateDto(
                        instrumentType = s.newPositionType,
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
}
