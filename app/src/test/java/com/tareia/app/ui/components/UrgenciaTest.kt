package com.tareia.app.ui.components

import com.tareia.app.CourseItem
import com.tareia.app.TaskItem
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class UrgenciaTest {
    private val hoy = LocalDate.of(2026, 11, 12)
    private fun tarea(fecha: String, estado: String = "Pendiente") = TaskItem(title = "T", dueDate = fecha, status = estado)

    @Test fun vencidaAyer() {
        val u = urgenciaDe(tarea("2026-11-11"), hoy)
        assertEquals(NivelUrgencia.Vencida, u.nivel)
        assertEquals("Venció ayer", u.texto)
    }

    @Test fun vencidaVariosDias() = assertEquals("Venció hace 2 días", urgenciaDe(tarea("2026-11-10"), hoy).texto)

    @Test fun venceHoyYManana() {
        assertEquals(Urgencia(NivelUrgencia.Urgente, "Vence hoy", 0), urgenciaDe(tarea("2026-11-12"), hoy))
        assertEquals(Urgencia(NivelUrgencia.Urgente, "Vence mañana", 1), urgenciaDe(tarea("2026-11-13"), hoy))
    }

    @Test fun prontoHastaTresDias() {
        assertEquals(NivelUrgencia.Pronto, urgenciaDe(tarea("2026-11-14"), hoy).nivel)
        assertEquals(NivelUrgencia.Pronto, urgenciaDe(tarea("2026-11-15"), hoy).nivel)
        assertEquals(NivelUrgencia.Normal, urgenciaDe(tarea("2026-11-16"), hoy).nivel)
    }

    @Test fun completadaNoTieneUrgencia() = assertEquals(NivelUrgencia.Ninguna, urgenciaDe(tarea("2026-11-01", "Completada"), hoy).nivel)

    @Test fun fechaInvalidaNoTieneUrgencia() {
        assertEquals(NivelUrgencia.Ninguna, urgenciaDe(tarea(""), hoy).nivel)
        assertEquals(NivelUrgencia.Ninguna, urgenciaDe(tarea("12/11/2026"), hoy).nivel)
    }
}

class SimboloCursoTest {
    private val cursos = listOf(CourseItem(name = "Base de Datos II", abbreviation = "BD2"))

    @Test fun usaAbreviaturaRegistradaSinImportarMayusculas() = assertEquals("BD2", simboloCurso("base de datos ii", cursos))

    @Test fun inicialesIgnorandoArticulos() {
        assertEquals("IA", simboloCurso("Inteligencia Artificial", emptyList()))
        assertEquals("BD", simboloCurso("Base de Datos", emptyList()))
    }

    @Test fun palabraUnicaComoElemento() = assertEquals("Qu", simboloCurso("QUÍMICA", emptyList()))

    @Test fun maximoTresLetras() = assertEquals("TDS", simboloCurso("Taller de Diseño de Software Avanzado", emptyList()))

    @Test fun nombreVacio() = assertEquals("?", simboloCurso("   ", emptyList()))
}
