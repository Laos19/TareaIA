package com.tareia.app

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.time.LocalDate
import java.time.ZoneId

data class DeviceCalendar(val id: Long, val name: String, val account: String, val writable: Boolean)

object CalendarRepository {
    fun hasPermissions(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED

    fun calendars(context: Context): List<DeviceCalendar> {
        check(hasPermissions(context)) { "Concede permiso de lectura y escritura del calendario." }
        val projection = arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME, CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL)
        val result = mutableListOf<DeviceCalendar>()
        context.contentResolver.query(CalendarContract.Calendars.CONTENT_URI, projection, null, null, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
            val nameCol = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
            val accountCol = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
            val accessCol = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL)
            while (cursor.moveToNext()) {
                val access = cursor.getInt(accessCol)
                result += DeviceCalendar(cursor.getLong(idCol), cursor.getString(nameCol).orEmpty(), cursor.getString(accountCol).orEmpty(), access >= CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR)
            }
        }
        return result.filter { it.writable }
    }

    fun upsert(context: Context, task: TaskItem, calendarId: Long): Long {
        check(hasPermissions(context)) { "Concede permiso para crear eventos en el calendario." }
        val due = LocalDate.parse(task.dueDate).atTime(9, 0).atZone(ZoneId.systemDefault())
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, task.title)
            put(CalendarContract.Events.DESCRIPTION, listOf(task.course, task.description, "Origen: ${task.source}").filter { it.isNotBlank() }.joinToString("\n"))
            put(CalendarContract.Events.DTSTART, due.toInstant().toEpochMilli())
            put(CalendarContract.Events.DTEND, due.plusMinutes(30).toInstant().toEpochMilli())
            put(CalendarContract.Events.EVENT_TIMEZONE, ZoneId.systemDefault().id)
            put(CalendarContract.Events.HAS_ALARM, if (task.reminderEnabled) 1 else 0)
        }
        val eventUri = task.calendarEventId?.let { id ->
            val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id)
            val updated = context.contentResolver.update(uri, values, null, null)
            if (updated > 0) uri else context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
        } ?: context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
        val eventId = eventUri?.lastPathSegment?.toLongOrNull() ?: throw IllegalStateException("No se pudo guardar el evento.")
        if (task.reminderEnabled) addProviderReminder(context, eventId)
        return eventId
    }

    fun delete(context: Context, eventId: Long) {
        if (hasPermissions(context)) context.contentResolver.delete(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId), null, null)
    }

    private fun addProviderReminder(context: Context, eventId: Long) {
        runCatching {
            context.contentResolver.query(CalendarContract.Reminders.CONTENT_URI,
                arrayOf(CalendarContract.Reminders._ID),
                "${CalendarContract.Reminders.EVENT_ID}=? AND ${CalendarContract.Reminders.MINUTES}=? AND ${CalendarContract.Reminders.METHOD}=?",
                arrayOf(eventId.toString(), (24 * 60).toString(), CalendarContract.Reminders.METHOD_ALERT.toString()), null)?.use { if (it.moveToFirst()) return }
            val values = ContentValues().apply {
                put(CalendarContract.Reminders.EVENT_ID, eventId)
                put(CalendarContract.Reminders.MINUTES, 24 * 60)
                put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
            }
            context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, values)
        }
    }
}
