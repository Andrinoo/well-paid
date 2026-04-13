package com.wellpaid.core.network

import com.wellpaid.core.model.auth.UserMeDto
import com.wellpaid.core.model.auth.UserProfilePatchDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH

interface UserApi {
    @GET("auth/me")
    suspend fun getCurrentUser(): UserMeDto

    @PATCH("auth/me")
    suspend fun patchProfile(@Body body: UserProfilePatchDto): UserMeDto
}
