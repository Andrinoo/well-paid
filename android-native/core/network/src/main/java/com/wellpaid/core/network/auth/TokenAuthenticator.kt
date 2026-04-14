package com.wellpaid.core.network.auth

import com.wellpaid.core.model.auth.TokenStorage
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class TokenAuthenticator(
    private val tokenStorage: TokenStorage,
    private val tokenRefresher: TokenRefresher,
) : Authenticator {

    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) return null
        val path = response.request.url.encodedPath
        if (AuthPaths.skipRefreshOn401(path)) return null

        synchronized(lock) {
            val failedBearer = response.request.header("Authorization")
                ?.removePrefix("Bearer")
                ?.trim()
            val currentAccess = tokenStorage.getAccessToken()
            if (!currentAccess.isNullOrBlank() && currentAccess != failedBearer) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentAccess")
                    .build()
            }

            val tokens = tokenRefresher.refreshTokens() ?: return null
            return response.request.newBuilder()
                .header("Authorization", "Bearer ${tokens.accessToken}")
                .build()
        }
    }

    private fun responseCount(response: Response): Int {
        var result = 1
        var prior = response.priorResponse
        while (prior != null) {
            result++
            prior = prior.priorResponse
        }
        return result
    }
}
