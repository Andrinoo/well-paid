package com.wellpaid.ui.register

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wellpaid.R
import com.wellpaid.core.model.auth.RegisterRequestDto
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
class RegisterViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val authApi: AuthApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _events = Channel<RegisterEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.update { it.copy(confirmPassword = value, errorMessage = null) }
    }

    fun onFullNameChange(value: String) {
        _uiState.update { it.copy(fullName = value, errorMessage = null) }
    }

    fun onPhoneChange(value: String) {
        _uiState.update { it.copy(phone = value, errorMessage = null) }
    }

    fun submit() {
        val email = _uiState.value.email.trim().lowercase()
        val password = _uiState.value.password
        val confirm = _uiState.value.confirmPassword
        val fullName = _uiState.value.fullName.trim().takeIf { it.isNotEmpty() }
        val phone = _uiState.value.phone.trim().takeIf { it.isNotEmpty() }

        when {
            email.isEmpty() || password.isEmpty() -> {
                _uiState.update {
                    it.copy(errorMessage = appContext.getString(R.string.register_error_empty))
                }
            }
            password.length < 8 -> {
                _uiState.update {
                    it.copy(errorMessage = appContext.getString(R.string.register_error_password_short))
                }
            }
            password != confirm -> {
                _uiState.update {
                    it.copy(errorMessage = appContext.getString(R.string.register_error_password_mismatch))
                }
            }
            else -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                    runCatching {
                        authApi.register(
                            RegisterRequestDto(
                                email = email,
                                password = password,
                                fullName = fullName,
                                phone = phone,
                            ),
                        )
                    }.onSuccess { response ->
                        _uiState.update { it.copy(isLoading = false) }
                        _events.send(RegisterEvent.NavigateVerify(response.email))
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

data class RegisterUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val fullName: String = "",
    val phone: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface RegisterEvent {
    data class NavigateVerify(val email: String) : RegisterEvent
}
