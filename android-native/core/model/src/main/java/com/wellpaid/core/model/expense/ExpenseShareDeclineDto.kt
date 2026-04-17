package com.wellpaid.core.model.expense

import kotlinx.serialization.Serializable

@Serializable
data class ExpenseShareDeclineDto(
    val reason: String? = null,
)
