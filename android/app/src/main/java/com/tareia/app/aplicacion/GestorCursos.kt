package com.tareia.app.aplicacion

import com.tareia.app.dominio.Abreviatura
import com.tareia.app.dominio.Curso
import com.tareia.app.dominio.CursoRepository
import com.tareia.app.dominio.Prioridad
import java.text.Normalizer
import java.util.Locale

/**
 * GestorCursos (RF-16, RF-18) — AGREGADO respecto al diagrama: la app ya tenía la pantalla
 * de cursos y su lógica no correspondía a ninguno de los otros gestores.
 *
 * RN12: la abreviatura del curso es única.
 */
class GestorCursos(private val cursos: CursoRepository) {

    fun listar(): List<Curso> = cursos.listarActivos()
    fun buscar(id: String): Curso? = cursos.buscarPorId(id)

    /**
     * Crea o actualiza un curso. Devuelve false si la abreviatura ya la usa otro curso (RN12)
     * o si los datos no son válidos (los objetos de valor rechazan valores inválidos).
     */
    fun guardar(id: String?, nombre: String, abreviatura: String, prioridad: Int, dificultad: Int): Boolean {
        val abrev = runCatching { Abreviatura.de(abreviatura) }.getOrNull() ?: return false
        val prio = runCatching { Prioridad(prioridad) }.getOrNull() ?: return false
        if (cursos.listarActivos().any { it.id != id && it.abreviatura == abrev }) return false
        val existente = id?.let { cursos.buscarPorId(it) }
        val curso = if (existente != null) {
            existente.renombrar(nombre)
            existente.abreviatura = abrev
            existente.prioridad = prio
            existente.dificultad = dificultad.coerceIn(1, 5)
            existente
        } else {
            runCatching { Curso(id = id ?: java.util.UUID.randomUUID().toString(), nombre = nombre.trim(), abreviatura = abrev, prioridad = prio, dificultad = dificultad.coerceIn(1, 5)) }
                .getOrNull() ?: return false
        }
        cursos.guardar(curso)
        return true
    }

    /**
     * Devuelve el curso con ese nombre; si no existe, lo registra con una abreviatura
     * generada (iniciales). Los formularios de la app permiten escribir un curso nuevo
     * y ahora toda actividad debe apuntar a un Curso real (relación del diagrama).
     */
    fun obtenerOCrear(nombre: String): Curso? {
        val limpio = nombre.trim()
        if (limpio.isBlank()) return null
        cursos.buscarPorNombre(limpio)?.let { return it }
        val usadas = cursos.listarActivos().map { it.abreviatura.valor }.toSet()
        val base = iniciales(limpio)
        var candidata = base
        var n = 2
        while (candidata in usadas) { candidata = (base.take(10) + n).take(12); n++ }
        val curso = Curso(nombre = limpio, abreviatura = Abreviatura.de(candidata))
        cursos.guardar(curso)
        return curso
    }

    private fun iniciales(nombre: String): String {
        val sinTildes = Normalizer.normalize(nombre, Normalizer.Form.NFD).replace(Regex("\\p{M}"), "")
        val palabras = sinTildes.split(Regex("[^A-Za-z0-9]+")).filter { it.length > 2 || it.any(Char::isDigit) }
        val ini = palabras.joinToString("") { it.take(1) }.uppercase(Locale.ROOT)
        return ini.ifBlank { sinTildes.filter { it.isLetterOrDigit() }.take(4).uppercase(Locale.ROOT) }.ifBlank { "CUR" }.take(10)
    }
}
