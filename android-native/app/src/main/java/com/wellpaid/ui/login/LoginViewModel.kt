package com.wellpaid.ui.login

import android.content.Context
import android.content.SharedPreferences
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wellpaid.R
import com.wellpaid.core.model.auth.LoginRequestDto
import com.wellpaid.core.model.auth.RefreshRequestDto
import com.wellpaid.core.model.auth.TokenPairDto
import com.wellpaid.core.model.auth.TokenStorage
import com.wellpaid.core.network.auth.AuthApi
import com.wellpaid.security.BiometricLoginPayload
import com.wellpaid.security.BiometricLoginVault
import com.wellpaid.security.parseBiometricLoginPayload
import com.wellpaid.util.FastApiErrorMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import retrofit2.HttpException
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val authApi: AuthApi,
    private val tokenStorage: TokenStorage,
    private val biometricLoginVault: BiometricLoginVault,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = Channel<LoginEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val loginPrefs: SharedPreferences by lazy { RememberedLoginPreferences.open(appContext) }

    init {
        loadRememberedCredentials()
        refreshQuickLoginAvailability()
    }

    fun refreshQuickLoginAvailability() {
        _uiState.update {
            it.copy(
                quickLoginAvailable = biometricLoginVault.hasStoredCredentials() &&
                    biometricLoginVault.canUseStrongBiometric(),
            )
        }
    }

    fun loginWithBiometric(activity: FragmentActivity) {
        biometricLoginVault.retrievePayloadWithBiometric(activity) { json ->
            if (json == null) {
                _uiState.update {
                    it.copy(errorMessage = appContext.getString(R.string.login_biometric_unlock_failed))
                }
                return@retrievePayloadWithBiometric
            }
            val payload = parseBiometricLoginPayload(json)
            if (payload == null) {
                biometricLoginVault.clear()
                _uiState.update {
                    it.copy(errorMessage = appContext.getString(R.string.login_biometric_data_invalid))
                }
                return@retrievePayloadWithBiometric
            }
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                val emailNorm = payload.email.trim().lowercase()
                runCatching {
                    authenticateWithBiometricPayload(payload)
                }.onSuccess { pair ->
                    tokenStorage.setTokens(pair.accessToken, pair.refreshToken)
                    refreshQuickLoginAvailability()
                    _uiState.update { it.copy(isLoading = false) }
                    _events.send(LoginEvent.NavigateToMain)
                }.onFailure { t ->
                    refreshQuickLoginAvailability()
                    val msg = when {
                        t is HttpException && (t.code() == 401 || t.code() == 403) &&
                            rememberedCredentialsMatching(emailNorm) == null ->
                            appContext.getString(R.string.login_biometric_refresh_expired)
                        else -> FastApiErrorMapper.message(appContext, t)
                    }
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = msg)
                    }
                }
            }
        }
    }

    /**
     * Se o refresh guardado expirou, tenta login com e-mail + senha das credenciais lembradas (mesmo utilizador).
     */
    private suspend fun authenticateWithBiometricPayload(payload: BiometricLoginPayload): TokenPairDto {
        val emailNorm = payload.email.trim().lowercase()
        if (!payload.refreshToken.isNullOrBlank()) {
            try {
                return authApi.refresh(RefreshRequestDto(payload.refreshToken))
            } catch (e: HttpException) {
                if (e.code() == 401 || e.code() == 403) {
                    val remembered = rememberedCredentialsMatching(emailNorm)
                    if (remembered != null) {
                        val pair = authApi.login(
                            LoginRequestDto(email = remembered.first, password = remembered.second),
                        )
                        biometricLoginVault.clear()
                        return pair
                    }
                    biometricLoginVault.clear()
                    throw e
                }
                throw e
            }
        }
        if (!payload.password.isNullOrBlank()) {
            return authApi.login(LoginRequestDto(email = emailNorm, password = payload.password))
        }
        throw IOException("invalid stored login")
    }

    private fun rememberedCredentialsMatching(emailNorm: String): Pair<String, String>? =
        RememberedLoginPreferences.readCredentialsIfMatching(appContext, emailNorm)

    private fun loadRememberedCredentials() {
        if (!loginPrefs.getBoolean(RememberedLoginPreferences.KEY_REMEMBER, false)) return
        val email = loginPrefs.getString(RememberedLoginPreferences.KEY_EMAIL, "").orEmpty()
        val password = loginPrefs.getString(RememberedLoginPreferences.KEY_PASSWORD, "").orEmpty()
        _uiState.update {
            it.copy(
                email = email,
                password = password,
                rememberCredentials = true,
            )
        }
    }

    private fun persistRememberedCredentials(email: String, password: String) {
        loginPrefs.edit()
            .putBoolean(RememberedLoginPreferences.KEY_REMEMBER, true)
            .putString(RememberedLoginPreferences.KEY_EMAIL, email)
            .putString(RememberedLoginPreferences.KEY_PASSWORD, password)
            .apply()
    }

    private fun clearRememberedCredentials() {
        loginPrefs.edit().clear().apply()
    }

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun onRememberChange(checked: Boolean) {
        _uiState.update { it.copy(rememberCredentials = checked) }
        if (!checked) {
            clearRememberedCredentials()
        }
    }

    fun submit() {
        val email = _uiState.value.email.trim().lowercase()
        val password = _uiState.value.password
        if (email.isEmpty() || password.isEmpty()) {
            _uiState.update {
                it.copy(errorMessage = appContext.getString(R.string.login_error_empty))
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                authApi.login(LoginRequestDto(email = email, password = password))
            }.onSuccess { pair ->
                if (_uiState.value.rememberCredentials) {
                    persistRememberedCredentials(email, password)
                } else {
                    clearRememberedCredentials()
                }
                tokenStorage.setTokens(pair.accessToken, pair.refreshToken)
                _uiState.update { it.copy(isLoading = false) }
                _events.send(LoginEvent.NavigateToMain)
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

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val rememberCredentials: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    /** Entrada rápida com biometria (credenciais guardadas localmente após confirmação nas definições). */
    val quickLoginAvailable: Boolean = false,
)

sealed interface LoginEvent {
    data object NavigateToMain : LoginEvent
}
