package com.wellpaid.util

import com.wellpaid.core.model.expense.ExpenseDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseSplitFormTextsTest {

    private fun baseDto(
        isShared: Boolean = true,
        isMine: Boolean = true,
        amountCents: Int = 10_000,
        splitMode: String? = null,
        myShareCents: Int? = 5000,
        otherUserShareCents: Int? = 5000,
        ownerPercentBps: Int? = null,
        peerPercentBps: Int? = null,
    ): ExpenseDto = ExpenseDto(
        id = "e1",
        ownerUserId = "o1",
        isMine = isMine,
        description = "x",
        amountCents = amountCents,
        expenseDate = "2025-01-01",
        dueDate = null,
        status = "pending",
        categoryId = "c1",
        categoryKey = "k",
        categoryName = "Cat",
        syncStatus = 0,
        installmentTotal = 1,
        installmentNumber = 1,
        createdAt = "2025-01-01T00:00:00Z",
        updatedAt = "2025-01-01T00:00:00Z",
        isShared = isShared,
        splitMode = splitMode,
        ownerPercentBps = ownerPercentBps,
        peerPercentBps = peerPercentBps,
        myShareCents = myShareCents,
        otherUserShareCents = otherUserShareCents,
    )

    @Test
    fun splitTexts_notShared_returns_empty() {
        val t = splitTextsForExpenseEdit(baseDto(isShared = false))
        assertEquals("", t.ownerShareText)
        assertTrue(!t.usePercentSplit)
    }

    @Test
    fun splitTexts_percent_uses_api_bps_when_present() {
        val t = splitTextsForExpenseEdit(
            baseDto(
                splitMode = "percent",
                myShareCents = 5000,
                otherUserShareCents = 5000,
                ownerPercentBps = 5000,
                peerPercentBps = 5000,
            ),
        )
        assertTrue(t.usePercentSplit)
        assertEquals("50,00", t.ownerPercentText)
        assertEquals("50,00", t.peerPercentDisplayText)
    }

    @Test
    fun splitTexts_percent_derives_from_cents_when_bps_null() {
        val t = splitTextsForExpenseEdit(
            baseDto(
                splitMode = "percent",
                amountCents = 100,
                myShareCents = 33,
                otherUserShareCents = 67,
                ownerPercentBps = null,
            ),
        )
        assertTrue(t.usePercentSplit)
        assertEquals("33,00", t.ownerPercentText)
    }
}
