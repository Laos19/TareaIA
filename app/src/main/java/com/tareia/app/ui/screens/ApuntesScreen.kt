package com.tareia.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tareia.app.*
import com.tareia.app.ui.components.*

// Apuntes, carpetas y contenido sin clasificar.

@Composable
internal fun NotesScreen(data: AppSnapshot, unclassifiedOnly: Boolean, onSave: (AcademicNote) -> Unit,
                        onDelete: (AcademicNote) -> Unit, onFolder: (String) -> Unit) {
    var dialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<AcademicNote?>(null) }
    var folderName by remember { mutableStateOf("") }
    PageHeading(if (unclassifiedOnly) "Sin clasificar" else "Apuntes y carpetas", "Guarda notas breves y asígnalas a un curso o carpeta")
    if (!unclassifiedOnly) {
        AppButton("Nueva anotación", { editing = null; dialog = true })
        SurfaceCard {
            Text("Crear carpeta", fontWeight = FontWeight.Medium)
            OutlinedTextField(folderName, { folderName = it }, label = { Text("Nombre de carpeta") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            TextButton(onClick = { if (folderName.isNotBlank()) { onFolder(folderName.trim()); folderName = "" } }) { Text("Agregar carpeta") }
            Text("Carpetas: ${data.folders.joinToString()}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
    }
    val rows = if (unclassifiedOnly) data.notes.filter { it.folder == "Sin clasificar" || it.courseId.isBlank() } else data.notes
    if (rows.isEmpty()) SurfaceCard { Text(if (unclassifiedOnly) "No hay contenido pendiente de clasificar." else "Aún no hay apuntes.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
    rows.forEach { note -> SurfaceCard {
        Text(note.title, fontWeight = FontWeight.SemiBold)
        Text("${data.courses.firstOrNull { it.id == note.courseId }?.name ?: note.folder} · ${note.source}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        Text(note.content.take(400))
        Row {
            TextButton(onClick = { editing = note; dialog = true }) { Text(if (unclassifiedOnly) "Clasificar" else "Editar") }
            TextButton(onClick = { onDelete(note) }) { Text("Eliminar") }
        }
    } }
    if (dialog) NoteDialog(editing, data, onDismiss = { dialog = false }, onSave = { onSave(it); dialog = false })
}

@Composable
internal fun NoteDialog(note: AcademicNote?, data: AppSnapshot, onDismiss: () -> Unit, onSave: (AcademicNote) -> Unit) {
    var title by remember(note?.id) { mutableStateOf(note?.title.orEmpty()) }
    var content by remember(note?.id) { mutableStateOf(note?.content.orEmpty()) }
    var courseName by remember(note?.id) { mutableStateOf(data.courses.firstOrNull { it.id == note?.courseId }?.name.orEmpty()) }
    var folder by remember(note?.id) { mutableStateOf(note?.folder ?: "Sin clasificar") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (note == null) "Nueva anotación" else "Editar / clasificar") },
        text = { Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(title, { title = it }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(content, { content = it }, label = { Text("Contenido") }, minLines = 3, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(courseName, { courseName = it }, label = { Text("Curso (opcional)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(folder, { folder = it }, label = { Text("Carpeta") }, modifier = Modifier.fillMaxWidth())
            Text("Cursos: ${data.courses.joinToString { it.name }} · Carpetas: ${data.folders.joinToString()}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        } },
        confirmButton = { TextButton(enabled = title.isNotBlank() && content.isNotBlank(), onClick = {
            val course = data.courses.firstOrNull { it.name.equals(courseName.trim(), true) }
            onSave((note ?: AcademicNote(title = title.trim(), content = content.trim())).copy(
                title = title.trim(), content = content.trim(), courseId = course?.id.orEmpty(),
                folder = if (course != null) course.name else folder.ifBlank { "Sin clasificar" }
            ))
        }) { Text("Guardar") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}
