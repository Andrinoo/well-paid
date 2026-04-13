package com.wellpaid.ui.login

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.wellpaid.R
import com.wellpaid.core.model.auth.LoginRequestDto
import com.wellpaid.core.model.auth.TokenStorage
import com.wellpaid.core.network.auth.AuthApi
import com.wellpaid.util.FastApiErrorMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.security.GeneralSecurityException
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
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = Channel<LoginEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val loginPrefs: SharedPreferences by lazy { createLoginPrefs() }

    init {
        loadRememberedCredentials()
    }

    private fun createLoginPrefs(): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                appContext,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (e: GeneralSecurityException) {
            throw IllegalStateException("Falha ao criar prefs de login", e)
        } catch (e: IOException) {
            throw IllegalStateException("Falha ao criar prefs de login", e)
        }
    }

    private fun loadRememberedCredentials() {
        if (!loginPrefs.getBoolean(KEY_REMEMBER, false)) return
        val email = loginPrefs.getString(KEY_EMAIL, "").orEmpty()
        val password = loginPrefs.getString(KEY_PASSWORD, "").orEmpty()
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
            .putBoolean(KEY_REMEMBER, true)
            .putString(KEY_EMAIL, email)
            .putString(KEY_PASSWORD, password)
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

    companion object {
        private const val PREFS_NAME = "wellpaid_login_remember"
        private const val KEY_REMEMBER = "remember"
        private const val KEY_EMAIL = "email"
        private const val KEY_PASSWORD = "password"
    }
}

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val rememberCredentials: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface LoginEvent {
    data object NavigateToMain : LoginEvent
}
