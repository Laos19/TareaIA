package com.tareia.app

import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import java.net.URI
import java.time.Instant
import java.time.ZoneId
import javax.net.ssl.HttpsURLConnection

/** Minimal read-only Moodle REST client. It never sends a Moodle password. */
object MoodleClient {
    fun fetchAssignments(baseUrl: String, token: String): List<TaskItem> {
        val parsedBase = runCatching { URI(baseUrl.trim()) }.getOrElse { throw IllegalArgumentException("La URL de Moodle no es válida.") }
        require(parsedBase.scheme.equals("https", ignoreCase = true)) { "La instancia debe utilizar HTTPS." }
        require(parsedBase.userInfo == null && parsedBase.query == null && parsedBase.fragment == null) { "La URL no debe incluir usuario, contraseña, parámetros ni fragmentos." }
        require(token.isNotBlank()) { "Ingresa un token web service autorizado." }
        val base = baseUrl.trimEnd('/')
        val endpoint = if (base.endsWith("/webservice/rest/server.php")) base else "$base/webservice/rest/server.php"
        val site = call(endpoint, token, "core_webservice_get_site_info", emptyMap())
        if (site.has("exception")) throw IllegalStateException(site.optString("message", "Token rechazado por Moodle"))
        val userId = site.optLong("userid", -1)
        require(userId > 0) { "Moodle no devolvió un usuario válido." }
        val courseParams = mapOf("userid" to userId.toString())
        val coursesJson = call(endpoint, token, "core_enrol_get_users_courses", courseParams)
        if (coursesJson.has("exception")) throw IllegalStateException(coursesJson.optString("message", "No se pudieron consultar los cursos"))
        val courses = coursesJson.optJSONArray("courses") ?: org.json.JSONArray()
        val results = mutableListOf<TaskItem>()
        for (i in 0 until courses.length()) {
            val course = courses.optJSONObject(i) ?: continue
            val courseId = course.optLong("id", -1)
            if (courseId <= 0) continue
            val courseName = course.optString("fullname", course.optString("shortname", "Curso Moodle"))
            val assignmentParams = mapOf("courseids[0]" to courseId.toString())
            val assignmentsJson = call(endpoint, token, "mod_assign_get_assignments", assignmentParams)
            if (assignmentsJson.has("exception")) throw IllegalStateException(assignmentsJson.optString("message", "Moodle no permitió consultar tareas"))
            val assignmentCourses = assignmentsJson.optJSONArray("courses") ?: org.json.JSONArray()
            for (j in 0 until assignmentCourses.length()) {
                val assignmentCourse = assignmentCourses.optJSONObject(j) ?: continue
                val assignments = assignmentCourse.optJSONArray("assignments") ?: continue
                for (k in 0 until assignments.length()) {
                    val assignment = assignments.optJSONObject(k) ?: continue
                    val id = assignment.optLong("id", -1)
                    if (id <= 0) continue
                    val dueEpoch = assignment.optLong("duedate", 0)
                    val due = if (dueEpoch > 0) Instant.ofEpochSecond(dueEpoch).atZone(ZoneId.systemDefault()).toLocalDate().toString() else ""
                    results += TaskItem(
                        id = "moodle:$courseId:$id", title = assignment.optString("name", "Actividad Moodle"),
                        course = assignmentCourse.optString("fullname", courseName), dueDate = due,
                        status = "Pendiente", source = "Moodle", externalId = "moodle:$courseId:$id"
                    )
                }
            }
        }
        return results
    }

    private fun call(endpoint: String, token: String, function: String, params: Map<String, String>): JSONObject {
        val values = linkedMapOf(
            "wstoken" to token,
            "wsfunction" to function,
            "moodlewsrestformat" to "json"
        ).apply { putAll(params) }
        val body = values.entries.joinToString("&") { (key, value) -> encode(key) + "=" + encode(value) }
        val connection = (URL(endpoint).openConnection() as? HttpsURLConnection)
            ?: throw IllegalStateException("La conexión a Moodle debe usar HTTPS.")
        connection.apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 20000
            doOutput = true
            instanceFollowRedirects = false
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            setRequestProperty("Accept", "application/json")
        }
        try {
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) throw IllegalStateException("Moodle respondió HTTP $status")
            val parsed = org.json.JSONTokener(response).nextValue()
            return when (parsed) {
                is JSONObject -> parsed
                is org.json.JSONArray -> JSONObject().put("courses", parsed)
                else -> throw IllegalStateException("Moodle devolvió una respuesta JSON inesperada.")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun encode(value: String) = URLEncoder.encode(value, "UTF-8")
}
