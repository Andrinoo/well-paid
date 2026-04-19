package com.wellpaid.core.network

import com.wellpaid.core.model.goal.GoalContributeDto
import com.wellpaid.core.model.goal.GoalCreateDto
import com.wellpaid.core.model.goal.GoalDto
import com.wellpaid.core.model.goal.GoalPreviewFromUrlRequestDto
import com.wellpaid.core.model.goal.GoalPreviewFromUrlDto
import com.wellpaid.core.model.goal.GoalProductSearchRequestDto
import com.wellpaid.core.model.goal.GoalProductSearchResponseDto
import com.wellpaid.core.model.goal.GoalUpdateDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface GoalsApi {
    @GET("goals")
    suspend fun listGoals(): List<GoalDto>

    @GET("goals/{id}")
    suspend fun getGoal(@Path("id") id: String): GoalDto

    @POST("goals")
    suspend fun createGoal(@Body body: GoalCreateDto): GoalDto

    @PUT("goals/{id}")
    suspend fun updateGoal(
        @Path("id") id: String,
        @Body body: GoalUpdateDto,
    ): GoalDto

    @DELETE("goals/{id}")
    suspend fun deleteGoal(@Path("id") id: String): Response<Void>

    @POST("goals/{id}/contribute")
    suspend fun contribute(
        @Path("id") id: String,
        @Body body: GoalContributeDto,
    ): GoalDto

    @POST("goals/{id}/refresh-reference-price")
    suspend fun refreshReferencePrice(@Path("id") id: String): GoalDto

    @POST("goals/preview-from-url")
    suspend fun previewFromUrl(@Body body: GoalPreviewFromUrlRequestDto): GoalPreviewFromUrlDto

    @POST("goals/product-search")
    suspend fun productSearch(@Body body: GoalProductSearchRequestDto): GoalProductSearchResponseDto
}
