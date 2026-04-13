package com.wellpaid.core.network

import com.wellpaid.core.model.auth.UserMeDto
import retrofit2.http.GET

interface UserApi {
    @GET("auth/me")
    suspend fun getCurrentUser(): UserMeDto
}
