package com.wellpaid.core.model.family

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FamilyMeResponseDto(
    val family: FamilyOutDto? = null,
)

@Serializable
data class FamilyOutDto(
    val id: String,
    val name: String,
    val members: List<FamilyMemberDto> = emptyList(),
)

@Serializable
data class FamilyMemberDto(
    @SerialName("user_id") val userId: String,
    val email: String,
    @SerialName("full_name") val fullName: String? = null,
    val role: String,
    @SerialName("is_self") val isSelf: Boolean = false,
)

@Serializable
data class FamilyCreateDto(
    val name: String? = null,
)

@Serializable
data class FamilyUpdateDto(
    val name: String,
)

@Serializable
data class FamilyJoinRequestDto(
    val token: String,
)

@Serializable
data class FamilyInviteCreatedDto(
    val token: String,
    @SerialName("expires_at") val expiresAt: String,
    @SerialName("invite_url") val inviteUrl: String,
    @SerialName("invite_sent_email") val inviteSentEmail: String? = null,
    @SerialName("invite_sent") val inviteSent: Boolean = false,
)

@Serializable
data class FamilyInviteCreateRequestDto(
    @SerialName("invite_email") val inviteEmail: String? = null,
)

@Serializable
data class FamilyPendingInviteDto(
    @SerialName("invite_id") val inviteId: String,
    @SerialName("family_id") val familyId: String,
    @SerialName("family_name") val familyName: String,
    @SerialName("invite_email") val inviteEmail: String? = null,
    @SerialName("expires_at") val expiresAt: String,
    @SerialName("hours_remaining") val hoursRemaining: Int,
)
