package com.wellpaid.ui.expenses

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wellpaid.R
import com.wellpaid.core.model.expense.CategoryDto
import com.wellpaid.core.model.expense.ExpenseCoverRequestDto
import com.wellpaid.core.model.expense.ExpenseCreateDto
import com.wellpaid.core.model.expense.ExpenseDto
import com.wellpaid.core.model.expense.ExpensePayDto
import com.wellpaid.core.model.expense.ExpenseShareDeclineDto
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
    /** Parte do criador da despesa em BRL (só edição pelo dono). */
    val ownerShareText: String = "",
    /** Parte do outro membro em BRL. */
    val peerShareText: String = "",
    val showCoverDialog: Boolean = false,
    val coverSettleByIso: String = "",
    val showPayConfirm: Boolean = false,
    val payAllowAdvance: Boolean = false,
    val payAmountText: String = "",
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
                        val split = splitTextsForEdit(e)
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
                                ownerShareText = split.first,
                                peerShareText = split.second,
                                coverSettleByIso = LocalDate.now().plusMonths(1)
                                    .format(DateTimeFormatter.ISO_LOCAL_DATE),
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
        _uiState.update {
            val needDue =
                kind == NewExpenseKind.INSTALLMENTS || kind == NewExpenseKind.RECURRING
            var next = it.copy(
                expenseKind = kind,
                hasDueDate = if (needDue) true else it.hasDueDate,
            )
            if (kind == NewExpenseKind.INSTALLMENTS) {
                val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                val due = if (it.dueDate.isBlank()) today else it.dueDate
                next = next.copy(dueDate = due, expenseDate = due)
            }
            next
        }
    }

    fun setInstallmentTotal(value: Int) {
        _uiState.update { it.copy(installmentTotal = value.coerceIn(2, 999)) }
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
        _uiState.update { s ->
            val peers = peerMembersForShare()
            val peerId = when {
                !value -> null
                s.sharedWithUserId != null -> s.sharedWithUserId
                peers.size == 1 -> peers.first().userId
                else -> null
            }
            val totalCents = parseBrlToCents(s.amountText)
            val half = totalCents?.let { (it + 1) / 2 } ?: 0
            val other = totalCents?.let { it - half } ?: 0
            s.copy(
                isShared = value,
                sharedWithUserId = peerId,
                ownerShareText = if (value && totalCents != null && totalCents > 0) {
                    centsToBrlInput(half)
                } else if (!value) {
                    ""
                } else {
                    s.ownerShareText
                },
                peerShareText = if (value && totalCents != null && totalCents > 0) {
                    centsToBrlInput(other)
                } else if (!value) {
                    ""
                } else {
                    s.peerShareText
                },
            )
        }
    }

    fun setOwnerShareText(value: String) {
        _uiState.update { it.copy(ownerShareText = sanitizeBrlInput(value)) }
    }

    fun setPeerShareText(value: String) {
        _uiState.update { it.copy(peerShareText = sanitizeBrlInput(value)) }
    }

    private fun splitTextsForEdit(e: ExpenseDto): Pair<String, String> {
        if (!e.isShared || !e.isMine) return "" to ""
        val total = e.amountCents
        val my = e.myShareCents ?: ((total + 1) / 2)
        val other = e.otherUserShareCents ?: (total - my)
        return centsToBrlInput(my) to centsToBrlInput(other)
    }

    fun setSharedWithUserId(userId: String?) {
        _uiState.update { it.copy(sharedWithUserId = userId) }
    }

    fun canShareExpense(): Boolean = familyMeRepository.canShareExpense()

    fun peerMembersForShare(): List<FamilyMemberDto> = familyMeRepository.peerMembersExcludingSelf()

    fun setAmountText(value: String) {
        _uiState.update { it.copy(amountText = sanitizeBrlInput(value)) }
    }

    fun setExpenseDate(value: String) {
        _uiState.update { it.copy(expenseDate = value) }
    }

    fun setDueDate(value: String) {
        _uiState.update {
            if (it.expenseKind == NewExpenseKind.INSTALLMENTS) {
                it.copy(dueDate = value, expenseDate = value)
            } else {
                it.copy(dueDate = value)
            }
        }
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
        if (e.status != "pending" || e.isProjected) return false
        if (e.isShared && e.myShareDeclined) return false
        if (e.isShared && e.isMine && e.sharedExpensePeerDeclinedAlert) return false
        if (e.isShared) return true
        return e.isMine
    }

    fun canRequestCover(): Boolean {
        val e = _uiState.value.loadedExpense ?: return false
        return e.isShared &&
            e.status == "pending" &&
            !e.mySharePaid &&
            !e.myShareDeclined &&
            !e.isProjected
    }

    fun canDeclineShare(): Boolean {
        val e = _uiState.value.loadedExpense ?: return false
        return e.isShared &&
            !e.isMine &&
            e.status == "pending" &&
            !e.mySharePaid &&
            !e.myShareDeclined &&
            !e.isProjected
    }

    fun canAssumeFull(): Boolean {
        val e = _uiState.value.loadedExpense ?: return false
        return e.isMine &&
            e.isShared &&
            e.sharedExpensePeerDeclinedAlert &&
            e.status == "pending" &&
            !e.isProjected
    }

    fun openCoverDialog() {
        _uiState.update {
            it.copy(
                showCoverDialog = true,
                coverSettleByIso = it.coverSettleByIso.ifBlank {
                    LocalDate.now().plusMonths(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
                },
            )
        }
    }

    fun dismissCoverDialog() {
        _uiState.update { it.copy(showCoverDialog = false) }
    }

    fun setCoverSettleByIso(value: String) {
        _uiState.update { it.copy(coverSettleByIso = value) }
    }

    fun declineShare(onSuccess: () -> Unit) {
        val id = expenseId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            runCatching {
                expensesApi.declineExpenseShare(id, ExpenseShareDeclineDto())
            }
                .onSuccess { e ->
                    _uiState.update {
                        it.copy(isSaving = false, loadedExpense = e, errorMessage = null)
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

    fun assumeFullShare(onSuccess: () -> Unit) {
        val id = expenseId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            runCatching { expensesApi.assumeFullExpenseShare(id) }
                .onSuccess { e ->
                    _uiState.update {
                        it.copy(isSaving = false, loadedExpense = e, errorMessage = null)
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

    fun submitCover(onSuccess: () -> Unit) {
        val id = expenseId ?: return
        val settle = _uiState.value.coverSettleByIso.trim()
        if (settle.isEmpty()) {
            _uiState.update { it.copy(errorMessage = appContext.getString(R.string.expense_error_due_date)) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, showCoverDialog = false) }
            runCatching {
                expensesApi.requestShareCover(id, ExpenseCoverRequestDto(settleBy = settle))
            }
                .onSuccess { e ->
                    _uiState.update {
                        it.copy(isSaving = false, loadedExpense = e, errorMessage = null)
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

    fun requestPayConfirm() {
        val e = _uiState.value.loadedExpense ?: return
        _uiState.update {
            it.copy(
                showPayConfirm = true,
                payAllowAdvance = false,
                payAmountText = centsToBrlInput(e.amountCents),
                errorMessage = null,
            )
        }
    }

    fun dismissPayConfirm() {
        _uiState.update { it.copy(showPayConfirm = false, payAllowAdvance = false) }
    }

    fun setPayAllowAdvance(value: Boolean) {
        _uiState.update { it.copy(payAllowAdvance = value) }
    }

    fun setPayAmountText(value: String) {
        _uiState.update { it.copy(payAmountText = sanitizeBrlInput(value)) }
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
        val dueParsed = s.dueDate.trim().takeIf { it.isNotEmpty() }?.let { parseIsoDate(it) }
        if (s.dueDate.isNotBlank() && dueParsed == null) {
            _uiState.update { it.copy(errorMessage = appContext.getString(R.string.expense_error_due_date)) }
            return
        }
        val expenseDateStr: String = if (expenseId == null && s.expenseKind == NewExpenseKind.INSTALLMENTS) {
            val d = dueParsed ?: run {
                _uiState.update { it.copy(errorMessage = appContext.getString(R.string.expense_error_due_date)) }
                return
            }
            d.format(DateTimeFormatter.ISO_LOCAL_DATE)
        } else {
            val expenseDateParsed = parseIsoDate(s.expenseDate) ?: run {
                _uiState.update { it.copy(errorMessage = appContext.getString(R.string.expense_error_date)) }
                return
            }
            expenseDateParsed.format(DateTimeFormatter.ISO_LOCAL_DATE)
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
            if ((s.expenseKind == NewExpenseKind.INSTALLMENTS || s.expenseKind == NewExpenseKind.RECURRING) &&
                (!s.hasDueDate || dueParsed == null)
            ) {
                _uiState.update { it.copy(errorMessage = appContext.getString(R.string.expense_error_due_date)) }
                return
            }
        }
        val cat = s.categoryId ?: run {
            _uiState.update { it.copy(errorMessage = appContext.getString(R.string.expense_error_category)) }
            return
        }

        var ownerSplitCents: Int? = null
        var peerSplitCents: Int? = null
        if (s.isShared) {
            if (s.sharedWithUserId.isNullOrBlank()) {
                _uiState.update { it.copy(errorMessage = appContext.getString(R.string.expense_share_pick_member)) }
                return
            }
            val oc = parseBrlToCents(s.ownerShareText)
            val pc = parseBrlToCents(s.peerShareText)
            if (oc == null || pc == null || oc + pc != cents) {
                _uiState.update { it.copy(errorMessage = appContext.getString(R.string.expense_split_sum_mismatch)) }
                return
            }
            ownerSplitCents = oc
            peerSplitCents = pc
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val result = runCatching {
                if (expenseId == null) {
                    val installmentOut = when (s.expenseKind) {
                        NewExpenseKind.INSTALLMENTS -> s.installmentTotal.coerceIn(2, 999)
                        else -> 1
                    }
                    val recurringOut = when (s.expenseKind) {
                        NewExpenseKind.RECURRING -> s.recurringFrequency.lowercase()
                        else -> null
                    }
                    val dueOut = when {
                        s.expenseKind == NewExpenseKind.INSTALLMENTS ->
                            dueParsed?.format(DateTimeFormatter.ISO_LOCAL_DATE)
                        s.hasDueDate -> dueParsed?.format(DateTimeFormatter.ISO_LOCAL_DATE)
                        else -> null
                    }
                    val startOut: String? =
                        if (s.expenseKind == NewExpenseKind.INSTALLMENTS) null else expenseDateStr
                    expensesApi.createExpense(
                        ExpenseCreateDto(
                            description = desc,
                            amountCents = cents,
                            expenseDate = expenseDateStr,
                            startDate = startOut,
                            dueDate = dueOut,
                            categoryId = cat,
                            status = if (s.alreadyPaid) "paid" else "pending",
                            installmentTotal = installmentOut,
                            recurringFrequency = recurringOut,
                            isShared = s.isShared,
                            sharedWithUserId = if (s.isShared) s.sharedWithUserId else null,
                            splitMode = if (s.isShared) "amount" else null,
                            ownerShareCents = ownerSplitCents,
                            peerShareCents = peerSplitCents,
                            ownerPercentBps = null,
                            peerPercentBps = null,
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
                            splitMode = if (s.isShared) "amount" else null,
                            ownerShareCents = ownerSplitCents,
                            peerShareCents = peerSplitCents,
                            ownerPercentBps = null,
                            peerPercentBps = null,
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
        val current = _uiState.value.loadedExpense ?: return
        viewModelScope.launch {
            val payAmountCents = if (!current.recurringSeriesId.isNullOrBlank()) {
                val txt = _uiState.value.payAmountText.trim()
                if (txt.isBlank()) null else parseBrlToCents(txt)
            } else {
                null
            }
            if (!current.recurringSeriesId.isNullOrBlank() && payAmountCents != null && payAmountCents <= 0) {
                _uiState.update {
                    it.copy(errorMessage = appContext.getString(R.string.expense_error_amount))
                }
                return@launch
            }

            _uiState.update { it.copy(isSaving = true, errorMessage = null, showPayConfirm = false) }
            runCatching {
                expensesApi.payExpense(
                    id,
                    ExpensePayDto(
                        allowAdvance = _uiState.value.payAllowAdvance,
                        amountCents = payAmountCents,
                    ),
                )
            }
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

    fun delete(onSuccess: () -> Unit, installmentFullWipeIncludingPaid: Boolean = false) {
        val id = expenseId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, showDeleteConfirm = false) }
            runCatching {
                val e = _uiState.value.loadedExpense
                val resp = if (e != null && !e.installmentGroupId.isNullOrBlank()) {
                    when {
                        installmentFullWipeIncludingPaid -> expensesApi.deleteExpense(
                            id,
                            deleteTarget = "series",
                            deleteScope = "all",
                            confirmDeletePaid = true,
                        )
                        e.installmentPlanHasPaid == true -> expensesApi.deleteExpense(
                            id,
                            deleteTarget = "series",
                            deleteScope = "future_unpaid",
                        )
                        else -> expensesApi.deleteExpense(
                            id,
                            deleteTarget = "series",
                            deleteScope = "all",
                        )
                    }
                } else {
                    expensesApi.deleteExpense(id)
                }
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

    /**
     * Mantém o texto digitado num formato simples (apenas dígitos e separadores '.' / ','),
     * para que `backspace`/caret do teclado não "remonte" o valor de forma inesperada.
     *
     * A conversão para centavos é feita somente no `save()` via `parseBrlToCents()`.
     */
    private fun sanitizeBrlInput(raw: String): String {
        val filtered = raw.filter { it.isDigit() || it == ',' || it == '.' }
        if (filtered.isEmpty()) return ""

        val lastComma = filtered.lastIndexOf(',')
        val lastDot = filtered.lastIndexOf('.')
        val decSep = when {
            lastComma >= 0 && lastDot >= 0 -> if (lastComma > lastDot) ',' else '.'
            lastComma >= 0 -> ','
            lastDot >= 0 -> '.'
            else -> null
        } ?: return filtered

        val idx = filtered.lastIndexOf(decSep)
        val intPart = filtered.substring(0, idx)
        val decPart = filtered.substring(idx + 1).take(2)
        return intPart + decSep + decPart
    }
}
