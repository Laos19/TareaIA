package com.tareia.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/*
 * Formas: las celdas del tablero son casi cuadradas (radio corto);
 * las tarjetas de contenido, más suaves.
 */
internal val TareiaShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),   // celdas del tablero, chips
    medium = RoundedCornerShape(16.dp),  // tarjetas
    large = RoundedCornerShape(20.dp),   // diálogos, hojas
    extraLarge = RoundedCornerShape(28.dp),
)

/** Forma de celda de elemento (logo, cursos, contadores). */
val FormaCelda = RoundedCornerShape(10.dp)
