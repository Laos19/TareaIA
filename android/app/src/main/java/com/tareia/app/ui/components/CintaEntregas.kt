package com.tareia.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.tareia.app.TaskItem
import com.tareia.app.ui.theme.EstiloCifras
import com.tareia.app.ui.theme.TareiaTheme
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

// Cinta de entregas: los próximos 14 días en una línea, con un punto por cada entrega.

private val ES = Locale.forLanguageTag("es-PE")

/** Color del punto según la urgencia (el texto accesible repite la información). */
@Composable
internal fun colorUrgencia(nivel: NivelUrgencia): Color {
    val estados = TareiaTheme.colores.estados
    return when (nivel) {
        NivelUrgencia.Vencida, NivelUrgencia.Urgente -> estados.lacre
        NivelUrgencia.Pronto -> estados.ambar
        else -> MaterialTheme.colorScheme.primary
    }
}

@Composable
internal fun CintaEntregas(tareas: List<TaskItem>, modifier: Modifier = Modifier, hoy: LocalDate = LocalDate.now(), dias: Int = 14) {
    val pendientes = tareas.filter { it.status != "Completada" }
    val porFecha = pendientes.groupBy { runCatching { LocalDate.parse(it.dueDate) }.getOrNull() }
    val vencidas = pendientes.count { urgenciaDe(it, hoy).nivel == NivelUrgencia.Vencida }
    val fechas = (0 until dias).map { hoy.plusDays(it.toLong()) }
    val enRango = fechas.sumOf { porFecha[it]?.size ?: 0 }

    // TalkBack lee un resumen en lugar de 14 columnas sueltas.
    val resumen = buildString {
        append("Próximos $dias días: ")
        append(if (enRango == 0) "sin entregas" else if (enRango == 1) "1 entrega" else "$enRango entregas")
        fechas.forEach { f -> porFecha[f]?.let { ts ->
            append(". ${f.dayOfWeek.getDisplayName(TextStyle.FULL, ES)} ${f.dayOfMonth}: ")
            append(ts.joinToString(", ") { it.title })
        } }
        if (vencidas > 0) append(". $vencidas vencida${if (vencidas > 1) "s" else ""}")
    }

    val escala = LocalDensity.current.fontScale.coerceIn(1f, 1.8f)
    Column(modifier.clearAndSetSemantics { contentDescription = resumen }, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            fechas.forEach { fecha ->
                val esHoy = fecha == hoy
                val entregas = porFecha[fecha].orEmpty()
                Column(Modifier.width(40.dp * escala), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        fecha.dayOfWeek.getDisplayName(TextStyle.NARROW, ES).uppercase(ES),
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Box(
                        Modifier.size(30.dp * escala).then(if (esHoy) Modifier.background(TareiaTheme.colores.resaltador, CircleShape) else Modifier),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            fecha.dayOfMonth.toString(), style = MaterialTheme.typography.labelLarge.merge(EstiloCifras),
                            color = if (esHoy) TareiaTheme.colores.sobreResaltador else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Row(Modifier.height(8.dp * escala), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                        entregas.take(3).forEach { t ->
                            Box(Modifier.size(7.dp * escala).background(colorUrgencia(urgenciaDe(t, hoy).nivel), CircleShape))
                        }
                    }
                }
            }
        }
        if (vencidas > 0) {
            Text(
                if (vencidas == 1) "1 entrega vencida" else "$vencidas entregas vencidas",
                style = MaterialTheme.typography.labelMedium, color = TareiaTheme.colores.estados.lacre,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}
