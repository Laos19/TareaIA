package com.tareia.app

import java.time.LocalDate
import java.util.UUID

object TaskPlanner {
    fun propose(task: TaskItem, course: CourseItem?, availability: List<StudyBlock>, existing: List<StudySession>): List<StudySession> {
        val due = runCatching { LocalDate.parse(task.dueDate) }.getOrElse { return emptyList() }
        val today = LocalDate.now()
        if (due.isBefore(today)) return emptyList()
        val count = (1 + ((course?.difficulty ?: 3) + (course?.priority ?: 3)) / 2).coerceIn(2, 6)
        val days = generateSequence(due.minusDays(1)) { it.minusDays(1) }.takeWhile { !it.isBefore(today) }.toList().reversed()
        val sessions = mutableListOf<StudySession>()
        val available = availability.sortedWith(compareBy<StudyBlock> { it.dayOfWeek }.thenBy { it.startMinute })
        for (day in days) {
            if (sessions.size >= count) break
            val matching = available.filter { it.dayOfWeek == day.dayOfWeek.value }
            for (block in matching) {
                var start = block.startMinute
                while (start + 60 <= block.endMinute && sessions.size < count) {
                    val occupied = existing.any { it.date == day.toString() && overlaps(start, start + 60, it.startMinute, it.startMinute + it.durationMinutes) } ||
                        sessions.any { it.date == day.toString() && overlaps(start, start + 60, it.startMinute, it.startMinute + it.durationMinutes) }
                    if (!occupied) sessions += StudySession(UUID.randomUUID().toString(), task.id, task.title, day.toString(), start, 60)
                    start += 60
                }
            }
        }
        if (sessions.size < count && availability.isEmpty()) {
            val span = java.time.temporal.ChronoUnit.DAYS.between(today, due).coerceAtLeast(0)
            for (i in sessions.size until count) {
                val day = today.plusDays((span * (i + 1) / (count + 1)).coerceAtMost(span))
                sessions += StudySession(UUID.randomUUID().toString(), task.id, task.title, day.toString(), 18 * 60, 60)
            }
        }
        return sessions
    }

    private fun overlaps(aStart: Int, aEnd: Int, bStart: Int, bEnd: Int) = aStart < bEnd && bStart < aEnd
}
