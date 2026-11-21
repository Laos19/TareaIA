package com.tareia.app.remoto

import org.json.JSONObject
import java.time.Instant

/**
 * Cola en memoria de eventos de uso y logs pendientes de enviar (código puro, probado en la JVM).
 *
 * - Agrupa los envíos en lotes para gastar menos batería y datos.
 * - Si un envío falla, los elementos vuelven a la cola (hasta [capacidad]; los más antiguos se descartan).
 * - Nunca guarda datos personales: solo tipo, pantalla y métricas simples.
 */
class ColaTelemetria(private val capacidad: Int = 200, private val tamanoLote: Int = 20) {
    private val pendientes = ArrayDeque<JSONObject>()

    /** Cantidad de elementos esperando envío. */
    val tamano: Int @Synchronized get() = pendientes.size

    /** Agrega un elemento; si la cola está llena descarta el más antiguo. Devuelve true si ya hay un lote completo. */
    @Synchronized
    fun agregar(elemento: JSONObject): Boolean {
        if (pendientes.size >= capacidad) pendientes.removeFirst()
        pendientes.addLast(elemento)
        return pendientes.size >= tamanoLote
    }

    /** Saca hasta [tamanoLote] elementos para enviarlos. */
    @Synchronized
    fun tomarLote(): List<JSONObject> {
        val lote = mutableListOf<JSONObject>()
        while (lote.size < tamanoLote && pendientes.isNotEmpty()) lote += pendientes.removeFirst()
        return lote
    }

    /** Devuelve un lote que no se pudo enviar al frente de la cola (respetando la capacidad). */
    @Synchronized
    fun devolver(lote: List<JSONObject>) {
        lote.asReversed().forEach { if (pendientes.size < capacidad) pendientes.addFirst(it) }
    }

    companion object {
        /** Evento de uso con fecha del dispositivo. */
        fun evento(tipo: String, pantalla: String? = null, datos: Map<String, Any> = emptyMap(), ahora: Instant = Instant.now()): JSONObject =
            JSONObject()
                .put("tipo", tipo)
                .put("pantalla", pantalla ?: JSONObject.NULL)
                .put("datos", JSONObject(datos))
                .put("creadoEn", ahora.toString())

        /** Línea de log (solo WARN o ERROR llegan al servidor). */
        fun log(nivel: String, etiqueta: String, mensaje: String, versionApp: String): JSONObject =
            JSONObject()
                .put("nivel", nivel)
                .put("etiqueta", etiqueta.take(60))
                .put("mensaje", mensaje.take(2000))
                .put("versionApp", versionApp)
    }
}
