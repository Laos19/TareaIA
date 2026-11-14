package com.tareia.app.aplicacion

import com.tareia.app.dominio.ActividadAcademica
import com.tareia.app.dominio.ActividadRepository
import com.tareia.app.dominio.EventoCalendario
import com.tareia.app.infraestructura.CalendarioGateway
import java.time.LocalDateTime

/**
 * GestorSincronizacion (RF-08): envía las actividades a Google Calendar.
 *
 * - Solo conoce la interfaz CalendarioGateway (no sabe que por dentro es Google).
 * - Usa el patrón STATE: a.sincronizar() rechaza las actividades pendientes de validar (RN01)
 *   y pasa las confirmadas a "Sincronizada".
 * - Se crea un gestor nuevo cada vez que el estudiante autoriza su calendario, porque el
 *   GoogleCalendarAdapter necesita el token OAuth de ese momento.
 */
class GestorSincronizacion(
    private val calendario: CalendarioGateway,
    private val actividades: ActividadRepository
) {
    /** Crea o actualiza el evento de la actividad. Devuelve el id del evento. */
    suspend fun sincronizar(a: ActividadAcademica): String {
        val evento = a.evento
        check(evento == null || evento.cuenta.equals(calendario.cuenta, ignoreCase = true)) {
            "Esta tarea está vinculada al calendario de otra cuenta. No se sobrescribirá."
        }
        val estadoAnterior = a.estado
        a.sincronizar()      // STATE: si está pendiente de validar lanza error (RN01)
        try {
            val idEvento = if (evento == null) calendario.crearEvento(a)
                else { calendario.actualizarEvento(evento.idExterno, a); evento.idExterno }
            a.vincularEvento(EventoCalendario(idEvento, LocalDateTime.now(), calendario.cuenta))
            actividades.guardar(a)
            return idEvento
        } catch (e: Exception) {
            a.cambiarEstado(estadoAnterior)   // si Google falla, la actividad vuelve a su estado anterior
            throw e
        }
    }

    /** Borra el evento de Google (antes de enviar la actividad a la papelera). */
    suspend fun eliminarEvento(a: ActividadAcademica) {
        val evento = a.evento ?: return
        check(evento.cuenta.equals(calendario.cuenta, ignoreCase = true)) {
            "No puedes eliminar un evento vinculado a otra cuenta Google."
        }
        calendario.eliminarEvento(evento.idExterno, a)
        a.vincularEvento(null)
        actividades.guardar(a)
    }

    /** Enlace para abrir el evento en Google Calendar. */
    suspend fun enlace(a: ActividadAcademica): String {
        val evento = requireNotNull(a.evento) { "La actividad todavía no está en Google Calendar." }
        return calendario.enlaceEvento(evento.idExterno, a)
    }
}
