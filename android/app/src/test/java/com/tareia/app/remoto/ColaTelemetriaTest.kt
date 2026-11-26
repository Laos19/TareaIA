package com.tareia.app.remoto

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class ColaTelemetriaTest {
    private fun e(n: Int) = JSONObject().put("n", n)

    @Test
    fun avisaCuandoHayUnLoteCompleto() {
        val cola = ColaTelemetria(capacidad = 10, tamanoLote = 3)
        assertFalse(cola.agregar(e(1)))
        assertFalse(cola.agregar(e(2)))
        assertTrue(cola.agregar(e(3)))
    }

    @Test
    fun tomaLotesEnOrdenDeLlegada() {
        val cola = ColaTelemetria(capacidad = 10, tamanoLote = 2)
        (1..3).forEach { cola.agregar(e(it)) }
        assertEquals(listOf(1, 2), cola.tomarLote().map { it.getInt("n") })
        assertEquals(listOf(3), cola.tomarLote().map { it.getInt("n") })
        assertTrue(cola.tomarLote().isEmpty())
    }

    @Test
    fun descartaLoMasAntiguoAlLlenarse() {
        val cola = ColaTelemetria(capacidad = 3, tamanoLote = 5)
        (1..5).forEach { cola.agregar(e(it)) }
        assertEquals(3, cola.tamano)
        assertEquals(listOf(3, 4, 5), cola.tomarLote().map { it.getInt("n") })
    }

    @Test
    fun unLoteFallidoVuelveAlFrenteSinPasarLaCapacidad() {
        val cola = ColaTelemetria(capacidad = 3, tamanoLote = 2)
        (1..3).forEach { cola.agregar(e(it)) }
        val lote = cola.tomarLote()
        cola.agregar(e(4))
        cola.devolver(lote)
        assertEquals(3, cola.tamano)
        assertEquals(listOf(2, 3), cola.tomarLote().map { it.getInt("n") })
    }

    @Test
    fun eventoYLogTienenElFormatoDeLaApi() {
        val ev = ColaTelemetria.evento("pantalla_vista", "Inicio", mapOf("ms" to 120), Instant.parse("2026-11-20T10:00:00Z"))
        assertEquals("pantalla_vista", ev.getString("tipo"))
        assertEquals("Inicio", ev.getString("pantalla"))
        assertEquals(120, ev.getJSONObject("datos").getInt("ms"))
        assertEquals("2026-11-20T10:00:00Z", ev.getString("creadoEn"))
        assertTrue(ColaTelemetria.evento("app_abierta").isNull("pantalla"))
        val log = ColaTelemetria.log("ERROR", "e".repeat(80), "m".repeat(3000), "2.0.0")
        assertEquals(60, log.getString("etiqueta").length)
        assertEquals(2000, log.getString("mensaje").length)
    }
}
