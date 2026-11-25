package com.tareia.app.ingesta

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Entrega leída del calendario exportado del aula virtual (Moodle → Calendario → Exportar).
 *
 * @property uid Identificador único del evento en Moodle (estable entre descargas).
 * @property titulo Nombre de la actividad, sin sufijos como «vence» o «cierra».
 * @property fecha Fecha de vencimiento en la zona horaria del estudiante.
 * @property curso Curso al que pertenece (campo CATEGORIES), si viene.
 */
data class EventoAula(
    val uid: String,
    val titulo: String,
    val fecha: LocalDate,
    val curso: String?,
    val descripcion: String,
) {
    /** Id externo con el que se guarda la actividad (evita importar dos veces el mismo evento, RN02). */
    val idExterno: String get() = "aula:$uid"
}

/**
 * Lector mínimo de iCalendar (RFC 5545) para el calendario que exporta Moodle.
 *
 * Código puro (sin Android) para probarlo en la JVM. Soporta líneas plegadas, textos escapados,
 * fechas en UTC (`…Z`), con zona (`TZID=…`), locales y de día completo (`VALUE=DATE`).
 * Descarta los eventos de apertura («abre»), porque no son fechas de entrega.
 */
object LectorIcs {

    private val SUFIJOS_ENTREGA = listOf(
        " está en fecha de entrega", " esta en fecha de entrega", " se cierra", " cierra", " vence",
        " es debido", " is due", " closes", " due",
    )
    private val SUFIJOS_APERTURA = listOf(" se abre", " abre", " opens")
    private val FECHA_HORA = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
    private val FECHA = DateTimeFormatter.ofPattern("yyyyMMdd")

    /**
     * Lee el texto de un archivo .ics.
     * @param texto Contenido del calendario.
     * @param zona Zona horaria del estudiante (Perú: America/Lima) para convertir horas UTC en fechas.
     * @return Entregas encontradas, sin repetir UID, ordenadas por fecha.
     */
    fun leer(texto: String, zona: ZoneId = ZoneId.of("America/Lima")): List<EventoAula> {
        val lineas = desplegar(texto)
        val eventos = LinkedHashMap<String, EventoAula>()
        var actual: MutableMap<String, Pair<Map<String, String>, String>>? = null
        for (linea in lineas) {
            when {
                linea.equals("BEGIN:VEVENT", ignoreCase = true) -> actual = mutableMapOf()
                linea.equals("END:VEVENT", ignoreCase = true) -> {
                    actual?.let { aEvento(it, zona) }?.let { eventos[it.uid] = it }
                    actual = null
                }
                actual != null -> {
                    val sep = indiceSeparador(linea)
                    if (sep <= 0) continue
                    val cabecera = linea.substring(0, sep).split(';')
                    val nombre = cabecera.first().uppercase()
                    val params = cabecera.drop(1).mapNotNull { p ->
                        val i = p.indexOf('=')
                        if (i > 0) p.substring(0, i).uppercase() to p.substring(i + 1).trim('"') else null
                    }.toMap()
                    if (nombre !in actual) actual[nombre] = params to linea.substring(sep + 1)
                }
            }
        }
        return eventos.values.sortedWith(compareBy({ it.fecha }, { it.titulo }))
    }

    /** Une las líneas plegadas (las que empiezan con espacio o tabulación continúan la anterior). */
    internal fun desplegar(texto: String): List<String> {
        val salida = mutableListOf<String>()
        for (cruda in texto.replace("\r\n", "\n").replace('\r', '\n').split('\n')) {
            if ((cruda.startsWith(" ") || cruda.startsWith("\t")) && salida.isNotEmpty()) {
                salida[salida.lastIndex] = salida.last() + cruda.substring(1)
            } else if (cruda.isNotEmpty()) {
                salida += cruda
            }
        }
        return salida
    }

    /** Posición del «:» que separa nombre y valor (ignora los «:» dentro de parámetros entre comillas). */
    private fun indiceSeparador(linea: String): Int {
        var comillas = false
        for ((i, c) in linea.withIndex()) {
            if (c == '"') comillas = !comillas
            if (c == ':' && !comillas) return i
        }
        return -1
    }

    /** Quita los escapes de texto de iCalendar (\\n, \\, \; \\\\). */
    internal fun desescapar(valor: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < valor.length) {
            val c = valor[i]
            if (c == '\\' && i + 1 < valor.length) {
                when (val s = valor[i + 1]) {
                    'n', 'N' -> sb.append('\n')
                    else -> sb.append(s)
                }
                i += 2
            } else {
                sb.append(c); i++
            }
        }
        return sb.toString()
    }

    /** Convierte un valor DTSTART/DTEND en fecha local del estudiante. */
    internal fun fecha(valor: String, params: Map<String, String>, zona: ZoneId): LocalDate? = runCatching {
        val v = valor.trim()
        when {
            params["VALUE"].equals("DATE", ignoreCase = true) || v.length == 8 -> LocalDate.parse(v.take(8), FECHA)
            v.endsWith("Z") -> Instant.from(FECHA_HORA.withZone(ZoneId.of("UTC")).parse(v.dropLast(1))).atZone(zona).toLocalDate()
            params["TZID"] != null -> LocalDateTime.parse(v.take(15), FECHA_HORA).atZone(ZoneId.of(params["TZID"])).withZoneSameInstant(zona).toLocalDate()
            else -> LocalDateTime.parse(v.take(15), FECHA_HORA).toLocalDate()
        }
    }.getOrNull()

    /** Título sin el sufijo que Moodle agrega («Informe 3 vence» → «Informe 3»). Null si es un evento de apertura. */
    internal fun limpiarTitulo(resumen: String): String? {
        // Se antepone un espacio para que un título que es solo el sufijo («vence») también se reconozca.
        val t = " " + resumen.trim()
        if (SUFIJOS_APERTURA.any { t.endsWith(it, ignoreCase = true) }) return null
        val sufijo = SUFIJOS_ENTREGA.firstOrNull { t.endsWith(it, ignoreCase = true) }
        return (if (sufijo != null) t.dropLast(sufijo.length) else t).trim().ifBlank { null }
    }

    private fun aEvento(campos: Map<String, Pair<Map<String, String>, String>>, zona: ZoneId): EventoAula? {
        val uid = campos["UID"]?.second?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val titulo = campos["SUMMARY"]?.second?.let { limpiarTitulo(desescapar(it)) } ?: return null
        val (params, valor) = campos["DTEND"] ?: campos["DTSTART"] ?: return null
        val fecha = fecha(valor, params, zona) ?: return null
        val curso = campos["CATEGORIES"]?.second?.let { desescapar(it).split(',').first().trim() }?.ifBlank { null }
        val descripcion = campos["DESCRIPTION"]?.second?.let { desescapar(it).trim() }.orEmpty().take(2000)
        return EventoAula(uid.take(100), titulo.take(200), fecha, curso?.take(120), descripcion)
    }
}

/** Cambio de fecha detectado en una entrega que ya estaba en la app. */
data class CambioFechaAula(val idExterno: String, val titulo: String, val fechaAnterior: LocalDate?, val fechaNueva: LocalDate)

/** Qué hacer con lo que llegó del calendario. */
data class PlanSincronizacionAula(
    val nuevos: List<EventoAula>,
    val cambiosDeFecha: List<CambioFechaAula>,
    val sinCambios: Int,
    val pasados: Int,
) {
    companion object {
        /**
         * Compara los eventos con lo que ya hay en la app.
         * - Nuevos: su idExterno no existe y vencen hoy o después (no se importan entregas ya vencidas).
         * - Cambios de fecha: el docente movió la fecha; se proponen al estudiante, no se aplican solos.
         * @param existentes idExterno → fecha límite actual de las actividades ya importadas.
         */
        fun calcular(eventos: List<EventoAula>, existentes: Map<String, LocalDate?>, hoy: LocalDate): PlanSincronizacionAula {
            val nuevos = mutableListOf<EventoAula>()
            val cambios = mutableListOf<CambioFechaAula>()
            var iguales = 0
            var pasados = 0
            for (e in eventos) {
                if (existentes.containsKey(e.idExterno)) {
                    val anterior = existentes[e.idExterno]
                    if (anterior != e.fecha) cambios += CambioFechaAula(e.idExterno, e.titulo, anterior, e.fecha) else iguales++
                } else if (e.fecha.isBefore(hoy)) {
                    pasados++
                } else {
                    nuevos += e
                }
            }
            return PlanSincronizacionAula(nuevos, cambios, iguales, pasados)
        }
    }
}
