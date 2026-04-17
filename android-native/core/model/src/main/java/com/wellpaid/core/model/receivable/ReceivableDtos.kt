package com.wellpaid.core.model.receivable

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReceivablesListDto(
    @SerialName("as_creditor") val asCreditor: List<ReceivableDto> = emptyList(),
    @SerialName("as_debtor") val asDebtor: List<ReceivableDto> = emptyList(),
)

@Serializable
data class ReceivableDto(
    val id: String,
    @SerialName("creditor_user_id") val creditorUserId: String,
    @SerialName("debtor_user_id") val debtorUserId: String,
    @SerialName("amount_cents") val amountCents: Int,
    @SerialName("settle_by") val settleBy: String,
    @SerialName("source_expense_id") val sourceExpenseId: String? = null,
    val status: String,
    @SerialName("settled_at") val settledAt: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("debtor_display_name") val debtorDisplayName: String? = null,
    @SerialName("creditor_display_name") val creditorDisplayName: String? = null,
)

@Serializable
data class SettleReceivableDto(
    @SerialName("create_income") val createIncome: Boolean = false,
    @SerialName("income_category_id") val incomeCategoryId: String? = null,
    @SerialName("income_date") val incomeDate: String? = null,
)
