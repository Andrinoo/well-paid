package com.wellpaid.core.model.expense

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExpenseDto(
    val id: String,
    @SerialName("owner_user_id") val ownerUserId: String,
    @SerialName("is_mine") val isMine: Boolean = true,
    val description: String,
    @SerialName("amount_cents") val amountCents: Int,
    @SerialName("monthly_interest_bps") val monthlyInterestBps: Int? = null,
    @SerialName("expense_date") val expenseDate: String,
    @SerialName("due_date") val dueDate: String? = null,
    val status: String,
    @SerialName("category_id") val categoryId: String,
    @SerialName("category_key") val categoryKey: String,
    @SerialName("category_name") val categoryName: String,
    @SerialName("sync_status") val syncStatus: Int,
    @SerialName("installment_total") val installmentTotal: Int,
    @SerialName("installment_number") val installmentNumber: Int,
    @SerialName("installment_group_id") val installmentGroupId: String? = null,
    @SerialName("recurring_frequency") val recurringFrequency: String? = null,
    @SerialName("recurring_series_id") val recurringSeriesId: String? = null,
    @SerialName("recurring_generated_until") val recurringGeneratedUntil: String? = null,
    @SerialName("is_shared") val isShared: Boolean = false,
    @SerialName("is_family") val isFamily: Boolean = false,
    @SerialName("shared_with_user_id") val sharedWithUserId: String? = null,
    @SerialName("shared_with_label") val sharedWithLabel: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("paid_at") val paidAt: String? = null,
    @SerialName("installment_plan_has_paid") val installmentPlanHasPaid: Boolean? = null,
    @SerialName("is_projected") val isProjected: Boolean = false,
    @SerialName("is_advanced_payment") val isAdvancedPayment: Boolean = false,
    @SerialName("split_mode") val splitMode: String? = null,
    /** Basis points da parte do dono quando a partilha é em percentagem (0–10000). */
    @SerialName("owner_percent_bps") val ownerPercentBps: Int? = null,
    /** Basis points da parte do outro membro quando a partilha é em percentagem. */
    @SerialName("peer_percent_bps") val peerPercentBps: Int? = null,
    @SerialName("counterparty_label") val counterpartyLabel: String? = null,
    @SerialName("my_share_cents") val myShareCents: Int? = null,
    @SerialName("other_user_share_cents") val otherUserShareCents: Int? = null,
    @SerialName("my_share_paid") val mySharePaid: Boolean = false,
    @SerialName("other_share_paid") val otherSharePaid: Boolean = false,
    @SerialName("shared_expense_payment_alert") val sharedExpensePaymentAlert: Boolean = false,
    @SerialName("shared_expense_peer_declined_alert") val sharedExpensePeerDeclinedAlert: Boolean = false,
    @SerialName("my_share_declined") val myShareDeclined: Boolean = false,
)
