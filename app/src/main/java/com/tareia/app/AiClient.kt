package com.tareia.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.net.URL
import javax.net.ssl.HttpsURLConnection

data class AiConfig(val endpoint: String, val model: String, val apiKey: String)

object AiSettings {
    private const val PREF = "tareia_ai"
    fun load(context: Context) = AiConfig(
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString("endpoint", "").orEmpty(),
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString("model", "").orEmpty(),
        SecureTokenStore.readProtected(context, PREF, "api_key")
    )
    fun save(context: Context, endpoint: String, model: String, apiKey: String) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString("endpoint", endpoint.trim()).putString("model", model.trim()).apply()
        if (apiKey.isNotBlank()) SecureTokenStore.writeProtected(context, PREF, "api_key", apiKey.trim())
    }
    fun clearKey(context: Context) = SecureTokenStore.clearProtected(context, PREF, "api_key")
}

/** Optional OpenAI-compatible endpoint; content is only sent when the user explicitly opts in. */
object AiClient {
    fun extractTasks(config: AiConfig, text: String, courseNames: List<String>): List<TaskDraft> {
        val prompt = "Texto académico OCR:\n${text.take(24_000)}\n\nCursos disponibles: ${courseNames.joinToString()}\nDevuelve JSON válido exacto: {\"items\":[{\"title\":string,\"course\":string,\"dueDate\":\"YYYY-MM-DD\" o cadena vacía}]. Extrae solo actividades explícitas. No inventes curso ni fecha; si falta, déjalo vacío. Máximo 10 actividades."
        val raw = complete(config, "Eres un extractor académico preciso. Responde solo JSON, sin bloques Markdown.", prompt)
        val json = JSONObject(stripJson(raw))
        val items = json.optJSONArray("items") ?: JSONArray()
        return (0 until items.length()).mapNotNull { i ->
            val item = items.optJSONObject(i) ?: return@mapNotNull null
            val title = item.optString("title").trim()
            if (title.isBlank()) return@mapNotNull null
            val date = item.optString("dueDate").trim().takeIf { runCatching { java.time.LocalDate.parse(it) }.isSuccess }.orEmpty()
            TaskDraft(title, item.optString("course").trim(), date, "IA", text.take(1200))
        }.take(10)
    }

    fun plan(config: AiConfig, task: TaskItem, course: CourseItem?, schedule: List<StudyBlock>, count: Int): List<StudySession> {
        val prompt = JSONObject().apply {
            put("task", JSONObject().put("title", task.title).put("course", task.course).put("dueDate", task.dueDate))
            put("coursePriority", course?.priority ?: 3); put("difficulty", course?.difficulty ?: 3)
            put("availableBlocks", JSONArray().apply { schedule.forEach { put(JSONObject().put("dayOfWeek", it.dayOfWeek).put("startMinute", it.startMinute).put("endMinute", it.endMinute)) } })
            put("sessionsRequested", count); put("currentDate", java.time.LocalDate.now().toString())
        }.toString()
        val raw = complete(config, "Planifica estudio académico respetando estrictamente fecha límite y bloques libres. No generes sesiones en el pasado ni fuera de los bloques disponibles. Responde solo JSON: {\"sessions\":[{\"title\":string,\"date\":\"YYYY-MM-DD\",\"startMinute\":integer,\"durationMinutes\":integer}]}.", prompt)
        val array = JSONObject(stripJson(raw)).optJSONArray("sessions") ?: JSONArray()
        return (0 until array.length()).mapNotNull { i ->
            val item = array.optJSONObject(i) ?: return@mapNotNull null
            val date = item.optString("date")
            val parsed = runCatching { java.time.LocalDate.parse(date) }.getOrNull() ?: return@mapNotNull null
            if (parsed.isBefore(java.time.LocalDate.now()) || parsed.isAfter(java.time.LocalDate.parse(task.dueDate))) return@mapNotNull null
            val start = item.optInt("startMinute", -1)
            val duration = item.optInt("durationMinutes", 60).coerceIn(25, 180)
            if (start !in 0..1439) return@mapNotNull null
            if (schedule.isNotEmpty() && schedule.none { it.dayOfWeek == parsed.dayOfWeek.value && start >= it.startMinute && start + duration <= it.endMinute }) return@mapNotNull null
            StudySession(taskId = task.id, title = item.optString("title", task.title), date = date, startMinute = start, durationMinutes = duration)
        }.take(count)
    }

    private fun complete(config: AiConfig, system: String, user: String): String {
        require(config.apiKey.isNotBlank() && config.model.isNotBlank()) { "Configura proveedor, modelo y API key en Más > IA." }
        val uri = URI(config.endpoint.trim())
        require(uri.scheme.equals("https", true) && uri.userInfo == null) { "El endpoint de IA debe usar HTTPS y no incluir credenciales en la URL." }
        val root = config.endpoint.trimEnd('/')
        val endpoint = when {
            root.endsWith("/chat/completions") -> root
            root.endsWith("/v1") -> "$root/chat/completions"
            else -> "$root/v1/chat/completions"
        }
        val payload = JSONObject().apply {
            put("model", config.model)
            put("temperature", 0.1)
            put("messages", JSONArray().put(JSONObject().put("role", "system").put("content", system)).put(JSONObject().put("role", "user").put("content", user)))
        }.toString()
        val connection = (URL(endpoint).openConnection() as? HttpsURLConnection) ?: throw IllegalStateException("El endpoint de IA debe usar HTTPS.")
        connection.apply {
            requestMethod = "POST"; connectTimeout = 15000; readTimeout = 60000; doOutput = true; instanceFollowRedirects = false
            setRequestProperty("Authorization", "Bearer ${config.apiKey}")
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("Accept", "application/json")
        }
        try {
            connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) throw IllegalStateException("El proveedor de IA respondió HTTP $status.")
            return JSONObject(body).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
        } finally { connection.disconnect() }
    }

    private fun stripJson(value: String): String = value.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
}
