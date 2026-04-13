package com.wellpaid.core.model.expense

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExpenseCreateDto(
    val description: String,
    @SerialName("amount_cents") val amountCents: Int,
    @SerialName("expense_date") val expenseDate: String,
    @SerialName("due_date") val dueDate: String? = null,
    @SerialName("category_id") val categoryId: String,
    val status: String = "pending",
    @SerialName("installment_total") val installmentTotal: Int = 1,
    @SerialName("recurring_frequency") val recurringFrequency: String? = null,
    @SerialName("is_shared") val isShared: Boolean = false,
    @SerialName("shared_with_user_id") val sharedWithUserId: String? = null,
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
    @SerialName("shared_with_user_id") val sharedWithUserId: String? = null,
)
