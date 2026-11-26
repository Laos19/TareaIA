package com.tareia.app

import com.tareia.app.aplicacion.GestorActividades
import com.tareia.app.aplicacion.GestorAlertas
import com.tareia.app.aplicacion.GestorCursos
import com.tareia.app.aplicacion.GestorPlanificacion
import com.tareia.app.dominio.*
import com.tareia.app.ia.GeneradorPlanIA
import com.tareia.app.ia.PlanificacionConIA
import com.tareia.app.ia.PlanificacionLocalPorPrioridad
import com.tareia.app.infraestructura.ProgramadorNotificaciones
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * Pruebas de los patrones del diagrama de clases. Corren en la JVM, sin celular:
 *   gradlew test
 * Los repositorios se reemplazan por versiones EN MEMORIA: como los gestores solo conocen
 * las INTERFACES (patrón Repository), se les puede dar cualquier implementación.
 */
class PatronesTest {

    // ----------------------- Repositorios en memoria -----------------------
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
    private class MemPlanes : PlanRepository {
        val l = mutableListOf<PlanEstudio>()
        override fun guardar(p: PlanEstudio) { l.removeAll { it.id == p.id }; l += p }
        override fun obtenerVigente(e: Estudiante) = l.firstOrNull { it.estado == EstadoPlan.VIGENTE }
    }
    private class MemEstudiante : EstudianteRepository {
        var e: Estudiante? = null
        override fun obtener() = e
        override fun guardar(e: Estudiante) { this.e = e }
    }
    private class SinAlarmas : ProgramadorNotificaciones {
        override fun programar(r: Recordatorio) {}
        override fun cancelar(r: Recordatorio) {}
    }

    private lateinit var cursos: MemCursos
    private lateinit var actividades: MemActividades
    private lateinit var gestorCursos: GestorCursos
    private lateinit var gestor: GestorActividades
    private lateinit var ps: Curso
    private val fecha = FechaLimite(LocalDate.now().plusDays(5))

    @Before fun preparar() {
        cursos = MemCursos(); actividades = MemActividades()
        gestorCursos = GestorCursos(cursos)
        gestor = GestorActividades(actividades, DetectorDuplicados(actividades), GestorAlertas(SinAlarmas()), gestorCursos)
        gestorCursos.guardar(null, "Patrones de Software", "ps", 5, 4)
        ps = cursos.buscarPorNombre("Patrones de Software")!!
    }

    // ------------------------------- BUILDER -------------------------------
    @Test fun builder_manualCompletoNaceConfirmada() {
        val a = ActividadBuilder().conTitulo("Informe Lab 04").conCurso(ps).conFechaLimite(fecha).build()
        assertTrue(a.estado is Confirmada)
    }

    @Test fun builder_desdeIANacePendienteDeValidar() {
        val a = ActividadBuilder().conTitulo("Examen parcial").desdeIA().build()
        assertTrue(a.estado is PendienteValidar)          // RN09
    }

    @Test(expected = IllegalArgumentException::class)
    fun builder_manualSinFechaFalla() {
        ActividadBuilder().conTitulo("Sin fecha").build()  // RN05
    }

    // -------------------------------- STATE --------------------------------
    @Test fun state_noSeSincronizaSinConfirmar() {
        val a = ActividadBuilder().conTitulo("Tarea").conFechaLimite(fecha).desdeIA().build()
        assertTrue(runCatching { a.sincronizar() }.isFailure)   // RN01
        a.confirmar()
        a.sincronizar()
        assertTrue(a.estado is Sincronizada)
        a.completar()
        assertTrue(a.estado is Completada)
    }

    // ------------------------ REPOSITORY + DUPLICADOS ------------------------
    @Test fun repository_noRegistraDuplicados() {
        assertNotNull(gestor.crearManual(ActividadBuilder().conTitulo("Informe").conCurso(ps).conFechaLimite(fecha)))
        assertNull(gestor.crearManual(ActividadBuilder().conTitulo("informe").conCurso(ps).conFechaLimite(fecha)))   // RN02
    }

    @Test fun abreviaturaUnica() {
        assertFalse(gestorCursos.guardar(null, "Otro curso", "PS", 3, 3))   // RN12
    }

    // ------------------------- PROTOTYPE + STRATEGY -------------------------
    @Test fun prototype_laPropuestaNoTocaElPlanVigente() = runBlocking {
        val planes = MemPlanes()
        val gp = GestorPlanificacion(PlanificacionLocalPorPrioridad(), planes, cursos, MemEstudiante())
        val e = gp.obtenerEstudiante()
        for (d in 0..6) gp.agregarFranja(FranjaHoraria(LocalDate.now().plusDays(d.toLong()).dayOfWeek, LocalTime.of(18, 0), LocalTime.of(20, 0)))
        val a1 = gestor.crearManual(ActividadBuilder().conTitulo("Informe").conCurso(ps).conFechaLimite(fecha))!!
        val a2 = gestor.crearManual(ActividadBuilder().conTitulo("Proyecto").conCurso(ps).conFechaLimite(fecha))!!

        gp.confirmarPlan(gp.generarPropuesta(e, a1), e)
        val vigente = gp.planVigente(e)
        val propuesta = gp.generarPropuesta(e, a2)

        assertNotEquals(vigente.id, propuesta.id)
        assertTrue(gp.planVigente(e).sesionesDe(a2.id).isEmpty())       // el vigente no cambió
        assertTrue(propuesta.sesionesDe(a2.id).isNotEmpty())
        assertEquals(vigente.sesionesDe(a1.id), propuesta.sesionesDe(a1.id))   // copia profunda con los mismos datos
    }

    @Test fun strategy_seCambiaEnTiempoDeEjecucion() = runBlocking {
        val gp = GestorPlanificacion(PlanificacionLocalPorPrioridad(), MemPlanes(), cursos, MemEstudiante())
        val a = gestor.crearManual(ActividadBuilder().conTitulo("Informe").conCurso(ps).conFechaLimite(fecha))!!
        val iaFalsa = object : GeneradorPlanIA {
            override suspend fun generar(acts: List<ActividadAcademica>, franjas: List<FranjaHoraria>) = listOf(
                SesionEstudio(idActividad = acts[0].id, tituloActividad = "IA",
                    inicio = LocalDate.now().plusDays(1).atTime(9, 0), fin = LocalDate.now().plusDays(1).atTime(10, 0)))
        }
        gp.setEstrategia(PlanificacionConIA(iaFalsa))
        val propuesta = gp.generarPropuesta(gp.obtenerEstudiante(), a)
        assertEquals(listOf("IA"), propuesta.sesionesDe(a.id).map { it.tituloActividad })
    }

    // ------------------------- SERVICIO DE DOMINIO -------------------------
    @Test fun clasificador_usaLaAbreviaturaDelCurso() {
        val apunte = Apunte(titulo = "Clase 3", tipo = TipoContenido.PDF, textoReconocido = "Tarea de PS para el viernes")
        assertEquals(ps.carpeta.id, ClasificadorContenido().clasificar(apunte, cursos.listarActivos())?.id)   // RN13
    }
}
