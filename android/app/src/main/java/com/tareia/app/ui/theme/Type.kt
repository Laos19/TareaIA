package com.tareia.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.tareia.app.R

/*
 * Tipografía TARE·IA
 * - Archivo (variable): títulos y símbolos de las celdas del tablero.
 * - Atkinson Hyperlegible: texto de lectura; diseñada para máxima legibilidad.
 * - Atkinson Hyperlegible Mono: cifras (números atómicos, fechas, contadores).
 * Todas con licencia SIL OFL 1.1 (ver docs/MARCA.md).
 */

@OptIn(ExperimentalTextApi::class)
private fun archivo(weight: FontWeight) = Font(
    R.font.archivo_variable, weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
)

@OptIn(ExperimentalTextApi::class)
private fun mono(weight: FontWeight) = Font(
    R.font.atkinson_mono_variable, weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
)

val Archivo = FontFamily(
    archivo(FontWeight.Normal), archivo(FontWeight.Medium),
    archivo(FontWeight.SemiBold), archivo(FontWeight.Bold), archivo(FontWeight.ExtraBold),
)

val Atkinson = FontFamily(
    Font(R.font.atkinson_regular, FontWeight.Normal),
    Font(R.font.atkinson_bold, FontWeight.Bold),
)

val AtkinsonMono = FontFamily(mono(FontWeight.Normal), mono(FontWeight.Medium), mono(FontWeight.Bold))

private val base = Typography()

internal val TareiaTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = Archivo, fontWeight = FontWeight.ExtraBold),
    displayMedium = base.displayMedium.copy(fontFamily = Archivo, fontWeight = FontWeight.ExtraBold),
    displaySmall = base.displaySmall.copy(fontFamily = Archivo, fontWeight = FontWeight.Bold),
    headlineLarge = base.headlineLarge.copy(fontFamily = Archivo, fontWeight = FontWeight.Bold),
    headlineMedium = base.headlineMedium.copy(fontFamily = Archivo, fontWeight = FontWeight.Bold),
    headlineSmall = base.headlineSmall.copy(fontFamily = Archivo, fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
    titleLarge = base.titleLarge.copy(fontFamily = Archivo, fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontFamily = Archivo, fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.copy(fontFamily = Archivo, fontWeight = FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.copy(fontFamily = Atkinson),
    bodyMedium = base.bodyMedium.copy(fontFamily = Atkinson),
    bodySmall = base.bodySmall.copy(fontFamily = Atkinson),
    labelLarge = base.labelLarge.copy(fontFamily = Atkinson, fontWeight = FontWeight.Bold),
    labelMedium = base.labelMedium.copy(fontFamily = Atkinson, fontWeight = FontWeight.Bold),
    labelSmall = base.labelSmall.copy(fontFamily = Atkinson, fontWeight = FontWeight.Bold),
)

/** Estilo para cifras tabulares (fechas, contadores, números de celda). */
val EstiloCifras = TextStyle(fontFamily = AtkinsonMono, fontWeight = FontWeight.Medium)
