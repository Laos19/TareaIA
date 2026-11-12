package com.tareia.app

import java.util.UUID

data class TaskItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val course: String = "",
    val dueDate: String,
    val status: String = "Pendiente",
    val source: String = "TARE-IA",
    val externalId: String = "",
    val description: String = "",
    val calendarEventId: Long? = null,
    val reminderEnabled: Boolean = true
)

data class CourseItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val abbreviation: String,
    val priority: Int = 3,
    val difficulty: Int = 3
)

data class StudyBlock(
    val id: String = UUID.randomUUID().toString(),
    val dayOfWeek: Int,
    val startMinute: Int,
    val endMinute: Int,
    val label: String = "Disponible"
)

data class StudySession(
    val id: String = UUID.randomUUID().toString(),
    val taskId: String,
    val title: String,
    val date: String,
    val startMinute: Int,
    val durationMinutes: Int = 60
)

data class AcademicNote(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val courseId: String = "",
    val folder: String = "Sin clasificar",
    val source: String = "Nota"
)

data class TrashEntry(
    val id: String = UUID.randomUUID().toString(),
    val type: String,
    val label: String,
    val payload: String,
    val deletedAt: Long = System.currentTimeMillis()
)

data class UserProfile(
    val name: String = "",
    val email: String = "",
    val studentCode: String = "",
    val semester: String = "",
    val pinSalt: String = "",
    val pinHash: String = "",
    val configured: Boolean = false
)

data class AppSnapshot(
    val profile: UserProfile = UserProfile(),
    val tasks: List<TaskItem> = emptyList(),
    val courses: List<CourseItem> = emptyList(),
    val schedule: List<StudyBlock> = emptyList(),
    val sessions: List<StudySession> = emptyList(),
    val notes: List<AcademicNote> = emptyList(),
    val folders: List<String> = listOf("Sin clasificar"),
    val trash: List<TrashEntry> = emptyList(),
    val selectedCalendarId: Long? = null
)

data class TaskDraft(
    val title: String,
    val course: String = "",
    val dueDate: String = "",
    val source: String,
    val description: String = ""
)

data class IncomingShare(val text: String? = null, val uri: android.net.Uri? = null)
