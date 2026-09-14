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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    init {
        viewModelScope.launch {
            runCatching { authApi.captcha() }.onSuccess { cfg ->
                val key = cfg.siteKey?.trim().orEmpty()
                _uiState.update {
                    it.copy(
                        captchaEnabled = cfg.enabled && key.isNotEmpty(),
                        captchaSiteKey = key.takeIf { cfg.enabled && key.isNotEmpty() },
                    )
                }
            }
        }
    }

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

    fun onTurnstileToken(token: String) {
        viewModelScope.launch(Dispatchers.Main.immediate) {
            _uiState.update { it.copy(turnstileToken = token, errorMessage = null) }
        }
    }

    fun onTurnstileError() {
        viewModelScope.launch(Dispatchers.Main.immediate) {
            _uiState.update { it.copy(turnstileToken = "") }
        }
    }

    fun submit() {
        val email = _uiState.value.email.trim().lowercase()
        val password = _uiState.value.password
        val confirm = _uiState.value.confirmPassword
        val fullName = _uiState.value.fullName.trim().takeIf { it.isNotEmpty() }
        val phone = _uiState.value.phone.trim().takeIf { it.isNotEmpty() }
        val token = _uiState.value.turnstileToken.trim().takeIf { it.isNotEmpty() }

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
            _uiState.value.captchaEnabled && token.isNullOrEmpty() -> {
                _uiState.update {
                    it.copy(errorMessage = appContext.getString(R.string.register_error_captcha))
                }
            }
            else -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                    runCatching {
                        withContext(Dispatchers.IO) {
                            authApi.register(
                                RegisterRequestDto(
                                    email = email,
                                    password = password,
                                    fullName = fullName,
                                    phone = phone,
                                    turnstileToken = token,
                                ),
                            )
                        }
                    }.onSuccess { response ->
                        _uiState.update { it.copy(isLoading = false) }
                        _events.send(RegisterEvent.NavigateVerify(response.email))
                    }.onFailure { t ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                turnstileToken = "",
                                captchaNonce = it.captchaNonce + 1,
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
    val captchaEnabled: Boolean = false,
    val captchaSiteKey: String? = null,
    val turnstileToken: String = "",
    val captchaNonce: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface RegisterEvent {
    data class NavigateVerify(val email: String) : RegisterEvent
}
