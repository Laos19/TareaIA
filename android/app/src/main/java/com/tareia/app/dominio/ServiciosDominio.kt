package com.tareia.app.dominio

import java.util.Locale

// =====================================================================================
// SERVICIOS DE DOMINIO (DDD)
// Contienen reglas de negocio que no pertenecen a una sola entidad.
// =====================================================================================

/**
 * «DomainService» Detecta actividades duplicadas.
 * Reglas RN02 / RN06: dos actividades son la misma si tienen igual título, curso y fecha.
 * Una actividad duplicada no se registra y se avisa al estudiante.
 */
class DetectorDuplicados(private val repositorio: ActividadRepository) {

    fun esDuplicada(a: ActividadAcademica): Boolean {
        val existente = repositorio.buscarPor(a.titulo, a.curso, a.fechaLimite) ?: return false
        return existente.id != a.id   // la misma actividad editándose no cuenta como duplicada
    }
}

/**
 * «DomainService» Clasifica el contenido importado en la carpeta del curso que corresponde.
 * Regla RN13: si el texto o el título contiene la abreviatura o el nombre de un curso,
 * va a la carpeta de ese curso; si no, queda "Sin clasificar" (devuelve null).
 */
class ClasificadorContenido {

    fun clasificar(c: ContenidoAcademico, cursos: List<Curso>): Carpeta? {
        val texto = (c.titulo + "\n" + c.texto()).lowercase(Locale.ROOT)
        val curso = cursos.firstOrNull { curso ->
            contienePalabra(texto, curso.abreviatura.valor.lowercase(Locale.ROOT)) ||
                contienePalabra(texto, curso.nombre.lowercase(Locale.ROOT))
        }
        return curso?.carpeta
    }

    /** Busca la palabra completa (para que "PS" no coincida dentro de "lapso"). */
    private fun contienePalabra(texto: String, palabra: String): Boolean {
        if (palabra.isBlank()) return false
        return Regex("(^|[^\\p{L}\\p{N}])" + Regex.escape(palabra) + "($|[^\\p{L}\\p{N}])").containsMatchIn(texto)
    }
}
