package com.wellpaid.util

import com.wellpaid.core.model.expense.ExpenseDto
import java.time.Instant
import java.time.LocalDate

/**
 * Alinha com o repositório Flutter: mais recente por [ExpenseDto.expenseDate], empate por [ExpenseDto.createdAt].
 */
fun sortExpensesNewestFirst(list: List<ExpenseDto>): List<ExpenseDto> =
    list.sortedWith(
        compareByDescending<ExpenseDto> { e ->
            parseIsoDateLocal(e.expenseDate) ?: LocalDate.MIN
        }.thenByDescending { e ->
            parseCreatedAtInstant(e.createdAt) ?: Instant.EPOCH
        },
    )

private fun parseCreatedAtInstant(iso: String): Instant? =
    try {
        Instant.parse(iso)
    } catch (_: Exception) {
        null
    }
