package com.wellpaid.ui.receivables

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wellpaid.R
import com.wellpaid.core.model.income.IncomeCategoryDto
import com.wellpaid.core.model.receivable.ReceivableDto
import com.wellpaid.core.model.receivable.SettleReceivableDto
import com.wellpaid.core.network.IncomeCategoriesApi
import com.wellpaid.core.network.ReceivablesApi
import com.wellpaid.util.FastApiErrorMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class ReceivablesUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val asCreditor: List<ReceivableDto> = emptyList(),
    val asDebtor: List<ReceivableDto> = emptyList(),
    val incomeCategories: List<IncomeCategoryDto> = emptyList(),
    val isWorking: Boolean = false,
)

@HiltViewModel
class ReceivablesViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val receivablesApi: ReceivablesApi,
    private val incomeCategoriesApi: IncomeCategoriesApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReceivablesUiState())
    val uiState: StateFlow<ReceivablesUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                val cats = runCatching { incomeCategoriesApi.listIncomeCategories() }.getOrElse { emptyList() }
                val bucket = receivablesApi.listReceivables()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        incomeCategories = cats.sortedBy { c -> c.name },
                        asCreditor = bucket.asCreditor,
                        asDebtor = bucket.asDebtor,
                    )
                }
            }.onFailure { t ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = FastApiErrorMapper.message(appContext, t),
                    )
                }
            }
        }
    }

    fun settle(
        id: String,
        createIncome: Boolean,
        incomeCategoryId: String?,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            if (createIncome && incomeCategoryId.isNullOrBlank()) {
                _uiState.update {
                    it.copy(errorMessage = appContext.getString(R.string.receivables_settle_pick_category))
                }
                return@launch
            }
            _uiState.update { it.copy(isWorking = true, errorMessage = null) }
            runCatching {
                receivablesApi.settleReceivable(
                    id,
                    SettleReceivableDto(
                        createIncome = createIncome,
                        incomeCategoryId = incomeCategoryId,
                        incomeDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                    ),
                )
            }
                .onSuccess {
                    _uiState.update { it.copy(isWorking = false) }
                    refresh()
                    onDone()
                }
                .onFailure { t ->
                    _uiState.update {
                        it.copy(
                            isWorking = false,
                            errorMessage = FastApiErrorMapper.message(appContext, t),
                        )
                    }
                }
        }
    }
}
