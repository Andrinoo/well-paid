package com.wellpaid.ui.incomes

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wellpaid.R
import com.wellpaid.core.model.auth.TokenStorage
import com.wellpaid.core.model.income.IncomeCategoryDto
import com.wellpaid.core.model.income.IncomeDto
import com.wellpaid.core.network.IncomeCategoriesApi
import com.wellpaid.core.network.IncomesApi
import com.wellpaid.util.FastApiErrorMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class IncomesViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val incomesApi: IncomesApi,
    private val incomeCategoriesApi: IncomeCategoriesApi,
    private val tokenStorage: TokenStorage,
) : ViewModel() {

    private val _uiState = MutableStateFlow(IncomesUiState())
    val uiState: StateFlow<IncomesUiState> = _uiState.asStateFlow()

    init {
        refresh(loadCategoriesToo = true)
    }

    fun refresh(loadCategoriesToo: Boolean = false) {
        if (tokenStorage.getAccessToken().isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    incomes = emptyList(),
                    errorMessage = appContext.getString(R.string.incomes_need_login),
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            if (loadCategoriesToo || _uiState.value.categories.isEmpty()) {
                runCatching { incomeCategoriesApi.listIncomeCategories() }
                    .onSuccess { list ->
                        _uiState.update { it.copy(categories = list.sortedBy { c -> c.sortOrder }) }
                    }
            }

            val s = _uiState.value
            runCatching {
                incomesApi.listIncomes(
                    year = s.period.year,
                    month = s.period.monthValue,
                )
            }.onSuccess { list ->
                _uiState.update {
                    it.copy(isLoading = false, incomes = list, errorMessage = null)
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

    fun selectCategory(categoryId: String?) {
        _uiState.update { it.copy(categoryId = categoryId) }
    }

    fun previousMonth() {
        _uiState.update { it.copy(period = it.period.minusMonths(1)) }
        refresh(loadCategoriesToo = false)
    }

    fun nextMonth() {
        _uiState.update { it.copy(period = it.period.plusMonths(1)) }
        refresh(loadCategoriesToo = false)
    }
}

data class IncomesUiState(
    val period: YearMonth = YearMonth.now(),
    val categoryId: String? = null,
    val categories: List<IncomeCategoryDto> = emptyList(),
    val incomes: List<IncomeDto> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
) {
    val filteredIncomes: List<IncomeDto>
        get() = if (categoryId == null) incomes else incomes.filter { it.incomeCategoryId == categoryId }
}
