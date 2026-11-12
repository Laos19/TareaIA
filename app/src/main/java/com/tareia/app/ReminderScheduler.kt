package com.tareia.app

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.time.LocalDate
import java.time.ZoneId

object ReminderScheduler {
    const val ACTION_REMINDER = "com.tareia.app.ACTION_TASK_REMINDER"
    private const val CHANNEL = "tareia_due_dates"

    fun schedule(context: Context, task: TaskItem) {
        cancel(context, task.id)
        if (!task.reminderEnabled || task.status == "Completada" || task.dueDate.isBlank()) return
        val reminder = runCatching { LocalDate.parse(task.dueDate).minusDays(1).atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() }.getOrNull() ?: return
        if (reminder <= System.currentTimeMillis()) return
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder, pendingIntent(context, task.id))
    }

    fun cancel(context: Context, taskId: String) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.cancel(pendingIntent(context, taskId))
    }

    private fun pendingIntent(context: Context, taskId: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply { action = ACTION_REMINDER; putExtra("task_id", taskId) }
        return PendingIntent.getBroadcast(context, taskId.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun show(context: Context, task: TaskItem) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(NotificationChannel(CHANNEL, "Recordatorios académicos", NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(context, task.id.hashCode(), Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Entrega próxima: ${task.title}")
            .setContentText("${task.course.ifBlank { "Actividad académica" }} · ${task.dueDate}")
            .setContentIntent(open).setAutoCancel(true).build()
        manager.notify(task.id.hashCode(), notification)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ReminderScheduler.ACTION_REMINDER) return
        val taskId = intent.getStringExtra("task_id") ?: return
        val task = AppStore.load(context).tasks.firstOrNull { it.id == taskId } ?: return
        if (task.status != "Completada") ReminderScheduler.show(context, task)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) AppStore.load(context).tasks.forEach { ReminderScheduler.schedule(context, it) }
    }
}
