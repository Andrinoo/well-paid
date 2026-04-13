package com.wellpaid.ui.security

import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wellpaid.core.network.UserApi
import com.wellpaid.security.BiometricLoginPayload
import com.wellpaid.security.BiometricLoginVault
import com.wellpaid.security.toJsonString
import com.wellpaid.ui.login.RememberedLoginPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BiometricQuickLoginViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val userApi: UserApi,
    private val vault: BiometricLoginVault,
) : ViewModel() {

    private val _quickLoginEnabled = MutableStateFlow(vault.hasStoredCredentials())
    val quickLoginEnabled: StateFlow<Boolean> = _quickLoginEnabled.asStateFlow()

    fun refreshEnabled() {
        _quickLoginEnabled.value = vault.hasStoredCredentials()
    }

    enum class EnrollResult {
        Success,
        NetworkError,
        BiometricCancelled,
        /** Precisa de "Lembrar credenciais" no login com o mesmo utilizador (senha guardada encriptada). */
        NeedRememberedCredentials,
    }

    /**
     * Guarda e-mail + senha das credenciais lembradas, encriptados com biometria (estável: login directo, sem refresh).
     */
    fun enrollWithCurrentSession(
        activity: FragmentActivity,
        onResult: (EnrollResult) -> Unit,
    ) {
        viewModelScope.launch {
            val email = runCatching { userApi.getCurrentUser().email.trim().lowercase() }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
            if (email == null) {
                onResult(EnrollResult.NetworkError)
                return@launch
            }
            val remembered = RememberedLoginPreferences.readCredentialsIfMatching(appContext, email)
            if (remembered == null) {
                onResult(EnrollResult.NeedRememberedCredentials)
                return@launch
            }
            val (_, password) = remembered
            val json = BiometricLoginPayload(
                email = email,
                password = password,
                refreshToken = null,
            ).toJsonString()
            vault.storePayloadWithBiometric(activity, json) { ok ->
                if (ok) {
                    _quickLoginEnabled.value = true
                    onResult(EnrollResult.Success)
                } else {
                    onResult(EnrollResult.BiometricCancelled)
                }
            }
        }
    }

    fun disableQuickLogin() {
        vault.clear()
        _quickLoginEnabled.value = false
    }
}
