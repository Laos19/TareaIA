package com.tareia.app.infraestructura

import com.tareia.app.DatosEvento
import com.tareia.app.GoogleCalendarApi
import com.tareia.app.dominio.ActividadAcademica

/**
 * Puerto CalendarioGateway: lo que la app necesita de un calendario externo (RF-08).
 * GestorSincronizacion solo conoce esta interfaz, no a Google.
 */
interface CalendarioGateway {
    /** Correo de la cuenta del calendario (para no tocar eventos de otra cuenta). */
    val cuenta: String

    /** Crea el evento de la actividad y devuelve su id en el calendario. */
    suspend fun crearEvento(a: ActividadAcademica): String
    suspend fun actualizarEvento(idExterno: String, a: ActividadAcademica)
    suspend fun eliminarEvento(idExterno: String, a: ActividadAcademica)

    /** Agregado: enlace web del evento, para abrirlo después de sincronizar. */
    suspend fun enlaceEvento(idExterno: String, a: ActividadAcademica): String
}

/**
 * «Adapter» (patrón estructural, de la próxima unidad): adapta la API REST de Google
 * Calendar (GoogleCalendarApi) a la interfaz CalendarioGateway que espera la app.
 *
 * Recibe el token OAuth y el uid de Firebase del usuario: se crea uno nuevo cada vez que
 * el estudiante autoriza el acceso al calendario.
 */
class GoogleCalendarAdapter(
    private val token: String,
    private val uid: String,
    override val cuenta: String
) : CalendarioGateway {

    override suspend fun crearEvento(a: ActividadAcademica): String =
        GoogleCalendarApi.upsert(token, uid, datos(a, idEventoGoogle = ""))

    override suspend fun actualizarEvento(idExterno: String, a: ActividadAcademica) {
        GoogleCalendarApi.upsert(token, uid, datos(a, idEventoGoogle = idExterno))
    }

    override suspend fun eliminarEvento(idExterno: String, a: ActividadAcademica) =
        GoogleCalendarApi.delete(token, uid, a.id, idExterno)

    override suspend fun enlaceEvento(idExterno: String, a: ActividadAcademica): String =
        GoogleCalendarApi.getEventWebLink(token = token, ownerUid = uid, taskId = a.id, eventId = idExterno)

    /** Traduce la actividad del dominio a los datos que pide la API de Google. */
    private fun datos(a: ActividadAcademica, idEventoGoogle: String): DatosEvento {
        val fecha = requireNotNull(a.fechaLimite) { "La actividad no tiene fecha límite." }.valor
        return DatosEvento(
            idTarea = a.id,
            idEventoGoogle = idEventoGoogle,
            titulo = a.titulo,
            curso = a.curso?.nombre.orEmpty(),
            descripcion = a.descripcion,
            origen = a.origen.name,
            fecha = fecha,
            conRecordatorio = a.recordatorios.any { it.activo }
        )
    }
}
