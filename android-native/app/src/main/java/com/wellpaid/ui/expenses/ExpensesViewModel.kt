package com.wellpaid.ui.expenses

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wellpaid.R
import com.wellpaid.core.model.auth.TokenStorage
import com.wellpaid.core.model.expense.ExpenseDto
import com.wellpaid.core.network.ExpensesApi
import com.wellpaid.data.MainPrefetchTiming
import com.wellpaid.util.FastApiErrorMapper
import com.wellpaid.util.sortExpensesNewestFirst
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

/** Atalho na lista: tipo de eliminação a enviar à API. */
enum class ExpenseQuickDeleteMode {
    SIMPLE,
    INSTALLMENT_FUTURE,
    INSTALLMENT_FULL,
    RECURRING_OCCURRENCE,
}

enum class ExpenseStatusFilter {
    ALL,
    PENDING,
    PAID,
    ;

    fun toQueryValue(): String? = when (this) {
        ALL -> null
        PENDING -> "pending"
        PAID -> "paid"
    }
}

@HiltViewModel
class ExpensesViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val expensesApi: ExpensesApi,
    private val tokenStorage: TokenStorage,
    private val prefetchTiming: MainPrefetchTiming,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpensesUiState())
    val uiState: StateFlow<ExpensesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            delay(prefetchTiming.expensesDelayMs)
            refresh()
        }
    }

    fun refresh() {
        if (tokenStorage.getAccessToken().isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    expenses = emptyList(),
                    errorMessage = appContext.getString(R.string.expenses_need_login),
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val s = _uiState.value
            runCatching {
                expensesApi.listExpenses(
                    year = s.period.year,
                    month = s.period.monthValue,
                    categoryId = null,
                    status = s.statusFilter.toQueryValue(),
                )
            }.onSuccess { list ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        expenses = sortExpensesNewestFirst(list),
                        errorMessage = null,
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

    fun setStatusFilter(filter: ExpenseStatusFilter) {
        _uiState.update { it.copy(statusFilter = filter) }
        refresh()
    }

    fun previousMonth() {
        _uiState.update { it.copy(period = it.period.minusMonths(1)) }
        refresh()
    }

    fun nextMonth() {
        _uiState.update { it.copy(period = it.period.plusMonths(1)) }
        refresh()
    }

    fun payExpense(expenseId: String) {
        viewModelScope.launch {
            runCatching { expensesApi.payExpense(expenseId) }
                .onSuccess { updated ->
                    _uiState.update { s ->
                        s.copy(
                            expenses = sortExpensesNewestFirst(
                                s.expenses.map { if (it.id == updated.id) updated else it },
                            ),
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { t ->
                    _uiState.update {
                        it.copy(errorMessage = FastApiErrorMapper.message(appContext, t))
                    }
                }
        }
    }

    fun deleteExpenseQuick(expense: ExpenseDto, mode: ExpenseQuickDeleteMode) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                val resp = when (mode) {
                    ExpenseQuickDeleteMode.SIMPLE -> expensesApi.deleteExpense(expense.id)
                    ExpenseQuickDeleteMode.INSTALLMENT_FUTURE -> expensesApi.deleteExpense(
                        expense.id,
                        deleteTarget = "series",
                        deleteScope = "future_unpaid",
                    )
                    ExpenseQuickDeleteMode.INSTALLMENT_FULL -> expensesApi.deleteExpense(
                        expense.id,
                        deleteTarget = "series",
                        deleteScope = "all",
                        confirmDeletePaid = true,
                    )
                    ExpenseQuickDeleteMode.RECURRING_OCCURRENCE -> expensesApi.deleteExpense(
                        expense.id,
                        deleteTarget = "occurrence",
                    )
                }
                if (!resp.isSuccessful) error("HTTP ${resp.code()}")
            }
                .onSuccess {
                    refresh()
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
}

data class ExpensesUiState(
    val period: YearMonth = YearMonth.now(),
    val statusFilter: ExpenseStatusFilter = ExpenseStatusFilter.ALL,
    val expenses: List<ExpenseDto> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)
