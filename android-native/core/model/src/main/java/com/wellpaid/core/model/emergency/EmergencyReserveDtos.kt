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
    val details: String? = null,
    @SerialName("is_family") val isFamily: Boolean = false,
    @SerialName("monthly_target_cents") val monthlyTargetCents: Int,
    @SerialName("target_cents") val targetCents: Int? = null,
    @SerialName("balance_cents") val balanceCents: Int,
    @SerialName("opening_balance_cents") val openingBalanceCents: Int = 0,
    @SerialName("tracking_start") val trackingStart: String,
    @SerialName("target_end_date") val targetEndDate: String? = null,
    @SerialName("plan_duration_months") val planDurationMonths: Int? = null,
    @SerialName("months_total") val monthsTotal: Int? = null,
    @SerialName("months_passed") val monthsPassed: Int? = null,
    @SerialName("months_remaining") val monthsRemaining: Int? = null,
    @SerialName("monthly_needed_cents") val monthlyNeededCents: Int? = null,
    @SerialName("pace_status") val paceStatus: String = "unknown",
    @SerialName("pace_delta_cents") val paceDeltaCents: Int = 0,
    val status: String,
    @SerialName("completed_at") val completedAt: String? = null,
)

@Serializable
data class EmergencyReservePlanCreateDto(
    val title: String = "",
    val details: String? = null,
    @SerialName("is_family") val isFamily: Boolean = false,
    @SerialName("monthly_target_cents") val monthlyTargetCents: Int,
    @SerialName("target_cents") val targetCents: Int? = null,
    @SerialName("tracking_start") val trackingStart: String? = null,
    @SerialName("target_end_date") val targetEndDate: String? = null,
    @SerialName("plan_duration_months") val planDurationMonths: Int? = null,
    @SerialName("opening_balance_cents") val openingBalanceCents: Int? = null,
)

@Serializable
data class EmergencyReservePlanUpdateDto(
    val title: String = "",
    val details: String? = null,
    @SerialName("is_family") val isFamily: Boolean? = null,
    @SerialName("monthly_target_cents") val monthlyTargetCents: Int,
    @SerialName("target_cents") val targetCents: Int? = null,
    @SerialName("tracking_start") val trackingStart: String? = null,
    @SerialName("target_end_date") val targetEndDate: String? = null,
    @SerialName("plan_duration_months") val planDurationMonths: Int? = null,
    @SerialName("opening_balance_cents") val openingBalanceCents: Int? = null,
)

@Serializable
data class EmergencyReserveMonthRowDto(
    val year: Int,
    val month: Int,
    @SerialName("expected_cents") val expectedCents: Int,
    @SerialName("deposited_cents") val depositedCents: Int,
    @SerialName("shortfall_cents") val shortfallCents: Int,
    @SerialName("cumulative_expected_cents") val cumulativeExpectedCents: Int = 0,
    @SerialName("cumulative_deposited_cents") val cumulativeDepositedCents: Int = 0,
    @SerialName("cumulative_delta_cents") val cumulativeDeltaCents: Int = 0,
    @SerialName("pace_status") val paceStatus: String = "unknown",
)

@Serializable
data class EmergencyReserveCompleteDto(
    @SerialName("goal_id") val goalId: String? = null,
    @SerialName("to_plan_id") val toPlanId: String? = null,
)

@Serializable
data class EmergencyReserveContributionAllocationDto(
    @SerialName("plan_id") val planId: String,
    @SerialName("amount_cents") val amountCents: Int,
)

@Serializable
data class EmergencyReserveContributionCreateDto(
    @SerialName("contribution_date") val contributionDate: String? = null,
    @SerialName("total_amount_cents") val totalAmountCents: Int,
    val allocations: List<EmergencyReserveContributionAllocationDto>,
    val note: String? = null,
)

@Serializable
data class EmergencyReserveContributionItemDto(
    @SerialName("plan_id") val planId: String,
    @SerialName("amount_cents") val amountCents: Int,
)

@Serializable
data class EmergencyReserveContributionResponseDto(
    val id: String,
    @SerialName("contribution_date") val contributionDate: String,
    @SerialName("total_amount_cents") val totalAmountCents: Int,
    val note: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val items: List<EmergencyReserveContributionItemDto> = emptyList(),
)
