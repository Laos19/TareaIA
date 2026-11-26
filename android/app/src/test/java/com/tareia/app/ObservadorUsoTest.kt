package com.tareia.app

import com.tareia.app.aplicacion.ObservadorUso
import org.junit.Assert.assertEquals
import org.junit.Test

/** El observador recibe el evento y sus datos tal cual (contrato del patrón Observer). */
class ObservadorUsoTest {
    @Test
    fun recibeEventoYDatos() {
        val recibidos = mutableListOf<Pair<String, Map<String, Any>>>()
        val obs = ObservadorUso { evento, datos -> recibidos += evento to datos }
        obs.alOcurrir("actividad_creada", mapOf("origen" to "IA"))
        assertEquals(listOf("actividad_creada" to mapOf<String, Any>("origen" to "IA")), recibidos)
    }
}
