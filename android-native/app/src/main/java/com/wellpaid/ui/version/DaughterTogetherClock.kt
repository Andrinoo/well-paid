package com.wellpaid.ui.version

import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * Âncora simbólica: nascimento (10/02/2023 02:39, America/São Paulo).
 * Formato de elapsed: apenas números `d.h.m.s.mmmm` (milissegundos com 4 dígitos).
 */
object DaughterTogetherClock {

    private val ZONE: ZoneId = ZoneId.of("America/Sao_Paulo")

    private val ANCHOR_EPOCH_MS: Long =
        ZonedDateTime.of(
            LocalDate.of(2023, Month.FEBRUARY, 10),
            LocalTime.of(2, 39, 0),
            ZONE,
        ).toInstant().toEpochMilli()

    private val TODAY_BR: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZONE)

    /** Data de hoje na zona da âncora (para alinhar com o elapsed). */
    fun todayBrazil(nowEpochMillis: Long = System.currentTimeMillis()): String =
        TODAY_BR.format(Instant.ofEpochMilli(nowEpochMillis))

    fun formatElapsedNumeric(nowEpochMillis: Long = System.currentTimeMillis()): String {
        val delta = (nowEpochMillis - ANCHOR_EPOCH_MS).coerceAtLeast(0L)
        var rem = delta
        val days = rem / 86_400_000L
        rem %= 86_400_000L
        val hours = rem / 3_600_000L
        rem %= 3_600_000L
        val minutes = rem / 60_000L
        rem %= 60_000L
        val seconds = rem / 1_000L
        val millis = (rem % 1_000L).toInt()
        return "$days.$hours.$minutes.$seconds.${millis.toString().padStart(4, '0')}"
    }
}
