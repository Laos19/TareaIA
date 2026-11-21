package com.tareia.app.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TiempoTest {
    @Test fun formatea() {
        assertEquals("00:00", formatMinute(0))
        assertEquals("09:05", formatMinute(9 * 60 + 5))
        assertEquals("23:59", formatMinute(23 * 60 + 59))
    }

    @Test fun parseaFormatosValidos() {
        assertEquals(18 * 60, parseMinute("18:00"))
        assertEquals(7 * 60 + 30, parseMinute(" 7:30 "))
    }

    @Test fun rechazaFormatosInvalidos() {
        assertNull(parseMinute("25:00"))
        assertNull(parseMinute("18h"))
        assertNull(parseMinute(""))
    }

    @Test fun idaYVuelta() = (0 until 24 * 60 step 7).forEach { assertEquals(it, parseMinute(formatMinute(it))) }
}
