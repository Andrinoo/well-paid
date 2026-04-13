package com.wellpaid.security

import android.content.Context
import android.os.Build
import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.nio.charset.StandardCharsets
import java.security.InvalidKeyException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Guarda email + senha encriptados com chave do Android Keystore que exige biometria forte
 * para encriptar/desencriptar. Usado para "entrada rápida" na app chamando [com.wellpaid.core.network.auth.AuthApi.login]
 * após desbloquear — funciona mesmo após logout no servidor (novo par de tokens).
 */
@Singleton
class BiometricLoginVault @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun hasStoredCredentials(): Boolean =
        prefs.getString(KEY_PAYLOAD, null)?.isNotBlank() == true

    fun canUseStrongBiometric(): Boolean =
        BiometricManager.from(context).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG,
        ) == BiometricManager.BIOMETRIC_SUCCESS

    /**
     * Remove credenciais e a chave Keystore (para desligar entrada rápida ou substituir conta).
     */
    fun clear() {
        prefs.edit().remove(KEY_PAYLOAD).apply()
        runCatching {
            val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (ks.containsAlias(KEY_ALIAS)) ks.deleteEntry(KEY_ALIAS)
        }
    }

    /**
     * Encripta e grava [payload] após o utilizador autenticar com biometria.
     */
    fun storePayloadWithBiometric(
        activity: FragmentActivity,
        payloadUtf8: String,
        onResult: (Boolean) -> Unit,
    ) {
        clear()
        generateBiometricKey()
        val cipher = createCipher()
        val secretKey = getSecretKey()
        try {
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        } catch (_: InvalidKeyException) {
            clear()
            onResult(false)
            return
        }
        runBiometric(
            activity = activity,
            cryptoObject = BiometricPrompt.CryptoObject(cipher),
            titleRes = com.wellpaid.R.string.biometric_login_prompt_enroll_title,
            subtitleRes = com.wellpaid.R.string.biometric_login_prompt_enroll_subtitle,
            onSuccess = {
                val c = it.cryptoObject?.cipher
                if (c == null) {
                    clear()
                    onResult(false)
                } else {
                    try {
                        val plain = payloadUtf8.toByteArray(StandardCharsets.UTF_8)
                        val encrypted = c.doFinal(plain)
                        val iv = c.iv
                        if (iv == null || iv.isEmpty() || iv.size > 255) {
                            clear()
                            onResult(false)
                        } else {
                            val combined = ByteArray(1 + iv.size + encrypted.size)
                            combined[0] = iv.size.toByte()
                            System.arraycopy(iv, 0, combined, 1, iv.size)
                            System.arraycopy(encrypted, 0, combined, 1 + iv.size, encrypted.size)
                            prefs.edit()
                                .putString(KEY_PAYLOAD, Base64.encodeToString(combined, Base64.NO_WRAP))
                                .apply()
                            onResult(true)
                        }
                    } catch (_: Exception) {
                        clear()
                        onResult(false)
                    }
                }
            },
            onFail = {
                clear()
                onResult(false)
            },
        )
    }

    /**
     * Desencripta credenciais após biometria; devolve JSON simples `{"email":"...","password":"..."}`.
     */
    fun retrievePayloadWithBiometric(
        activity: FragmentActivity,
        onResult: (String?) -> Unit,
    ) {
        val combined = try {
            Base64.decode(prefs.getString(KEY_PAYLOAD, null), Base64.NO_WRAP)
        } catch (_: Exception) {
            onResult(null)
            return
        }
        if (combined.isEmpty()) {
            onResult(null)
            return
        }
        val ivLen = combined[0].toInt() and 0xFF
        if (ivLen <= 0 || 1 + ivLen > combined.size) {
            onResult(null)
            return
        }
        val iv = combined.copyOfRange(1, 1 + ivLen)
        val cipherBytes = combined.copyOfRange(1 + ivLen, combined.size)
        val cipher = createCipher()
        val secretKey = getSecretKey()
        try {
            val spec = GCMParameterSpec(GCM_TAG_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        } catch (e: Exception) {
            if (e is java.security.UnrecoverableKeyException || e is InvalidKeyException) {
                clear()
            }
            onResult(null)
            return
        }
        runBiometric(
            activity = activity,
            cryptoObject = BiometricPrompt.CryptoObject(cipher),
            titleRes = com.wellpaid.R.string.biometric_login_prompt_unlock_title,
            subtitleRes = com.wellpaid.R.string.biometric_login_prompt_unlock_subtitle,
            onSuccess = {
                val c = it.cryptoObject?.cipher
                if (c == null) {
                    onResult(null)
                } else {
                    try {
                        val plain = c.doFinal(cipherBytes)
                        onResult(String(plain, StandardCharsets.UTF_8))
                    } catch (_: Exception) {
                        clear()
                        onResult(null)
                    }
                }
            },
            onFail = { onResult(null) },
        )
    }

    private fun runBiometric(
        activity: FragmentActivity,
        cryptoObject: BiometricPrompt.CryptoObject,
        titleRes: Int,
        subtitleRes: Int,
        onSuccess: (BiometricPrompt.AuthenticationResult) -> Unit,
        onFail: () -> Unit,
    ) {
        val executor = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess(result)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onFail()
                }

                override fun onAuthenticationFailed() {
                    onFail()
                }
            },
        )
        val b = BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(titleRes))
            .setSubtitle(activity.getString(subtitleRes))
            .setNegativeButtonText(activity.getString(com.wellpaid.R.string.common_cancel))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            b.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        }
        val info = b.build()
        prompt.authenticate(info, cryptoObject)
    }

    private fun generateBiometricKey() {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE,
        )
        val builder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setUserAuthenticationParameters(
                0,
                KeyProperties.AUTH_BIOMETRIC_STRONG,
            )
        } else {
            @Suppress("DEPRECATION")
            builder.setUserAuthenticationValidityDurationSeconds(0)
        }
        keyGenerator.init(builder.build())
        keyGenerator.generateKey()
    }

    private fun getSecretKey(): SecretKey {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return ks.getKey(KEY_ALIAS, null) as SecretKey
    }

    private fun createCipher(): Cipher = Cipher.getInstance("AES/GCM/NoPadding")

    private companion object {
        const val PREFS_NAME = "wellpaid_biometric_quick_login"
        const val KEY_PAYLOAD = "payload_v1"
        const val KEY_ALIAS = "wellpaid_biometric_login_aes"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val GCM_TAG_BITS = 128
    }
}
