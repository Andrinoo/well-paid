package com.wellpaid.core.network

import com.wellpaid.core.model.emergency.EmergencyReserveAccrualDto
import com.wellpaid.core.model.emergency.EmergencyReserveDto
import com.wellpaid.core.model.emergency.EmergencyReserveUpdateDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
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
}
