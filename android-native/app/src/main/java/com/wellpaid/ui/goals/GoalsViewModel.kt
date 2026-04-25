package com.wellpaid.ui.goals

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wellpaid.R
import com.wellpaid.core.model.auth.TokenStorage
import com.wellpaid.core.model.goal.GoalDto
import com.wellpaid.core.network.GoalsApi
import com.wellpaid.data.MainPrefetchTiming
import com.wellpaid.util.FastApiErrorMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GoalsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val goalsApi: GoalsApi,
    private val tokenStorage: TokenStorage,
    private val prefetchTiming: MainPrefetchTiming,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalsUiState())
    val uiState: StateFlow<GoalsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            delay(prefetchTiming.goalsDelayMs)
            refresh()
        }
    }

    fun refresh() {
        if (tokenStorage.getAccessToken().isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    goals = emptyList(),
                    errorMessage = appContext.getString(R.string.goals_need_login),
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { goalsApi.listGoals() }
                .onSuccess { list ->
                    _uiState.update {
                        it.copy(isLoading = false, goals = list, errorMessage = null)
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

data class GoalsUiState(
    val goals: List<GoalDto> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)
