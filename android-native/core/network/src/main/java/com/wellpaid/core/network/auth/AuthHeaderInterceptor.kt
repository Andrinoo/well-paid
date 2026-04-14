package com.wellpaid.core.network.auth

import com.wellpaid.core.model.auth.TokenStorage
import okhttp3.Interceptor
import okhttp3.Response

class AuthHeaderInterceptor(
    private val tokenStorage: TokenStorage,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        if (AuthPaths.skipAuthorizationHeader(path)) {
            return chain.proceed(request)
        }
        val token = tokenStorage.getAccessToken() ?: return chain.proceed(request)
        return chain.proceed(
            request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build(),
        )
    }
}
