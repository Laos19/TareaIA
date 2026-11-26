package com.tareia.app.dominio

import org.junit.Assert.*
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Builder, objetos de valor y entidades del dominio. */
class ConstruccionYValorTest {
    private val fecha = FechaLimite(LocalDate.of(2030, 12, 1))

    @Test fun builderManualConFechaNaceConfirmada() {
        val curso = Curso(nombre = "Base de Datos II", abreviatura = Abreviatura.de("bd2"))
        val a = ActividadBuilder().conId("a1").conTitulo("  Examen parcial  ").conCurso(curso).conFechaLimite(fecha)
            .conDescripcion("  cap 1-3 ").conContenidoOrigen("c1").build()
        assertEquals("a1", a.id)
        assertEquals("Examen parcial", a.titulo)
        assertEquals("cap 1-3", a.descripcion)
        assertEquals(TipoActividad.EXAMEN, a.tipo)
        assertEquals(OrigenActividad.MANUAL, a.origen)
        assertEquals("c1", a.idContenidoOrigen)
        assertTrue(a.estado is Confirmada)
        assertTrue(a.esConfirmable())
    }

    @Test fun builderValidaTituloYFechaSoloParaManual() {
        assertThrows(IllegalArgumentException::class.java) { ActividadBuilder().conTitulo("   ").conFechaLimite(fecha).build() }
        assertThrows(IllegalArgumentException::class.java) { ActividadBuilder().conTitulo("Sin fecha").build() }
        val moodle = ActividadBuilder().conTitulo("Tarea Moodle").desdeIntegracion("moodle:1:2").build()
        assertEquals(OrigenActividad.INTEGRACION, moodle.origen)
        assertEquals("moodle:1:2", moodle.idExterno)
        assertTrue(moodle.estado is PendienteValidar)
        assertFalse(moodle.esConfirmable())
    }

    @Test fun builderRespetaElTipoIndicado() {
        val a = ActividadBuilder().conTitulo("Examen").conTipo(TipoActividad.TAREA).conFechaLimite(fecha).build()
        assertEquals(TipoActividad.TAREA, a.tipo)
        assertEquals("Física", ActividadBuilder().conNombreCursoDetectado(" Física ").nombreCursoDetectado)
    }

    @Test fun deduccionDelTipoPorPalabrasDelTitulo() {
        assertEquals(TipoActividad.EXAMEN, TipoActividad.deducir("Práctica calificada 2"))
        assertEquals(TipoActividad.EXAMEN, TipoActividad.deducir("Evaluación final"))
        assertEquals(TipoActividad.PROYECTO, TipoActividad.deducir("Proyecto integrador"))
        assertEquals(TipoActividad.ENTREGABLE, TipoActividad.deducir("Entrega del informe"))
        assertEquals(TipoActividad.ENTREGABLE, TipoActividad.deducir("Informe de laboratorio"))
        assertEquals(TipoActividad.ENTREGABLE, TipoActividad.deducir("Entregable 1"))
        assertEquals(TipoActividad.TAREA, TipoActividad.deducir("Lectura"))
    }

    @Test fun editarActualizaDatosYPapeleraMarcaFecha() {
        val a = ActividadBuilder().conTitulo("Tarea").conFechaLimite(fecha).build()
        val c = Curso(nombre = "Calidad", abreviatura = Abreviatura.de("si784"))
        a.editar("Tarea 2", null, c, "nueva")
        assertEquals("Tarea 2", a.titulo); assertNull(a.fechaLimite); assertEquals(c, a.curso); assertEquals("nueva", a.descripcion)
        assertFalse(a.esConfirmable())
        assertEquals("Tarea 2", a.etiquetaPapelera())
        assertFalse(a.estaEnPapelera())
        a.enviarAPapelera(); assertTrue(a.estaEnPapelera())
        a.restaurar(); assertFalse(a.estaEnPapelera())
    }

    @Test fun abreviaturaYPrioridadSeValidan() {
        assertEquals("SI-784", Abreviatura.de("  si-784 ").valor)
        assertThrows(IllegalArgumentException::class.java) { Abreviatura("") }
        assertThrows(IllegalArgumentException::class.java) { Abreviatura("ABCDEFGHIJKLM") }
        assertEquals(12, Abreviatura("ABCDEFGHIJKL").valor.length)
        assertThrows(IllegalArgumentException::class.java) { Abreviatura("si") }
        assertEquals(1, Prioridad(1).nivel); assertEquals(5, Prioridad(5).nivel)
        assertThrows(IllegalArgumentException::class.java) { Prioridad(0) }
        assertThrows(IllegalArgumentException::class.java) { Prioridad(6) }
        assertEquals(3, Prioridad.NORMAL.nivel)
    }

    @Test fun fechaLimiteDesdeTextoYFutura() {
        assertEquals(LocalDate.of(2030, 1, 2), FechaLimite.desdeTexto(" 2030-01-02 ")!!.valor)
        assertNull(FechaLimite.desdeTexto("2030-13-40"))
        assertNull(FechaLimite.desdeTexto("   "))
        assertNull(FechaLimite.desdeTexto(null))
        val hoy = LocalDate.of(2026, 11, 20)
        assertTrue(FechaLimite(hoy).esFutura(hoy))
        assertTrue(FechaLimite(hoy.plusDays(1)).esFutura(hoy))
        assertFalse(FechaLimite(hoy.minusDays(1)).esFutura(hoy))
        assertEquals("2026-11-20", FechaLimite(hoy).toString())
    }

    @Test fun franjasSeSolapanSoloElMismoDiaYConCruce() {
        val f = FranjaHoraria(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(10, 0))
        assertTrue(f.seSolapaCon(FranjaHoraria(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0))))
        assertTrue(f.seSolapaCon(FranjaHoraria(DayOfWeek.MONDAY, LocalTime.of(7, 0), LocalTime.of(8, 30))))
        assertFalse(f.seSolapaCon(FranjaHoraria(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(11, 0))))
        assertFalse(f.seSolapaCon(FranjaHoraria(DayOfWeek.MONDAY, LocalTime.of(6, 0), LocalTime.of(8, 0))))
        assertFalse(f.seSolapaCon(FranjaHoraria(DayOfWeek.TUESDAY, LocalTime.of(8, 0), LocalTime.of(10, 0))))
        assertEquals(480, f.minutoInicio); assertEquals(600, f.minutoFin)
        assertThrows(IllegalArgumentException::class.java) { FranjaHoraria(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(10, 0)) }
    }

    @Test fun estudianteNoAceptaFranjasCruzadasYFiltraDisponibles() {
        val e = Estudiante(nombre = "Ana", correo = "ana@upt.pe")
        val clase = FranjaHoraria(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0), TipoFranja.CLASE)
        val libre = FranjaHoraria(DayOfWeek.MONDAY, LocalTime.of(14, 0), LocalTime.of(16, 0))
        assertTrue(e.agregarFranja(clase)); assertTrue(e.agregarFranja(libre))
        assertFalse(e.agregarFranja(FranjaHoraria(DayOfWeek.MONDAY, LocalTime.of(11, 0), LocalTime.of(13, 0))))
        assertEquals(listOf(libre), e.franjasDisponibles())
        e.quitarFranja(libre)
        assertEquals(listOf(clase), e.franjas)
    }

    @Test fun prototypeClonaElPlanEnProfundidad() {
        val s = SesionEstudio(idActividad = "a1", tituloActividad = "Informe", inicio = LocalDateTime.of(2030, 1, 1, 9, 0), fin = LocalDateTime.of(2030, 1, 1, 10, 30))
        val vigente = PlanEstudio(estado = EstadoPlan.VIGENTE, sesiones = listOf(s))
        val copia = vigente.clonar()
        assertNotEquals(vigente.id, copia.id)
        assertEquals(EstadoPlan.PROPUESTA, copia.estado)
        assertEquals(vigente.sesiones, copia.sesiones)
        copia.reemplazarSesionesDe("a1", emptyList())
        assertEquals(1, vigente.sesiones.size)
        assertTrue(copia.sesionesDe("a1").isEmpty())
        assertEquals(90, s.duracionMinutos)
        assertEquals(EstadoPlan.VIGENTE, PlanEstudio.vacio().estado)
        vigente.quitarSesionesDe("a1"); assertTrue(vigente.sesiones.isEmpty())
    }

    @Test fun sesionesSeCruzanYValidanHoras() {
        val base = LocalDateTime.of(2030, 1, 1, 9, 0)
        val a = SesionEstudio(idActividad = "a", tituloActividad = "A", inicio = base, fin = base.plusHours(1))
        assertTrue(a.seCruzaCon(a.copy(inicio = base.plusMinutes(30), fin = base.plusHours(2))))
        assertFalse(a.seCruzaCon(a.copy(inicio = base.plusHours(1), fin = base.plusHours(2))))
        assertFalse(a.seCruzaCon(a.copy(inicio = base.minusHours(1), fin = base)))
        assertEquals(a, a.clonar())
        assertThrows(IllegalArgumentException::class.java) { SesionEstudio(idActividad = "a", tituloActividad = "A", inicio = base, fin = base) }
    }
}
