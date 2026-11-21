package com.tareia.app

import org.junit.Assert.*
import org.junit.Test

class GoogleCalendarIdTest {
    @Test fun idDeterministaYCompatibleConGoogle() {
        val id = GoogleCalendarApi.stableEventId("firebase-usuario-1", "tarea-1")
        assertEquals(id, GoogleCalendarApi.stableEventId("firebase-usuario-1", "tarea-1"))
        assertTrue(id.matches(Regex("[0-9a-v]{5,1024}")))
    }

    @Test fun idsNoSeReutilizanEntreCuentasOTareas() {
        val a = GoogleCalendarApi.stableEventId("usuario-A", "tarea-1")
        assertNotEquals(a, GoogleCalendarApi.stableEventId("usuario-B", "tarea-1"))
        assertNotEquals(a, GoogleCalendarApi.stableEventId("usuario-A", "tarea-2"))
    }
}
