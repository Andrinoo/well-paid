package com.wellpaid.ui.investments

import com.wellpaid.core.model.investment.InvestmentAssetType
import java.util.Locale

/**
 * Ações, FIIs, BDR, ETF, cripto: taxa anual de rendimento (CDI, etc.) não se aplica;
 * o app envia 0 bps e o backend aceita.
 */
fun isVariableIncomePositionType(raw: String?): Boolean {
    if (raw.isNullOrBlank()) return false
    val s = raw.trim().lowercase(Locale.ROOT)
    if (s == "crypto" || s == "cripto") return true
    if (s == "stocks" || s == "stock" || s == "fii" || s == "bdr" || s == "etf") return true
    return when (InvestmentAssetType.fromRaw(s).key) {
        "stock", "fii", "bdr", "etf" -> true
        else -> false
    }
}
