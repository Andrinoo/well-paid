package com.wellpaid.core.model.shopping

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ShoppingListItemDto(
    val id: String,
    @SerialName("sort_order") val sortOrder: Int,
    val label: String,
    val quantity: Int,
    @SerialName("line_amount_cents") val lineAmountCents: Int? = null,
)

@Serializable
data class ShoppingListSummaryDto(
    val id: String,
    @SerialName("owner_user_id") val ownerUserId: String,
    @SerialName("is_mine") val isMine: Boolean = true,
    val title: String? = null,
    @SerialName("store_name") val storeName: String? = null,
    val status: String,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("expense_id") val expenseId: String? = null,
    @SerialName("total_cents") val totalCents: Int? = null,
    @SerialName("items_count") val itemsCount: Int,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class ShoppingListDetailDto(
    val id: String,
    @SerialName("owner_user_id") val ownerUserId: String,
    @SerialName("is_mine") val isMine: Boolean = true,
    val title: String? = null,
    @SerialName("store_name") val storeName: String? = null,
    val status: String,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("expense_id") val expenseId: String? = null,
    @SerialName("total_cents") val totalCents: Int? = null,
    val items: List<ShoppingListItemDto> = emptyList(),
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class ShoppingListCreateDto(
    val title: String? = null,
    @SerialName("store_name") val storeName: String? = null,
)

@Serializable
data class ShoppingListPatchDto(
    val title: String? = null,
    @SerialName("store_name") val storeName: String? = null,
    @SerialName("sync_total_from_line_items") val syncTotalFromLineItems: Boolean? = null,
)

@Serializable
data class ShoppingListItemCreateDto(
    val label: String,
    val quantity: Int = 1,
    @SerialName("line_amount_cents") val lineAmountCents: Int? = null,
)

@Serializable
data class ShoppingListCompleteDto(
    @SerialName("category_id") val categoryId: String,
    @SerialName("expense_date") val expenseDate: String,
    val status: String = "paid",
    @SerialName("total_cents") val totalCents: Int? = null,
    @SerialName("discount_cents") val discountCents: Int? = null,
    @SerialName("is_shared") val isShared: Boolean = false,
    @SerialName("shared_with_user_id") val sharedWithUserId: String? = null,
)

/** Corpo de POST /shopping-lists/price-suggestions (sugestões mercearia ao adicionar item). */
@Serializable
data class ShoppingListGroceryPriceRequestDto(
    val query: String,
    val unit: String? = null,
)
