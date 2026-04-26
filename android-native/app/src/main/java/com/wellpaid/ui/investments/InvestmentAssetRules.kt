package com.wellpaid.ui.investments

import java.util.Locale

fun isCryptoTypeRule(raw: String?): Boolean {
    val s = raw?.trim()?.lowercase(Locale.ROOT).orEmpty()
    return s == "crypto" || s == "cripto"
}

fun isTraditionalEquityTypeRule(raw: String?): Boolean {
    return when (raw?.trim()?.lowercase(Locale.ROOT)) {
        "stock", "stocks", "fii", "bdr", "etf" -> true
        else -> false
    }
}

fun isVariableIncomeTypeRule(raw: String?): Boolean {
    return isTraditionalEquityTypeRule(raw) || isCryptoTypeRule(raw)
}
