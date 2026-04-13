package com.wellpaid.core.model.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequestDto(
    @SerialName("email") val email: String,
    @SerialName("password") val password: String,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("phone") val phone: String? = null,
)

@Serializable
data class RegisterResponseDto(
    @SerialName("message") val message: String,
    @SerialName("email") val email: String,
    @SerialName("dev_verification_token") val devVerificationToken: String? = null,
    @SerialName("dev_verification_code") val devVerificationCode: String? = null,
)
