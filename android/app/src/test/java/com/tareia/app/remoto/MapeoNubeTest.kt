package com.tareia.app.remoto

import com.tareia.app.dominio.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

/** El JSON que la app envía a POST /api/sync debe coincidir con el contrato de la API. */
class MapeoNubeTest {
    private val curso = Curso(nombre = "Calidad y Pruebas", abreviatura = Abreviatura.de("si-784"))
    private val fecha = FechaLimite(LocalDate.of(2030, 11, 26))

    @Test
    fun cursoSeTraduceConSusValores() {
        val j = MapeoNube.curso(curso)
        assertEquals(curso.id, j.getString("id"))
        assertEquals("SI-784", j.getString("abreviatura"))
        assertEquals(3, j.getInt("prioridad"))
        assertTrue(j.isNull("eliminadoEn"))
    }

    @Test
    fun actividadManualViajaConfirmadaYConCurso() {
        val a = ActividadBuilder().conTitulo("Proyecto de Unidad II").conCurso(curso).conFechaLimite(fecha).build()
        val j = MapeoNube.actividad(a)
        assertEquals("CONFIRMADA", j.getString("estado"))
        assertEquals("MANUAL", j.getString("origen"))
        assertEquals("PROYECTO", j.getString("tipo"))
        assertEquals(curso.id, j.getString("cursoId"))
        assertEquals("2030-11-26", j.getString("fechaLimite"))
        assertTrue(j.isNull("idExterno"))
        assertTrue(j.isNull("eventoCalendarioId"))
    }

    @Test
    fun propuestaDeIaSinFechaViajaPendienteYConNulos() {
        val a = ActividadBuilder().conTitulo("Leer capítulo").desdeIA().build()
        val j = MapeoNube.actividad(a)
        assertEquals("PENDIENTE_VALIDAR", j.getString("estado"))
        assertEquals("IA", j.getString("origen"))
        assertTrue(j.isNull("fechaLimite"))
        assertTrue(j.isNull("cursoId"))
    }

    @Test
    fun estadosSeTraducenComoEnLaBaseDeDatos() {
        assertEquals("PENDIENTE_VALIDAR", MapeoNube.codigoEstado(PendienteValidar()))
        assertEquals("CONFIRMADA", MapeoNube.codigoEstado(Confirmada()))
        assertEquals("SINCRONIZADA", MapeoNube.codigoEstado(Sincronizada()))
        assertEquals("COMPLETADA", MapeoNube.codigoEstado(Completada()))
    }

    @Test
    fun papeleraSeEnviaComoFechaIso() {
        val a = ActividadBuilder().conTitulo("Tarea").conFechaLimite(fecha).build()
        a.enviarAPapelera()
        val iso = MapeoNube.actividad(a).getString("eliminadoEn")
        assertTrue(iso.matches(Regex("""\d{4}-\d{2}-\d{2}T.*Z""")))
        assertEquals(JSONObject.NULL, MapeoNube.instante(null))
    }

    @Test
    fun loteIncluyeDesdeSoloSiSeIndica() {
        val a = ActividadBuilder().conTitulo("Tarea").conFechaLimite(fecha).build()
        val sin = MapeoNube.lote(listOf(curso), listOf(a), null)
        assertEquals(1, sin.getJSONArray("cursos").length())
        assertEquals(1, sin.getJSONArray("actividades").length())
        assertFalse(sin.has("desde"))
        assertEquals("2026-11-20T00:00:00Z", MapeoNube.lote(emptyList(), emptyList(), "2026-11-20T00:00:00Z").getString("desde"))
    }

    @Test
    fun recortaTextosLargosAlLimiteDeLaApi() {
        val a = ActividadBuilder().conTitulo("x".repeat(300)).conFechaLimite(fecha).build()
        assertEquals(200, MapeoNube.actividad(a).getString("titulo").length)
    }
}
