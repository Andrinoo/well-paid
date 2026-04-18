package com.wellpaid.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExpenseSplitFormMathTest {

    @Test
    fun parsePercentStringToBps_accepts_comma_decimals() {
        assertEquals(5050, ExpenseSplitFormMath.parsePercentStringToBps("50,5"))
        assertEquals(5000, ExpenseSplitFormMath.parsePercentStringToBps("50"))
    }

    @Test
    fun parsePercentStringToBps_rejects_out_of_range() {
        assertNull(ExpenseSplitFormMath.parsePercentStringToBps("101"))
        assertNull(ExpenseSplitFormMath.parsePercentStringToBps("-1"))
    }

    @Test
    fun sanitizePercentInput_caps_over_100_percent() {
        assertEquals("100,00", ExpenseSplitFormMath.sanitizePercentInput("100,01"))
    }

    @Test
    fun bpsToBrPercentText_formats() {
        assertEquals("50,00", ExpenseSplitFormMath.bpsToBrPercentText(5000))
        assertEquals("0,50", ExpenseSplitFormMath.bpsToBrPercentText(50))
    }

    @Test
    fun allocateCentsFromOwnerBps_matches_backend_half_up() {
        val (a, b) = ExpenseSplitFormMath.allocateCentsFromOwnerBps(100, 3333)
        assertEquals(33, a)
        assertEquals(67, b)
    }
}
