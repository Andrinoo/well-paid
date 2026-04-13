package com.wellpaid.ui.auth

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wellpaid.R
import com.wellpaid.core.model.auth.ResetPasswordRequestDto
import com.wellpaid.core.network.auth.AuthApi
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
class ResetPasswordViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle,
    private val authApi: AuthApi,
) : ViewModel() {

    private val initialToken: String =
        savedStateHandle.get<String>("token")
            ?.let { android.net.Uri.decode(it) }
            .orEmpty()

    private val _uiState = MutableStateFlow(ResetPasswordUiState(token = initialToken))
    val uiState: StateFlow<ResetPasswordUiState> = _uiState.asStateFlow()

    private val _events = Channel<ResetPasswordEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onTokenChange(value: String) {
        _uiState.update { it.copy(token = value.trim(), errorMessage = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(newPassword = value, errorMessage = null) }
    }

    fun onConfirmChange(value: String) {
        _uiState.update { it.copy(confirmPassword = value, errorMessage = null) }
    }

    fun submit() {
        val token = _uiState.value.token.trim()
        val pass = _uiState.value.newPassword
        val confirm = _uiState.value.confirmPassword
        when {
            token.isEmpty() -> {
                _uiState.update {
                    it.copy(errorMessage = appContext.getString(R.string.reset_password_error_token))
                }
            }
            pass.length < 8 -> {
                _uiState.update {
                    it.copy(errorMessage = appContext.getString(R.string.register_error_password_short))
                }
            }
            pass != confirm -> {
                _uiState.update {
                    it.copy(errorMessage = appContext.getString(R.string.register_error_password_mismatch))
                }
            }
            else -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                    runCatching {
                        authApi.resetPassword(ResetPasswordRequestDto(token = token, newPassword = pass))
                    }.onSuccess {
                        _uiState.update { it.copy(isLoading = false) }
                        _events.send(ResetPasswordEvent.NavigateToLogin)
                    }.onFailure { t ->
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
    }
}

data class ResetPasswordUiState(
    val token: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface ResetPasswordEvent {
    data object NavigateToLogin : ResetPasswordEvent
}
