package com.wellpaid.ui.emergency

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wellpaid.R
import com.wellpaid.core.model.auth.TokenStorage
import com.wellpaid.core.model.emergency.EmergencyReserveContributionAllocationDto
import com.wellpaid.core.model.emergency.EmergencyReserveContributionCreateDto
import com.wellpaid.core.model.emergency.EmergencyReserveDto
import com.wellpaid.core.model.emergency.EmergencyReservePlanCreateDto
import com.wellpaid.core.model.emergency.EmergencyReserveMonthRowDto
import com.wellpaid.core.model.emergency.EmergencyReservePlanDto
import com.wellpaid.core.model.emergency.EmergencyReservePlanUpdateDto
import com.wellpaid.core.model.emergency.EmergencyReserveUpdateDto
import com.wellpaid.core.network.EmergencyReserveApi
import com.wellpaid.data.FamilyMeRepository
import com.wellpaid.data.MainPrefetchTiming
import com.wellpaid.data.UiPreferencesRepository
import com.wellpaid.util.FastApiErrorMapper
import com.wellpaid.util.centsToBrlInput
import com.wellpaid.util.parseBrlToCents
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.time.Month
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import kotlin.math.ceil

/** Aviso de plano com início retroativo: mensal corrigida e meta implícita (M × meses). */
data class EmergencyRetroactivePlanOffer(
    val monthsPassed: Int,
    val monthsRemaining: Int,
    val goalCentsForMessage: Int,
    val adjustedMonthlyCents: Int,
    /** Meta total se só houver mensal “original” (M × meses do período). */
    val impliedCeilingCents: Int,
    val hasExplicitTarget: Boolean,
)

data class EmergencyReserveUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isCreatingPlan: Boolean = false,
    val reserve: EmergencyReserveDto? = null,
    val plans: List<EmergencyReservePlanDto> = emptyList(),
    val monthlyTargetText: String = "",
    val selectedPlanId: String? = null,
    val selectedPlanContributionText: String = "",
    val newPlanTitleText: String = "",
    val newPlanDetailsText: String = "",
    val newPlanMonthlyText: String = "",
    val newPlanTargetText: String = "",
    val newPlanDurationMonthsText: String = "",
    val newPlanTrackingStartText: String = "",
    val newPlanTargetEndText: String = "",
    /** Aporte inicial (dinheiro já na reserva ao criar o plano). */
    val newPlanOpeningBalanceText: String = "",
    val newPlanRecommendedMonthlyCents: Int? = null,
    val newPlanRetroOffer: EmergencyRetroactivePlanOffer? = null,
    val newPlanRetroDismissFingerprint: String? = null,
    val editingPlanId: String? = null,
    val editingPlanTitleText: String = "",
    val editingPlanDetailsText: String = "",
    val editingPlanMonthlyText: String = "",
    val editingPlanTargetText: String = "",
    val editingPlanDurationMonthsText: String = "",
    val editingPlanTrackingStartText: String = "",
    val editingPlanTargetEndText: String = "",
    val editingPlanOpeningBalanceText: String = "",
    val editingPlanRecommendedMonthlyCents: Int? = null,
    val editingPlanBalanceCents: Int = 0,
    val editingPlanInitialOpeningCents: Int = 0,
    val editingPlanInitialBalanceCents: Int = 0,
    val editingPlanRetroOffer: EmergencyRetroactivePlanOffer? = null,
    val editingPlanRetroDismissFingerprint: String? = null,
    val isUpdatingPlan: Boolean = false,
    val deletingPlanId: String? = null,
    val showDeletePlanConfirm: Boolean = false,
    val errorMessage: String? = null,
    /** Só o titular da família altera a meta quando existe agregado. */
    val canEditReserve: Boolean = true,
    /** Breakdown mensal (GET …/plans/{id}/months) — ecrã de detalhe do plano. */
    val planDetailMonthRows: List<EmergencyReserveMonthRowDto> = emptyList(),
    val planDetailMonthsLoading: Boolean = false,
    val planDetailMonthsError: String? = null,
)

@HiltViewModel
class EmergencyReserveViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val api: EmergencyReserveApi,
    private val tokenStorage: TokenStorage,
    familyMeRepository: FamilyMeRepository,
    private val prefetchTiming: MainPrefetchTiming,
    uiPreferencesRepository: UiPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmergencyReserveUiState())
    val uiState: StateFlow<EmergencyReserveUiState> = _uiState.asStateFlow()

    /** Em Definições: ocultar data-alvo fim e usar só o período (meses); a data é derivada ao gravar. */
    val hideEmergencyPlanTargetEnd: StateFlow<Boolean> =
        uiPreferencesRepository.emergencyPlanHideTargetEndFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = false,
            )

    private var syncingPlanSchedule = false

    init {
        familyMeRepository.family
            .onEach {
                // Não bloquear edição localmente para evitar falso negativo de role/isSelf.
                // O backend é a autoridade final de permissão.
                _uiState.update { s -> s.copy(canEditReserve = true) }
            }
            .launchIn(viewModelScope)
        viewModelScope.launch {
            delay(prefetchTiming.emergencyDelayMs)
            refresh()
        }
    }

    /**
     * @param rebindEditingPlanId Se definido, após atualizar listas volta a preencher o formulário de edição
     * com esse plano (útil após gravar no ecrã de detalhe).
     */
    fun refresh(rebindEditingPlanId: String? = null) {
        if (tokenStorage.getAccessToken().isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    reserve = null,
                    plans = emptyList(),
                    errorMessage = appContext.getString(R.string.emergency_need_login),
                )
            }
            return
        }

        viewModelScope.launch {
            loadEmergencySnapshot()
            applyRebindAfterRefresh(rebindEditingPlanId)
        }
    }

    private suspend fun loadEmergencySnapshot() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        val reserveResult = runCatching { api.getReserve() }
        val plansResult = runCatching { api.listPlans() }
        val reserve = reserveResult.getOrNull()
        _uiState.update {
            it.copy(
                isLoading = false,
                reserve = reserve,
                plans = plansResult.getOrElse { emptyList() },
                monthlyTargetText = reserve?.let { r -> centsToBrlInput(r.monthlyTargetCents) }
                    ?: it.monthlyTargetText,
                selectedPlanId = it.selectedPlanId ?: plansResult.getOrElse { emptyList() }.firstOrNull()?.id,
                errorMessage = reserveResult.exceptionOrNull()?.let { e ->
                    FastApiErrorMapper.message(appContext, e)
                },
            )
        }
    }

    private fun applyRebindAfterRefresh(rebindEditingPlanId: String?) {
        val rebind = rebindEditingPlanId ?: return
        val plan = _uiState.value.plans.firstOrNull { it.id == rebind } ?: return
        startEditingPlan(plan)
        loadPlanMonthBreakdown(rebind)
    }

    fun prepareEmergencyPlanDetail(planId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(errorMessage = null) }
            if (tokenStorage.getAccessToken().isNullOrBlank()) {
                _uiState.update {
                    it.copy(errorMessage = appContext.getString(R.string.emergency_need_login))
                }
                return@launch
            }
            if (_uiState.value.plans.none { it.id == planId }) {
                loadEmergencySnapshot()
            }
            val plan = _uiState.value.plans.firstOrNull { it.id == planId }
            if (plan == null) {
                _uiState.update {
                    it.copy(errorMessage = appContext.getString(R.string.emergency_plan_detail_not_found))
                }
                return@launch
            }
            selectPlanForContribution(planId)
            startEditingPlan(plan)
            loadPlanMonthBreakdown(planId)
        }
    }

    fun loadPlanMonthBreakdown(planId: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(planDetailMonthsLoading = true, planDetailMonthsError = null)
            }
            runCatching { api.listPlanMonths(planId) }
                .onSuccess { rows ->
                    _uiState.update {
                        it.copy(
                            planDetailMonthRows = rows,
                            planDetailMonthsLoading = false,
                            planDetailMonthsError = null,
                        )
                    }
                }
                .onFailure { t ->
                    _uiState.update {
                        it.copy(
                            planDetailMonthsLoading = false,
                            planDetailMonthsError = FastApiErrorMapper.message(appContext, t),
                        )
                    }
                }
        }
    }

    fun setMonthlyTargetText(value: String) {
        _uiState.update { it.copy(monthlyTargetText = value) }
    }

    fun setNewPlanTitleText(value: String) {
        _uiState.update { it.copy(newPlanTitleText = value) }
    }

    fun setNewPlanMonthlyText(value: String) {
        _uiState.update { it.copy(newPlanMonthlyText = value) }
        recalcNewPlanRecommendation()
    }

    fun setNewPlanDetailsText(value: String) {
        _uiState.update { it.copy(newPlanDetailsText = value.take(1200)) }
    }

    fun setNewPlanTargetText(value: String) {
        _uiState.update { it.copy(newPlanTargetText = value) }
        recalcNewPlanRecommendation()
    }

    fun setNewPlanDurationMonthsText(value: String) {
        val filtered = value.filter { c -> c.isDigit() }.take(3)
        _uiState.update { it.copy(newPlanDurationMonthsText = filtered) }
        if (filtered.isBlank()) {
            syncingPlanSchedule = true
            try {
                _uiState.update { it.copy(newPlanTargetEndText = "") }
            } finally {
                syncingPlanSchedule = false
            }
        } else {
            syncNewPlanEndFromDuration()
        }
        recalcNewPlanRecommendation()
    }

    fun setNewPlanOpeningBalanceText(value: String) {
        _uiState.update { it.copy(newPlanOpeningBalanceText = value) }
        recalcNewPlanRecommendation()
    }

    fun resetNewPlanFormFields() {
        _uiState.update {
            it.copy(
                newPlanTitleText = "",
                newPlanMonthlyText = "",
                newPlanDetailsText = "",
                newPlanTargetText = "",
                newPlanDurationMonthsText = "",
                newPlanTrackingStartText = "",
                newPlanTargetEndText = "",
                newPlanOpeningBalanceText = "",
                newPlanRecommendedMonthlyCents = null,
                newPlanRetroOffer = null,
                newPlanRetroDismissFingerprint = null,
                errorMessage = null,
            )
        }
    }

    fun startEditingPlan(plan: EmergencyReservePlanDto) {
        _uiState.update {
            it.copy(
                editingPlanId = plan.id,
                editingPlanTitleText = plan.title,
                editingPlanDetailsText = plan.details.orEmpty(),
                editingPlanMonthlyText = centsToBrlInput(plan.monthlyTargetCents),
                editingPlanTargetText = plan.targetCents?.let { centsToBrlInput(it) }.orEmpty(),
                editingPlanDurationMonthsText = plan.planDurationMonths?.toString().orEmpty(),
                editingPlanTrackingStartText = plan.trackingStart,
                editingPlanTargetEndText = plan.targetEndDate.orEmpty(),
                editingPlanOpeningBalanceText = centsToBrlInput(plan.openingBalanceCents),
                editingPlanRecommendedMonthlyCents = plan.monthlyNeededCents,
                editingPlanBalanceCents = plan.balanceCents,
                editingPlanInitialOpeningCents = plan.openingBalanceCents,
                editingPlanInitialBalanceCents = plan.balanceCents,
                editingPlanRetroOffer = null,
                editingPlanRetroDismissFingerprint = null,
                errorMessage = null,
            )
        }
        recalcEditingPlanRecommendation()
    }

    fun cancelEditingPlan() {
        _uiState.update {
            it.copy(
                editingPlanId = null,
                editingPlanTitleText = "",
                editingPlanMonthlyText = "",
                editingPlanDetailsText = "",
                editingPlanTargetText = "",
                editingPlanDurationMonthsText = "",
                editingPlanTrackingStartText = "",
                editingPlanTargetEndText = "",
                editingPlanOpeningBalanceText = "",
                editingPlanRecommendedMonthlyCents = null,
                editingPlanBalanceCents = 0,
                editingPlanInitialOpeningCents = 0,
                editingPlanInitialBalanceCents = 0,
                editingPlanRetroOffer = null,
                editingPlanRetroDismissFingerprint = null,
                isUpdatingPlan = false,
            )
        }
    }

    fun setEditingPlanTitleText(value: String) {
        _uiState.update { it.copy(editingPlanTitleText = value) }
    }

    fun setEditingPlanMonthlyText(value: String) {
        _uiState.update { it.copy(editingPlanMonthlyText = value) }
        recalcEditingPlanRecommendation()
    }

    fun setEditingPlanDetailsText(value: String) {
        _uiState.update { it.copy(editingPlanDetailsText = value.take(1200)) }
    }

    fun setEditingPlanTargetText(value: String) {
        _uiState.update { it.copy(editingPlanTargetText = value) }
        recalcEditingPlanRecommendation()
    }

    fun setEditingPlanOpeningBalanceText(value: String) {
        _uiState.update { it.copy(editingPlanOpeningBalanceText = value) }
        recalcEditingPlanRecommendation()
    }

    fun setEditingPlanDurationMonthsText(value: String) {
        val filtered = value.filter { c -> c.isDigit() }.take(3)
        _uiState.update { it.copy(editingPlanDurationMonthsText = filtered) }
        if (filtered.isBlank()) {
            syncingPlanSchedule = true
            try {
                _uiState.update { it.copy(editingPlanTargetEndText = "") }
            } finally {
                syncingPlanSchedule = false
            }
        } else {
            syncEditingPlanEndFromDuration()
        }
        recalcEditingPlanRecommendation()
    }

    fun setNewPlanTrackingStartText(value: String) {
        _uiState.update { it.copy(newPlanTrackingStartText = value) }
        val s = _uiState.value
        when {
            s.newPlanDurationMonthsText.toIntOrNull()?.takeIf { it > 0 } != null ->
                syncNewPlanEndFromDuration()
            parseIsoDateOrNull(s.newPlanTargetEndText) != null ->
                syncNewPlanDurationFromEnd()
        }
        recalcNewPlanRecommendation()
    }

    fun setNewPlanTargetEndText(value: String) {
        _uiState.update { it.copy(newPlanTargetEndText = value) }
        if (value.isBlank()) {
            syncingPlanSchedule = true
            try {
                _uiState.update { it.copy(newPlanDurationMonthsText = "") }
            } finally {
                syncingPlanSchedule = false
            }
        } else {
            syncNewPlanDurationFromEnd()
        }
        recalcNewPlanRecommendation()
    }

    fun setEditingPlanTrackingStartText(value: String) {
        _uiState.update { it.copy(editingPlanTrackingStartText = value) }
        val s = _uiState.value
        when {
            s.editingPlanDurationMonthsText.toIntOrNull()?.takeIf { it > 0 } != null ->
                syncEditingPlanEndFromDuration()
            parseIsoDateOrNull(s.editingPlanTargetEndText) != null ->
                syncEditingPlanDurationFromEnd()
        }
        recalcEditingPlanRecommendation()
    }

    fun setEditingPlanTargetEndText(value: String) {
        _uiState.update { it.copy(editingPlanTargetEndText = value) }
        if (value.isBlank()) {
            syncingPlanSchedule = true
            try {
                _uiState.update { it.copy(editingPlanDurationMonthsText = "") }
            } finally {
                syncingPlanSchedule = false
            }
        } else {
            syncEditingPlanDurationFromEnd()
        }
        recalcEditingPlanRecommendation()
    }

    fun selectPlanForContribution(planId: String) {
        _uiState.update { it.copy(selectedPlanId = planId, selectedPlanContributionText = "") }
    }

    fun setSelectedPlanContributionText(value: String) {
        _uiState.update { it.copy(selectedPlanContributionText = value) }
    }

    fun saveSelectedPlanContribution() {
        val s = _uiState.value
        val planId = s.selectedPlanId
        if (planId.isNullOrBlank()) {
            _uiState.update {
                it.copy(errorMessage = appContext.getString(R.string.emergency_error_plan_name))
            }
            return
        }
        val cents = parseBrlToCents(s.selectedPlanContributionText)?.takeIf { it > 0 }
        if (cents == null) {
            _uiState.update {
                it.copy(errorMessage = appContext.getString(R.string.emergency_error_target))
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            runCatching {
                api.createContribution(
                    EmergencyReserveContributionCreateDto(
                        totalAmountCents = cents,
                        allocations = listOf(
                            EmergencyReserveContributionAllocationDto(
                                planId = planId,
                                amountCents = cents,
                            ),
                        ),
                    ),
                )
            }.onSuccess {
                val rebind = _uiState.value.editingPlanId
                _uiState.update { it.copy(isSaving = false, selectedPlanContributionText = "") }
                refresh(rebindEditingPlanId = rebind)
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

    fun requestDeletePlan(planId: String) {
        _uiState.update { it.copy(showDeletePlanConfirm = true, deletingPlanId = planId) }
    }

    fun dismissDeletePlan() {
        _uiState.update { it.copy(showDeletePlanConfirm = false, deletingPlanId = null) }
    }

    fun createNamedPlan(onSuccess: (() -> Unit)? = null) {
        val s = _uiState.value
        val title = s.newPlanTitleText.trim()
        if (title.isEmpty()) {
            _uiState.update {
                it.copy(errorMessage = appContext.getString(R.string.emergency_error_plan_name))
            }
            return
        }
        val cents = parseBrlToCents(s.newPlanMonthlyText)?.takeIf { it > 0 }
            ?: parseBrlToCents(s.monthlyTargetText)?.takeIf { it > 0 }
        if (cents == null) {
            _uiState.update {
                it.copy(errorMessage = appContext.getString(R.string.emergency_error_target))
            }
            return
        }

        val targetCents = parseBrlToCents(s.newPlanTargetText)?.takeIf { it > 0 }
        val trackingStart = parseIsoDateOrNull(s.newPlanTrackingStartText)
        if (s.newPlanTrackingStartText.isNotBlank() && trackingStart == null) {
            _uiState.update { it.copy(errorMessage = appContext.getString(R.string.expense_error_date)) }
            return
        }
        val targetEndParsed = parseIsoDateOrNull(s.newPlanTargetEndText)
        if (s.newPlanTargetEndText.isNotBlank() && targetEndParsed == null) {
            _uiState.update { it.copy(errorMessage = appContext.getString(R.string.expense_error_due_date)) }
            return
        }

        val durationMonths = s.newPlanDurationMonthsText.toIntOrNull()
        if (s.newPlanDurationMonthsText.isNotBlank() && durationMonths == null) {
            _uiState.update {
                it.copy(errorMessage = appContext.getString(R.string.emergency_error_plan_duration))
            }
            return
        }

        val resolvedEnd = resolveNewPlanEndLocal(s)
        val targetEndForApi = targetEndParsed ?: resolvedEnd

        val openingCents = parseBrlToCents(s.newPlanOpeningBalanceText)?.takeIf { it >= 0 }

        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingPlan = true, errorMessage = null) }
            runCatching {
                api.createPlan(
                    EmergencyReservePlanCreateDto(
                        title = title,
                        details = s.newPlanDetailsText.trim().ifBlank { null },
                        monthlyTargetCents = cents,
                        targetCents = targetCents,
                        trackingStart = trackingStart?.toString(),
                        targetEndDate = targetEndForApi?.toString(),
                        planDurationMonths = durationMonths,
                        openingBalanceCents = openingCents,
                    ),
                )
            }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isCreatingPlan = false,
                            newPlanTitleText = "",
                            newPlanMonthlyText = "",
                            newPlanDetailsText = "",
                            newPlanTargetText = "",
                            newPlanDurationMonthsText = "",
                            newPlanTrackingStartText = "",
                            newPlanTargetEndText = "",
                            newPlanOpeningBalanceText = "",
                            newPlanRecommendedMonthlyCents = null,
                            newPlanRetroOffer = null,
                            newPlanRetroDismissFingerprint = null,
                            errorMessage = null,
                        )
                    }
                    onSuccess?.invoke()
                    refresh()
                }
                .onFailure { t ->
                    _uiState.update {
                        it.copy(
                            isCreatingPlan = false,
                            errorMessage = FastApiErrorMapper.message(appContext, t),
                        )
                    }
                }
        }
    }

    fun saveEditingPlan() {
        val s = _uiState.value
        val planId = s.editingPlanId ?: return
        val existingPlan = s.plans.firstOrNull { it.id == planId }
        val title = s.editingPlanTitleText.trim()
        if (title.isEmpty()) {
            _uiState.update {
                it.copy(errorMessage = appContext.getString(R.string.emergency_error_plan_name))
            }
            return
        }
        val cents = parseBrlToCents(s.editingPlanMonthlyText)?.takeIf { it > 0 }
            ?: existingPlan?.monthlyTargetCents?.takeIf { it > 0 }
        if (cents == null) {
            _uiState.update {
                it.copy(errorMessage = appContext.getString(R.string.emergency_error_target))
            }
            return
        }
        val targetCents = parseBrlToCents(s.editingPlanTargetText)?.takeIf { it > 0 }
        val trackingStart = parseIsoDateOrNull(s.editingPlanTrackingStartText)
        if (s.editingPlanTrackingStartText.isNotBlank() && trackingStart == null) {
            _uiState.update { it.copy(errorMessage = appContext.getString(R.string.expense_error_date)) }
            return
        }
        val targetEndParsed = parseIsoDateOrNull(s.editingPlanTargetEndText)
        if (s.editingPlanTargetEndText.isNotBlank() && targetEndParsed == null) {
            _uiState.update { it.copy(errorMessage = appContext.getString(R.string.expense_error_due_date)) }
            return
        }
        val durationMonths = s.editingPlanDurationMonthsText.toIntOrNull()
        if (s.editingPlanDurationMonthsText.isNotBlank() && durationMonths == null) {
            _uiState.update {
                it.copy(errorMessage = appContext.getString(R.string.emergency_error_plan_duration))
            }
            return
        }

        val resolvedEnd = resolveEditingPlanEndLocal(s)
        val targetEndForApi = targetEndParsed ?: resolvedEnd
        val openingCents = parseBrlToCents(s.editingPlanOpeningBalanceText.trim()) ?: 0

        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingPlan = true, errorMessage = null) }
            runCatching {
                api.updatePlan(
                    planId,
                    EmergencyReservePlanUpdateDto(
                        title = title,
                        details = s.editingPlanDetailsText.trim().ifBlank { null },
                        monthlyTargetCents = cents,
                        targetCents = targetCents,
                        trackingStart = trackingStart?.toString(),
                        targetEndDate = targetEndForApi?.toString(),
                        planDurationMonths = durationMonths,
                        openingBalanceCents = openingCents,
                    ),
                )
            }.onSuccess {
                _uiState.update { it.copy(isUpdatingPlan = false) }
                refresh(rebindEditingPlanId = planId)
            }.onFailure { t ->
                _uiState.update {
                    it.copy(
                        isUpdatingPlan = false,
                        errorMessage = FastApiErrorMapper.message(appContext, t),
                    )
                }
            }
        }
    }

    fun deletePlanConfirmed(onDeleted: (() -> Unit)? = null) {
        val s = _uiState.value
        val planId = s.deletingPlanId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingPlan = true, errorMessage = null) }
            runCatching {
                val resp = api.deletePlan(planId)
                if (!resp.isSuccessful) error("HTTP ${resp.code()}")
            }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isUpdatingPlan = false,
                            showDeletePlanConfirm = false,
                            deletingPlanId = null,
                        )
                    }
                    if (_uiState.value.editingPlanId == planId) cancelEditingPlan()
                    refresh()
                    onDeleted?.invoke()
                }
                .onFailure { t ->
                    _uiState.update {
                        it.copy(
                            isUpdatingPlan = false,
                            errorMessage = FastApiErrorMapper.message(appContext, t),
                        )
                    }
                }
        }
    }

    fun saveMonthlyTarget() {
        val s = _uiState.value
        val cents = parseBrlToCents(s.monthlyTargetText)
        if (cents == null || cents < 0) {
            _uiState.update {
                it.copy(errorMessage = appContext.getString(R.string.emergency_error_target))
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            runCatching {
                api.updateReserve(EmergencyReserveUpdateDto(monthlyTargetCents = cents))
            }
                .onSuccess { r ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            reserve = r,
                            monthlyTargetText = centsToBrlInput(r.monthlyTargetCents),
                            errorMessage = null,
                        )
                    }
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

    fun formatAccrualMonth(year: Int, month: Int): String {
        val m = Month.of(month).getDisplayName(TextStyle.SHORT, Locale("pt", "PT"))
        return "${m.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("pt", "PT")) else it.toString() }} $year"
    }

    private fun syncNewPlanEndFromDuration() {
        if (syncingPlanSchedule) return
        val s = _uiState.value
        val start = parseIsoDateOrNull(s.newPlanTrackingStartText)?.let { firstOfMonth(it) } ?: return
        val dur = s.newPlanDurationMonthsText.toIntOrNull()?.takeIf { it > 0 } ?: return
        val end = start.plusMonths((dur - 1).toLong())
        val endStr = end.toString()
        if (s.newPlanTargetEndText == endStr) return
        syncingPlanSchedule = true
        try {
            _uiState.update { it.copy(newPlanTargetEndText = endStr) }
        } finally {
            syncingPlanSchedule = false
        }
    }

    private fun syncNewPlanDurationFromEnd() {
        if (syncingPlanSchedule) return
        val s = _uiState.value
        val start = parseIsoDateOrNull(s.newPlanTrackingStartText)?.let { firstOfMonth(it) } ?: return
        val end = parseIsoDateOrNull(s.newPlanTargetEndText)?.let { firstOfMonth(it) } ?: return
        if (end < start) return
        val months = monthsBetweenInclusive(start, end)
        val md = months.toString()
        if (s.newPlanDurationMonthsText == md) return
        syncingPlanSchedule = true
        try {
            _uiState.update { it.copy(newPlanDurationMonthsText = md) }
        } finally {
            syncingPlanSchedule = false
        }
    }

    private fun syncEditingPlanEndFromDuration() {
        if (syncingPlanSchedule) return
        val s = _uiState.value
        val start = parseIsoDateOrNull(s.editingPlanTrackingStartText)?.let { firstOfMonth(it) } ?: return
        val dur = s.editingPlanDurationMonthsText.toIntOrNull()?.takeIf { it > 0 } ?: return
        val end = start.plusMonths((dur - 1).toLong())
        val endStr = end.toString()
        if (s.editingPlanTargetEndText == endStr) return
        syncingPlanSchedule = true
        try {
            _uiState.update { it.copy(editingPlanTargetEndText = endStr) }
        } finally {
            syncingPlanSchedule = false
        }
    }

    private fun syncEditingPlanDurationFromEnd() {
        if (syncingPlanSchedule) return
        val s = _uiState.value
        val start = parseIsoDateOrNull(s.editingPlanTrackingStartText)?.let { firstOfMonth(it) } ?: return
        val end = parseIsoDateOrNull(s.editingPlanTargetEndText)?.let { firstOfMonth(it) } ?: return
        if (end < start) return
        val months = monthsBetweenInclusive(start, end)
        val md = months.toString()
        if (s.editingPlanDurationMonthsText == md) return
        syncingPlanSchedule = true
        try {
            _uiState.update { it.copy(editingPlanDurationMonthsText = md) }
        } finally {
            syncingPlanSchedule = false
        }
    }

    private fun parseIsoDateOrNull(value: String): LocalDate? {
        if (value.isBlank()) return null
        return runCatching { LocalDate.parse(value.trim()) }.getOrNull()
    }

    fun dismissNewPlanRetroOffer() {
        val s = _uiState.value
        _uiState.update {
            it.copy(
                newPlanRetroOffer = null,
                newPlanRetroDismissFingerprint = newPlanRetroFingerprint(s),
            )
        }
    }

    fun applyNewPlanRetroCorrection() {
        val offer = _uiState.value.newPlanRetroOffer ?: return
        _uiState.update { s ->
            s.copy(
                newPlanMonthlyText = centsToBrlInput(offer.adjustedMonthlyCents),
                newPlanTargetText = if (!offer.hasExplicitTarget) {
                    centsToBrlInput(offer.impliedCeilingCents)
                } else {
                    s.newPlanTargetText
                },
                newPlanRetroOffer = null,
                newPlanRetroDismissFingerprint = null,
            )
        }
        recalcNewPlanRecommendation()
    }

    fun dismissEditingPlanRetroOffer() {
        val s = _uiState.value
        _uiState.update {
            it.copy(
                editingPlanRetroOffer = null,
                editingPlanRetroDismissFingerprint = editingPlanRetroFingerprint(s),
            )
        }
    }

    fun applyEditingPlanRetroCorrection() {
        val offer = _uiState.value.editingPlanRetroOffer ?: return
        _uiState.update { s ->
            s.copy(
                editingPlanMonthlyText = centsToBrlInput(offer.adjustedMonthlyCents),
                editingPlanTargetText = if (!offer.hasExplicitTarget) {
                    centsToBrlInput(offer.impliedCeilingCents)
                } else {
                    s.editingPlanTargetText
                },
                editingPlanRetroOffer = null,
                editingPlanRetroDismissFingerprint = null,
            )
        }
        recalcEditingPlanRecommendation()
    }

    private fun firstOfMonth(d: LocalDate): LocalDate = d.withDayOfMonth(1)

    private fun monthsBetweenInclusive(start: LocalDate, end: LocalDate): Int {
        val s = firstOfMonth(start)
        val e = firstOfMonth(end)
        if (e < s) return 0
        return (e.year - s.year) * 12 + (e.monthValue - s.monthValue) + 1
    }

    /** Alinha ao backend: meses totais início→fim, meses já decorridos até hoje, meses restantes (mín. 1). */
    private fun timelineMonthsAndRemaining(
        trackingStart: LocalDate,
        targetEnd: LocalDate,
        today: LocalDate = LocalDate.now(),
    ): Triple<Int, Int, Int> {
        val startM = firstOfMonth(trackingStart)
        val endM = firstOfMonth(targetEnd)
        val todayM = firstOfMonth(today)
        val monthsTotal = monthsBetweenInclusive(startM, endM).coerceAtLeast(1)
        val cap = if (todayM < endM) todayM else endM
        val monthsPassed = monthsBetweenInclusive(startM, cap).coerceIn(0, monthsTotal)
        val monthsRemaining = (monthsTotal - monthsPassed).coerceAtLeast(1)
        return Triple(monthsTotal, monthsPassed, monthsRemaining)
    }

    private fun safeMultiplyCents(monthlyCents: Int, months: Int): Int {
        val p = monthlyCents.toLong() * months.toLong()
        return p.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
    }

    private fun newPlanRetroFingerprint(s: EmergencyReserveUiState): String =
        "${s.newPlanTrackingStartText}|${s.newPlanTargetEndText}|${s.newPlanDurationMonthsText}|${s.newPlanOpeningBalanceText}|${s.newPlanMonthlyText}|${s.newPlanTargetText}"

    private fun editingPlanRetroFingerprint(s: EmergencyReserveUiState): String =
        "${s.editingPlanTrackingStartText}|${s.editingPlanTargetEndText}|${s.editingPlanDurationMonthsText}|${s.editingPlanOpeningBalanceText}|${s.editingPlanMonthlyText}|${s.editingPlanTargetText}"

    private fun resolveNewPlanEndLocal(s: EmergencyReserveUiState): LocalDate? {
        parseIsoDateOrNull(s.newPlanTargetEndText)?.let { return firstOfMonth(it) }
        val start = parseIsoDateOrNull(s.newPlanTrackingStartText)?.let { firstOfMonth(it) } ?: return null
        val dur = s.newPlanDurationMonthsText.toIntOrNull()?.takeIf { it > 0 } ?: return null
        return start.plusMonths((dur - 1).toLong())
    }

    private fun resolveEditingPlanEndLocal(s: EmergencyReserveUiState): LocalDate? {
        parseIsoDateOrNull(s.editingPlanTargetEndText)?.let { return firstOfMonth(it) }
        val start = parseIsoDateOrNull(s.editingPlanTrackingStartText)?.let { firstOfMonth(it) } ?: return null
        val dur = s.editingPlanDurationMonthsText.toIntOrNull()?.takeIf { it > 0 } ?: return null
        return start.plusMonths((dur - 1).toLong())
    }

    private fun editingCashCentsForRecalc(s: EmergencyReserveUiState): Int {
        val contribPart = s.editingPlanInitialBalanceCents - s.editingPlanInitialOpeningCents
        val open = parseBrlToCents(s.editingPlanOpeningBalanceText)?.takeIf { it >= 0 }
            ?: s.editingPlanInitialOpeningCents
        return open + contribPart
    }

    private fun recalcNewPlanRecommendation() {
        val s = _uiState.value
        val end = resolveNewPlanEndLocal(s) ?: run {
            _uiState.update {
                it.copy(
                    newPlanRecommendedMonthlyCents = null,
                    newPlanRetroOffer = null,
                )
            }
            return
        }
        val start = parseIsoDateOrNull(s.newPlanTrackingStartText)?.let { firstOfMonth(it) }
            ?: LocalDate.now().withDayOfMonth(1)
        val today = LocalDate.now()
        val (monthsTotal, monthsPassed, monthsRemaining) = timelineMonthsAndRemaining(start, end, today)
        val monthly = parseBrlToCents(s.newPlanMonthlyText)?.takeIf { it > 0 }
        val explicitTarget = parseBrlToCents(s.newPlanTargetText)?.takeIf { it > 0 }
        val impliedCeiling = monthly?.let { safeMultiplyCents(it, monthsTotal) }
        val targetPacingCents = explicitTarget ?: impliedCeiling
        if (targetPacingCents == null || targetPacingCents <= 0) {
            _uiState.update {
                it.copy(
                    newPlanRecommendedMonthlyCents = null,
                    newPlanRetroOffer = null,
                )
            }
            return
        }
        val openingCents = parseBrlToCents(s.newPlanOpeningBalanceText)?.takeIf { it >= 0 } ?: 0
        val remainingAmount = (targetPacingCents - openingCents).coerceAtLeast(0)
        val recommended = if (remainingAmount == 0) {
            0
        } else {
            ceil(remainingAmount.toDouble() / monthsRemaining.toDouble()).toInt()
        }
        val fp = newPlanRetroFingerprint(s)
        val typedDiffers = monthly == null || monthly != recommended
        val offer = if (
            monthsPassed > 0 &&
            recommended > 0 &&
            typedDiffers &&
            fp != s.newPlanRetroDismissFingerprint
        ) {
            val hasExplicit = explicitTarget != null
            val goalMsg = explicitTarget ?: (impliedCeiling ?: targetPacingCents)
            val impliedForApply = impliedCeiling ?: explicitTarget ?: 0
            EmergencyRetroactivePlanOffer(
                monthsPassed = monthsPassed,
                monthsRemaining = monthsRemaining,
                goalCentsForMessage = goalMsg,
                adjustedMonthlyCents = recommended,
                impliedCeilingCents = impliedForApply,
                hasExplicitTarget = hasExplicit,
            )
        } else {
            null
        }
        _uiState.update {
            it.copy(
                newPlanRecommendedMonthlyCents = recommended.takeIf { it > 0 },
                newPlanRetroOffer = offer,
            )
        }
    }

    private fun recalcEditingPlanRecommendation() {
        val s = _uiState.value
        val end = resolveEditingPlanEndLocal(s) ?: run {
            _uiState.update {
                it.copy(
                    editingPlanRecommendedMonthlyCents = null,
                    editingPlanRetroOffer = null,
                )
            }
            return
        }
        val start = parseIsoDateOrNull(s.editingPlanTrackingStartText)?.let { firstOfMonth(it) }
            ?: LocalDate.now().withDayOfMonth(1)
        val today = LocalDate.now()
        val (monthsTotal, monthsPassed, monthsRemaining) = timelineMonthsAndRemaining(start, end, today)
        val monthly = parseBrlToCents(s.editingPlanMonthlyText)?.takeIf { it > 0 }
        val explicitTarget = parseBrlToCents(s.editingPlanTargetText)?.takeIf { it > 0 }
        val impliedCeiling = monthly?.let { safeMultiplyCents(it, monthsTotal) }
        val targetPacingCents = explicitTarget ?: impliedCeiling
        if (targetPacingCents == null || targetPacingCents <= 0) {
            _uiState.update {
                it.copy(
                    editingPlanRecommendedMonthlyCents = null,
                    editingPlanRetroOffer = null,
                )
            }
            return
        }
        val cashCents = editingCashCentsForRecalc(s)
        val remainingAmount = (targetPacingCents - cashCents).coerceAtLeast(0)
        val recommended = if (remainingAmount == 0) {
            0
        } else {
            ceil(remainingAmount.toDouble() / monthsRemaining.toDouble()).toInt()
        }
        val fp = editingPlanRetroFingerprint(s)
        val typedDiffers = monthly == null || monthly != recommended
        val offer = if (
            monthsPassed > 0 &&
            recommended > 0 &&
            typedDiffers &&
            fp != s.editingPlanRetroDismissFingerprint
        ) {
            val hasExplicit = explicitTarget != null
            val goalMsg = explicitTarget ?: (impliedCeiling ?: targetPacingCents)
            val impliedForApply = impliedCeiling ?: explicitTarget ?: 0
            EmergencyRetroactivePlanOffer(
                monthsPassed = monthsPassed,
                monthsRemaining = monthsRemaining,
                goalCentsForMessage = goalMsg,
                adjustedMonthlyCents = recommended,
                impliedCeilingCents = impliedForApply,
                hasExplicitTarget = hasExplicit,
            )
        } else {
            null
        }
        _uiState.update {
            it.copy(
                editingPlanRecommendedMonthlyCents = recommended.takeIf { it > 0 },
                editingPlanRetroOffer = offer,
            )
        }
    }
}
