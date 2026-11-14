package com.tareia.app

import android.content.Context
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Almacenamiento del PERFIL de acceso (nombre, correo, PIN). La autenticación quedó fuera
 * del diagrama de clases, por eso este archivo sigue igual que antes en lo esencial.
 *
 * Antes AppStore guardaba TODOS los datos de la app en un solo JSON. Ahora los datos
 * académicos los guardan los repositorios (patrón Repository) en AppDatabase (Singleton).
 * cargarDatosAnteriores() solo se usa una vez, para migrar los datos de versiones previas.
 */
object AppStore {
    private const val PREF = "tareia_local_data"
    private const val KEY = "snapshot_v2"

    private fun raiz(context: Context): JSONObject = runCatching {
        JSONObject(SecureTokenStore.readProtected(context, PREF, KEY).ifBlank { "{}" })
    }.getOrDefault(JSONObject())

    fun cargarPerfil(context: Context): UserProfile =
        raiz(context).optJSONObject("profile")?.toProfile() ?: UserProfile()

    /** Guarda el perfil conservando el resto del archivo (por si faltara migrar datos antiguos). */
    fun guardarPerfil(context: Context, perfil: UserProfile) {
        val root = raiz(context).put("profile", perfil.toJson())
        SecureTokenStore.writeProtected(context, PREF, KEY, root.toString())
    }

    /** Datos guardados por la versión anterior de la app (solo para la migración). */
    fun cargarDatosAnteriores(context: Context): AppSnapshot = runCatching {
        val root = raiz(context)
        AppSnapshot(
            profile = root.optJSONObject("profile")?.toProfile() ?: UserProfile(),
            tasks = root.optJSONArray("tasks").toListOf { it.toTask() },
            courses = root.optJSONArray("courses").toListOf { it.toCourse() },
            schedule = root.optJSONArray("schedule").toListOf { it.toBlock() },
            sessions = root.optJSONArray("sessions").toListOf { it.toSession() },
            notes = root.optJSONArray("notes").toListOf { it.toNote() },
            folders = root.optJSONArray("folders").toStringList().ifEmpty { listOf("Sin clasificar") },
            trash = emptyList(),
            selectedCalendarId = null
        )
    }.getOrDefault(AppSnapshot())

    private fun JSONObject.toTask() = TaskItem(
        id = optString("id"), title = optString("title"), course = optString("course"), dueDate = optString("dueDate"),
        status = optString("status", "Pendiente"), source = optString("source", "TARE-IA"), externalId = optString("externalId"),
        description = optString("description"), calendarEventId = optLong("calendarEventId", -1L).takeIf { it >= 0 },
        reminderEnabled = optBoolean("reminderEnabled", true),
        googleEventId = optString("googleEventId"), googleCalendarAccount = optString("googleCalendarAccount")
    )
    private fun JSONObject.toCourse() = CourseItem(optString("id"), optString("name"), optString("abbreviation"), optInt("priority", 3), optInt("difficulty", 3))
    private fun JSONObject.toBlock() = StudyBlock(optString("id"), optInt("dayOfWeek"), optInt("startMinute"), optInt("endMinute"), optString("label", "Disponible"))
    private fun JSONObject.toSession() = StudySession(optString("id"), optString("taskId"), optString("title"), optString("date"), optInt("startMinute"), optInt("durationMinutes", 60))
    private fun JSONObject.toNote() = AcademicNote(optString("id"), optString("title"), optString("content"), optString("courseId"), optString("folder", "Sin clasificar"), optString("source", "Nota"))
    private fun UserProfile.toJson() = JSONObject().apply {
        put("name", name); put("email", email); put("studentCode", studentCode); put("semester", semester)
        put("pinSalt", pinSalt); put("pinHash", pinHash); put("configured", configured)
    }
    private fun JSONObject.toProfile() = UserProfile(optString("name"), optString("email"), optString("studentCode"), optString("semester"), optString("pinSalt"), optString("pinHash"), optBoolean("configured"))
    private fun JSONArray?.toStringList(): List<String> = this?.let { array -> (0 until array.length()).map { array.optString(it) }.filter { it.isNotBlank() } }.orEmpty()
    private inline fun <T> JSONArray?.toListOf(parse: (JSONObject) -> T): List<T> = this?.let { array -> (0 until array.length()).mapNotNull { array.optJSONObject(it)?.let(parse) } }.orEmpty()
}

object LocalAuth {
    fun create(name: String, email: String, studentCode: String, semester: String, pin: String): UserProfile {
        require(name.isNotBlank() && email.contains("@") && pin.length >= 6) { "Completa nombre, correo y PIN de al menos 6 caracteres." }
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        return UserProfile(
            name = name.trim(), email = email.trim(), studentCode = studentCode.trim(), semester = semester.trim(),
            pinSalt = Base64.encodeToString(salt, Base64.NO_WRAP),
            pinHash = derive(pin, salt), configured = true
        )
    }

    fun verify(profile: UserProfile, pin: String): Boolean {
        if (!profile.configured || pin.isBlank()) return false
        val salt = runCatching { Base64.decode(profile.pinSalt, Base64.NO_WRAP) }.getOrNull() ?: return false
        val actual = runCatching { derive(pin, salt) }.getOrNull() ?: return false
        return MessageDigest.isEqual(actual.toByteArray(), profile.pinHash.toByteArray())
    }

    private fun derive(pin: String, salt: ByteArray): String {
        val spec = PBEKeySpec(pin.toCharArray(), salt, 120_000, 256)
        val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        spec.clearPassword()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}
