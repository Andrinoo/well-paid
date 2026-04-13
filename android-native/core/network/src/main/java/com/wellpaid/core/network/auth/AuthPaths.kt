package com.wellpaid.core.network.auth

/**
 * Rotas em `/auth/` que **não** usam access token no header (corpo JSON ou anónimas).
 * `/auth/me`, `/auth/profile/...` e similares **exigem** Bearer — não entram aqui.
 */
private val AUTH_PATHS_WITHOUT_BEARER: Set<String> = setOf(
    "/auth/login",
    "/auth/register",
    "/auth/refresh",
    "/auth/forgot-password",
    "/auth/reset-password",
    "/auth/verify-email",
    "/auth/resend-verification",
    "/auth/logout",
)

object AuthPaths {

    fun skipAuthorizationHeader(encodedPath: String): Boolean =
        encodedPath in AUTH_PATHS_WITHOUT_BEARER

    fun skipRefreshOn401(encodedPath: String): Boolean =
        encodedPath in AUTH_PATHS_WITHOUT_BEARER
}
