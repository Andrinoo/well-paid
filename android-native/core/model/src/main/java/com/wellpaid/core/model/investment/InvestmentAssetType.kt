package com.wellpaid.core.model.investment

import java.util.Locale

enum class InvestmentAssetType(val key: String) {
    STOCK("stock"),
    FII("fii"),
    BDR("bdr"),
    ETF("etf"),
    TREASURY("treasury"),
    CDB("cdb"),
    CDI("cdi"),
    FIXED_INCOME("fixed_income"),
    UNKNOWN("unknown");

    companion object {
        fun fromRaw(value: String?): InvestmentAssetType {
            val normalized = value
                ?.trim()
                ?.lowercase(Locale.ROOT)
                ?.replace(' ', '_')
                ?: return UNKNOWN
            return when (normalized) {
                "stock", "stocks", "acao", "acoes", "equity" -> STOCK
                "fii", "fiis", "reit", "reits" -> FII
                "bdr", "bdrs" -> BDR
                "etf", "etfs" -> ETF
                "treasury", "tesouro", "tesourodireto" -> TREASURY
                "cdb" -> CDB
                "cdi" -> CDI
                "fixed_income", "renda_fixa" -> FIXED_INCOME
                else -> UNKNOWN
            }
        }
    }
}

