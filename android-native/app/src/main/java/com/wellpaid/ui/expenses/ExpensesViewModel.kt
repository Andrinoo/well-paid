package com.wellpaid.ui.expenses

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wellpaid.R
import com.wellpaid.core.model.auth.TokenStorage
import com.wellpaid.core.model.expense.CategoryDto
import com.wellpaid.core.model.expense.ExpenseDto
import com.wellpaid.core.network.CategoriesApi
import com.wellpaid.core.network.ExpensesApi
import com.wellpaid.util.FastApiErrorMapper
import com.wellpaid.util.sortExpensesNewestFirst
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

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
    private val categoriesApi: CategoriesApi,
    private val tokenStorage: TokenStorage,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpensesUiState())
    val uiState: StateFlow<ExpensesUiState> = _uiState.asStateFlow()

    init {
        refresh(loadCategoriesToo = true)
    }

    fun refresh(loadCategoriesToo: Boolean = false) {
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

            if (loadCategoriesToo || _uiState.value.categories.isEmpty()) {
                runCatching { categoriesApi.listCategories() }
                    .onSuccess { list ->
                        _uiState.update { it.copy(categories = list.sortedBy { c -> c.sortOrder }) }
                    }
            }

            val s = _uiState.value
            runCatching {
                expensesApi.listExpenses(
                    year = s.period.year,
                    month = s.period.monthValue,
                    categoryId = s.categoryId,
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
        refresh(loadCategoriesToo = false)
    }

    fun selectCategory(categoryId: String?) {
        _uiState.update { it.copy(categoryId = categoryId) }
        refresh(loadCategoriesToo = false)
    }

    fun previousMonth() {
        _uiState.update { it.copy(period = it.period.minusMonths(1)) }
        refresh(loadCategoriesToo = false)
    }

    fun nextMonth() {
        _uiState.update { it.copy(period = it.period.plusMonths(1)) }
        refresh(loadCategoriesToo = false)
    }

    fun payExpense(expenseId: String) {
        viewModelScope.launch {
            runCatching { expensesApi.payExpense(expenseId) }
                .onSuccess {
                    refresh(loadCategoriesToo = false)
                }
                .onFailure { t ->
                    _uiState.update {
                        it.copy(errorMessage = FastApiErrorMapper.message(appContext, t))
                    }
                }
        }
    }
}

data class ExpensesUiState(
    val period: YearMonth = YearMonth.now(),
    val statusFilter: ExpenseStatusFilter = ExpenseStatusFilter.ALL,
    val categoryId: String? = null,
    val categories: List<CategoryDto> = emptyList(),
    val expenses: List<ExpenseDto> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)
