package com.wellpaid.core.datastore

import android.content.Context
import android.content.SharedPreferences
import com.wellpaid.core.model.auth.TokenStorage
import java.io.IOException
import java.security.GeneralSecurityException

class EncryptedTokenStorage(
    context: Context,
) : TokenStorage {

    private val appContext = context.applicationContext

    private val prefs: SharedPreferences by lazy { createPrefs() }

    private fun createPrefs(): SharedPreferences {
        return try {
            EncryptedSharedPreferencesFactory.create(appContext, PREFS_NAME)
        } catch (e: GeneralSecurityException) {
            throw IllegalStateException("Falha ao criar EncryptedSharedPreferences", e)
        } catch (e: IOException) {
            throw IllegalStateException("Falha ao criar EncryptedSharedPreferences", e)
        }
    }

    override fun getAccessToken(): String? =
        prefs.getString(KEY_ACCESS, null)?.takeIf { it.isNotBlank() }

    override fun getRefreshToken(): String? =
        prefs.getString(KEY_REFRESH, null)?.takeIf { it.isNotBlank() }

    override fun setTokens(accessToken: String, refreshToken: String) {
        prefs.edit()
            .putString(KEY_ACCESS, accessToken)
            .putString(KEY_REFRESH, refreshToken)
            .apply()
    }

    override fun clear() {
        prefs.edit().remove(KEY_ACCESS).remove(KEY_REFRESH).apply()
    }

    private companion object {
        const val PREFS_NAME = "well_paid_auth_tokens"
        const val KEY_ACCESS = "access_token"
        const val KEY_REFRESH = "refresh_token"
    }
}
