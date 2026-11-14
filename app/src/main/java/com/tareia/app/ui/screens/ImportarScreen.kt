package com.tareia.app.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tareia.app.*
import com.tareia.app.ui.components.*
import java.time.LocalDate

/** Pantalla de importación: el estado vive en TareIAApp, no en esta pestaña.
 * El usuario puede navegar, regresar y continuar el borrador incluso tras reiniciar.
 */
@Composable
internal fun ImportScreen(
    state: AppSnapshot,
    workspace: ImportWorkspace,
    isWorking: Boolean,
    onWorkspaceChange: (ImportWorkspace) -> Unit,
    onDocument: (Uri) -> Unit,
    onCamera: (Bitmap) -> Unit,
    onAnalyze: () -> Unit,
    onSaveDrafts: () -> Unit,
    onSaveNote: () -> Unit,
    onOpenNotes: () -> Unit,
    onClear: () -> Unit,
    // Nombre del proveedor de la fábrica de IA en uso (patrón Abstract Factory).
    nombreProveedorIA: String = "Gemini (Google)",
    // Sugerencias sin IA: las calcula GestorAnalisisIA con ExtractorLocal.
    onLocalSuggestions: () -> Unit = {}
) {
    var confirmDiscardAll by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onDocument(uri)
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) onCamera(bitmap)
    }

    PageHeading("Importar y reconocer", "PDF, imagen JPG/PNG o contenido compartido desde otra aplicación")
    SurfaceCard {
        Text("El OCR se ejecuta en el dispositivo. Revisa las propuestas antes de guardarlas.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { picker.launch(arrayOf("application/pdf", "image/jpeg", "image/png")) },
                enabled = !isWorking,
                modifier = Modifier.weight(1f)
            ) { Text("Elegir archivo") }
            OutlinedButton(
                onClick = { camera.launch(null) }, enabled = !isWorking,
                modifier = Modifier.weight(1f)
            ) { Text("Tomar foto") }
        }
        if (isWorking) PuntoPensando(workspace.feedback.ifBlank { "Procesando…" })
        else if (workspace.feedback.isNotBlank()) {
            Text(workspace.feedback, color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall)
        }
    }

    if (workspace.extractedText.isNotBlank()) {
        SurfaceCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("Borrador de importación", fontWeight = FontWeight.SemiBold)
                TextButton(onClick = { confirmClear = true }, enabled = !isWorking) {
                    Text("Limpiar proceso")
                }
            }
            Text("Tu avance se conserva al cambiar de pestaña y al volver a abrir la app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = workspace.documentTitle,
                onValueChange = { onWorkspaceChange(workspace.copy(documentTitle = it)) },
                label = { Text("Título del apunte") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isWorking
            )
            OutlinedTextField(
                value = workspace.extractedText,
                onValueChange = { newText ->
                    onWorkspaceChange(workspace.copy(
                        extractedText = newText,
                        drafts = emptyList(),
                        proposalOrigin = "Local",
                        feedback = "Texto modificado. Vuelve a generar propuestas antes de guardarlas."
                    ))
                },
                label = { Text("Texto reconocido") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isWorking
            )
            Text("Al pulsar Analizar se enviará el texto a $nombreProveedorIA. " +
                 "Solo se guardarán actividades cuando las confirmes.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            OutlinedButton(
                onClick = onAnalyze,
                enabled = !isWorking,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Analizar apunte con IA") }
            TextButton(
                onClick = onLocalSuggestions,
                enabled = !isWorking
            ) { Text("Volver a sugerencias locales (sin IA)") }
        }

        if (workspace.drafts.isNotEmpty()) {
            SurfaceCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("Propuestas (${workspace.proposalOrigin}) · ${workspace.drafts.size}")
                    TextButton(onClick = { confirmDiscardAll = true }, enabled = !isWorking) {
                        Text("Descartar todas")
                    }
                }
                Text("Puedes rechazar cualquier propuesta sin borrar el texto del apunte.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            workspace.drafts.forEachIndexed { index, draft ->
                DraftEditor(
                    draft = draft,
                    courses = state.courses,
                    enabled = !isWorking,
                    onChange = { edited ->
                        val updated = workspace.drafts.toMutableList().also { it[index] = edited }
                        onWorkspaceChange(workspace.copy(drafts = updated))
                    },
                    onDiscard = {
                        val remaining = workspace.drafts.toMutableList().also { it.removeAt(index) }
                        onWorkspaceChange(workspace.copy(
                            drafts = remaining,
                            feedback = "Propuesta descartada. Quedan ${remaining.size} por revisar."
                        ))
                    }
                )
            }
            val invalid = workspace.drafts.count { draft ->
                draft.title.isBlank() || runCatching { LocalDate.parse(draft.dueDate) }.isFailure
            }
            if (invalid > 0) {
                Text("$invalid propuesta(s) sin título o fecha válida. Corrígelas o descártalas antes de confirmar.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }
            AppButton("Confirmar y guardar ${workspace.drafts.size} propuesta(s)",
                onClick = onSaveDrafts,
                enabled = !isWorking && invalid == 0)
        } else {
            SurfaceCard {
                Text("Sin propuestas pendientes. Puedes conservar el texto, analizarlo otra vez o elegir un documento nuevo.",
                    style = MaterialTheme.typography.bodyMedium)
            }
        }

        SurfaceCard {
            SectionTitle("Conservar el apunte original")
            Text("Puedes guardar el texto aunque rechaces todas las propuestas de actividades.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall)
            AppButton(
                if (workspace.savedNoteId.isBlank()) "Guardar texto completo en Apuntes"
                else "Actualizar apunte guardado",
                onClick = onSaveNote,
                enabled = !isWorking && workspace.documentTitle.isNotBlank()
            )
            if (workspace.savedNoteId.isNotBlank()) {
                OutlinedButton(onClick = onOpenNotes, modifier = Modifier.fillMaxWidth()) {
                    Text("Ver en Más → Apuntes")
                }
            }
            if (workspace.feedback.isNotBlank() && !isWorking) {
                Text(workspace.feedback, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (confirmDiscardAll) {
        AlertDialog(
            onDismissRequest = { confirmDiscardAll = false },
            title = { Text("Descartar todas las propuestas") },
            text = { Text("Se quitarán las actividades sugeridas, pero conservarás el texto importado y los apuntes que ya guardaste.") },
            confirmButton = {
                TextButton(onClick = {
                    onWorkspaceChange(workspace.copy(
                        drafts = emptyList(),
                        feedback = "Propuestas descartadas. El texto del apunte se conserva."
                    ))
                    confirmDiscardAll = false
                }) { Text("Descartar propuestas") }
            },
            dismissButton = { TextButton(onClick = { confirmDiscardAll = false }) { Text("Cancelar") } }
        )
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("¿Limpiar importación?") },
            text = { Text("Eliminarás el texto y las propuestas de esta importación en curso. Las tareas y los apuntes ya guardados no se borrarán.") },
            confirmButton = {
                TextButton(onClick = { onClear(); confirmClear = false }) { Text("Limpiar") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
internal fun DraftEditor(
    draft: TaskDraft,
    courses: List<CourseItem>,
    enabled: Boolean = true,
    onChange: (TaskDraft) -> Unit,
    onDiscard: () -> Unit
) {
    SurfaceCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            SectionTitle("Propuesta detectada")
            SelloIA("Revisar")
        }
        OutlinedTextField(
            value = draft.title,
            onValueChange = { onChange(draft.copy(title = it)) },
            label = { Text("Actividad") },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled
        )
        CampoCurso(draft.course, { onChange(draft.copy(course = it)) }, courses,
            etiqueta = "Curso (vacío = sin clasificar)")
        CampoFecha(draft.dueDate, { onChange(draft.copy(dueDate = it)) })
        if (runCatching { LocalDate.parse(draft.dueDate) }.isFailure) {
            Text("Falta una fecha válida. Corrígela o descarta esta propuesta.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall)
        }
        Text("Fuente: ${draft.source}", fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = onDiscard, enabled = enabled) {
            Text("Descartar esta propuesta")
        }
    }
}
