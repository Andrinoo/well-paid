package com.wellpaid.core.model.income

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IncomeDto(
    val id: String,
    @SerialName("owner_user_id") val ownerUserId: String,
    @SerialName("is_mine") val isMine: Boolean = true,
    val description: String,
    @SerialName("amount_cents") val amountCents: Int,
    @SerialName("income_date") val incomeDate: String,
    @SerialName("income_category_id") val incomeCategoryId: String,
    @SerialName("category_key") val categoryKey: String,
    @SerialName("category_name") val categoryName: String,
    val notes: String? = null,
    @SerialName("sync_status") val syncStatus: Int,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)
