package com.wellpaid.ui.goals

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wellpaid.R
import com.wellpaid.core.model.goal.GoalCreateDto
import com.wellpaid.core.model.goal.GoalDto
import com.wellpaid.core.model.goal.GoalUpdateDto
import com.wellpaid.core.network.GoalsApi
import com.wellpaid.util.FastApiErrorMapper
import com.wellpaid.util.centsToBrlInput
import com.wellpaid.util.formatBrlFromCents
import com.wellpaid.util.parseBrlToCents
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GoalFormUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isRefreshingPrice: Boolean = false,
    val loaded: GoalDto? = null,
    val title: String = "",
    val targetText: String = "",
    val initialText: String = "",
    val targetUrl: String = "",
    val isActive: Boolean = true,
    val referencePriceLabel: String? = null,
    val errorMessage: String? = null,
    val showDeleteConfirm: Boolean = false,
)

@HiltViewModel
class GoalFormViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle,
    private val goalsApi: GoalsApi,
) : ViewModel() {

    private val goalId: String? = savedStateHandle.get<String>("goalId")

    private val _uiState = MutableStateFlow(GoalFormUiState())
    val uiState: StateFlow<GoalFormUiState> = _uiState.asStateFlow()

    val isEditMode: Boolean get() = goalId != null

    init {
        val id = goalId
        if (id == null) {
            _uiState.update { it.copy(isLoading = false) }
        } else {
            viewModelScope.launch {
                runCatching { goalsApi.getGoal(id) }
                    .onSuccess { g ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                loaded = g,
                                title = g.title,
                                targetText = centsToBrlInput(g.targetCents),
                                targetUrl = g.targetUrl.orEmpty(),
                                isActive = g.isActive,
                                referencePriceLabel = g.referencePriceCents?.let { c ->
                                    formatBrlFromCents(c)
                                },
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

    fun setTitle(value: String) {
        _uiState.update { it.copy(title = value) }
    }

    fun setTargetText(value: String) {
        _uiState.update { it.copy(targetText = value) }
    }

    fun setInitialText(value: String) {
        _uiState.update { it.copy(initialText = value) }
    }

    fun setActive(value: Boolean) {
        _uiState.update { it.copy(isActive = value) }
    }

    fun setTargetUrl(value: String) {
        _uiState.update { it.copy(targetUrl = value) }
    }

    fun refreshReferencePrice() {
        val id = goalId ?: return
        val url = _uiState.value.targetUrl.trim()
        if (url.isEmpty()) {
            _uiState.update {
                it.copy(errorMessage = appContext.getString(R.string.goal_error_url_required_for_refresh))
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshingPrice = true, errorMessage = null) }
            runCatching { goalsApi.refreshReferencePrice(id) }
                .onSuccess { g ->
                    _uiState.update {
                        it.copy(
                            isRefreshingPrice = false,
                            loaded = g,
                            referencePriceLabel = g.referencePriceCents?.let { c -> formatBrlFromCents(c) },
                        )
                    }
                }
                .onFailure { t ->
                    _uiState.update {
                        it.copy(
                            isRefreshingPrice = false,
                            errorMessage = FastApiErrorMapper.message(appContext, t),
                        )
                    }
                }
        }
    }

    fun dismissDeleteConfirm() {
        _uiState.update { it.copy(showDeleteConfirm = false) }
    }

    fun requestDeleteConfirm() {
        _uiState.update { it.copy(showDeleteConfirm = true) }
    }

    fun canDelete(): Boolean {
        val g = _uiState.value.loaded ?: return false
        return g.isMine && g.currentCents == 0
    }

    fun save(onSuccess: () -> Unit) {
        val s = _uiState.value
        val title = s.title.trim()
        if (title.isEmpty()) {
            _uiState.update { it.copy(errorMessage = appContext.getString(R.string.goal_error_title)) }
            return
        }
        val target = parseBrlToCents(s.targetText)
        if (target == null || target <= 0) {
            _uiState.update { it.copy(errorMessage = appContext.getString(R.string.goal_error_target)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val initialForCreate: Int? = if (goalId == null) {
                if (s.initialText.isBlank()) {
                    0
                } else {
                    parseBrlToCents(s.initialText) ?: run {
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                errorMessage = appContext.getString(R.string.goal_error_initial),
                            )
                        }
                        return@launch
                    }
                }
            } else {
                null
            }
            val result = runCatching {
                if (goalId == null) {
                    val url = s.targetUrl.trim().takeIf { it.isNotEmpty() }
                    goalsApi.createGoal(
                        GoalCreateDto(
                            title = title,
                            targetCents = target,
                            currentCents = initialForCreate!!,
                            isActive = s.isActive,
                            targetUrl = url,
                        ),
                    )
                } else {
                    val loaded = s.loaded ?: error("missing")
                    val url = s.targetUrl.trim().takeIf { it.isNotEmpty() }
                    goalsApi.updateGoal(
                        goalId,
                        GoalUpdateDto(
                            title = title,
                            targetCents = target,
                            currentCents = loaded.currentCents,
                            isActive = s.isActive,
                            targetUrl = url,
                            referenceProductName = loaded.referenceProductName,
                            referencePriceCents = loaded.referencePriceCents,
                            referenceCurrency = loaded.referenceCurrency,
                            priceSource = loaded.priceSource,
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
        val id = goalId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, showDeleteConfirm = false) }
            runCatching {
                val resp = goalsApi.deleteGoal(id)
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

    private fun centsToTargetInput(cents: Int): String {
        val reais = cents / 100
        val c = cents % 100
        return "$reais,${"%02d".format(c)}"
    }
}
