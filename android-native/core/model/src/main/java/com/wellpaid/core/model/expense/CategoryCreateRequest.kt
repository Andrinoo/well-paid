package com.wellpaid.core.model.expense

import kotlinx.serialization.Serializable

@Serializable
data class CategoryCreateRequest(val name: String)
