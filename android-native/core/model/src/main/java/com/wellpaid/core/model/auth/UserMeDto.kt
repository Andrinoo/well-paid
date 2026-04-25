package com.wellpaid.core.model.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserMeDto(
    val email: String,
    @SerialName("full_name") val fullName: String? = null,
    /** Se definido, usado na saudação do ecrã inicial em vez do primeiro nome do cadastro. */
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("family_mode_enabled") val familyModeEnabled: Boolean = false,
)
