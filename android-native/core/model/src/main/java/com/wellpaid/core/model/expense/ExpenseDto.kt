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
    @SerialName("shared_with_user_id") val sharedWithUserId: String? = null,
    @SerialName("shared_with_label") val sharedWithLabel: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("paid_at") val paidAt: String? = null,
    @SerialName("installment_plan_has_paid") val installmentPlanHasPaid: Boolean? = null,
    @SerialName("is_projected") val isProjected: Boolean = false,
    @SerialName("is_advanced_payment") val isAdvancedPayment: Boolean = false,
)
