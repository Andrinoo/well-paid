package com.wellpaid.util

/** Modo “caixa”: cadeia só com dígitos = centavos (como Flutter `BrCurrencyInputFormatter`). */
const val BRL_CENT_DIGIT_CHAIN_MAX = 12

fun brlDigitChainToCents(digits: String): Long {
    val d = digits.filter { it.isDigit() }.take(BRL_CENT_DIGIT_CHAIN_MAX)
    if (d.isEmpty()) return 0L
    return d.toLongOrNull() ?: 0L
}

fun centsToBrlDigitChainDisplay(cents: Long): String =
    centsToBrlInput(cents.coerceIn(0L, 999_999_999_999L))
