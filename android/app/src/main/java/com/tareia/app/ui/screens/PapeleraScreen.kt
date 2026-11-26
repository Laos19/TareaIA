package com.tareia.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.tareia.app.*
import com.tareia.app.ui.components.*

// Papelera con restauración.

@Composable
internal fun TrashScreen(trash: List<TrashEntry>, onRestore: (TrashEntry) -> Unit, onPurge: (TrashEntry) -> Unit) {
    PageHeading("Papelera", "Restaura cursos, tareas y apuntes eliminados")
    if (trash.isEmpty()) SurfaceCard { Text("La papelera está vacía.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
    trash.forEach { item -> SurfaceCard {
        Text(item.label, fontWeight = FontWeight.Medium)
        Text("${item.type} · eliminado ${java.time.Instant.ofEpochMilli(item.deletedAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate()}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        Row { TextButton(onClick = { onRestore(item) }) { Text("Restaurar") }; TextButton(onClick = { onPurge(item) }) { Text("Eliminar definitivamente") } }
    } }
}
