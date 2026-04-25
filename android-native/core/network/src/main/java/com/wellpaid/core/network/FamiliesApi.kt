package com.wellpaid.core.network

import com.wellpaid.core.model.family.FamilyCreateDto
import com.wellpaid.core.model.family.FamilyInviteCreateRequestDto
import com.wellpaid.core.model.family.FamilyInviteCreatedDto
import com.wellpaid.core.model.family.FamilyJoinRequestDto
import com.wellpaid.core.model.family.FamilyMeResponseDto
import com.wellpaid.core.model.family.FamilyOutDto
import com.wellpaid.core.model.family.FamilyPendingInviteDto
import com.wellpaid.core.model.family.FamilyUpdateDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface FamiliesApi {
    @GET("families/me")
    suspend fun getMe(): FamilyMeResponseDto

    @POST("families/me")
    suspend fun createFamily(@Body body: FamilyCreateDto): FamilyOutDto

    @PATCH("families/me")
    suspend fun updateFamily(@Body body: FamilyUpdateDto): FamilyOutDto

    @POST("families/me/invites")
    suspend fun createInvite(@Body body: FamilyInviteCreateRequestDto): FamilyInviteCreatedDto

    @GET("families/invites/pending")
    suspend fun listPendingInvites(): List<FamilyPendingInviteDto>

    @POST("families/join")
    suspend fun joinFamily(@Body body: FamilyJoinRequestDto): FamilyOutDto

    @DELETE("families/me/members/{userId}")
    suspend fun removeMember(@Path("userId") userId: String): Response<Void>

    @DELETE("families/me")
    suspend fun leaveFamily(): Response<Void>
}
