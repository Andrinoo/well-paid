package com.wellpaid.core.network

import com.wellpaid.core.model.auth.UserMeDto
import com.wellpaid.core.model.auth.UserProfilePatchDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST

interface UserApi {
    @GET("auth/me")
    suspend fun getCurrentUser(): UserMeDto

    @PATCH("auth/me")
    suspend fun patchProfile(@Body body: UserProfilePatchDto): UserMeDto

    /** Preferido na app: POST evita 404 em alguns ambientes sem PATCH correctamente encaminhado. */
    @POST("auth/profile/display-name")
    suspend fun updateDisplayName(@Body body: UserProfilePatchDto): UserMeDto
}
