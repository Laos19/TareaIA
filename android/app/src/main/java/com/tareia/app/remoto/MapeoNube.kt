package com.tareia.app.remoto

import com.tareia.app.dominio.ActividadAcademica
import com.tareia.app.dominio.Completada
import com.tareia.app.dominio.Confirmada
import com.tareia.app.dominio.Curso
import com.tareia.app.dominio.EstadoActividad
import com.tareia.app.dominio.PendienteValidar
import com.tareia.app.dominio.Sincronizada
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/**
 * Traduce el modelo de dominio de la app al formato JSON de la API (`POST /api/sync`).
 *
 * Es código puro (sin Android) para poder probarlo en la JVM. Los nombres de estado coinciden con
 * la columna `actividad.estado` de PostgreSQL, donde un trigger valida las mismas transiciones
 * que las clases del patrón State de la app.
 */
object MapeoNube {

    /** Código del estado para la API. */
    fun codigoEstado(e: EstadoActividad): String = when (e) {
        is PendienteValidar -> "PENDIENTE_VALIDAR"
        is Confirmada -> "CONFIRMADA"
        is Sincronizada -> "SINCRONIZADA"
        is Completada -> "COMPLETADA"
        else -> "PENDIENTE_VALIDAR"
    }

    /** Milisegundos de época → ISO-8601 (o null). */
    fun instante(ms: Long?): Any = ms?.let { Instant.ofEpochMilli(it).toString() } ?: JSONObject.NULL

    /** Curso → JSON. */
    fun curso(c: Curso): JSONObject = JSONObject()
        .put("id", c.id)
        .put("nombre", c.nombre.take(120))
        .put("abreviatura", c.abreviatura.valor)
        .put("prioridad", c.prioridad.nivel)
        .put("dificultad", c.dificultad.coerceIn(1, 5))
        .put("eliminadoEn", instante(c.fechaEliminacion))

    /** Actividad → JSON. */
    fun actividad(a: ActividadAcademica): JSONObject = JSONObject()
        .put("id", a.id)
        .put("cursoId", a.curso?.id ?: JSONObject.NULL)
        .put("titulo", a.titulo.take(200))
        .put("descripcion", a.descripcion.take(5000))
        .put("tipo", a.tipo.name)
        .put("origen", a.origen.name)
        .put("estado", codigoEstado(a.estado))
        .put("fechaLimite", a.fechaLimite?.valor?.toString() ?: JSONObject.NULL)
        .put("idExterno", a.idExterno.ifBlank { null } ?: JSONObject.NULL)
        .put("eventoCalendarioId", a.evento?.idExterno ?: JSONObject.NULL)
        .put("eliminadoEn", instante(a.fechaEliminacion))

    /** Cuerpo completo de una sincronización. */
    fun lote(cursos: List<Curso>, actividades: List<ActividadAcademica>, desde: String?): JSONObject {
        val cuerpo = JSONObject()
            .put("cursos", JSONArray().apply { cursos.forEach { put(curso(it)) } })
            .put("actividades", JSONArray().apply { actividades.forEach { put(actividad(it)) } })
        if (!desde.isNullOrBlank()) cuerpo.put("desde", desde)
        return cuerpo
    }
}
