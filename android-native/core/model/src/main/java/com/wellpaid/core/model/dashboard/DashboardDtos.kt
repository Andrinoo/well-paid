package com.wellpaid.core.model.dashboard

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PeriodMonthDto(
    val year: Int,
    val month: Int,
)

@Serializable
data class CategorySpendDto(
    @SerialName("category_key") val categoryKey: String,
    val name: String,
    @SerialName("amount_cents") val amountCents: Int,
    @SerialName("share_bps") val shareBps: Int? = null,
)

@Serializable
data class PendingExpenseItemDto(
    val id: String,
    val description: String,
    @SerialName("amount_cents") val amountCents: Int,
    @SerialName("due_date") val dueDate: String? = null,
    @SerialName("is_mine") val isMine: Boolean = true,
)

@Serializable
data class GoalSummaryItemDto(
    val id: String,
    val title: String,
    @SerialName("current_cents") val currentCents: Int,
    @SerialName("target_cents") val targetCents: Int,
    @SerialName("is_mine") val isMine: Boolean = true,
)

@Serializable
data class DashboardCashflowDto(
    val dynamic: Boolean,
    @SerialName("forecast_months") val forecastMonths: Int,
    val months: List<PeriodMonthDto>,
    @SerialName("income_cents") val incomeCents: List<Int>,
    @SerialName("expense_paid_cents") val expensePaidCents: List<Int>,
    @SerialName("expense_forecast_cents") val expenseForecastCents: List<Int>,
)

@Serializable
data class DashboardOverviewDto(
    val period: PeriodMonthDto,
    @SerialName("month_income_cents") val monthIncomeCents: Int = 0,
    @SerialName("month_expense_total_cents") val monthExpenseTotalCents: Int,
    @SerialName("month_balance_cents") val monthBalanceCents: Int,
    @SerialName("spending_by_category") val spendingByCategory: List<CategorySpendDto>,
    @SerialName("pending_total_cents") val pendingTotalCents: Int,
    @SerialName("pending_preview") val pendingPreview: List<PendingExpenseItemDto> = emptyList(),
    @SerialName("upcoming_due") val upcomingDue: List<PendingExpenseItemDto> = emptyList(),
    @SerialName("goals_preview") val goalsPreview: List<GoalSummaryItemDto> = emptyList(),
    @SerialName("emergency_reserve_balance_cents") val emergencyReserveBalanceCents: Int = 0,
    @SerialName("emergency_reserve_monthly_target_cents")
    val emergencyReserveMonthlyTargetCents: Int = 0,
)
