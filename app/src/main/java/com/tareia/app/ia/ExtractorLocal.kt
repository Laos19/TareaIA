package com.tareia.app.ia

import com.tareia.app.dominio.ActividadBuilder
import com.tareia.app.dominio.Curso
import com.tareia.app.dominio.FechaLimite
import java.time.LocalDate
import java.util.Locale

/**
 * Extractor SIN IA (agregado respecto al diagrama): busca fechas y palabras clave con
 * expresiones regulares. Es el antiguo TaskExtractor de la app.
 *
 * Implementa la misma interfaz ExtractorParametros, así GestorAnalisisIA lo usa igual
 * que a los de la IA. Sirve como respaldo cuando no hay internet o falla el proveedor
 * (RNF-04) y para las "sugerencias locales" de la pantalla Importar.
 *
 * No pertenece a ninguna fábrica porque no es de ningún proveedor de IA.
 * Sus propuestas también se marcan para validación (RN09): son detecciones automáticas.
 */
class ExtractorLocal : ExtractorParametros {

    private val fechaNumerica = Regex("\\b(\\d{1,2})[/-](\\d{1,2})[/-](\\d{4})\\b")
    private val fechaIso = Regex("\\b(\\d{4})-(\\d{2})-(\\d{2})\\b")
    private val fechaEnTexto = Regex("(?i)\\b(\\d{1,2})\\s+de\\s+(enero|febrero|marzo|abril|mayo|junio|julio|agosto|septiembre|setiembre|octubre|noviembre|diciembre)(?:\\s+de\\s+(\\d{4}))?\\b")
    private val meses = mapOf("enero" to 1, "febrero" to 2, "marzo" to 3, "abril" to 4, "mayo" to 5, "junio" to 6, "julio" to 7, "agosto" to 8, "septiembre" to 9, "setiembre" to 9, "octubre" to 10, "noviembre" to 11, "diciembre" to 12)

    override suspend fun extraer(texto: String, cursos: List<Curso>): List<ActividadBuilder> = extraerAhora(texto, cursos)

    /** Versión normal (no suspend): la regex es instantánea y se puede llamar desde la pantalla. */
    fun extraerAhora(texto: String, cursos: List<Curso>): List<ActividadBuilder> {
        val lineas = texto.lineSequence().map { it.trim().trimStart('-', '•', '*', '·', ' ') }
            .filter { it.length >= 8 }.distinct().toList()
        val candidatas = lineas.filter { linea ->
            val l = linea.lowercase(Locale.ROOT)
            fechaNumerica.containsMatchIn(linea) || fechaIso.containsMatchIn(linea) || fechaEnTexto.containsMatchIn(linea) ||
                listOf("entrega", "tarea", "examen", "proyecto", "informe", "evaluación", "fecha límite", "vence").any(l::contains)
        }.ifEmpty { lineas.take(1) }.take(10)

        return candidatas.map { linea ->
            val curso = cursos.firstOrNull { c ->
                linea.contains(c.nombre, true) || linea.contains(c.abreviatura.valor, true)
            }
            val limpio = linea.replace(fechaNumerica, " ").replace(fechaIso, " ").replace(fechaEnTexto, " ")
                .replace(Regex("(?i)\\b(entregar|entrega|vence|fecha límite|fecha de entrega|tarea|examen|proyecto|evaluación)\\b"), " ")
                .replace(Regex("\\s+"), " ").trim(' ', ':', ',', '.', ';', '-')
            ActividadBuilder()
                .conTitulo(limpio.take(120).ifBlank { linea.take(120) })
                .conCurso(curso)
                .conNombreCursoDetectado(curso?.nombre.orEmpty())
                .conFechaLimite(FechaLimite.desdeTexto(leerFecha(linea)))
                .conDescripcion(linea)
                .desdeIA()
        }
    }

    private fun leerFecha(texto: String): String {
        runCatching {
            fechaIso.find(texto)?.let { m -> return LocalDate.of(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt()).toString() }
            fechaNumerica.find(texto)?.let { m -> return LocalDate.of(m.groupValues[3].toInt(), m.groupValues[2].toInt(), m.groupValues[1].toInt()).toString() }
            fechaEnTexto.find(texto)?.let { m ->
                val mes = meses[m.groupValues[2].lowercase(Locale.ROOT)] ?: return ""
                val anio = m.groupValues[3].toIntOrNull() ?: LocalDate.now().year
                return LocalDate.of(anio, mes, m.groupValues[1].toInt()).toString()
            }
        }
        return ""
    }
}
