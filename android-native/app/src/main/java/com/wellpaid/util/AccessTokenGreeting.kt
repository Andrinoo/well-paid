package com.wellpaid.util

import android.util.Base64
import java.nio.charset.StandardCharsets
import java.util.Locale
import org.json.JSONObject

/**
 * Extrai um primeiro nome só a partir do claim **email** do JWT (nunca `sub` — costuma ser UUID).
 * Usado quando [com.wellpaid.core.network.UserApi.getCurrentUser] falha.
 */
fun greetingFirstNameFromAccessToken(accessToken: String?): String? {
    if (accessToken.isNullOrBlank()) return null
    val parts = accessToken.split('.')
    if (parts.size < 2) return null
    return try {
        val payloadJson = base64UrlDecodeToString(parts[1])
        val json = JSONObject(payloadJson)
        val email = json.optString("email", "").trim().ifBlank { return null }
        val local = email.substringBefore('@').trim().ifBlank { return null }
        if (local.looksLikeUuid()) return null
        local.replaceFirstChar { ch ->
            if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
        }
    } catch (_: Exception) {
        null
    }
}

private fun base64UrlDecodeToString(segment: String): String {
    val padded = segment + "=".repeat((4 - segment.length % 4) % 4)
    val bytes = Base64.decode(
        padded.replace('-', '+').replace('_', '/'),
        Base64.DEFAULT,
    )
    return String(bytes, StandardCharsets.UTF_8)
}
