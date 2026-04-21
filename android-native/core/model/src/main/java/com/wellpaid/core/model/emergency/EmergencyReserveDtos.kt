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

@Serializable
data class EmergencyReservePlanDto(
    val id: String,
    val title: String,
    @SerialName("monthly_target_cents") val monthlyTargetCents: Int,
    @SerialName("balance_cents") val balanceCents: Int,
    @SerialName("tracking_start") val trackingStart: String,
    @SerialName("plan_duration_months") val planDurationMonths: Int? = null,
    val status: String,
    @SerialName("completed_at") val completedAt: String? = null,
)

@Serializable
data class EmergencyReservePlanCreateDto(
    val title: String = "",
    @SerialName("monthly_target_cents") val monthlyTargetCents: Int,
    @SerialName("tracking_start") val trackingStart: String? = null,
    @SerialName("plan_duration_months") val planDurationMonths: Int? = null,
)

@Serializable
data class EmergencyReservePlanUpdateDto(
    val title: String = "",
    @SerialName("monthly_target_cents") val monthlyTargetCents: Int,
    @SerialName("tracking_start") val trackingStart: String? = null,
    @SerialName("plan_duration_months") val planDurationMonths: Int? = null,
)

@Serializable
data class EmergencyReserveMonthRowDto(
    val year: Int,
    val month: Int,
    @SerialName("expected_cents") val expectedCents: Int,
    @SerialName("deposited_cents") val depositedCents: Int,
    @SerialName("shortfall_cents") val shortfallCents: Int,
)

@Serializable
data class EmergencyReserveCompleteDto(
    @SerialName("goal_id") val goalId: String? = null,
    @SerialName("to_plan_id") val toPlanId: String? = null,
)
