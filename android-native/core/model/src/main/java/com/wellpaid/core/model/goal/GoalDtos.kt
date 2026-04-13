package com.wellpaid.core.model.goal

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GoalDto(
    val id: String,
    @SerialName("owner_user_id") val ownerUserId: String,
    @SerialName("is_mine") val isMine: Boolean = true,
    val title: String,
    @SerialName("target_cents") val targetCents: Int,
    @SerialName("current_cents") val currentCents: Int,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class GoalCreateDto(
    val title: String,
    @SerialName("target_cents") val targetCents: Int,
    @SerialName("current_cents") val currentCents: Int = 0,
    @SerialName("is_active") val isActive: Boolean = true,
)

@Serializable
data class GoalUpdateDto(
    val title: String,
    @SerialName("target_cents") val targetCents: Int,
    @SerialName("current_cents") val currentCents: Int,
    @SerialName("is_active") val isActive: Boolean,
)

@Serializable
data class GoalContributeDto(
    @SerialName("amount_cents") val amountCents: Int,
    val note: String? = null,
)
