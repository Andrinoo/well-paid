package com.wellpaid.core.model.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfilePatchDto(
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("family_mode_enabled") val familyModeEnabled: Boolean? = null,
)
