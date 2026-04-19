package com.wellpaid.core.network

import com.wellpaid.core.model.emergency.EmergencyReserveAccrualDto
import com.wellpaid.core.model.emergency.EmergencyReserveCompleteDto
import com.wellpaid.core.model.emergency.EmergencyReserveDto
import com.wellpaid.core.model.emergency.EmergencyReserveMonthRowDto
import com.wellpaid.core.model.emergency.EmergencyReservePlanCreateDto
import com.wellpaid.core.model.emergency.EmergencyReservePlanDto
import com.wellpaid.core.model.emergency.EmergencyReserveUpdateDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface EmergencyReserveApi {
    @GET("emergency-reserve")
    suspend fun getReserve(): EmergencyReserveDto

    @PUT("emergency-reserve")
    suspend fun updateReserve(@Body body: EmergencyReserveUpdateDto): EmergencyReserveDto

    @GET("emergency-reserve/accruals")
    suspend fun listAccruals(
        @Query("limit") limit: Int = 12,
    ): List<EmergencyReserveAccrualDto>

    @GET("emergency-reserve/plans")
    suspend fun listPlans(): List<EmergencyReservePlanDto>

    @POST("emergency-reserve/plans")
    suspend fun createPlan(@Body body: EmergencyReservePlanCreateDto): EmergencyReservePlanDto

    @GET("emergency-reserve/plans/{planId}/months")
    suspend fun listPlanMonths(@Path("planId") planId: String): List<EmergencyReserveMonthRowDto>

    @POST("emergency-reserve/plans/{planId}/complete")
    suspend fun completePlan(
        @Path("planId") planId: String,
        @Body body: EmergencyReserveCompleteDto,
    ): EmergencyReservePlanDto
}
