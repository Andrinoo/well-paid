package com.wellpaid.util

import java.text.NumberFormat
import java.util.Locale

private val PtBr: Locale = Locale("pt", "BR")

/**
 * Números com separador de milhar (.) e decimal (,) no padrão usado no Brasil.
 */
fun formatDecimalPtBr(value: Double, minFractionDigits: Int = 2, maxFractionDigits: Int = 2): String {
    val nf = NumberFormat.getNumberInstance(PtBr)
    nf.minimumFractionDigits = minFractionDigits
    nf.maximumFractionDigits = maxFractionDigits
    return nf.format(value)
}
