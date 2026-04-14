package com.wellpaid.core.model.emergency

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EmergencyReserveDto(
    @SerialName("monthly_target_cents") val monthlyTargetCents: Int,
    @SerialName("balance_cents") val balanceCents: Int,
    @SerialName("tracking_start") val trackingStart: String,
    val configured: Boolean,
)

@Serializable
data class EmergencyReserveUpdateDto(
    @SerialName("monthly_target_cents") val monthlyTargetCents: Int,
)

@Serializable
data class EmergencyReserveAccrualDto(
    val year: Int,
    val month: Int,
    @SerialName("amount_cents") val amountCents: Int,
    @SerialName("created_at") val createdAt: String? = null,
)
