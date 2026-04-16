package com.wellpaid.core.model.announcement

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AnnouncementDto(
    val id: String,
    val title: String,
    val body: String,
    val kind: String,
    val placement: String,
    val priority: Int,
    @SerialName("cta_label") val ctaLabel: String? = null,
    @SerialName("cta_url") val ctaUrl: String? = null,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("starts_at") val startsAt: String? = null,
    @SerialName("ends_at") val endsAt: String? = null,
)

@Serializable
data class AnnouncementListDto(
    val items: List<AnnouncementDto>,
    val total: Int,
    val skip: Int,
    val limit: Int,
)
