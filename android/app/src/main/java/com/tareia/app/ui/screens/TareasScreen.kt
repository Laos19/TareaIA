package com.tareia.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tareia.app.*
import com.tareia.app.ui.components.*
import java.time.LocalDate

// Actividades: crear, editar, completar, sincronizar.

@Composable
internal fun TasksScreen(
    data: AppSnapshot, message: String, onAdd: (TaskItem) -> Boolean,
    onEdit: (TaskItem, Boolean) -> Boolean, onDelete: (TaskItem) -> Unit,
    onToggle: (TaskItem) -> Unit, onSync: (TaskItem) -> Unit, onPlan: (TaskItem) -> Unit
) {
    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<TaskItem?>(null) }
    var feedback by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Todas") }
    PageHeading("Actividades", "Crear, validar, planificar y sincronizar tareas")
    AppButton("Nueva actividad", { editing = null; showEditor = true })
    if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // Filtros = estados del patrón State (EstadoActividad.nombre()).
        listOf("Todas", "Pendiente de validar", "Confirmada", "Sincronizada", "Completada").forEach { filter ->
            FilterChip(selected = status == filter, onClick = { status = filter }, label = { Text(filter) })
        }
    }
    if (feedback.isNotBlank()) Text(feedback, color = MaterialTheme.colorScheme.onSurfaceVariant)
    val rows = data.tasks.filter { status == "Todas" || it.status == status }.sortedBy { it.dueDate }
    if (rows.isEmpty()) SurfaceCard { Text("No hay actividades en este filtro.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
    // key: cada tarjeta conserva su propio estado (p. ej. la animación del tachado) al reordenarse.
    rows.forEach { task -> key(task.id) { TaskCard(task, data.courses,
        onToggle = { onToggle(task) },
        onEdit = { editing = task; showEditor = true },
        onDelete = { onDelete(task) },
        onSync = { onSync(task) },
        onPlan = { onPlan(task) }
    ) } }
    if (showEditor) TaskEditorDialog(editing, data.courses, onDismiss = { showEditor = false }, onSave = { task, replan ->
        val ok = if (editing == null) onAdd(task) else onEdit(task, replan)
        if (!ok) feedback = "Ya existe una actividad con el mismo título, curso y fecha."
        else { feedback = "Actividad guardada."; showEditor = false }
    })
}

@Composable
internal fun TaskEditorDialog(task: TaskItem?, courses: List<CourseItem>, onDismiss: () -> Unit, onSave: (TaskItem, Boolean) -> Unit) {
    var title by remember(task?.id) { mutableStateOf(task?.title.orEmpty()) }
    var course by remember(task?.id) { mutableStateOf(task?.course.orEmpty()) }
    var date by remember(task?.id) { mutableStateOf(task?.dueDate ?: LocalDate.now().plusDays(7).toString()) }
    var details by remember(task?.id) { mutableStateOf(task?.description.orEmpty()) }
    var replan by remember(task?.id) { mutableStateOf(true) }
    val validDate = runCatching { LocalDate.parse(date) }.isSuccess
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (task == null) "Nueva actividad" else "Editar actividad") },
        text = { Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(title, { title = it }, label = { Text("Título") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            CampoCurso(course, { course = it }, courses)
            CampoFecha(date, { date = it })
            OutlinedTextField(details, { details = it }, label = { Text("Descripción (opcional)") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            if (task != null && task.dueDate != date) Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(replan, { replan = it }); Text("Proponer una nueva planificación al guardar", fontSize = 13.sp)
            }
            if (!validDate) Text("Elige una fecha límite.", color = MaterialTheme.colorScheme.error)
        } },
        confirmButton = { TextButton(enabled = title.isNotBlank() && validDate, onClick = {
            onSave((task ?: TaskItem(title = title.trim(), course = course.trim(), dueDate = date.trim())).copy(
                title = title.trim(), course = course.trim(), dueDate = date.trim(), description = details.trim()
            ), replan)
        }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
