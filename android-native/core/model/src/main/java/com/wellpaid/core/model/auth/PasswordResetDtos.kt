package com.wellpaid.core.model.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ForgotPasswordRequestDto(
    @SerialName("email") val email: String,
)

@Serializable
data class ForgotPasswordResponseDto(
    @SerialName("message") val message: String,
    @SerialName("dev_reset_token") val devResetToken: String? = null,
)

@Serializable
data class ResetPasswordRequestDto(
    @SerialName("token") val token: String,
    @SerialName("new_password") val newPassword: String,
)
