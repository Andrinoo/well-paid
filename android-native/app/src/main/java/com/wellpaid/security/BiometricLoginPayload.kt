package com.wellpaid.security

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class BiometricLoginPayload(
    val email: String,
    /** Entrada rápida: senha encriptada no vault (com biometria ao activar). */
    val password: String? = null,
    /** Instalações antigas: só refresh; ainda suportado no login. */
    val refreshToken: String? = null,
)

private val payloadJson = Json { ignoreUnknownKeys = true }

fun BiometricLoginPayload.toJsonString(): String = payloadJson.encodeToString(this)

fun parseBiometricLoginPayload(raw: String): BiometricLoginPayload? =
    runCatching { payloadJson.decodeFromString<BiometricLoginPayload>(raw) }.getOrNull()
