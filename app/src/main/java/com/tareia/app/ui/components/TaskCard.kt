package com.tareia.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tareia.app.TaskItem
import com.tareia.app.ui.theme.Espacio

// Tarjeta de actividad.

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TaskCard(task: TaskItem, onToggle: (() -> Unit)? = null, onEdit: (() -> Unit)? = null,
                     onDelete: (() -> Unit)? = null, onSync: (() -> Unit)? = null, onPlan: (() -> Unit)? = null) {
    val secundario = MaterialTheme.colorScheme.onSurfaceVariant
    SurfaceCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Espacio.sm), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(task.title, style = MaterialTheme.typography.titleMedium)
                Text(task.course.ifBlank { "Sin curso" }, color = secundario)
                Text("Entrega: ${task.dueDate.ifBlank { "Sin fecha" }}", color = secundario, style = MaterialTheme.typography.bodySmall)
            }
            StatusBadge(task.status)
        }
        Text("Origen: ${task.source}${if (task.calendarEventId != null) " · Sincronizada" else ""}", color = secundario, style = MaterialTheme.typography.labelSmall)
        if (onToggle != null || onEdit != null || onDelete != null || onSync != null || onPlan != null) {
            // FlowRow: con letra grande las acciones bajan de línea en vez de quedar cortadas.
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Espacio.xs)) {
                if (onToggle != null) TextButton(onClick = onToggle) { Text(if (task.status == "Completada") "Marcar pendiente" else "Completar") }
                if (onEdit != null) TextButton(onClick = onEdit) { Text("Editar") }
                if (onPlan != null) TextButton(onClick = onPlan) { Text("Planificar") }
                if (onSync != null) TextButton(onClick = onSync) { Text("Calendario") }
                if (onDelete != null) TextButton(onClick = onDelete) { Text("Eliminar") }
            }
        }
    }
}
