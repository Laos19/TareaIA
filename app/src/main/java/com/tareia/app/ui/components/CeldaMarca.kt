package com.tareia.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.platform.LocalDensity
import com.tareia.app.ui.theme.Archivo
import com.tareia.app.ui.theme.EstiloCifras
import com.tareia.app.ui.theme.FormaCelda
import com.tareia.app.ui.theme.TareiaTheme

/**
 * Marca TARE·IA: una celda de la tabla periódica.
 * - Número «27»: los requisitos funcionales del proyecto (RF-01 a RF-27).
 * - Símbolo «T·»: la T de Tarea y el punto resaltador, el instante en que
 *   un apunte se convierte en plan.
 * - Nombre: TARE·IA.
 * Las medidas son proporcionales a [lado] para usarla en cualquier tamaño.
 */
@Composable
fun CeldaMarca(modifier: Modifier = Modifier, lado: Dp = 88.dp) {
    val tinta = MaterialTheme.colorScheme.primary
    val escala = lado.value / 88f
    // Un logotipo mantiene sus proporciones: los textos se fijan en dp y no
    // crecen con la escala de fuente del sistema (que sí respeta el resto de la UI).
    val densidad = LocalDensity.current
    fun t(valor: Float): TextUnit = with(densidad) { (valor * escala).dp.toSp() }
    Box(
        modifier
            .size(lado)
            .border(2.dp, tinta, FormaCelda)
            .background(MaterialTheme.colorScheme.surface, FormaCelda)
            .clearAndSetSemantics { contentDescription = "Logotipo TARE·IA" }
    ) {
        Text(
            "27", style = EstiloCifras, fontSize = t(11f), color = tinta,
            modifier = Modifier.align(Alignment.TopStart).padding((7 * escala).dp)
        )
        Row(Modifier.align(Alignment.Center).offset(y = (-5 * escala).dp), verticalAlignment = Alignment.Bottom) {
            Text("T", fontFamily = Archivo, fontWeight = FontWeight.ExtraBold, fontSize = t(36f),
                lineHeight = t(36f), color = tinta)
            Box(
                Modifier.padding(start = (3 * escala).dp, bottom = (7 * escala).dp)
                    .size((9 * escala).dp)
                    .background(TareiaTheme.colores.resaltador, CircleShape)
            )
        }
        Text(
            "TARE·IA", fontFamily = Archivo, fontWeight = FontWeight.SemiBold, fontSize = t(9f),
            letterSpacing = t(1f), color = tinta,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = (6 * escala).dp)
        )
    }
}
