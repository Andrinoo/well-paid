package com.wellpaid.ui.emergency

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wellpaid.R
import com.wellpaid.core.model.auth.TokenStorage
import com.wellpaid.core.model.emergency.EmergencyReserveAccrualDto
import com.wellpaid.core.model.emergency.EmergencyReserveDto
import com.wellpaid.core.model.emergency.EmergencyReservePlanCreateDto
import com.wellpaid.core.model.emergency.EmergencyReservePlanDto
import com.wellpaid.core.model.emergency.EmergencyReservePlanUpdateDto
import com.wellpaid.core.model.emergency.EmergencyReserveUpdateDto
import com.wellpaid.core.network.EmergencyReserveApi
import com.wellpaid.data.FamilyMeRepository
import com.wellpaid.data.MainPrefetchTiming
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

data class EmergencyReserveUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isCreatingPlan: Boolean = false,
    val reserve: EmergencyReserveDto? = null,
    val plans: List<EmergencyReservePlanDto> = emptyList(),
    val accruals: List<EmergencyReserveAccrualDto> = emptyList(),
    val monthlyTargetText: String = "",
    val newPlanTitleText: String = "",
    val newPlanDetailsText: String = "",
    val newPlanMonthlyText: String = "",
    val newPlanTargetText: String = "",
    val newPlanDurationMonthsText: String = "",
    val editingPlanId: String? = null,
    val editingPlanTitleText: String = "",
    val editingPlanDetailsText: String = "",
    val editingPlanMonthlyText: String = "",
    val editingPlanTargetText: String = "",
    val editingPlanDurationMonthsText: String = "",
    val isUpdatingPlan: Boolean = false,
    val deletingPlanId: String? = null,
    val showDeletePlanConfirm: Boolean = false,
    val errorMessage: String? = null,
    /** Só o titular da família altera a meta quando existe agregado. */
    val canEditReserve: Boolean = true,
)

@HiltViewModel
class EmergencyReserveViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val api: EmergencyReserveApi,
    private val tokenStorage: TokenStorage,
    familyMeRepository: FamilyMeRepository,
    private val prefetchTiming: MainPrefetchTiming,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmergencyReserveUiState())
    val uiState: StateFlow<EmergencyReserveUiState> = _uiState.asStateFlow()

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

    fun refresh() {
        if (tokenStorage.getAccessToken().isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    reserve = null,
                    plans = emptyList(),
                    accruals = emptyList(),
                    errorMessage = appContext.getString(R.string.emergency_need_login),
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val reserveResult = runCatching { api.getReserve() }
            val accrualsResult = runCatching { api.listAccruals(limit = 12) }
            val plansResult = runCatching { api.listPlans() }
            val reserve = reserveResult.getOrNull()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    reserve = reserve,
                    plans = plansResult.getOrElse { emptyList() },
                    accruals = accrualsResult.getOrElse { emptyList() },
                    monthlyTargetText = reserve?.let { r -> centsToBrlInput(r.monthlyTargetCents) }
                        ?: it.monthlyTargetText,
                    errorMessage = reserveResult.exceptionOrNull()?.let { e ->
                        FastApiErrorMapper.message(appContext, e)
                    },
                )
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
    }

    fun setNewPlanDetailsText(value: String) {
        _uiState.update { it.copy(newPlanDetailsText = value.take(1200)) }
    }

    fun setNewPlanTargetText(value: String) {
        _uiState.update { it.copy(newPlanTargetText = value) }
    }

    fun setNewPlanDurationMonthsText(value: String) {
        _uiState.update { it.copy(newPlanDurationMonthsText = value.filter { c -> c.isDigit() }.take(3)) }
    }

    fun resetNewPlanFormFields() {
        _uiState.update {
            it.copy(
                newPlanTitleText = "",
                newPlanMonthlyText = "",
                newPlanDetailsText = "",
                newPlanTargetText = "",
                newPlanDurationMonthsText = "",
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
                errorMessage = null,
            )
        }
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
                isUpdatingPlan = false,
            )
        }
    }

    fun setEditingPlanTitleText(value: String) {
        _uiState.update { it.copy(editingPlanTitleText = value) }
    }

    fun setEditingPlanMonthlyText(value: String) {
        _uiState.update { it.copy(editingPlanMonthlyText = value) }
    }

    fun setEditingPlanDetailsText(value: String) {
        _uiState.update { it.copy(editingPlanDetailsText = value.take(1200)) }
    }

    fun setEditingPlanTargetText(value: String) {
        _uiState.update { it.copy(editingPlanTargetText = value) }
    }

    fun setEditingPlanDurationMonthsText(value: String) {
        _uiState.update { it.copy(editingPlanDurationMonthsText = value.filter { c -> c.isDigit() }.take(3)) }
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

        val durationMonths = s.newPlanDurationMonthsText.toIntOrNull()
        if (s.newPlanDurationMonthsText.isNotBlank() && durationMonths == null) {
            _uiState.update {
                it.copy(errorMessage = appContext.getString(R.string.emergency_error_plan_duration))
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingPlan = true, errorMessage = null) }
            runCatching {
                api.createPlan(
                    EmergencyReservePlanCreateDto(
                        title = title,
                        details = s.newPlanDetailsText.trim().ifBlank { null },
                        monthlyTargetCents = cents,
                        targetCents = targetCents,
                        planDurationMonths = durationMonths,
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
        val title = s.editingPlanTitleText.trim()
        if (title.isEmpty()) {
            _uiState.update {
                it.copy(errorMessage = appContext.getString(R.string.emergency_error_plan_name))
            }
            return
        }
        val cents = parseBrlToCents(s.editingPlanMonthlyText)?.takeIf { it > 0 }
        if (cents == null) {
            _uiState.update {
                it.copy(errorMessage = appContext.getString(R.string.emergency_error_target))
            }
            return
        }
        val targetCents = parseBrlToCents(s.editingPlanTargetText)?.takeIf { it > 0 }
        val durationMonths = s.editingPlanDurationMonthsText.toIntOrNull()
        if (s.editingPlanDurationMonthsText.isNotBlank() && durationMonths == null) {
            _uiState.update {
                it.copy(errorMessage = appContext.getString(R.string.emergency_error_plan_duration))
            }
            return
        }

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
                        planDurationMonths = durationMonths,
                    ),
                )
            }.onSuccess {
                cancelEditingPlan()
                refresh()
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

    fun deletePlanConfirmed() {
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
                    viewModelScope.launch {
                        runCatching { api.listAccruals(limit = 12) }
                            .onSuccess { list ->
                                _uiState.update { it.copy(accruals = list) }
                            }
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
}
