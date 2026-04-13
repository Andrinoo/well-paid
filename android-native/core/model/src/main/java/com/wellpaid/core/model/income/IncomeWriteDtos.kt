package com.wellpaid.core.model.income

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IncomeCreateDto(
    val description: String,
    @SerialName("amount_cents") val amountCents: Int,
    @SerialName("income_date") val incomeDate: String,
    @SerialName("income_category_id") val incomeCategoryId: String,
    val notes: String? = null,
)

@Serializable
data class IncomeUpdateDto(
    val description: String,
    @SerialName("amount_cents") val amountCents: Int,
    @SerialName("income_date") val incomeDate: String,
    @SerialName("income_category_id") val incomeCategoryId: String,
    val notes: String? = null,
)
