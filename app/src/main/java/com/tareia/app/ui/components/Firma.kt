package com.tareia.app.ui.components

import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.tareia.app.ui.theme.Espacio
import com.tareia.app.ui.theme.TareiaTheme

// Componentes firma de la marca TARE·IA (ver docs/MARCA.md).

/** true si el usuario desactivó las animaciones del sistema (Opciones de accesibilidad). */
@Composable
internal fun animacionesReducidas(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/**
 * Tachado con marcador: al completar, un trazo de resaltador recorre el texto
 * línea por línea y encima queda una raya fina de tinta. Es el momento de recompensa.
 */
@Composable
internal fun TextoTachable(texto: String, tachado: Boolean, style: TextStyle, modifier: Modifier = Modifier) {
    val reducidas = animacionesReducidas()
    val progreso by animateFloatAsState(
        targetValue = if (tachado) 1f else 0f,
        animationSpec = if (reducidas) tween(0) else tween(if (tachado) 560 else 200, easing = FastOutSlowInEasing),
        label = "tachado",
    )
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val marcador = TareiaTheme.colores.resaltador.copy(alpha = 0.55f)
    val tinta = MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        texto, style = style,
        color = if (tachado) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified,
        onTextLayout = { layout = it },
        modifier = modifier.drawWithContent {
            val l = layout
            if (l != null && progreso > 0f) recorrerLineas(l, progreso) { x0, x1, y, alto ->
                drawLine(marcador, Offset(x0, y), Offset(x1, y), strokeWidth = alto * 0.5f, cap = StrokeCap.Round)
            }
            drawContent()
            if (l != null && progreso > 0f) recorrerLineas(l, progreso) { x0, x1, y, _ ->
                drawLine(tinta, Offset(x0, y), Offset(x1, y), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
            }
        },
    )
}

/** Recorre las líneas del texto hasta cubrir [progreso] (0–1) del largo total. */
private inline fun DrawScope.recorrerLineas(l: TextLayoutResult, progreso: Float, dibujar: DrawScope.(Float, Float, Float, Float) -> Unit) {
    val anchos = (0 until l.lineCount).map { l.getLineRight(it) - l.getLineLeft(it) }
    var restante = anchos.sum() * progreso
    for (i in 0 until l.lineCount) {
        if (restante <= 0f) break
        val ancho = minOf(anchos[i], restante)
        restante -= ancho
        val x0 = l.getLineLeft(i)
        dibujar(x0, x0 + ancho, (l.getLineTop(i) + l.getLineBottom(i)) / 2f, l.getLineBottom(i) - l.getLineTop(i))
    }
}

/**
 * Sello IA: marca todo lo que propuso la IA (o una importación) y aún requiere revisión
 * humana. Usa el color `sello`, reservado exclusivamente para esto.
 */
@Composable
internal fun SelloIA(texto: String = "Por validar", modifier: Modifier = Modifier) {
    val sello = TareiaTheme.colores.estados.sello
    Surface(
        modifier = modifier.clearAndSetSemantics { contentDescription = "Sugerido automáticamente: $texto" },
        shape = MaterialTheme.shapes.small, color = Color.Transparent, border = BorderStroke(1.dp, sello), contentColor = sello,
    ) {
        Row(Modifier.padding(horizontal = Espacio.sm, vertical = Espacio.xs), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(6.dp).background(sello, CircleShape))
            Text("IA · $texto", style = MaterialTheme.typography.labelMedium)
        }
    }
}

/**
 * «El punto que piensa»: reemplaza las barras de progreso genéricas. El «·» del logo
 * late mientras la app trabaja. Con animaciones desactivadas queda fijo.
 * TalkBack lo anuncia como progreso indeterminado con el [texto].
 */
@Composable
internal fun PuntoPensando(texto: String, modifier: Modifier = Modifier) {
    val reducidas = animacionesReducidas()
    val escala = if (reducidas) 1f else {
        val transicion = rememberInfiniteTransition(label = "punto")
        transicion.animateFloat(0.55f, 1f, infiniteRepeatable(tween(650, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "latido").value
    }
    Row(
        modifier.clearAndSetSemantics {
            contentDescription = texto
            progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
            liveRegion = LiveRegionMode.Polite
        },
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espacio.md),
    ) {
        Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(16.dp).scale(escala)
                    .background(TareiaTheme.colores.resaltador, CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), CircleShape)
            )
        }
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
