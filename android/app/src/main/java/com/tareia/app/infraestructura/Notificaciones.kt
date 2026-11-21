package com.tareia.app.infraestructura

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.tareia.app.MainActivity
import com.tareia.app.ReminderReceiver
import com.tareia.app.dominio.ActividadAcademica
import com.tareia.app.dominio.Recordatorio
import java.time.ZoneId

/**
 * Puerto ProgramadorNotificaciones: programa y cancela los avisos de los recordatorios
 * (RF-09). GestorAlertas solo conoce esta interfaz.
 */
interface ProgramadorNotificaciones {
    fun programar(r: Recordatorio)
    fun cancelar(r: Recordatorio)
}

/**
 * «Adapter» Implementación con AlarmManager de Android.
 *
 * Cambio respecto al diagrama (WorkManagerNotificaciones): WorkManager sirve para trabajos
 * en segundo plano que pueden retrasarse; un recordatorio debe sonar a una HORA EXACTA, y
 * para eso Android recomienda AlarmManager, que es lo que la app ya usaba.
 */
class AlarmManagerNotificaciones(private val context: Context) : ProgramadorNotificaciones {

    override fun programar(r: Recordatorio) {
        cancelar(r)
        if (!r.activo) return
        val momento = r.fechaHora.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (momento <= System.currentTimeMillis()) return
        val alarma = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarma.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, momento, intentPendiente(r))
    }

    override fun cancelar(r: Recordatorio) {
        val alarma = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarma.cancel(intentPendiente(r))
    }

    private fun intentPendiente(r: Recordatorio): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACCION_RECORDATORIO
            putExtra(EXTRA_ACTIVIDAD, r.idActividad)
        }
        return PendingIntent.getBroadcast(context, r.id.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    companion object {
        const val ACCION_RECORDATORIO = "com.tareia.app.ACTION_TASK_REMINDER"
        const val EXTRA_ACTIVIDAD = "task_id"
        private const val CANAL = "tareia_due_dates"

        /** Muestra la notificación de "entrega próxima" (la llama ReminderReceiver). */
        fun mostrar(context: Context, a: ActividadAcademica) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= 26) {
                manager.createNotificationChannel(NotificationChannel(CANAL, "Recordatorios académicos", NotificationManager.IMPORTANCE_DEFAULT))
            }
            val abrir = PendingIntent.getActivity(context, a.id.hashCode(), Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val notificacion = NotificationCompat.Builder(context, CANAL)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Entrega próxima: ${a.titulo}")
                .setContentText("${a.curso?.nombre ?: "Actividad académica"} · ${a.fechaLimite ?: ""}")
                .setContentIntent(abrir).setAutoCancel(true).build()
            manager.notify(a.id.hashCode(), notificacion)
        }
    }
}
