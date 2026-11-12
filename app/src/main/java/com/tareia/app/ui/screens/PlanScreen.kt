package com.tareia.app.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.tareia.app.*
import com.tareia.app.ui.components.*
import com.tareia.app.ui.util.*
import java.time.LocalDate
import kotlinx.coroutines.launch

// Planificación de sesiones (manual y por voz).

@Composable
internal fun PlannerScreen(data: AppSnapshot, initialTaskId: String, onSaveTask: (TaskItem) -> Boolean, onConfirmPlan: (List<StudySession>) -> Unit) {
    val context = LocalContext.current
    var selectedId by remember(data.tasks, initialTaskId) { mutableStateOf(initialTaskId.takeIf { id -> data.tasks.any { it.id == id } } ?: data.tasks.firstOrNull()?.id.orEmpty()) }
    var transcript by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var course by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    var proposal by remember { mutableStateOf<List<StudySession>>(emptyList()) }
    var message by remember { mutableStateOf("") }
    var listening by remember { mutableStateOf(false) }
    var hasMic by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) }
    val recognizer = remember(context) { if (SpeechRecognizer.isRecognitionAvailable(context)) SpeechRecognizer.createSpeechRecognizer(context) else null }
    DisposableEffect(recognizer) { onDispose { recognizer?.destroy() } }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> hasMic = granted; if (!granted) message = "Permiso de micrófono denegado; puedes escribir los datos manualmente." }

    fun startVoice() {
        val engine = recognizer
        if (engine == null) { message = "El reconocimiento de voz no está disponible."; return }
        engine.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { listening = true }
            override fun onBeginningOfSpeech() { listening = true }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { listening = false }
            override fun onError(error: Int) { listening = false; message = "No se reconoció el audio. Intenta de nuevo o escribe la instrucción." }
            override fun onResults(results: Bundle?) {
                listening = false
                transcript = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                val candidate = TaskExtractor.extract(transcript, data.courses, "Audio").firstOrNull()
                if (candidate != null) { title = candidate.title; course = candidate.course; dueDate = candidate.dueDate }
                message = "Revisa la transcripción, el curso y la fecha antes de generar el plan."
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-PE")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Dicta la tarea, el curso y la fecha límite")
        }
        runCatching { engine.startListening(intent); listening = true }.onFailure { message = "No se pudo iniciar el micrófono." }
    }

    PageHeading("Planificar", "Genera sesiones con fechas, dificultad, prioridad y horario disponible")
    SurfaceCard {
        Text("Plan para una actividad existente", fontWeight = FontWeight.SemiBold)
        if (data.tasks.isEmpty()) Text("Primero registra una tarea o dicta una nueva.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        data.tasks.forEach { task ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selectedId == task.id, onClick = { selectedId = task.id; proposal = emptyList() })
                Column { Text(task.title, fontWeight = FontWeight.Medium); Text("${task.course} · ${task.dueDate}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) }
            }
        }
        AppButton("Generar propuesta", {
            val task = data.tasks.firstOrNull { it.id == selectedId }
            if (task != null) {
                val sessions = TaskPlanner.propose(task, data.courses.firstOrNull { it.name == task.course }, data.schedule, data.sessions.filterNot { it.taskId == task.id })
                proposal = sessions
                message = if (sessions.isEmpty()) "No se pudo proponer sesiones. Revisa la fecha y el horario disponible." else "Propuesta lista; aún no se guardó."
            }
        }, enabled = selectedId.isNotBlank())
    }
    SurfaceCard {
        Text("Crear planificación por audio", fontWeight = FontWeight.SemiBold)
        Text("Dicta una instrucción o escribe los datos; la transcripción se puede corregir.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = { if (hasMic) startVoice() else permission.launch(Manifest.permission.RECORD_AUDIO) }, enabled = !listening, modifier = Modifier.fillMaxWidth()) { Text(if (listening) "Escuchando…" else "Dictar") }
        if (listening) LinearProgressIndicator(Modifier.fillMaxWidth())
        OutlinedTextField(transcript, { transcript = it }, label = { Text("Transcripción") }, minLines = 2, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(title, { title = it }, label = { Text("Actividad") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(course, { course = it }, label = { Text("Curso") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(dueDate, { dueDate = it }, label = { Text("Fecha límite (AAAA-MM-DD)") }, modifier = Modifier.fillMaxWidth())
        AppButton("Proponer sesiones desde audio", {
            val task = TaskItem(title = title.trim(), course = course.trim(), dueDate = dueDate.trim(), source = "Audio")
            val generated = TaskPlanner.propose(task, data.courses.firstOrNull { it.name == course }, data.schedule, data.sessions)
            if (generated.isNotEmpty()) { proposal = generated; selectedId = task.id; message = "Propuesta preparada. Confirma para guardar la actividad y las sesiones." }
            else message = "Completa título y fecha futura; añade disponibilidad en Más > Horario."
        }, enabled = title.isNotBlank() && runCatching { LocalDate.parse(dueDate) }.isSuccess)
    }
    if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    if (proposal.isNotEmpty()) {
        SurfaceCard {
            Text("Propuesta de ${proposal.size} sesión(es)", fontWeight = FontWeight.SemiBold)
            proposal.forEach { Text("${it.date} · ${formatMinute(it.startMinute)} · ${it.durationMinutes} min · ${it.title}") }
            AppButton("Confirmar planificación", {
                val isNewAudio = proposal.any { it.taskId == selectedId } && data.tasks.none { it.id == selectedId }
                if (isNewAudio) {
                    val task = TaskItem(id = selectedId, title = title.trim(), course = course.trim(), dueDate = dueDate.trim(), source = "Audio")
                    if (onSaveTask(task)) onConfirmPlan(proposal) else message = "Ya existe una actividad con esos datos."
                } else onConfirmPlan(proposal)
                proposal = emptyList()
            })
        }
    }
}
