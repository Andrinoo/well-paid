package com.wellpaid.core.model.income

import kotlinx.serialization.Serializable

@Serializable
data class IncomeCategoryCreateRequest(val name: String)
