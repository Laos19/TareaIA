package com.tareia.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.tareia.app.CourseItem
import com.tareia.app.TaskItem
import com.tareia.app.ui.theme.Espacio
import com.tareia.app.ui.theme.TareiaTheme

// Tarjeta de actividad.

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TaskCard(
    task: TaskItem, cursos: List<CourseItem> = emptyList(),
    onToggle: (() -> Unit)? = null, onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null, onSync: (() -> Unit)? = null, onPlan: (() -> Unit)? = null,
) {
    val secundario = MaterialTheme.colorScheme.onSurfaceVariant
    val completada = task.status == "Completada"
    val urgencia = urgenciaDe(task)
    val haptica = LocalHapticFeedback.current
    SurfaceCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Espacio.md), verticalAlignment = Alignment.Top) {
            if (task.course.isNotBlank()) SimboloCurso(simboloCurso(task.course, cursos), TareiaTheme.colores.familiaDe(task.course))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                TextoTachable(task.title, tachado = completada, style = MaterialTheme.typography.titleMedium)
                Text(task.course.ifBlank { "Sin curso" }, color = secundario)
            }
            StatusBadge(task.status)
        }
        // Fecha + urgencia: el color siempre va con texto e ícono.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val destacada = urgencia.nivel in setOf(NivelUrgencia.Vencida, NivelUrgencia.Urgente, NivelUrgencia.Pronto)
            val color = if (destacada) colorUrgencia(urgencia.nivel) else secundario
            Icon(
                if (urgencia.nivel == NivelUrgencia.Vencida) Icons.Outlined.ErrorOutline else Icons.Outlined.Schedule,
                contentDescription = null, tint = color, modifier = Modifier.size(16.dp),
            )
            Text(
                buildString {
                    append("Entrega: ${task.dueDate.ifBlank { "sin fecha" }}")
                    if (urgencia.texto.isNotBlank()) append(" · ${urgencia.texto}")
                },
                color = color, style = MaterialTheme.typography.bodySmall,
            )
        }
        Text("Origen: ${task.source}${if (task.calendarEventId != null) " · Sincronizada" else ""}", color = secundario, style = MaterialTheme.typography.labelSmall)
        if (onToggle != null || onEdit != null || onDelete != null || onSync != null || onPlan != null) {
            // FlowRow: con letra grande las acciones bajan de línea en vez de quedar cortadas.
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Espacio.xs)) {
                if (onToggle != null) TextButton(onClick = {
                    if (!completada) haptica.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggle()
                }) { Text(if (completada) "Marcar pendiente" else "Completar") }
                if (onEdit != null) TextButton(onClick = onEdit) { Text("Editar") }
                if (onPlan != null) TextButton(onClick = onPlan) { Text("Planificar") }
                if (onSync != null) TextButton(onClick = onSync) { Text("Calendario") }
                if (onDelete != null) TextButton(onClick = onDelete) { Text("Eliminar") }
            }
        }
    }
}
