package com.wellpaid.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/** Converte prefixo ISO `YYYY-MM-DD` para `dd/MM/yyyy`. */
fun formatIsoDateToBr(isoDate: String): String {
    val datePart = isoDate.take(10)
    val p = datePart.split('-')
    return if (p.size == 3) "${p[2]}/${p[1]}/${p[0]}" else isoDate
}

fun parseIsoDateLocal(isoDate: String): LocalDate? =
    try {
        LocalDate.parse(isoDate.take(10), DateTimeFormatter.ISO_LOCAL_DATE)
    } catch (_: DateTimeParseException) {
        null
    }
