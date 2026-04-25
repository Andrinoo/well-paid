package com.wellpaid.core.model.expense

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExpenseCreateDto(
    val description: String,
    @SerialName("amount_cents") val amountCents: Int,
    @SerialName("expense_date") val expenseDate: String,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("due_date") val dueDate: String? = null,
    @SerialName("category_id") val categoryId: String,
    val status: String = "pending",
    @SerialName("installment_total") val installmentTotal: Int = 1,
    @SerialName("recurring_frequency") val recurringFrequency: String? = null,
    @SerialName("is_shared") val isShared: Boolean = false,
    @SerialName("is_family") val isFamily: Boolean = false,
    @SerialName("shared_with_user_id") val sharedWithUserId: String? = null,
    @SerialName("split_mode") val splitMode: String? = null,
    @SerialName("owner_share_cents") val ownerShareCents: Int? = null,
    @SerialName("peer_share_cents") val peerShareCents: Int? = null,
    @SerialName("owner_percent_bps") val ownerPercentBps: Int? = null,
    @SerialName("peer_percent_bps") val peerPercentBps: Int? = null,
)

@Serializable
data class ExpenseCreateOutcomeDto(
    @SerialName("installment_group_id") val installmentGroupId: String? = null,
    val expenses: List<ExpenseDto>,
)

@Serializable
data class ExpenseUpdateDto(
    val description: String,
    @SerialName("amount_cents") val amountCents: Int,
    @SerialName("expense_date") val expenseDate: String,
    @SerialName("due_date") val dueDate: String? = null,
    @SerialName("category_id") val categoryId: String,
    val status: String,
    @SerialName("is_shared") val isShared: Boolean? = null,
    @SerialName("is_family") val isFamily: Boolean? = null,
    @SerialName("shared_with_user_id") val sharedWithUserId: String? = null,
    @SerialName("split_mode") val splitMode: String? = null,
    @SerialName("owner_share_cents") val ownerShareCents: Int? = null,
    @SerialName("peer_share_cents") val peerShareCents: Int? = null,
    @SerialName("owner_percent_bps") val ownerPercentBps: Int? = null,
    @SerialName("peer_percent_bps") val peerPercentBps: Int? = null,
)

@Serializable
data class ExpenseCoverRequestDto(
    @SerialName("settle_by") val settleBy: String,
)

@Serializable
data class ExpensePayDto(
    @SerialName("allow_advance") val allowAdvance: Boolean = false,
    @SerialName("amount_cents") val amountCents: Int? = null,
)
