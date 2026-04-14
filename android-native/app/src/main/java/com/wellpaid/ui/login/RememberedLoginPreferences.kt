package com.wellpaid.ui.login

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.IOException
import java.security.GeneralSecurityException

/**
 * Mesmas prefs encriptadas que [LoginViewModel] usa para "lembrar credenciais".
 */
object RememberedLoginPreferences {
    const val PREFS_NAME = "wellpaid_login_remember"
    const val KEY_REMEMBER = "remember"
    const val KEY_EMAIL = "email"
    const val KEY_PASSWORD = "password"

    fun open(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
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

    /** E-mail + senha se "lembrar" estiver ativo e o e-mail coincidir (normalizado). */
    fun readCredentialsIfMatching(context: Context, emailNorm: String): Pair<String, String>? {
        val prefs = runCatching { open(context) }.getOrNull() ?: return null
        if (!prefs.getBoolean(KEY_REMEMBER, false)) return null
        val e = prefs.getString(KEY_EMAIL, "").orEmpty().trim().lowercase()
        val p = prefs.getString(KEY_PASSWORD, "").orEmpty()
        if (e != emailNorm || p.isEmpty()) return null
        return e to p
    }
}
