package com.wellpaid.ui.expenses

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wellpaid.R
import com.wellpaid.core.model.expense.CategoryDto
import com.wellpaid.core.model.expense.ExpenseCreateDto
import com.wellpaid.core.model.expense.ExpenseDto
import com.wellpaid.core.model.expense.ExpenseUpdateDto
import com.wellpaid.core.model.family.FamilyMemberDto
import com.wellpaid.core.network.CategoriesApi
import com.wellpaid.core.network.ExpensesApi
import com.wellpaid.data.FamilyMeRepository
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

enum class NewExpenseKind {
    SINGLE,
    INSTALLMENTS,
    RECURRING,
}

data class ExpenseFormUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val loadedExpense: ExpenseDto? = null,
    val categories: List<CategoryDto> = emptyList(),
    val description: String = "",
    val amountText: String = "",
    val expenseDate: String = "",
    val dueDate: String = "",
    val categoryId: String? = null,
    val status: String = "pending",
    val showDeleteConfirm: Boolean = false,
    /** Só criação. */
    val expenseKind: NewExpenseKind = NewExpenseKind.SINGLE,
    val installmentTotal: Int = 2,
    /** `monthly` | `weekly` | `yearly` */
    val recurringFrequency: String = "monthly",
    val alreadyPaid: Boolean = false,
    val hasDueDate: Boolean = false,
    val isShared: Boolean = false,
    /** `null` = partilha com toda a família quando [isShared] é true. */
    val sharedWithUserId: String? = null,
)

@HiltViewModel
class ExpenseFormViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle,
    private val expensesApi: ExpensesApi,
    private val categoriesApi: CategoriesApi,
    private val familyMeRepository: FamilyMeRepository,
) : ViewModel() {

    private val expenseId: String? = savedStateHandle["expenseId"]

    private val _uiState = MutableStateFlow(ExpenseFormUiState())
    val uiState: StateFlow<ExpenseFormUiState> = _uiState.asStateFlow()

    val isEditMode: Boolean get() = expenseId != null

    init {
        viewModelScope.launch {
            runCatching { categoriesApi.listCategories() }
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

            val id = expenseId
            if (id == null) {
                val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                _uiState.update {
                    it.copy(isLoading = false, expenseDate = today)
                }
            } else {
                runCatching { expensesApi.getExpense(id) }
                    .onSuccess { e ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                loadedExpense = e,
                                description = e.description,
                                amountText = centsToBrlInput(e.amountCents),
                                expenseDate = e.expenseDate,
                                dueDate = e.dueDate.orEmpty(),
                                hasDueDate = !e.dueDate.isNullOrBlank(),
                                categoryId = e.categoryId,
                                status = e.status,
                                isShared = e.isShared,
                                sharedWithUserId = e.sharedWithUserId,
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
        _uiState.update { it.copy(description = value.take(500)) }
    }

    fun setExpenseKind(kind: NewExpenseKind) {
        _uiState.update { it.copy(expenseKind = kind) }
    }

    fun setInstallmentTotal(value: Int) {
        _uiState.update { it.copy(installmentTotal = value.coerceIn(2, 60)) }
    }

    fun setRecurringFrequency(value: String) {
        _uiState.update { it.copy(recurringFrequency = value) }
    }

    fun setAlreadyPaid(value: Boolean) {
        _uiState.update { it.copy(alreadyPaid = value) }
    }

    fun setHasDueDate(value: Boolean) {
        _uiState.update {
            if (value) it.copy(hasDueDate = true) else it.copy(hasDueDate = false, dueDate = "")
        }
    }

    fun setShared(value: Boolean) {
        _uiState.update {
            it.copy(
                isShared = value,
                sharedWithUserId = if (value) it.sharedWithUserId else null,
            )
        }
    }

    fun setSharedWithUserId(userId: String?) {
        _uiState.update { it.copy(sharedWithUserId = userId) }
    }

    fun canShareExpense(): Boolean = familyMeRepository.canShareExpense()

    fun peerMembersForShare(): List<FamilyMemberDto> = familyMeRepository.peerMembersExcludingSelf()

    fun setAmountText(value: String) {
        _uiState.update { it.copy(amountText = value) }
    }

    fun setExpenseDate(value: String) {
        _uiState.update { it.copy(expenseDate = value) }
    }

    fun setDueDate(value: String) {
        _uiState.update { it.copy(dueDate = value) }
    }

    fun setCategoryId(id: String) {
        _uiState.update { it.copy(categoryId = id) }
    }

    fun setStatus(status: String) {
        _uiState.update { it.copy(status = status) }
    }

    fun dismissDeleteConfirm() {
        _uiState.update { it.copy(showDeleteConfirm = false) }
    }

    fun requestDeleteConfirm() {
        _uiState.update { it.copy(showDeleteConfirm = true) }
    }

    fun canEditFields(): Boolean {
        if (!isEditMode) return true
        val e = _uiState.value.loadedExpense ?: return false
        return e.isMine && !e.isProjected
    }

    fun canPay(): Boolean {
        val e = _uiState.value.loadedExpense ?: return false
        return e.isMine && e.status == "pending"
    }

    fun canDelete(): Boolean {
        val e = _uiState.value.loadedExpense ?: return false
        return e.isMine && !e.isProjected
    }

    fun save(onSuccess: () -> Unit) {
        val s = _uiState.value
        val loaded = s.loadedExpense
        if (isEditMode && loaded != null && (!loaded.isMine || loaded.isProjected)) {
            return
        }

        val desc = s.description.trim()
        if (desc.isEmpty()) {
            _uiState.update { it.copy(errorMessage = appContext.getString(R.string.expense_error_description)) }
            return
        }
        val cents = parseBrlToCents(s.amountText)
        if (cents == null || cents <= 0) {
            _uiState.update { it.copy(errorMessage = appContext.getString(R.string.expense_error_amount)) }
            return
        }
        val expenseDateParsed = parseIsoDate(s.expenseDate) ?: run {
            _uiState.update { it.copy(errorMessage = appContext.getString(R.string.expense_error_date)) }
            return
        }
        val expenseDateStr = expenseDateParsed.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val dueParsed = s.dueDate.trim().takeIf { it.isNotEmpty() }?.let { parseIsoDate(it) }
        if (s.dueDate.isNotBlank() && dueParsed == null) {
            _uiState.update { it.copy(errorMessage = appContext.getString(R.string.expense_error_due_date)) }
            return
        }
        if (expenseId == null) {
            if (s.expenseKind == NewExpenseKind.INSTALLMENTS && s.installmentTotal < 2) {
                _uiState.update { it.copy(errorMessage = appContext.getString(R.string.expense_error_installments)) }
                return
            }
            if (s.expenseKind == NewExpenseKind.RECURRING) {
                val rf = s.recurringFrequency.lowercase()
                if (rf !in setOf("monthly", "weekly", "yearly")) {
                    _uiState.update { it.copy(errorMessage = appContext.getString(R.string.expense_error_recurring)) }
                    return
                }
            }
            if (s.hasDueDate && (s.dueDate.isBlank() || dueParsed == null)) {
                _uiState.update { it.copy(errorMessage = appContext.getString(R.string.expense_error_due_date)) }
                return
            }
        }
        val cat = s.categoryId ?: run {
            _uiState.update { it.copy(errorMessage = appContext.getString(R.string.expense_error_category)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val result = runCatching {
                if (expenseId == null) {
                    val installmentOut = when (s.expenseKind) {
                        NewExpenseKind.INSTALLMENTS -> s.installmentTotal.coerceIn(2, 60)
                        else -> 1
                    }
                    val recurringOut = when (s.expenseKind) {
                        NewExpenseKind.RECURRING -> s.recurringFrequency.lowercase()
                        else -> null
                    }
                    val dueOut = if (s.hasDueDate) {
                        dueParsed?.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    } else {
                        null
                    }
                    expensesApi.createExpense(
                        ExpenseCreateDto(
                            description = desc,
                            amountCents = cents,
                            expenseDate = expenseDateStr,
                            dueDate = dueOut,
                            categoryId = cat,
                            status = if (s.alreadyPaid) "paid" else "pending",
                            installmentTotal = installmentOut,
                            recurringFrequency = recurringOut,
                            isShared = s.isShared,
                            sharedWithUserId = if (s.isShared) s.sharedWithUserId else null,
                        ),
                    )
                } else {
                    expensesApi.updateExpense(
                        expenseId,
                        ExpenseUpdateDto(
                            description = desc,
                            amountCents = cents,
                            expenseDate = expenseDateStr,
                            dueDate = dueParsed?.format(DateTimeFormatter.ISO_LOCAL_DATE),
                            categoryId = cat,
                            status = s.status,
                            isShared = s.isShared,
                            sharedWithUserId = if (s.isShared) s.sharedWithUserId else null,
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

    fun pay(onSuccess: () -> Unit) {
        val id = expenseId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            runCatching { expensesApi.payExpense(id) }
                .onSuccess { e ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            loadedExpense = e,
                            status = e.status,
                            errorMessage = null,
                        )
                    }
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

    fun delete(onSuccess: () -> Unit) {
        val id = expenseId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, showDeleteConfirm = false) }
            runCatching {
                val resp = expensesApi.deleteExpense(id)
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
