package com.wellpaid.ui.expenses

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Urgência de vencimento (alinhado ao doc Flutter / ecrã «A pagar»):
 * compara [anchor] com hoje só em termos de calendário.
 */
enum class DueUrgency {
    Overdue,
    DueToday,
    DueSoon,
    Upcoming,
    Safe,
}

fun daysUntilDue(anchor: LocalDate, today: LocalDate = LocalDate.now()): Long =
    ChronoUnit.DAYS.between(today, anchor)

fun dueUrgencyForDays(days: Long): DueUrgency = when {
    days < 0 -> DueUrgency.Overdue
    days == 0L -> DueUrgency.DueToday
    days in 1L..3L -> DueUrgency.DueSoon
    days in 4L..10L -> DueUrgency.Upcoming
    else -> DueUrgency.Safe
}

fun dueUrgencyColorOnLight(urgency: DueUrgency): Color = when (urgency) {
    DueUrgency.Overdue -> Color(0xFFB71C1C)
    DueUrgency.DueToday -> Color(0xFFD32F2F)
    DueUrgency.DueSoon -> Color(0xFFE65100)
    DueUrgency.Upcoming -> Color(0xFF388E3C)
    DueUrgency.Safe -> Color(0xFF2E7D32)
}

fun dueUrgencyFontWeight(urgency: DueUrgency): FontWeight = when (urgency) {
    DueUrgency.Overdue, DueUrgency.DueToday -> FontWeight.ExtraBold
    DueUrgency.DueSoon -> FontWeight.Bold
    DueUrgency.Upcoming -> FontWeight.SemiBold
    DueUrgency.Safe -> FontWeight.Medium
}
