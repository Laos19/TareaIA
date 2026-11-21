package com.tareia.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.security.MessageDigest
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * Datos que necesita Google Calendar para crear o actualizar un evento.
 * (Antes la API recibía el TaskItem de la pantalla; ahora el GoogleCalendarAdapter
 * arma estos datos a partir de la ActividadAcademica del dominio.)
 */
internal data class DatosEvento(
    val idTarea: String,
    val idEventoGoogle: String,
    val titulo: String,
    val curso: String,
    val descripcion: String,
    val origen: String,
    val fecha: LocalDate,
    val conRecordatorio: Boolean
)

/** Integración REAL con Google Calendar REST v3: calendario principal del usuario OAuth.
 * No usa CalendarContract ni permisos READ_CALENDAR/WRITE_CALENDAR.
 * La usa GoogleCalendarAdapter (patrón Adapter: traduce esta API a CalendarioGateway).
 */
internal object GoogleCalendarApi {
    private const val BASE = "https://www.googleapis.com/calendar/v3/calendars/primary/events"
    private const val OWNER = "tareiaOwnerUid"
    private const val TASK = "tareiaTaskId"
    private val jsonType = "application/json; charset=utf-8".toMediaType()
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(18, TimeUnit.SECONDS)
        .build()

    private class CalendarHttpError(val status: Int, message: String) : IOException(message)

    /** Comprueba la conexión usando el mismo permiso calendar.events ya concedido. */
    suspend fun verify(token: String): Unit = withContext(Dispatchers.IO) {
        request("GET", "$BASE?maxResults=1", token)
        Unit
    }

    /** ID determinista por usuario + tarea: evita crear duplicados al reintentar un POST. */
    fun stableEventId(uid: String, taskId: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("tareia:v1:$uid:$taskId".toByteArray(Charsets.UTF_8))
        return "tareia" + digest.take(24).joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }

    suspend fun upsert(token: String, ownerUid: String, task: DatosEvento): String = withContext(Dispatchers.IO) {
        if (task.idEventoGoogle.isNotBlank() && task.idEventoGoogle != stableEventId(ownerUid, task.idTarea)) {
            throw IllegalStateException("Este evento pertenece a otra vinculación. No se sobrescribió.")
        }
        val eventId = stableEventId(ownerUid, task.idTarea)
        val remote = eventUrl(eventId)
        val existing = try {
            request("GET", remote, token)
        } catch (e: CalendarHttpError) {
            if (e.status == 404) null else throw e
        }
        val body = eventJson(task, ownerUid, eventId).toString()
        val updateBody = eventJson(task, ownerUid, eventId).apply { remove("id") }.toString()
        if (existing != null) {
            assertOwned(existing, ownerUid, task.idTarea)
            request("PATCH", remote, token, updateBody)
        } else {
            try {
                request("POST", BASE, token, body)
            } catch (e: CalendarHttpError) {
                if (e.status != 409) throw e
                // Otra petición ya creó el evento: comprobar propiedad y actualizar.
                val current = request("GET", remote, token)
                assertOwned(current, ownerUid, task.idTarea)
                request("PATCH", remote, token, updateBody)
            }
        }
        eventId
    }


    suspend fun getEventWebLink(
        token: String,
        ownerUid: String,
        taskId: String,
        eventId: String
    ): String = withContext(Dispatchers.IO) {

        require(eventId == stableEventId(ownerUid, taskId)) {
            "El evento no corresponde a esta cuenta."
        }

        val evento = request(
            "GET",
            eventUrl(eventId),
            token
        )

        assertOwned(evento, ownerUid, taskId)

        val enlace = evento.optString("htmlLink")

        require(enlace.startsWith("https://")) {
            "Google Calendar no devolvió un enlace válido."
        }

        enlace
    }

    suspend fun delete(token: String, ownerUid: String, taskId: String, googleEventId: String): Unit = withContext(Dispatchers.IO) {
        if (googleEventId.isBlank()) return@withContext
        require(googleEventId == stableEventId(ownerUid, taskId)) { "El vínculo de Google Calendar no coincide con la cuenta." }
        val url = eventUrl(googleEventId)
        val existing = try { request("GET", url, token) }
            catch (e: CalendarHttpError) { if (e.status == 404) return@withContext else throw e }
        assertOwned(existing, ownerUid, taskId)
        try { request("DELETE", url, token) }
        catch (e: CalendarHttpError) { if (e.status != 404) throw e }
    }

    private fun eventUrl(eventId: String) = "$BASE/$eventId"

    private fun ownerFingerprint(uid: String) = MessageDigest.getInstance("SHA-256")
        .digest(uid.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private fun assertOwned(remote: JSONObject, uid: String, taskId: String) {
        val properties = remote.optJSONObject("extendedProperties")?.optJSONObject("private")
        require(properties?.optString(OWNER) == ownerFingerprint(uid) && properties?.optString(TASK) == taskId) {
            "El evento ya existente no pertenece a esta tarea de TARE·IA."
        }
    }

    private fun eventJson(task: DatosEvento, uid: String, eventId: String): JSONObject {
        val zone = ZoneId.systemDefault()
        val start = task.fecha.atTime(9, 0).atZone(zone)
        val end = start.plusMinutes(30)
        fun dateTime(value: java.time.ZonedDateTime) = JSONObject()
            .put("dateTime", value.toOffsetDateTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
            .put("timeZone", zone.id)
        val overrides = JSONArray()
        if (task.conRecordatorio) overrides.put(JSONObject().put("method", "popup").put("minutes", 1440))
        return JSONObject()
            .put("id", eventId)
            .put("summary", task.titulo)
            .put("description", listOf(task.curso, task.descripcion, "Origen: ${task.origen}")
                .filter { it.isNotBlank() }.joinToString("\n"))
            .put("start", dateTime(start))
            .put("end", dateTime(end))
            .put("reminders", JSONObject().put("useDefault", false).put("overrides", overrides))
            .put("extendedProperties", JSONObject().put("private", JSONObject()
                .put(OWNER, ownerFingerprint(uid)).put(TASK, task.idTarea)))
    }

    private fun request(method: String, url: String, token: String, body: String? = null): JSONObject {
        val builder = Request.Builder().url(url)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
        val requestBody = body?.toRequestBody(jsonType)
        when (method) {
            "GET" -> builder.get()
            "POST" -> builder.post(requireNotNull(requestBody))
            "PATCH" -> builder.patch(requireNotNull(requestBody))
            "DELETE" -> builder.delete()
            else -> error("Operación HTTP no permitida: $method")
        }
        client.newCall(builder.build()).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val reason = when (response.code) {
                    401 -> "La sesión de Calendar caducó. Vuelve a conectar tu cuenta."
                    403 -> "Google Calendar rechazó el permiso o la cuenta no tiene acceso."
                    404 -> "No existe el evento en Google Calendar."
                    409 -> "El evento ya existe en Google Calendar."
                    else -> "Google Calendar devolvió un error HTTP ${response.code}."
                }
                throw CalendarHttpError(response.code, reason)
            }
            return if (text.isBlank()) JSONObject() else JSONObject(text)
        }
    }
}
