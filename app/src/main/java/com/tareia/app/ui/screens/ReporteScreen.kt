package com.tareia.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tareia.app.*
import com.tareia.app.ui.components.*
import java.time.LocalDate
import kotlinx.coroutines.launch

// Reporte filtrado y exportación PDF.

@Composable
internal fun ReportScreen(tasks: List<TaskItem>) {
    val context = LocalContext.current
    var course by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Todas") }
    var pending by remember { mutableStateOf<List<TaskItem>>(emptyList()) }
    var message by remember { mutableStateOf("") }
    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) runCatching { PdfReport.write(context, uri, pending) }
            .onSuccess { message = "Reporte guardado." }.onFailure { message = it.message ?: "No se pudo generar el PDF." }
    }
    val filtered = tasks.filter { (course.isBlank() || it.course.contains(course, true)) && (status == "Todas" || it.status == status) }
    PageHeading("Reporte de actividades", "Filtra y exporta tareas a PDF")
    SurfaceCard {
        OutlinedTextField(course, { course = it }, label = { Text("Filtrar por curso (opcional)") }, modifier = Modifier.fillMaxWidth())
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Todas", "Pendiente de validar", "Confirmada", "Sincronizada", "Completada").forEach { FilterChip(status == it, { status = it }, label = { Text(it) }) }
        }
        Text("${filtered.size} actividad(es) incluidas", color = MaterialTheme.colorScheme.onSurfaceVariant)
        AppButton("Guardar PDF", { pending = filtered; create.launch("TARE-IA-reporte-${LocalDate.now()}.pdf") }, enabled = filtered.isNotEmpty())
        if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    filtered.forEach { TaskCard(it) }
}
