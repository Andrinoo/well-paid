package com.wellpaid.core.network.auth

object AuthPaths {

    fun skipAuthorizationHeader(encodedPath: String): Boolean =
        encodedPath.startsWith("/auth/") || encodedPath == "/auth"

    fun skipRefreshOn401(encodedPath: String): Boolean {
        if (encodedPath.startsWith("/auth/") || encodedPath == "/auth") return true
        return false
    }
}
