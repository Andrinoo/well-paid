package com.wellpaid.core.model.expense

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CategoryDto(
    val id: String,
    val key: String,
    val name: String,
    @SerialName("sort_order") val sortOrder: Int,
)
