package com.wellpaid.core.network.auth

import com.wellpaid.core.model.auth.RefreshRequestDto
import com.wellpaid.core.model.auth.TokenPairDto
import com.wellpaid.core.model.auth.TokenStorage
import java.io.IOException

class TokenRefresher(
    private val authApi: AuthApi,
    private val tokenStorage: TokenStorage,
) {

    fun refreshTokens(): TokenPairDto? {
        val refresh = tokenStorage.getRefreshToken() ?: return null
        val call = authApi.refreshCall(RefreshRequestDto(refresh))
        return try {
            val response = call.execute()
            if (!response.isSuccessful) {
                if (response.code() == 401 || response.code() == 403) {
                    tokenStorage.clear()
                }
                null
            } else {
                val body = response.body()
                if (body == null) {
                    tokenStorage.clear()
                    null
                } else {
                    tokenStorage.setTokens(body.accessToken, body.refreshToken)
                    body
                }
            }
        } catch (_: IOException) {
            null
        }
    }
}
