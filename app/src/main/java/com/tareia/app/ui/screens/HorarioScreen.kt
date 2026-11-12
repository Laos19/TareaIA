package com.tareia.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tareia.app.*
import com.tareia.app.ui.components.*
import com.tareia.app.ui.util.*

// Bloques de horario disponible.

@Composable
internal fun ScheduleScreen(blocks: List<StudyBlock>, onAdd: (StudyBlock) -> Boolean, onDelete: (StudyBlock) -> Unit) {
    val weekdays = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
    var day by remember { mutableIntStateOf(1) }
    var start by remember { mutableStateOf("18:00") }
    var end by remember { mutableStateOf("20:00") }
    var label by remember { mutableStateOf("Disponible") }
    var message by remember { mutableStateOf("") }
    PageHeading("Horario disponible", "Añade bloques libres; el planificador no usará horas ocupadas")
    SurfaceCard {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            weekdays.forEachIndexed { index, name -> FilterChip(day == index + 1, { day = index + 1 }, label = { Text(name.take(3)) }) }
        }
        OutlinedTextField(start, { start = it }, label = { Text("Inicio (HH:mm)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(end, { end = it }, label = { Text("Fin (HH:mm)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(label, { label = it }, label = { Text("Descripción") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        AppButton("Agregar bloque disponible", {
            val a = parseMinute(start); val b = parseMinute(end)
            if (a == null || b == null || b <= a) message = "Revisa el formato y el intervalo horario."
            else {
                val block = StudyBlock(dayOfWeek = day, startMinute = a, endMinute = b, label = label.ifBlank { "Disponible" })
                message = if (onAdd(block)) "Bloque guardado." else "Ese horario se cruza con otro bloque."
            }
        })
        if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    blocks.sortedWith(compareBy<StudyBlock> { it.dayOfWeek }.thenBy { it.startMinute }).forEach { block ->
        SurfaceCard {
            Text("${weekdays.getOrElse(block.dayOfWeek - 1) { "Día" }} · ${formatMinute(block.startMinute)}–${formatMinute(block.endMinute)}", fontWeight = FontWeight.Medium)
            Text(block.label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = { onDelete(block) }) { Text("Eliminar") }
        }
    }
}
