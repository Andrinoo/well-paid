package com.wellpaid.core.model.auth

interface TokenStorage {
    fun getAccessToken(): String?
    fun getRefreshToken(): String?
    fun setTokens(accessToken: String, refreshToken: String)
    fun clear()
}
