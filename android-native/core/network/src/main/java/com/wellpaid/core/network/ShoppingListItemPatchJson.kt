package com.wellpaid.core.network

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

fun shoppingListItemPatchJson(
    label: String? = null,
    quantity: Int? = null,
    lineAmountCents: Int? = null,
    clearLineAmount: Boolean = false,
    isPicked: Boolean? = null,
) = buildJsonObject {
    label?.let { put("label", it) }
    quantity?.let { put("quantity", it) }
    when {
        clearLineAmount -> put("line_amount_cents", JsonNull)
        lineAmountCents != null -> put("line_amount_cents", lineAmountCents)
    }
    isPicked?.let { put("is_picked", it) }
}
