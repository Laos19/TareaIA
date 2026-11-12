package com.tareia.app

import android.content.Context
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object AppStore {
    private const val PREF = "tareia_local_data"
    private const val KEY = "snapshot_v2"

    fun load(context: Context): AppSnapshot = runCatching {
        val root = JSONObject(SecureTokenStore.readProtected(context, PREF, KEY).ifBlank { "{}" })
        AppSnapshot(
            profile = root.optJSONObject("profile")?.toProfile() ?: UserProfile(),
            tasks = root.optJSONArray("tasks").toListOf { it.toTask() },
            courses = root.optJSONArray("courses").toListOf { it.toCourse() },
            schedule = root.optJSONArray("schedule").toListOf { it.toBlock() },
            sessions = root.optJSONArray("sessions").toListOf { it.toSession() },
            notes = root.optJSONArray("notes").toListOf { it.toNote() },
            folders = root.optJSONArray("folders").toStringList().ifEmpty { listOf("Sin clasificar") },
            trash = root.optJSONArray("trash").toListOf { it.toTrash() },
            selectedCalendarId = root.optLong("selectedCalendarId", -1L).takeIf { it >= 0 }
        )
    }.getOrDefault(AppSnapshot())

    fun save(context: Context, snapshot: AppSnapshot) {
        val root = JSONObject().apply {
            put("profile", snapshot.profile.toJson())
            put("tasks", JSONArray().apply { snapshot.tasks.forEach { put(it.toJson()) } })
            put("courses", JSONArray().apply { snapshot.courses.forEach { put(it.toJson()) } })
            put("schedule", JSONArray().apply { snapshot.schedule.forEach { put(it.toJson()) } })
            put("sessions", JSONArray().apply { snapshot.sessions.forEach { put(it.toJson()) } })
            put("notes", JSONArray().apply { snapshot.notes.forEach { put(it.toJson()) } })
            put("folders", JSONArray(snapshot.folders))
            put("trash", JSONArray().apply { snapshot.trash.forEach { put(it.toJson()) } })
            put("selectedCalendarId", snapshot.selectedCalendarId ?: -1L)
        }
        SecureTokenStore.writeProtected(context, PREF, KEY, root.toString())
    }

    fun encodeTask(item: TaskItem) = item.toJson().toString()
    fun decodeTask(value: String) = JSONObject(value).toTask()
    fun encodeCourse(item: CourseItem) = item.toJson().toString()
    fun decodeCourse(value: String) = JSONObject(value).toCourse()
    fun encodeNote(item: AcademicNote) = item.toJson().toString()
    fun decodeNote(value: String) = JSONObject(value).toNote()

    private fun TaskItem.toJson() = JSONObject().apply {
        put("id", id); put("title", title); put("course", course); put("dueDate", dueDate); put("status", status)
        put("source", source); put("externalId", externalId); put("description", description)
        put("calendarEventId", calendarEventId ?: -1L); put("reminderEnabled", reminderEnabled)
    }
    private fun JSONObject.toTask() = TaskItem(
        id = optString("id"), title = optString("title"), course = optString("course"), dueDate = optString("dueDate"),
        status = optString("status", "Pendiente"), source = optString("source", "TARE-IA"), externalId = optString("externalId"),
        description = optString("description"), calendarEventId = optLong("calendarEventId", -1L).takeIf { it >= 0 },
        reminderEnabled = optBoolean("reminderEnabled", true)
    )
    private fun CourseItem.toJson() = JSONObject().apply {
        put("id", id); put("name", name); put("abbreviation", abbreviation); put("priority", priority); put("difficulty", difficulty)
    }
    private fun JSONObject.toCourse() = CourseItem(optString("id"), optString("name"), optString("abbreviation"), optInt("priority", 3), optInt("difficulty", 3))
    private fun StudyBlock.toJson() = JSONObject().apply {
        put("id", id); put("dayOfWeek", dayOfWeek); put("startMinute", startMinute); put("endMinute", endMinute); put("label", label)
    }
    private fun JSONObject.toBlock() = StudyBlock(optString("id"), optInt("dayOfWeek"), optInt("startMinute"), optInt("endMinute"), optString("label", "Disponible"))
    private fun StudySession.toJson() = JSONObject().apply {
        put("id", id); put("taskId", taskId); put("title", title); put("date", date); put("startMinute", startMinute); put("durationMinutes", durationMinutes)
    }
    private fun JSONObject.toSession() = StudySession(optString("id"), optString("taskId"), optString("title"), optString("date"), optInt("startMinute"), optInt("durationMinutes", 60))
    private fun AcademicNote.toJson() = JSONObject().apply {
        put("id", id); put("title", title); put("content", content); put("courseId", courseId); put("folder", folder); put("source", source)
    }
    private fun JSONObject.toNote() = AcademicNote(optString("id"), optString("title"), optString("content"), optString("courseId"), optString("folder", "Sin clasificar"), optString("source", "Nota"))
    private fun TrashEntry.toJson() = JSONObject().apply {
        put("id", id); put("type", type); put("label", label); put("payload", payload); put("deletedAt", deletedAt)
    }
    private fun JSONObject.toTrash() = TrashEntry(optString("id"), optString("type"), optString("label"), optString("payload"), optLong("deletedAt", System.currentTimeMillis()))
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
