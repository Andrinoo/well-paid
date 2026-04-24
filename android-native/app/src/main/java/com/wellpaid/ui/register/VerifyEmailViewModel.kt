package com.wellpaid.ui.register

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wellpaid.R
import com.wellpaid.core.model.auth.ResendVerificationRequestDto
import com.wellpaid.core.model.auth.TokenStorage
import com.wellpaid.core.model.auth.VerifyEmailRequestDto
import com.wellpaid.core.network.DashboardApi
import com.wellpaid.core.network.UserApi
import com.wellpaid.core.network.auth.AuthApi
import com.wellpaid.data.HomeDashboardCacheRepository
import com.wellpaid.util.FastApiErrorMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VerifyEmailViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle,
    private val authApi: AuthApi,
    private val tokenStorage: TokenStorage,
    private val homeDashboardCache: HomeDashboardCacheRepository,
    private val dashboardApi: DashboardApi,
    private val userApi: UserApi,
) : ViewModel() {

    private val initialEmail: String =
        savedStateHandle.get<String>("email")
            ?.takeIf { it.isNotBlank() }
            ?.let { android.net.Uri.decode(it) }
            .orEmpty()

    private val _uiState = MutableStateFlow(VerifyEmailUiState(email = initialEmail))
    val uiState: StateFlow<VerifyEmailUiState> = _uiState.asStateFlow()

    private val _events = Channel<VerifyEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null, infoMessage = null) }
    }

    fun onCodeChange(value: String) {
        val digits = value.filter { it.isDigit() }.take(6)
        _uiState.update { it.copy(code = digits, errorMessage = null, infoMessage = null) }
    }

    fun onTokenChange(value: String) {
        _uiState.update { it.copy(linkToken = value.trim(), errorMessage = null, infoMessage = null) }
    }

    fun submit() {
        val token = _uiState.value.linkToken.trim()
        val email = _uiState.value.email.trim().lowercase()
        val code = _uiState.value.code.trim()

        val body = when {
            token.isNotEmpty() -> VerifyEmailRequestDto(token = token)
            code.length == 6 && email.isNotEmpty() ->
                VerifyEmailRequestDto(email = email, code = code)
            else -> {
                _uiState.update {
                    it.copy(errorMessage = appContext.getString(R.string.verify_error_incomplete))
                }
                return
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
            runCatching { authApi.verifyEmail(body) }
                .onSuccess { pair ->
                    tokenStorage.setTokens(pair.accessToken, pair.refreshToken)
                    runCatching { homeDashboardCache.warmAfterAuth(dashboardApi, userApi) }
                    _uiState.update { it.copy(isLoading = false) }
                    _events.send(VerifyEvent.NavigateToMain)
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

    fun resend() {
        val email = _uiState.value.email.trim().lowercase()
        if (email.isEmpty()) {
            _uiState.update {
                it.copy(errorMessage = appContext.getString(R.string.verify_error_email_required))
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isResending = true, errorMessage = null, infoMessage = null) }
            runCatching {
                authApi.resendVerification(ResendVerificationRequestDto(email))
            }.onSuccess { response ->
                _uiState.update {
                    it.copy(
                        isResending = false,
                        infoMessage = response.message,
                    )
                }
            }.onFailure { t ->
                _uiState.update {
                    it.copy(
                        isResending = false,
                        errorMessage = FastApiErrorMapper.message(appContext, t),
                    )
                }
            }
        }
    }
}

data class VerifyEmailUiState(
    val email: String = "",
    val code: String = "",
    val linkToken: String = "",
    val isLoading: Boolean = false,
    val isResending: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
)

sealed interface VerifyEvent {
    data object NavigateToMain : VerifyEvent
}
