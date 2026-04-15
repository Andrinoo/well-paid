package com.wellpaid.core.network

import com.wellpaid.core.model.telemetry.TelemetryPingRequestDto
import com.wellpaid.core.model.telemetry.TelemetryPingResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

interface TelemetryApi {
    @POST("telemetry/ping")
    suspend fun ping(@Body body: TelemetryPingRequestDto): TelemetryPingResponseDto
}
