package com.tareia.app.ui.screens

import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tareia.app.*
import com.tareia.app.ui.components.*
import kotlinx.coroutines.launch

// Importación con OCR local e IA opcional.

@Composable
internal fun ImportScreen(state: AppSnapshot, shared: IncomingShare?, onShareConsumed: () -> Unit,
    onAiExtract: (String) -> List<TaskDraft>,
                         onSaveDrafts: (List<TaskDraft>) -> Int, onSaveUnclassified: (String, String, String) -> Unit) {
    val context = LocalContext.current
    var extractedText by remember { mutableStateOf("") }
    val drafts = remember { mutableStateListOf<TaskDraft>() }
    var isWorking by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf("") }
    var selectedSource by remember { mutableStateOf("Documento") }

    fun acceptText(text: String, source: String) {
        extractedText = text
        selectedSource = source
        drafts.clear()
        drafts.addAll(TaskExtractor.extract(text, state.courses, source))
        feedback = if (text.isBlank()) "No se pudo extraer texto legible. Puedes escribir los datos manualmente." else "Revisa cada propuesta; la extracción puede contener errores."
    }
    fun runOcr(uri: android.net.Uri, source: String) {
        isWorking = true; feedback = "Procesando documento e imagen con OCR local…"
        Thread {
            val result = runCatching { DocumentProcessor.extractText(context, uri) }
            Handler(Looper.getMainLooper()).post {
                isWorking = false
                result.onSuccess { acceptText(it, source) }.onFailure { feedback = it.message ?: "No se pudo procesar el documento." }
            }
        }.start()
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) runOcr(uri, "Documento") }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            isWorking = true; feedback = "Reconociendo el texto de la captura…"
            Thread {
                val result = runCatching { DocumentProcessor.extractBitmap(bitmap) }
                Handler(Looper.getMainLooper()).post { isWorking = false; result.onSuccess { acceptText(it, "Cámara") }.onFailure { feedback = it.message ?: "No se pudo procesar la imagen." }; bitmap.recycle() }
            }.start()
        }
    }
    LaunchedEffect(shared) {
        if (shared != null) {
            if (shared.uri != null) runOcr(shared.uri, "Compartido")
            else shared.text?.let { acceptText(it, "Compartido") }
            onShareConsumed()
        }
    }

    PageHeading("Importar y reconocer", "PDF, imagen, texto o contenido compartido desde otra aplicación")
    SurfaceCard {
        Text("El OCR se ejecuta en el dispositivo. Los datos detectados siempre requieren revisión antes de guardarse.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { picker.launch(arrayOf("application/pdf", "image/*", "text/plain")) }, modifier = Modifier.weight(1f)) { Text("Elegir archivo") }
            OutlinedButton(onClick = { camera.launch(null) }, modifier = Modifier.weight(1f)) { Text("Tomar foto") }
        }
        if (isWorking) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (feedback.isNotBlank()) Text(feedback, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
    }
    if (extractedText.isNotBlank()) {
        SurfaceCard {
            Text("Texto reconocido", fontWeight = FontWeight.SemiBold)
            OutlinedTextField(extractedText, { extractedText = it }, minLines = 4, modifier = Modifier.fillMaxWidth())
            OutlinedButton(onClick = {
                isWorking = true
                feedback = "Enviando el texto al proveedor de IA configurado…"
                val sourceText = extractedText
                Thread {
                    val result = runCatching { onAiExtract(sourceText) }
                    Handler(Looper.getMainLooper()).post {
                        isWorking = false
                        result.onSuccess { proposals -> drafts.clear(); drafts.addAll(proposals); feedback = "La IA propuso ${proposals.size} actividad(es). Revisa y corrige cada campo antes de guardar." }
                            .onFailure { feedback = it.message ?: "No se pudo analizar con IA." }
                    }
                }.start()
            }, enabled = !isWorking, modifier = Modifier.fillMaxWidth()) { Text("Analizar texto con IA configurada") }
            Text("Al usar esta opción, el texto reconocido se envía al proveedor que configures en Más > IA.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            if (drafts.isEmpty()) {
                AppButton("Guardar como contenido sin clasificar", {
                    onSaveUnclassified("Importación $selectedSource", extractedText, selectedSource)
                    feedback = "Contenido guardado en Sin clasificar."
                })
            }
        }
        drafts.forEachIndexed { index, draft ->
            DraftEditor(draft, state.courses, onChange = { drafts[index] = it })
        }
        if (drafts.isNotEmpty()) AppButton("Confirmar y guardar ${drafts.size} propuesta(s)", {
            val count = onSaveDrafts(drafts.toList())
            feedback = "$count actividad(es) guardada(s). Revisa las que quedaron sin clasificar."
            drafts.clear()
        })
        AppButton("Guardar el texto completo en Apuntes", {
            onSaveUnclassified("Apunte importado", extractedText, selectedSource)
            feedback = "Apunte guardado."
        })
    }
}

@Composable
internal fun DraftEditor(draft: TaskDraft, courses: List<CourseItem>, onChange: (TaskDraft) -> Unit) {
    SurfaceCard {
        Text("Propuesta detectada", fontWeight = FontWeight.SemiBold)
        OutlinedTextField(draft.title, { onChange(draft.copy(title = it)) }, label = { Text("Actividad") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(draft.course, { onChange(draft.copy(course = it)) }, label = { Text("Curso (si no se reconoce, quedará sin clasificar)") },
            supportingText = { Text(courses.joinToString { it.name }.ifBlank { "Añade cursos en Más > Cursos" }) }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(draft.dueDate, { onChange(draft.copy(dueDate = it)) }, label = { Text("Fecha límite (AAAA-MM-DD)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Text("Fuente: ${draft.source}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
