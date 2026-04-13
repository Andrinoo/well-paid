package com.wellpaid.core.model.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserMeDto(
    val email: String,
    @SerialName("full_name") val fullName: String? = null,
)
