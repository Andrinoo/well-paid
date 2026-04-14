package com.wellpaid.ui.categories

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wellpaid.R
import com.wellpaid.core.model.expense.CategoryCreateRequest
import com.wellpaid.core.model.expense.CategoryDto
import com.wellpaid.core.model.income.IncomeCategoryCreateRequest
import com.wellpaid.core.model.income.IncomeCategoryDto
import com.wellpaid.core.network.CategoriesApi
import com.wellpaid.core.network.IncomeCategoriesApi
import com.wellpaid.util.FastApiErrorMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ManageCategoriesViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val categoriesApi: CategoriesApi,
    private val incomeCategoriesApi: IncomeCategoriesApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManageCategoriesUiState())
    val uiState: StateFlow<ManageCategoriesUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                categoriesApi.listCategories() to incomeCategoriesApi.listIncomeCategories()
            }.onSuccess { (exp, inc) ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        expenseCategories = exp.sortedBy { c -> c.sortOrder },
                        incomeCategories = inc.sortedBy { c -> c.sortOrder },
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

    fun onExpenseNameChange(value: String) {
        _uiState.update { it.copy(newExpenseName = value, errorMessage = null) }
    }

    fun onIncomeNameChange(value: String) {
        _uiState.update { it.copy(newIncomeName = value, errorMessage = null) }
    }

    fun submitExpenseCategory() {
        val name = _uiState.value.newExpenseName.trim()
        if (name.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingExpense = true, errorMessage = null) }
            runCatching {
                categoriesApi.createCategory(CategoryCreateRequest(name = name))
            }.onSuccess { created ->
                _uiState.update { s ->
                    s.copy(
                        isSavingExpense = false,
                        newExpenseName = "",
                        expenseCategories = (s.expenseCategories + created).sortedBy { it.sortOrder },
                        successMessage = appContext.getString(R.string.manage_categories_saved_expense),
                    )
                }
            }.onFailure { t ->
                _uiState.update {
                    it.copy(
                        isSavingExpense = false,
                        errorMessage = FastApiErrorMapper.message(appContext, t),
                    )
                }
            }
        }
    }

    fun submitIncomeCategory() {
        val name = _uiState.value.newIncomeName.trim()
        if (name.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingIncome = true, errorMessage = null) }
            runCatching {
                incomeCategoriesApi.createIncomeCategory(IncomeCategoryCreateRequest(name = name))
            }.onSuccess { created ->
                _uiState.update { s ->
                    s.copy(
                        isSavingIncome = false,
                        newIncomeName = "",
                        incomeCategories = (s.incomeCategories + created).sortedBy { it.sortOrder },
                        successMessage = appContext.getString(R.string.manage_categories_saved_income),
                    )
                }
            }.onFailure { t ->
                _uiState.update {
                    it.copy(
                        isSavingIncome = false,
                        errorMessage = FastApiErrorMapper.message(appContext, t),
                    )
                }
            }
        }
    }

    fun consumeError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun consumeSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }
}

data class ManageCategoriesUiState(
    val expenseCategories: List<CategoryDto> = emptyList(),
    val incomeCategories: List<IncomeCategoryDto> = emptyList(),
    val newExpenseName: String = "",
    val newIncomeName: String = "",
    val isLoading: Boolean = true,
    val isSavingExpense: Boolean = false,
    val isSavingIncome: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)
