package com.wellpaid.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSecurityPreferences @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val appContext = context.applicationContext

    private val prefs: SharedPreferences by lazy { createPrefs() }

    private fun createPrefs(): SharedPreferences {
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
            throw IllegalStateException("Falha ao criar EncryptedSharedPreferences (segurança)", e)
        } catch (e: IOException) {
            throw IllegalStateException("Falha ao criar EncryptedSharedPreferences (segurança)", e)
        }
    }

    var privacyHideAmounts: Boolean
        get() = prefs.getBoolean(KEY_PRIVACY_HIDE, false)
        set(value) = prefs.edit().putBoolean(KEY_PRIVACY_HIDE, value).apply()

    var appLockEnabled: Boolean
        get() = prefs.getBoolean(KEY_APP_LOCK, false)
        set(value) = prefs.edit().putBoolean(KEY_APP_LOCK, value).apply()

    var biometricUnlockEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC, false)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC, value).apply()

    var lockMethod: AppLockMethod
        get() = AppLockMethod.fromStorage(prefs.getString(KEY_LOCK_METHOD, null))
        set(value) = prefs.edit().putString(KEY_LOCK_METHOD, value.toStorage()).apply()

    fun hasPin(): Boolean =
        !prefs.getString(KEY_PIN_SALT, null).isNullOrBlank() &&
            !prefs.getString(KEY_PIN_HASH, null).isNullOrBlank()

    fun setPinHash(saltHex: String, hashHex: String) {
        prefs.edit()
            .putString(KEY_PIN_SALT, saltHex)
            .putString(KEY_PIN_HASH, hashHex)
            .apply()
    }

    fun clearPin() {
        prefs.edit().remove(KEY_PIN_SALT).remove(KEY_PIN_HASH).apply()
    }

    fun getPinSaltHex(): String? = prefs.getString(KEY_PIN_SALT, null)

    fun getPinHashHex(): String? = prefs.getString(KEY_PIN_HASH, null)

    companion object {
        private const val PREFS_NAME = "well_paid_app_security"
        private const val KEY_PRIVACY_HIDE = "privacy_hide_amounts"
        private const val KEY_APP_LOCK = "app_lock_enabled"
        private const val KEY_BIOMETRIC = "biometric_unlock_enabled"
        private const val KEY_LOCK_METHOD = "lock_method"
        private const val KEY_PIN_SALT = "pin_salt_hex"
        private const val KEY_PIN_HASH = "pin_hash_hex"
    }
}
