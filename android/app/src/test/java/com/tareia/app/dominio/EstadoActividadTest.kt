package com.tareia.app.dominio

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/** Patrón State: cada estado decide qué acciones permite (RN01, RN05, RN09). */
class EstadoActividadTest {
    private val fecha = FechaLimite(LocalDate.of(2030, 12, 1))
    private fun ia(conFecha: Boolean = true) = ActividadBuilder().conTitulo("Informe").apply { if (conFecha) conFechaLimite(fecha) }.desdeIA().build()
    private fun manual() = ActividadBuilder().conTitulo("Informe").conFechaLimite(fecha).build()

    @Test fun pendienteSeConfirmaSiTieneFecha() {
        val a = ia()
        a.confirmar()
        assertTrue(a.estado is Confirmada)
        assertFalse(a.estaPendienteDeValidar())
    }

    @Test fun pendienteSinFechaNoSeConfirma() {
        val a = ia(conFecha = false)
        val e = assertThrows(IllegalStateException::class.java) { a.confirmar() }
        assertTrue(e.message!!.contains("RN05"))
        assertTrue(a.estaPendienteDeValidar())
    }

    @Test fun pendienteNoSeSincronizaNiSeCompleta() {
        val a = ia()
        assertTrue(assertThrows(IllegalStateException::class.java) { a.sincronizar() }.message!!.contains("RN01"))
        assertThrows(IllegalStateException::class.java) { a.completar() }
        a.reabrir()
        assertTrue(a.estado is PendienteValidar)
    }

    @Test fun confirmadaSeSincronizaYSeCompleta() {
        val a = manual()
        a.confirmar(); assertTrue(a.estado is Confirmada)
        a.reabrir(); assertTrue(a.estado is Confirmada)
        a.sincronizar(); assertTrue(a.estado is Sincronizada)
        a.confirmar(); a.sincronizar(); a.reabrir(); assertTrue(a.estado is Sincronizada)
        a.completar(); assertTrue(a.estaCompletada())
    }

    @Test fun completadaSoloSeReabre() {
        val a = manual()
        a.completar()
        a.confirmar(); a.sincronizar(); a.completar()
        assertTrue(a.estado is Completada)
        a.reabrir()
        assertTrue(a.estado is Confirmada)
    }

    @Test fun reabrirConEventoVuelveASincronizada() {
        val a = manual()
        a.vincularEvento(EventoCalendario("ev1", LocalDateTime.now(), "ana@upt.pe"))
        a.completar(); a.reabrir()
        assertTrue(a.estado is Sincronizada)
    }

    @Test fun nombresYReconstruccionDesdeLaBaseDeDatos() {
        assertEquals("Pendiente de validar", PendienteValidar().nombre())
        assertEquals("Confirmada", Confirmada().nombre())
        assertEquals("Sincronizada", Sincronizada().nombre())
        assertEquals("Completada", Completada().nombre())
        assertTrue(EstadoActividad.desdeNombre("Confirmada") is Confirmada)
        assertTrue(EstadoActividad.desdeNombre("Sincronizada") is Sincronizada)
        assertTrue(EstadoActividad.desdeNombre("Completada") is Completada)
        assertTrue(EstadoActividad.desdeNombre("cualquier cosa") is PendienteValidar)
    }
}
