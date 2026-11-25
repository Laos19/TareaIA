package com.tareia.app

import com.tareia.app.aplicacion.GestorActividades
import com.tareia.app.aplicacion.GestorAlertas
import com.tareia.app.aplicacion.GestorAulaVirtual
import com.tareia.app.aplicacion.GestorCursos
import com.tareia.app.dominio.*
import com.tareia.app.infraestructura.ProgramadorNotificaciones
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** Sincronización con el calendario del aula virtual: entregas «Por validar», sin duplicados y cambios de fecha. */
class GestorAulaVirtualTest {
    private class MemCursos : CursoRepository {
        val m = linkedMapOf<String, Curso>()
        override fun guardar(c: Curso) { m[c.id] = c }
        override fun listarActivos() = m.values.filter { !it.estaEnPapelera() }
        override fun buscarPorId(id: String) = m[id]
        override fun buscarPorNombre(nombre: String) = listarActivos().firstOrNull { it.nombre.equals(nombre, true) }
        override fun listarEnPapelera() = m.values.filter { it.estaEnPapelera() }
        override fun eliminar(id: String) { m.remove(id) }
    }
    private class MemActividades : ActividadRepository {
        val m = linkedMapOf<String, ActividadAcademica>()
        override fun guardar(a: ActividadAcademica) { m[a.id] = a }
        override fun buscarPorId(id: String) = m[id]
        override fun buscarPor(titulo: String, curso: Curso?, fecha: FechaLimite?) =
            listarTodas().firstOrNull { it.titulo.equals(titulo, true) && it.curso?.id == curso?.id && it.fechaLimite == fecha }
        override fun listarPendientes() = listarTodas().filter { !it.estaCompletada() }
        override fun listarTodas() = m.values.filter { !it.estaEnPapelera() }
        override fun listarEnPapelera() = m.values.filter { it.estaEnPapelera() }
        override fun buscarPorIdExterno(idExterno: String) = m.values.firstOrNull { idExterno.isNotBlank() && it.idExterno == idExterno }
        override fun eliminar(id: String) { m.remove(id) }
    }
    private class SinAlarmas : ProgramadorNotificaciones {
        override fun programar(r: Recordatorio) {}
        override fun cancelar(r: Recordatorio) {}
    }

    private val lima = ZoneId.of("America/Lima")
    private val hoy = LocalDate.of(2026, 11, 20)
    private lateinit var actividades: MemActividades
    private lateinit var gestor: GestorActividades
    private lateinit var aula: GestorAulaVirtual

    private fun ics(vararg eventos: Triple<String, String, String>) = buildString {
        append("BEGIN:VCALENDAR\r\nVERSION:2.0\r\n")
        for ((uid, resumen, fecha) in eventos) {
            append("BEGIN:VEVENT\r\nUID:$uid\r\nSUMMARY:$resumen\r\nDTSTART:$fecha\r\nCATEGORIES:Calidad y Pruebas\r\nEND:VEVENT\r\n")
        }
        append("END:VCALENDAR\r\n")
    }

    @Before fun preparar() {
        val cursos = MemCursos(); actividades = MemActividades()
        gestor = GestorActividades(actividades, DetectorDuplicados(actividades), GestorAlertas(SinAlarmas()), GestorCursos(cursos))
        aula = GestorAulaVirtual(gestor)
    }

    @Test fun importaEntregasPorValidarConSuCurso() {
        val r = aula.sincronizar(ics(Triple("1@upt", "Informe final vence", "20261126T045900Z"), Triple("2@upt", "Quiz 4 cierra", "20261128T235900")), lima, hoy)
        assertEquals(2, r.leidos)
        assertEquals(2, r.nuevas)
        val a = gestor.listar().first { it.titulo == "Informe final" }
        assertTrue(a.estado is PendienteValidar)
        assertEquals(OrigenActividad.INTEGRACION, a.origen)
        assertEquals("aula:1@upt", a.idExterno)
        assertEquals(LocalDate.of(2026, 11, 25), a.fechaLimite?.valor)
        assertEquals("Calidad y Pruebas", a.curso?.nombre)
    }

    @Test fun sincronizarDosVecesNoDuplica() {
        val texto = ics(Triple("1@upt", "Informe final vence", "20261126T045900Z"))
        assertEquals(1, aula.sincronizar(texto, lima, hoy).nuevas)
        assertEquals(0, aula.sincronizar(texto, lima, hoy).nuevas)
        assertEquals(1, gestor.listar().size)
    }

    @Test fun noImportaEntregasYaVencidas() {
        val r = aula.sincronizar(ics(Triple("1@upt", "Tarea vieja vence", "20261101T045900Z")), lima, hoy)
        assertEquals(0, r.nuevas)
        assertEquals(1, r.pasados)
    }

    @Test fun detectaYAplicaUnCambioDeFecha() {
        aula.sincronizar(ics(Triple("1@upt", "Informe final vence", "20261126T045900Z")), lima, hoy)
        val r = aula.sincronizar(ics(Triple("1@upt", "Informe final vence", "20261130T045900Z")), lima, hoy)
        assertEquals(0, r.nuevas)
        assertEquals(1, r.cambiosDeFecha.size)
        val cambio = r.cambiosDeFecha[0]
        assertEquals(LocalDate.of(2026, 11, 25), cambio.fechaAnterior)
        assertEquals(LocalDate.of(2026, 11, 29), cambio.fechaNueva)
        // La fecha no cambia sola: el estudiante decide
        assertEquals(LocalDate.of(2026, 11, 25), gestor.listar().first().fechaLimite?.valor)
        assertTrue(aula.aplicarCambio(cambio))
        assertEquals(LocalDate.of(2026, 11, 29), gestor.listar().first().fechaLimite?.valor)
        // Aceptar la nueva fecha no valida la entrega: sigue «Por validar»
        assertTrue(gestor.listar().first().estado is PendienteValidar)
        assertTrue(aula.sincronizar(ics(Triple("1@upt", "Informe final vence", "20261130T045900Z")), lima, hoy).cambiosDeFecha.isEmpty())
    }
}
