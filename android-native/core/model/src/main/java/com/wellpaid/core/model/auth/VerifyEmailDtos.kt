package com.wellpaid.core.model.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VerifyEmailRequestDto(
    @SerialName("email") val email: String? = null,
    @SerialName("token") val token: String? = null,
    @SerialName("code") val code: String? = null,
)

@Serializable
data class ResendVerificationRequestDto(
    @SerialName("email") val email: String,
)

@Serializable
data class ResendVerificationResponseDto(
    @SerialName("message") val message: String,
    @SerialName("dev_verification_token") val devVerificationToken: String? = null,
    @SerialName("dev_verification_code") val devVerificationCode: String? = null,
)
