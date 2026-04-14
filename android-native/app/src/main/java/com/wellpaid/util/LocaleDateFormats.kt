package com.wellpaid.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import java.util.Locale

/** Padrão de data na UI: pt → dd/MM/yyyy; en (US) → MM/dd/yyyy (alinhado a listas / Flutter). */
fun uiDateFormatter(locale: Locale): DateTimeFormatter =
    if (locale.language.equals("en", ignoreCase = true)) {
        DateTimeFormatter.ofPattern("MM/dd/uuuu").withResolverStyle(ResolverStyle.STRICT)
    } else {
        DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT)
    }

private val isoDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ISO_LOCAL_DATE.withResolverStyle(ResolverStyle.STRICT)

/** ISO yyyy-MM-dd → texto conforme locale da UI. */
fun formatIsoToUiDate(isoDate: String, locale: Locale): String {
    val d = parseIsoDateLocal(isoDate) ?: return isoDate
    return d.format(uiDateFormatter(locale))
}

/** Texto da UI → LocalDate ou null. Aceita também ISO. */
fun parseUiDateOrIso(text: String, locale: Locale): LocalDate? {
    val t = text.trim()
    if (t.isEmpty()) return null
    parseIsoDateLocal(t)?.let { return it }
    return try {
        LocalDate.parse(t, uiDateFormatter(locale))
    } catch (_: DateTimeParseException) {
        null
    }
}

fun localDateToIso(d: LocalDate): String = d.format(isoDateFormatter)

fun localDateToUtcStartMillis(d: LocalDate): Long =
    d.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

fun millisToLocalDate(ms: Long): LocalDate =
    Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()

/** Listas / resumo: dd/MM/yyyy (pt) ou M/d/yyyy (en) — separador / para leitura rápida. */
fun formatIsoDateForList(isoDate: String, locale: Locale): String {
    val d = parseIsoDateLocal(isoDate) ?: return isoDate
    val fmt = if (locale.language.equals("en", ignoreCase = true)) {
        DateTimeFormatter.ofPattern("M/d/uuuu", locale)
    } else {
        DateTimeFormatter.ofPattern("dd/MM/uuuu", locale)
    }
    return d.format(fmt)
}
