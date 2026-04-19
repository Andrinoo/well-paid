package com.wellpaid.core.model.goal

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GoalPriceAlternativeDto(
    val label: String = "",
    @SerialName("price_cents") val priceCents: Int = 0,
    val url: String? = null,
)

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
    @SerialName("target_url") val targetUrl: String? = null,
    @SerialName("reference_product_name") val referenceProductName: String? = null,
    @SerialName("reference_price_cents") val referencePriceCents: Int? = null,
    @SerialName("reference_currency") val referenceCurrency: String = "BRL",
    @SerialName("price_checked_at") val priceCheckedAt: String? = null,
    @SerialName("price_source") val priceSource: String? = null,
    @SerialName("price_alternatives") val priceAlternatives: List<GoalPriceAlternativeDto> = emptyList(),
)

@Serializable
data class GoalCreateDto(
    val title: String,
    @SerialName("target_cents") val targetCents: Int,
    @SerialName("current_cents") val currentCents: Int = 0,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("target_url") val targetUrl: String? = null,
    @SerialName("reference_product_name") val referenceProductName: String? = null,
    @SerialName("reference_price_cents") val referencePriceCents: Int? = null,
    @SerialName("reference_currency") val referenceCurrency: String = "BRL",
    @SerialName("price_source") val priceSource: String? = null,
)

@Serializable
data class GoalUpdateDto(
    val title: String,
    @SerialName("target_cents") val targetCents: Int,
    @SerialName("current_cents") val currentCents: Int,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("target_url") val targetUrl: String? = null,
    @SerialName("reference_product_name") val referenceProductName: String? = null,
    @SerialName("reference_price_cents") val referencePriceCents: Int? = null,
    @SerialName("reference_currency") val referenceCurrency: String? = null,
    @SerialName("price_source") val priceSource: String? = null,
)

@Serializable
data class GoalContributeDto(
    @SerialName("amount_cents") val amountCents: Int,
    val note: String? = null,
)
