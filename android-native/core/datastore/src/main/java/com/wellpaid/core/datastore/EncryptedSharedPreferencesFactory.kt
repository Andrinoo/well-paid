@file:Suppress("DEPRECATION")

package com.wellpaid.core.datastore

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * EncryptedSharedPreferences e MasterKey estão deprecados em `security-crypto` 1.1.0; a migração recomendada
 * a longo prazo é DataStore + Keystore/Tink. Mantemos o mesmo esquema de encriptação; a supressão fica só neste ficheiro.
 */
object EncryptedSharedPreferencesFactory {

    fun create(context: Context, fileName: String): SharedPreferences {
        val appContext = context.applicationContext
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            appContext,
            fileName,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }
}
