package com.tareia.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tareia.app.*
import com.tareia.app.ui.components.*

// Cursos con prioridad y dificultad.

@Composable
internal fun CoursesScreen(courses: List<CourseItem>, onSave: (CourseItem) -> Boolean, onDelete: (CourseItem) -> Unit) {
    var dialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<CourseItem?>(null) }
    var feedback by remember { mutableStateOf("") }
    PageHeading("Cursos", "Organiza actividades y define dificultad y prioridad")
    AppButton("Agregar curso", { editing = null; dialog = true })
    if (feedback.isNotBlank()) Text(feedback, color = MaterialTheme.colorScheme.onSurfaceVariant)
    courses.forEach { course -> SurfaceCard {
        Text(course.name, fontWeight = FontWeight.SemiBold)
        Text("Abreviatura: ${course.abbreviation} · Prioridad: ${course.priority}/5 · Dificultad: ${course.difficulty}/5", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row {
            TextButton(onClick = { editing = course; dialog = true }) { Text("Editar") }
            TextButton(onClick = { onDelete(course) }) { Text("Eliminar a papelera") }
        }
    } }
    if (dialog) CourseDialog(editing, onDismiss = { dialog = false }, onSave = {
        if (onSave(it)) { feedback = "Curso guardado."; dialog = false } else feedback = "Esa abreviatura ya está asignada a otro curso."
    })
}

@Composable
internal fun CourseDialog(course: CourseItem?, onDismiss: () -> Unit, onSave: (CourseItem) -> Unit) {
    var name by remember(course?.id) { mutableStateOf(course?.name.orEmpty()) }
    var abbreviation by remember(course?.id) { mutableStateOf(course?.abbreviation.orEmpty()) }
    var priority by remember(course?.id) { mutableIntStateOf(course?.priority ?: 3) }
    var difficulty by remember(course?.id) { mutableIntStateOf(course?.difficulty ?: 3) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (course == null) "Nuevo curso" else "Editar curso") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(abbreviation, { abbreviation = it.uppercase() }, label = { Text("Abreviatura única") }, modifier = Modifier.fillMaxWidth())
            Text("Prioridad académica: $priority/5"); Slider(priority.toFloat(), { priority = it.toInt().coerceIn(1, 5) }, valueRange = 1f..5f, steps = 3)
            Text("Dificultad percibida: $difficulty/5"); Slider(difficulty.toFloat(), { difficulty = it.toInt().coerceIn(1, 5) }, valueRange = 1f..5f, steps = 3)
        } },
        confirmButton = { TextButton(enabled = name.isNotBlank() && abbreviation.isNotBlank(), onClick = {
            onSave((course ?: CourseItem(name = name.trim(), abbreviation = abbreviation.trim())).copy(name = name.trim(), abbreviation = abbreviation.trim(), priority = priority, difficulty = difficulty))
        }) { Text("Guardar") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}
