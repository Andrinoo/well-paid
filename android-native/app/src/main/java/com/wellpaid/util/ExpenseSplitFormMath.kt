package com.wellpaid.util

import kotlin.math.roundToInt

/**
 * Entrada e formatação de percentagens (0–100 %) para partilha em modo `percent` (basis points 0–10000).
 */
object ExpenseSplitFormMath {

    fun sanitizeBrlLikeInput(raw: String): String {
        val filtered = raw.filter { it.isDigit() || it == ',' || it == '.' }
        if (filtered.isEmpty()) return ""

        val lastComma = filtered.lastIndexOf(',')
        val lastDot = filtered.lastIndexOf('.')
        val decSep = when {
            lastComma >= 0 && lastDot >= 0 -> if (lastComma > lastDot) ',' else '.'
            lastComma >= 0 -> ','
            lastDot >= 0 -> '.'
            else -> null
        } ?: return filtered

        val idx = filtered.lastIndexOf(decSep)
        val intPart = filtered.substring(0, idx)
        val decPart = filtered.substring(idx + 1).take(2)
        return intPart + decSep + decPart
    }

    fun sanitizePercentInput(raw: String): String {
        val s = sanitizeBrlLikeInput(raw)
        if (s.isEmpty()) return ""
        val bps = parsePercentStringToBps(s)
        if (bps != null && bps > 10000) return "100,00"
        val d = s.replace(',', '.').toDoubleOrNull()
        if (d != null && d > 100.0) return "100,00"
        return s
    }

    fun parsePercentStringToBps(raw: String): Int? {
        var t = raw.trim()
        if (t.isEmpty()) return null
        if (t.endsWith(',') || t.endsWith('.')) {
            t = t.dropLast(1).trim()
            if (t.isEmpty()) return null
        }
        val normalized = t.replace(',', '.')
        val d = normalized.toDoubleOrNull() ?: return null
        if (d < 0 || d > 100) return null
        return (d * 100.0).roundToInt().coerceIn(0, 10000)
    }

    fun bpsToBrPercentText(bps: Int): String {
        val w = (bps / 100).coerceIn(0, 100)
        val f = bps % 100
        return "%d,%02d".format(w, f)
    }

    /**
     * Alinha ao backend [`_allocate_cents_from_percent_bps`]: primeiro sujeito arredonda half-up.
     */
    fun allocateCentsFromOwnerBps(totalCents: Int, ownerBps: Int): Pair<Int, Int> {
        require(totalCents >= 0)
        require(ownerBps in 0..10000)
        val peerBps = 10000 - ownerBps
        val owner = ((totalCents.toLong() * ownerBps + 5000L) / 10000L).toInt().coerceIn(0, totalCents)
        val peer = totalCents - owner
        return owner to peer
    }
}
