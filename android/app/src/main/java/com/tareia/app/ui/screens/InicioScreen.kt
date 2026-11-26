package com.tareia.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.tareia.app.AppSnapshot
import com.tareia.app.StudySession
import com.tareia.app.ui.components.PageHeading
import com.tareia.app.ui.components.SectionTitle
import com.tareia.app.ui.components.SurfaceCard
import com.tareia.app.ui.components.TaskCard
import com.tareia.app.ui.components.CintaEntregas
import com.tareia.app.ui.components.TablaCursos
import com.tareia.app.ui.theme.EstiloCifras
import com.tareia.app.ui.theme.Espacio
import com.tareia.app.ui.util.formatMinute
import java.time.LocalDate

// Inicio: resumen académico.

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DashboardScreen(data: AppSnapshot) {
    val today = LocalDate.now().toString()
    val pending = data.tasks.filter { it.status != "Completada" }
    val thisWeek = pending.count {
        it.dueDate >= today && runCatching { LocalDate.parse(it.dueDate).isBefore(LocalDate.now().plusDays(8)) }.getOrDefault(false)
    }
    PageHeading("Hola, ${data.profile.name.substringBefore(' ')}", "Resumen de tu actividad académica")
    // Con letra grande, tres columnas parten las palabras («Pendiente/s»): se apilan en una.
    val columnas = if (LocalDensity.current.fontScale >= 1.3f) 1 else 3
    FlowRow(
        Modifier.fillMaxWidth(), maxItemsInEachRow = columnas,
        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MetricCard("Pendientes", pending.size, Modifier.weight(1f))
        MetricCard("Cursos", data.courses.size, Modifier.weight(1f))
        MetricCard("Esta semana", thisWeek, Modifier.weight(1f))
    }
    SectionTitle("Próximas dos semanas")
    SurfaceCard { CintaEntregas(data.tasks) }
    if (data.courses.isNotEmpty()) {
        SectionTitle("Tu tabla de cursos")
        TablaCursos(data.courses, data.tasks)
    }
    SectionTitle("Próximas entregas")
    val dueSoon = pending.filter { it.dueDate.isNotBlank() }.sortedBy { it.dueDate }.take(6)
    if (dueSoon.isEmpty()) SurfaceCard { Text("No hay entregas próximas. Importa un apunte o registra una actividad.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
    dueSoon.forEach { TaskCard(it, data.courses) }
    SectionTitle("Sesiones planificadas")
    val sessions = data.sessions.sortedWith(compareBy<StudySession> { it.date }.thenBy { it.startMinute }).take(5)
    if (sessions.isEmpty()) SurfaceCard { Text("Aún no hay sesiones. Genera un plan desde la pestaña Plan.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
    sessions.forEach { session ->
        SurfaceCard {
            Text(session.title, style = MaterialTheme.typography.titleSmall)
            Text("${session.date} · ${formatMinute(session.startMinute)} · ${session.durationMinutes} min", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MetricCard(label: String, value: Int, modifier: Modifier = Modifier) {
    Card(
        modifier.clearAndSetSemantics { contentDescription = "$label: $value" },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(Espacio.md), verticalArrangement = Arrangement.spacedBy(Espacio.xs)) {
            Text(value.toString(), style = MaterialTheme.typography.headlineMedium.merge(EstiloCifras))
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

