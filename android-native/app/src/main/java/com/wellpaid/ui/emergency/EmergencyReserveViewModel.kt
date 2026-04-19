package com.wellpaid.ui.emergency

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wellpaid.R
import com.wellpaid.core.model.auth.TokenStorage
import com.wellpaid.core.model.emergency.EmergencyReserveAccrualDto
import com.wellpaid.core.model.emergency.EmergencyReserveDto
import com.wellpaid.core.model.emergency.EmergencyReservePlanDto
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
    val reserve: EmergencyReserveDto? = null,
    val plans: List<EmergencyReservePlanDto> = emptyList(),
    val accruals: List<EmergencyReserveAccrualDto> = emptyList(),
    val monthlyTargetText: String = "",
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
            .onEach { f ->
                val can = f == null || (f.members.find { it.isSelf }?.role == "owner")
                _uiState.update { it.copy(canEditReserve = can) }
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

    fun saveMonthlyTarget() {
        val s = _uiState.value
        if (!s.canEditReserve) {
            _uiState.update {
                it.copy(errorMessage = appContext.getString(R.string.emergency_readonly_not_owner))
            }
            return
        }
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
