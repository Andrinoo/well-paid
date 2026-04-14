package com.wellpaid.security

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.wellpaid.core.model.auth.TokenStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSecurityManager @Inject constructor(
    private val tokenStorage: TokenStorage,
    private val prefs: AppSecurityPreferences,
) : DefaultLifecycleObserver {

    private val _locked = MutableStateFlow(false)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    private val _privacyHideAmounts = MutableStateFlow(prefs.privacyHideAmounts)
    val privacyHideAmounts: StateFlow<Boolean> = _privacyHideAmounts.asStateFlow()

    private val _lockMethod = MutableStateFlow(prefs.lockMethod)
    val lockMethod: StateFlow<AppLockMethod> = _lockMethod.asStateFlow()

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStop(owner: LifecycleOwner) {
        if (shouldEnforceLock()) {
            _locked.value = true
        }
    }

    /** Chamado quando a UI principal fica disponível com possível sessão (evita flash antes do token). */
    fun applyColdStartLockIfNeeded() {
        if (shouldEnforceLock()) {
            _locked.value = true
        }
    }

    fun setPrivacyHideAmounts(hide: Boolean) {
        prefs.privacyHideAmounts = hide
        _privacyHideAmounts.value = hide
    }

    fun setAppLockEnabled(enabled: Boolean) {
        prefs.appLockEnabled = enabled
        if (!enabled) {
            prefs.biometricUnlockEnabled = false
            prefs.clearPin()
            prefs.lockMethod = AppLockMethod.BOTH
            _lockMethod.value = AppLockMethod.BOTH
            _locked.value = false
        }
    }

    fun setLockMethod(method: AppLockMethod) {
        prefs.lockMethod = method
        when (method) {
            AppLockMethod.PIN_ONLY -> prefs.biometricUnlockEnabled = false
            AppLockMethod.BIOMETRIC_ONLY -> {
                if (prefs.appLockEnabled) prefs.clearPin()
                prefs.biometricUnlockEnabled = true
            }
            AppLockMethod.BOTH -> Unit
        }
        _lockMethod.value = method
    }

    fun setBiometricUnlockEnabled(enabled: Boolean) {
        prefs.biometricUnlockEnabled = enabled
    }

    fun saveNewPin(pin: String) {
        val salt = PinHasher.newSaltHex()
        val hash = PinHasher.hashPin(pin, salt)
        prefs.setPinHash(salt, hash)
        prefs.appLockEnabled = true
    }

    /** Ativa bloqueio só com biometria (remove PIN guardado). */
    fun enableBiometricOnlyLock() {
        prefs.clearPin()
        prefs.lockMethod = AppLockMethod.BIOMETRIC_ONLY
        prefs.biometricUnlockEnabled = true
        prefs.appLockEnabled = true
        _lockMethod.value = AppLockMethod.BIOMETRIC_ONLY
    }

    fun verifyPin(pin: String): Boolean =
        PinHasher.verify(pin, prefs.getPinSaltHex(), prefs.getPinHashHex())

    fun tryUnlockWithPin(pin: String): Boolean {
        if (!verifyPin(pin)) return false
        _locked.value = false
        return true
    }

    fun unlockFromBiometric() {
        if (!prefs.appLockEnabled || !prefs.biometricUnlockEnabled) return
        when (prefs.lockMethod) {
            AppLockMethod.PIN_ONLY -> return
            AppLockMethod.BIOMETRIC_ONLY, AppLockMethod.BOTH -> _locked.value = false
        }
    }

    fun onLoggedOut() {
        _locked.value = false
    }

    fun hasPin(): Boolean = prefs.hasPin()

    fun appLockEnabled(): Boolean = prefs.appLockEnabled

    fun biometricUnlockEnabled(): Boolean = prefs.biometricUnlockEnabled

    fun currentLockMethod(): AppLockMethod = prefs.lockMethod

    private fun shouldEnforceLock(): Boolean {
        if (!prefs.appLockEnabled) return false
        if (tokenStorage.getAccessToken().isNullOrBlank()) return false
        return when (prefs.lockMethod) {
            AppLockMethod.PIN_ONLY -> prefs.hasPin()
            AppLockMethod.BIOMETRIC_ONLY -> prefs.biometricUnlockEnabled
            AppLockMethod.BOTH -> prefs.hasPin()
        }
    }
}
