package com.tareia.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tareia.app.ui.theme.Espacio
import com.tareia.app.ui.theme.TareiaTheme

// Componentes base compartidos por todas las pantallas.

/** Título de pantalla; TalkBack lo anuncia como encabezado para navegar por secciones. */
@Composable
internal fun PageHeading(title: String, subtitle: String = "") {
    Column(verticalArrangement = Arrangement.spacedBy(Espacio.xs)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Subtítulo de sección dentro de una pantalla (también navegable como encabezado). */
@Composable
internal fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
}

@Composable
internal fun SurfaceCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(Espacio.lg), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
internal fun AppButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier.fillMaxWidth()) { Text(text) }
}

/**
 * Estado de una actividad. Reemplaza el AssistChip anterior, que TalkBack
 * anunciaba como botón aunque no hacía nada. El estado se comunica con texto
 * y color a la vez (nunca solo con color).
 */
@Composable
internal fun StatusBadge(status: String, modifier: Modifier = Modifier) {
    val estados = TareiaTheme.colores.estados
    val color: Color = when (status) {
        "Completada" -> estados.exito
        "Pendiente de validar" -> estados.sello
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        modifier = modifier.semantics { contentDescription = "Estado: $status" },
        shape = MaterialTheme.shapes.small,
        color = Color.Transparent,
        border = BorderStroke(1.dp, color),
        contentColor = color,
    ) {
        Text(status, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = Espacio.sm, vertical = Espacio.xs))
    }
}
