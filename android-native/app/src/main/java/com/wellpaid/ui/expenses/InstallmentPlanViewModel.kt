package com.wellpaid.ui.expenses

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wellpaid.R
import com.wellpaid.core.model.expense.ExpenseDto
import com.wellpaid.core.network.ExpensesApi
import com.wellpaid.util.FastApiErrorMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InstallmentPlanUiState(
    val isLoading: Boolean = true,
    val isDeleting: Boolean = false,
    val errorMessage: String? = null,
    val installments: List<ExpenseDto> = emptyList(),
)

@HiltViewModel
class InstallmentPlanViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle,
    private val expensesApi: ExpensesApi,
) : ViewModel() {

    private val groupId: String = checkNotNull(savedStateHandle["groupId"])

    private val _uiState = MutableStateFlow(InstallmentPlanUiState())
    val uiState: StateFlow<InstallmentPlanUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                expensesApi.listExpenses(installmentGroupId = groupId)
            }
                .onSuccess { list ->
                    val sorted = list.sortedBy { it.installmentNumber }
                    _uiState.update {
                        it.copy(isLoading = false, installments = sorted, errorMessage = null)
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

    fun descriptionFromFirst(): String =
        _uiState.value.installments.firstOrNull()?.description
            ?: appContext.getString(R.string.installment_plan_title)

    private fun anyLineId(): String? =
        _uiState.value.installments.firstOrNull()?.id

    fun removeFutureUnpaid(onSuccess: () -> Unit) {
        val id = anyLineId() ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, errorMessage = null) }
            runCatching {
                val resp = expensesApi.deleteExpense(
                    id,
                    deleteTarget = "series",
                    deleteScope = "future_unpaid",
                )
                if (!resp.isSuccessful) error("HTTP ${resp.code()}")
            }
                .onSuccess {
                    _uiState.update { it.copy(isDeleting = false) }
                    onSuccess()
                }
                .onFailure { t ->
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            errorMessage = FastApiErrorMapper.message(appContext, t),
                        )
                    }
                }
        }
    }

    fun removeEntirePlanIncludingPaid(onSuccess: () -> Unit) {
        val id = anyLineId() ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, errorMessage = null) }
            runCatching {
                val resp = expensesApi.deleteExpense(
                    id,
                    deleteTarget = "series",
                    deleteScope = "all",
                    confirmDeletePaid = true,
                )
                if (!resp.isSuccessful) error("HTTP ${resp.code()}")
            }
                .onSuccess {
                    _uiState.update { it.copy(isDeleting = false) }
                    onSuccess()
                }
                .onFailure { t ->
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            errorMessage = FastApiErrorMapper.message(appContext, t),
                        )
                    }
                }
        }
    }
}
