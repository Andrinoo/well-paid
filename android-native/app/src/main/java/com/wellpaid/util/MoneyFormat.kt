package com.wellpaid.util

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Formatação BRL alinhada ao cliente Flutter: centavos inteiros, sem `Double` no valor final,
 * prefixo `R$ `, milhar `.`, decimal `,`.
 */
fun formatBrlFromCents(cents: Int): String {
    val neg = cents < 0
    val a = kotlin.math.abs(cents.toLong())
    val reais = a / 100
    val frac = (a % 100).toInt()
    val reaisPart = formatIntWithThousandsDots(reais)
    return "${if (neg) "-" else ""}R$ $reaisPart,${"%02d".format(frac)}"
}

/** Texto de campo sem prefixo (ex.: `1.234,56`), com milhares. */
fun formatBrlInputFromCents(cents: Int): String = centsToBrlInput(cents)

/**
 * Exibe preço de anúncio (minor units = centavos da moeda) para pesquisas fora de BRL.
 */
fun formatMinorCurrencyFromCents(cents: Int, currencyCode: String): String {
    val c = currencyCode.trim().uppercase().ifBlank { "BRL" }
    if (c == "BRL") return formatBrlFromCents(cents)
    return try {
        val locale = when (c) {
            "ARS" -> Locale("es", "AR")
            "MXN" -> Locale("es", "MX")
            "USD" -> Locale.US
            "UYU" -> Locale("es", "UY")
            "CLP" -> Locale("es", "CL")
            "COP" -> Locale("es", "CO")
            "PEN" -> Locale("es", "PE")
            else -> Locale.getDefault()
        }
        val fmt = NumberFormat.getCurrencyInstance(locale)
        fmt.currency = Currency.getInstance(c)
        fmt.format(cents / 100.0)
    } catch (_: Exception) {
        "$c ${cents / 100.0}"
    }
}

/** @see formatBrlInputFromCents */
fun centsToBrlInput(cents: Int): String = centsToBrlInput(cents.toLong())

fun centsToBrlInput(cents: Long): String {
    val neg = cents < 0
    val a = kotlin.math.abs(cents)
    val reais = a / 100
    val frac = (a % 100).toInt()
    val reaisPart = formatIntWithThousandsDots(reais)
    return "${if (neg) "-" else ""}$reaisPart,${"%02d".format(frac)}"
}

/**
 * Campo de valor em modo “caixa”: extrai só dígitos (ordem = centavos), aplica máscara pt-BR
 * (`1.256,36`). String vazia se não houver dígitos — alinhado a [brlDigitChainToCents].
 */
fun formatBrlAmountFromDigitInput(raw: String): String {
    val digits = raw.filter { it.isDigit() }.take(BRL_CENT_DIGIT_CHAIN_MAX)
    if (digits.isEmpty()) return ""
    return centsToBrlInput(brlDigitChainToCents(digits))
}

private fun formatIntWithThousandsDots(reais: Long): String {
    val s = reais.toString()
    return buildString {
        var i = 0
        for (ch in s.reversed()) {
            if (i > 0 && i % 3 == 0) append('.')
            append(ch)
            i++
        }
    }.reversed()
}

/**
 * Parse livre → centavos: aceita `12,50`, `1.234,56`, remove `R$` e espaços.
 * Regra simples: último `,` ou `.` como decimal se ambos existirem; senão vírgula como decimal (pt).
 */
fun parseBrlToCents(text: String): Int? {
    val t = text.trim()
    if (t.isEmpty()) return null
    val cleaned = t
        .replace("R$", "", ignoreCase = true)
        .replace(" ", "")
        .replace("\u00A0", "")
    if (cleaned.isEmpty()) return null
    val lastComma = cleaned.lastIndexOf(',')
    val lastDot = cleaned.lastIndexOf('.')
    val decimalSep = when {
        lastComma >= 0 && lastDot >= 0 -> if (lastComma > lastDot) ',' else '.'
        lastComma >= 0 -> ','
        lastDot >= 0 -> '.'
        else -> null
    }
    val normalized = if (decimalSep == ',') {
        val intPart = cleaned.substring(0, lastComma).replace(".", "")
        val decPart = cleaned.substring(lastComma + 1).filter { it.isDigit() }.take(2).padEnd(2, '0')
        "$intPart.$decPart"
    } else if (decimalSep == '.') {
        val intPart = cleaned.substring(0, lastDot).replace(",", "").replace(".", "")
        val decPart = cleaned.substring(lastDot + 1).filter { it.isDigit() }.take(2).padEnd(2, '0')
        "$intPart.$decPart"
    } else {
        cleaned.filter { it.isDigit() }
    }
    if (normalized.isEmpty()) return null
    val v = normalized.toDoubleOrNull()
    if (v != null && !v.isNaN() && !v.isInfinite()) {
        return kotlin.math.round(v * 100).toInt()
    }
    val legacy = cleaned.replace(".", "").replace(",", ".")
    val v2 = legacy.toDoubleOrNull() ?: return null
    return kotlin.math.round(v2 * 100).toInt()
}
