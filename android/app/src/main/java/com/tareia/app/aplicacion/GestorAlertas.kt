package com.tareia.app.aplicacion

import com.tareia.app.dominio.ActividadAcademica
import com.tareia.app.dominio.Recordatorio
import com.tareia.app.infraestructura.ProgramadorNotificaciones
import java.time.LocalTime

/**
 * GestorAlertas (RF-09): crea, reprograma y cancela los recordatorios de las actividades.
 * Por defecto: un aviso el día anterior a la entrega a las 9:00.
 * Solo conoce la interfaz ProgramadorNotificaciones (no sabe que por dentro es AlarmManager).
 */
class GestorAlertas(private val programador: ProgramadorNotificaciones) {

    /** Reemplaza los recordatorios de la actividad por el recordatorio por defecto. */
    fun programarPorDefecto(a: ActividadAcademica) {
        cancelar(a)
        a.recordatorios.clear()
        val fecha = a.fechaLimite ?: return
        if (a.estaCompletada()) return
        val r = Recordatorio(idActividad = a.id, fechaHora = fecha.valor.minusDays(1).atTime(LocalTime.of(9, 0)))
        a.recordatorios += r
        programador.programar(r)
    }

    /** Cuando cambia la fecha: se recalcula el recordatorio (regla "RF21" del SRS). */
    fun reprogramar(a: ActividadAcademica) = programarPorDefecto(a)

    /** Cancela los avisos programados (al completar o enviar a la papelera). */
    fun cancelar(a: ActividadAcademica) {
        a.recordatorios.forEach { programador.cancelar(it) }
    }

    /** Vuelve a programar todos los avisos (después de reiniciar el celular). */
    fun reprogramarTodas(actividades: List<ActividadAcademica>) {
        actividades.filter { !it.estaCompletada() }.forEach { a -> a.recordatorios.forEach { programador.programar(it) } }
    }
}
