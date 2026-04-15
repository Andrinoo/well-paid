package com.wellpaid.core.model.telemetry

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TelemetryPingRequestDto(
    @SerialName("event_type") val eventType: String = "app_open",
)

@Serializable
data class TelemetryPingResponseDto(
    @SerialName("accepted") val accepted: Boolean,
    @SerialName("deduped") val deduped: Boolean,
    @SerialName("event_type") val eventType: String,
)
