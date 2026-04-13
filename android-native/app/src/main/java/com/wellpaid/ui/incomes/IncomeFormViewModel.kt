package com.wellpaid.ui.incomes

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wellpaid.R
import com.wellpaid.core.model.income.IncomeCategoryDto
import com.wellpaid.core.model.income.IncomeCreateDto
import com.wellpaid.core.model.income.IncomeDto
import com.wellpaid.core.model.income.IncomeUpdateDto
import com.wellpaid.core.network.IncomeCategoriesApi
import com.wellpaid.core.network.IncomesApi
import com.wellpaid.util.FastApiErrorMapper
import com.wellpaid.util.centsToBrlInput
import com.wellpaid.util.parseBrlToCents
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.inject.Inject

data class IncomeFormUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val loadedIncome: IncomeDto? = null,
    val categories: List<IncomeCategoryDto> = emptyList(),
    val description: String = "",
    val amountText: String = "",
    val incomeDate: String = "",
    val notes: String = "",
    val categoryId: String? = null,
    val showDeleteConfirm: Boolean = false,
)

@HiltViewModel
class IncomeFormViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle,
    private val incomesApi: IncomesApi,
    private val incomeCategoriesApi: IncomeCategoriesApi,
) : ViewModel() {

    private val incomeId: String? = savedStateHandle.get<String>("incomeId")

    private val _uiState = MutableStateFlow(IncomeFormUiState())
    val uiState: StateFlow<IncomeFormUiState> = _uiState.asStateFlow()

    val isEditMode: Boolean get() = incomeId != null

    init {
        viewModelScope.launch {
            runCatching { incomeCategoriesApi.listIncomeCategories() }
                .onSuccess { list ->
                    _uiState.update {
                        val sorted = list.sortedBy { c -> c.sortOrder }
                        val defaultCat = it.categoryId ?: sorted.firstOrNull()?.id
                        it.copy(categories = sorted, categoryId = defaultCat)
                    }
                }
                .onFailure { t ->
                    _uiState.update {
                        it.copy(errorMessage = FastApiErrorMapper.message(appContext, t))
                    }
                }

            val id = incomeId
            if (id == null) {
                val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                _uiState.update {
                    it.copy(isLoading = false, incomeDate = today)
                }
            } else {
                runCatching { incomesApi.getIncome(id) }
                    .onSuccess { row ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                loadedIncome = row,
                                description = row.description,
                                amountText = centsToBrlInput(row.amountCents),
                                incomeDate = row.incomeDate,
                                notes = row.notes.orEmpty(),
                                categoryId = row.incomeCategoryId,
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
    }

    fun setDescription(value: String) {
        _uiState.update { it.copy(description = value) }
    }

    fun setAmountText(value: String) {
        _uiState.update { it.copy(amountText = value) }
    }

    fun setIncomeDate(value: String) {
        _uiState.update { it.copy(incomeDate = value) }
    }

    fun setNotes(value: String) {
        _uiState.update { it.copy(notes = value) }
    }

    fun setCategoryId(id: String) {
        _uiState.update { it.copy(categoryId = id) }
    }

    fun dismissDeleteConfirm() {
        _uiState.update { it.copy(showDeleteConfirm = false) }
    }

    fun requestDeleteConfirm() {
        _uiState.update { it.copy(showDeleteConfirm = true) }
    }

    fun canEditFields(): Boolean {
        if (!isEditMode) return true
        val row = _uiState.value.loadedIncome ?: return false
        return row.isMine
    }

    fun canDelete(): Boolean {
        val row = _uiState.value.loadedIncome ?: return false
        return row.isMine
    }

    fun save(onSuccess: () -> Unit) {
        val s = _uiState.value
        val loaded = s.loadedIncome
        if (isEditMode && loaded != null && !loaded.isMine) {
            return
        }

        val desc = s.description.trim()
        if (desc.isEmpty()) {
            _uiState.update { it.copy(errorMessage = appContext.getString(R.string.income_error_description)) }
            return
        }
        val cents = parseBrlToCents(s.amountText)
        if (cents == null || cents <= 0) {
            _uiState.update { it.copy(errorMessage = appContext.getString(R.string.income_error_amount)) }
            return
        }
        val incomeDateParsed = parseIsoDate(s.incomeDate) ?: run {
            _uiState.update { it.copy(errorMessage = appContext.getString(R.string.income_error_date)) }
            return
        }
        val incomeDateStr = incomeDateParsed.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val cat = s.categoryId ?: run {
            _uiState.update { it.copy(errorMessage = appContext.getString(R.string.income_error_category)) }
            return
        }
        val notesTrim = s.notes.trim().takeIf { it.isNotEmpty() }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val result = runCatching {
                if (incomeId == null) {
                    incomesApi.createIncome(
                        IncomeCreateDto(
                            description = desc,
                            amountCents = cents,
                            incomeDate = incomeDateStr,
                            incomeCategoryId = cat,
                            notes = notesTrim,
                        ),
                    )
                } else {
                    incomesApi.updateIncome(
                        incomeId,
                        IncomeUpdateDto(
                            description = desc,
                            amountCents = cents,
                            incomeDate = incomeDateStr,
                            incomeCategoryId = cat,
                            notes = notesTrim,
                        ),
                    )
                }
            }
            result.onSuccess {
                _uiState.update { it.copy(isSaving = false) }
                onSuccess()
            }.onFailure { t ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = FastApiErrorMapper.message(appContext, t),
                    )
                }
            }
        }
    }

    fun delete(onSuccess: () -> Unit) {
        val id = incomeId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, showDeleteConfirm = false) }
            runCatching {
                val resp = incomesApi.deleteIncome(id)
                if (!resp.isSuccessful) {
                    error("HTTP ${resp.code()}")
                }
            }
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false) }
                    onSuccess()
                }
                .onFailure { t ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = FastApiErrorMapper.message(appContext, t),
                        )
                    }
                }
        }
    }

    private fun parseIsoDate(s: String): LocalDate? =
        try {
            LocalDate.parse(s.trim(), DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (_: DateTimeParseException) {
            null
        }
}
