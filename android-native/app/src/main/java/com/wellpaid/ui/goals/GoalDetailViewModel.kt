package com.wellpaid.ui.goals

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wellpaid.R
import com.wellpaid.core.model.goal.GoalContributeDto
import com.wellpaid.core.model.goal.GoalDto
import com.wellpaid.core.model.goal.GoalPriceHistoryItemDto
import com.wellpaid.core.network.GoalsApi
import com.wellpaid.util.FastApiErrorMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.Response
import javax.inject.Inject

data class GoalDetailUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val isRefreshingFromLink: Boolean = false,
    val goal: GoalDto? = null,
    val priceHistory: List<GoalPriceHistoryItemDto> = emptyList(),
    val errorMessage: String? = null,
)

@HiltViewModel
class GoalDetailViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle,
    private val goalsApi: GoalsApi,
) : ViewModel() {

    private val goalId: String = checkNotNull(savedStateHandle.get<String>("goalId"))

    private val _uiState = MutableStateFlow(GoalDetailUiState())
    val uiState: StateFlow<GoalDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            loadInternal()
            savedStateHandle.getStateFlow("goal_detail_refresh", 0L).collect { token ->
                if (token != 0L) {
                    loadInternal()
                }
            }
        }
    }

    private suspend fun loadInternal() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        runCatching { goalsApi.getGoal(goalId) }
            .onSuccess { g ->
                val history = runCatching { goalsApi.priceHistory(goalId).items }.getOrDefault(emptyList())
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        goal = g,
                        priceHistory = history,
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

    fun contribute(amountCents: Int, note: String?, onSuccess: () -> Unit) {
        if (amountCents <= 0) {
            _uiState.update {
                it.copy(errorMessage = appContext.getString(R.string.goal_error_contribute_amount))
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            runCatching {
                goalsApi.contribute(
                    goalId,
                    GoalContributeDto(
                        amountCents = amountCents,
                        note = note?.trim()?.takeIf { it.isNotEmpty() },
                    ),
                )
            }
                .onSuccess { g ->
                    val history = runCatching { goalsApi.priceHistory(goalId).items }.getOrDefault(emptyList())
                    _uiState.update { it.copy(isSaving = false, goal = g, priceHistory = history) }
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

    fun refreshTargetFromLink(onDone: () -> Unit = {}) {
        val g = _uiState.value.goal ?: return
        if (!g.isMine) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshingFromLink = true, errorMessage = null) }
            runCatching { goalsApi.refreshReferencePrice(goalId) }
                .onSuccess { updated ->
                    val history = runCatching { goalsApi.priceHistory(goalId).items }.getOrDefault(emptyList())
                    _uiState.update {
                        it.copy(
                            isRefreshingFromLink = false,
                            goal = updated,
                            priceHistory = history,
                            errorMessage = null,
                        )
                    }
                    onDone()
                }
                .onFailure { t ->
                    _uiState.update {
                        it.copy(
                            isRefreshingFromLink = false,
                            errorMessage = FastApiErrorMapper.message(appContext, t),
                        )
                    }
                }
        }
    }

    fun canDelete(): Boolean {
        val g = _uiState.value.goal ?: return false
        return g.isMine && g.currentCents == 0
    }

    fun deleteGoal(onSuccess: () -> Unit) {
        if (!canDelete()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, errorMessage = null) }
            runCatching {
                val resp: Response<Void> = goalsApi.deleteGoal(goalId)
                if (!resp.isSuccessful) {
                    error("HTTP ${resp.code()}")
                }
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
