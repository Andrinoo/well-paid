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
import com.wellpaid.util.ExpenseSplitFormMath
import com.wellpaid.util.FastApiErrorMapper
import com.wellpaid.util.centsToBrlInput
import com.wellpaid.util.parseBrlToCents
import com.wellpaid.util.splitTextsForExpenseEdit
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
    val isFamily: Boolean = false,
    val isShared: Boolean = false,
    /** Membro da família com quem partilhar; obrigatório para guardar se [isShared] (API exige peer). */
    val sharedWithUserId: String? = null,
    /** Parte do criador da despesa em BRL (só edição pelo dono). */
    val ownerShareText: String = "",
    /** Parte do outro membro em BRL (sempre derivada do total − dono em modo valor). */
    val peerShareText: String = "",
    /** Se true, partilha por percentagem (`split_mode` = percent); senão por montante. */
    val usePercentSplit: Boolean = false,
    /** Percentagem do dono (0–100), texto tipo `50` ou `50,00`. */
    val ownerPercentText: String = "",
    /** Parte do outro em % (calculada), só leitura na UI. */
    val peerPercentDisplayText: String = "",
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

    /**
     * Partilha exige família com 2+ membros. Observa [FamilyMeRepository.family] para o toggle
     * aparecer quando o /families/me completar (antes a UI não recomputava).
     */
    val canShareExpenseState: StateFlow<Boolean> =
        familyMeRepository.family
            .map { f -> f != null && f.members.size >= 2 }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = false,
            )

    val isEditMode: Boolean get() = expenseId != null

    init {
        viewModelScope.launch { familyMeRepository.refresh() }
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
                        val split = splitTextsForExpenseEdit(e)
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
                                isFamily = e.isFamily,
                                isShared = e.isShared,
                                sharedWithUserId = e.sharedWithUserId,
                                usePercentSplit = split.usePercentSplit,
                                ownerPercentText = split.ownerPercentText,
                                peerPercentDisplayText = split.peerPercentDisplayText,
                                ownerShareText = split.ownerShareText,
                                peerShareText = split.peerShareText,
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
            if (!value) {
                return@update s.copy(
                    isShared = false,
                    sharedWithUserId = null,
                    usePercentSplit = false,
                    ownerPercentText = "",
                    peerPercentDisplayText = "",
                    ownerShareText = "",
                    peerShareText = "",
                )
            }
            val peers = peerMembersForShare()
            val peerId = when {
                s.sharedWithUserId != null -> s.sharedWithUserId
                peers.size == 1 -> peers.first().userId
                else -> null
            }
            val totalCents = parseBrlToCents(s.amountText)
            val half = totalCents?.let { (it + 1) / 2 } ?: 0
            val other = totalCents?.let { it - half } ?: 0
            s.copy(
                isShared = true,
                sharedWithUserId = peerId,
                usePercentSplit = false,
                ownerPercentText = "",
                peerPercentDisplayText = "",
                ownerShareText = if (totalCents != null && totalCents > 0) {
                    centsToBrlInput(half)
                } else {
                    s.ownerShareText
                },
                peerShareText = if (totalCents != null && totalCents > 0) {
                    centsToBrlInput(other)
                } else {
                    s.peerShareText
                },
            )
        }
    }

    fun setFamily(value: Boolean) {
        _uiState.update { it.copy(isFamily = value) }
    }

    fun setUsePercentSplit(value: Boolean) {
        _uiState.update { s ->
            if (!s.isShared) return@update s
            if (!value) {
                val total = parseBrlToCents(s.amountText)
                if (total != null && total > 0 && s.usePercentSplit) {
                    val ob = ExpenseSplitFormMath.parsePercentStringToBps(s.ownerPercentText)
                    val (oc, pc) = if (ob != null) {
                        ExpenseSplitFormMath.allocateCentsFromOwnerBps(total, ob)
                    } else {
                        val half = (total + 1) / 2
                        half to (total - half)
                    }
                    return@update s.copy(
                        usePercentSplit = false,
                        ownerPercentText = "",
                        peerPercentDisplayText = "",
                        ownerShareText = centsToBrlInput(oc),
                        peerShareText = centsToBrlInput(pc),
                    )
                }
                val half = total?.let { (it + 1) / 2 } ?: 0
                val other = total?.let { it - half } ?: 0
                return@update s.copy(
                    usePercentSplit = false,
                    ownerPercentText = "",
                    peerPercentDisplayText = "",
                    ownerShareText = if (total != null && total > 0) centsToBrlInput(half) else s.ownerShareText,
                    peerShareText = if (total != null && total > 0) centsToBrlInput(other) else s.peerShareText,
                )
            }
            val total = parseBrlToCents(s.amountText) ?: return@update s.copy(
                usePercentSplit = true,
                ownerPercentText = "50,00",
                peerPercentDisplayText = "50,00",
            )
            val oc = parseBrlToCents(s.ownerShareText)
            val ownerBps = when {
                oc != null && total > 0 ->
                    ((oc.toLong() * 10000L) / total).toInt().coerceIn(0, 10000)
                else -> 5000
            }
            val ot = ExpenseSplitFormMath.bpsToBrPercentText(ownerBps)
            val pt = ExpenseSplitFormMath.bpsToBrPercentText(10000 - ownerBps)
            s.copy(
                usePercentSplit = true,
                ownerPercentText = ot,
                peerPercentDisplayText = pt,
            )
        }
    }

    fun setOwnerShareText(value: String) {
        _uiState.update { s ->
            val next = s.copy(ownerShareText = ExpenseSplitFormMath.sanitizeBrlLikeInput(value))
            if (!next.isShared || next.usePercentSplit) return@update next
            syncPeerShareFromOwnerAndTotal(next)
        }
    }

    fun setOwnerPercentText(value: String) {
        _uiState.update { s ->
            val ot = ExpenseSplitFormMath.sanitizePercentInput(value)
            val ob = ExpenseSplitFormMath.parsePercentStringToBps(ot)
            val peerDisp = if (ob != null) ExpenseSplitFormMath.bpsToBrPercentText(10000 - ob) else ""
            s.copy(ownerPercentText = ot, peerPercentDisplayText = peerDisp)
        }
    }

    fun setSharedWithUserId(userId: String?) {
        _uiState.update { it.copy(sharedWithUserId = userId) }
    }

    fun peerMembersForShare(): List<FamilyMemberDto> = familyMeRepository.peerMembersExcludingSelf()

    /**
     * Montantes em BRL alinhados ao backend (alocação por bps) para pré-visualização no modo %.
     */
    fun percentSplitDerivedBrlPreview(): Pair<String, String> {
        val s = _uiState.value
        if (!s.isShared || !s.usePercentSplit) return "" to ""
        val total = parseBrlToCents(s.amountText) ?: return "" to ""
        if (total <= 0) return "" to ""
        val ob = ExpenseSplitFormMath.parsePercentStringToBps(s.ownerPercentText) ?: return "" to ""
        val (oc, pc) = ExpenseSplitFormMath.allocateCentsFromOwnerBps(total, ob)
        return centsToBrlInput(oc) to centsToBrlInput(pc)
    }

    fun setAmountText(value: String) {
        _uiState.update { s ->
            val next = s.copy(amountText = ExpenseSplitFormMath.sanitizeBrlLikeInput(value))
            if (!next.isShared || next.usePercentSplit) return@update next
            syncPeerShareFromOwnerAndTotal(next)
        }
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
        _uiState.update { it.copy(payAmountText = ExpenseSplitFormMath.sanitizeBrlLikeInput(value)) }
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
        var ownerPercentBpsOut: Int? = null
        var peerPercentBpsOut: Int? = null
        if (s.isShared) {
            if (s.sharedWithUserId.isNullOrBlank()) {
                _uiState.update { it.copy(errorMessage = appContext.getString(R.string.expense_share_pick_member)) }
                return
            }
            if (s.usePercentSplit) {
                val ob = ExpenseSplitFormMath.parsePercentStringToBps(s.ownerPercentText)
                if (ob == null) {
                    _uiState.update {
                        it.copy(errorMessage = appContext.getString(R.string.expense_split_percent_invalid))
                    }
                    return
                }
                ownerPercentBpsOut = ob
                peerPercentBpsOut = 10000 - ob
            } else {
                val oc = parseBrlToCents(s.ownerShareText)
                if (oc == null || oc < 0 || oc > cents) {
                    _uiState.update { it.copy(errorMessage = appContext.getString(R.string.expense_split_sum_mismatch)) }
                    return
                }
                val pc = cents - oc
                ownerSplitCents = oc
                peerSplitCents = pc
            }
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
                            isFamily = s.isFamily,
                            isShared = s.isShared,
                            sharedWithUserId = if (s.isShared) s.sharedWithUserId else null,
                            splitMode = when {
                                !s.isShared -> null
                                s.usePercentSplit -> "percent"
                                else -> "amount"
                            },
                            ownerShareCents = ownerSplitCents,
                            peerShareCents = peerSplitCents,
                            ownerPercentBps = ownerPercentBpsOut,
                            peerPercentBps = peerPercentBpsOut,
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
                            isFamily = s.isFamily,
                            isShared = s.isShared,
                            sharedWithUserId = if (s.isShared) s.sharedWithUserId else null,
                            splitMode = when {
                                !s.isShared -> null
                                s.usePercentSplit -> "percent"
                                else -> "amount"
                            },
                            ownerShareCents = ownerSplitCents,
                            peerShareCents = peerSplitCents,
                            ownerPercentBps = ownerPercentBpsOut,
                            peerPercentBps = peerPercentBpsOut,
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

    private fun syncPeerShareFromOwnerAndTotal(s: ExpenseFormUiState): ExpenseFormUiState {
        val total = parseBrlToCents(s.amountText)
        val oc = parseBrlToCents(s.ownerShareText)
        if (total == null || oc == null) return s.copy(peerShareText = "")
        val pc = (total - oc).coerceAtLeast(0)
        return s.copy(peerShareText = centsToBrlInput(pc))
    }
}
