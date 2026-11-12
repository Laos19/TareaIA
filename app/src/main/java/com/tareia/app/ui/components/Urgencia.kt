package com.tareia.app.ui.components

import com.tareia.app.CourseItem
import com.tareia.app.TaskItem
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

// Reglas puras (sin Compose) para la urgencia de una entrega y el símbolo de un curso.

enum class NivelUrgencia { Ninguna, Normal, Pronto, Urgente, Vencida }

data class Urgencia(val nivel: NivelUrgencia, val texto: String, val dias: Long? = null)

/**
 * Urgencia de una actividad respecto a [hoy]:
 * vencida (< 0 días) · urgente (hoy o mañana) · pronto (2–3 días) · normal.
 * Las completadas o sin fecha válida no tienen urgencia.
 */
fun urgenciaDe(task: TaskItem, hoy: LocalDate = LocalDate.now()): Urgencia {
    if (task.status == "Completada") return Urgencia(NivelUrgencia.Ninguna, "")
    val fecha = runCatching { LocalDate.parse(task.dueDate) }.getOrNull() ?: return Urgencia(NivelUrgencia.Ninguna, "")
    val dias = ChronoUnit.DAYS.between(hoy, fecha)
    return when {
        dias < 0 -> Urgencia(NivelUrgencia.Vencida, if (dias == -1L) "Venció ayer" else "Venció hace ${-dias} días", dias)
        dias == 0L -> Urgencia(NivelUrgencia.Urgente, "Vence hoy", dias)
        dias == 1L -> Urgencia(NivelUrgencia.Urgente, "Vence mañana", dias)
        dias <= 3 -> Urgencia(NivelUrgencia.Pronto, "En $dias días", dias)
        else -> Urgencia(NivelUrgencia.Normal, "En $dias días", dias)
    }
}

/**
 * Símbolo de «elemento» para un curso: su abreviatura registrada o, si no existe,
 * las iniciales del nombre al estilo de la tabla periódica («Inteligencia Artificial» → «IA»,
 * «Química» → «Qu»). Máximo 3 caracteres.
 */
fun simboloCurso(nombre: String, cursos: List<CourseItem>): String {
    cursos.firstOrNull { it.name.trim().equals(nombre.trim(), ignoreCase = true) }
        ?.abbreviation?.takeIf { it.isNotBlank() }?.let { return it.take(3) }
    val palabras = nombre.trim().split(Regex("\\s+")).filter { it.length > 2 || it.firstOrNull()?.isUpperCase() == true }
    return when {
        palabras.isEmpty() -> "?"
        palabras.size == 1 -> palabras[0].take(2).lowercase(Locale.ROOT).replaceFirstChar { it.titlecase(Locale.ROOT) }
        else -> palabras.take(3).joinToString("") { it.first().uppercase(Locale.ROOT) }
    }
}
