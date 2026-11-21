package com.tareia.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tareia.app.aplicacion.Dependencias
import com.tareia.app.infraestructura.AlarmManagerNotificaciones

// Receptores de Android para los recordatorios (RF-09). Conservan el nombre y el paquete
// que ya tenían porque están declarados en AndroidManifest.xml.

/** Cuando llega la hora de un recordatorio, muestra la notificación de la actividad. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmManagerNotificaciones.ACCION_RECORDATORIO) return
        val id = intent.getStringExtra(AlarmManagerNotificaciones.EXTRA_ACTIVIDAD) ?: return
        val actividad = Dependencias(context).gestorActividades.buscar(id) ?: return
        if (!actividad.estaCompletada() && !actividad.estaEnPapelera()) AlarmManagerNotificaciones.mostrar(context, actividad)
    }
}

/** Al reiniciar el celular, Android borra las alarmas: se vuelven a programar todas. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val dep = Dependencias(context)
        dep.gestorAlertas.reprogramarTodas(dep.gestorActividades.listar())
    }
}
