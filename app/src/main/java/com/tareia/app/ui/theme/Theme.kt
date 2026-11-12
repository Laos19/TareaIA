package com.tareia.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Tokens de marca que Material 3 no cubre. */
@Immutable
data class TareiaColores(
    val grafito: Color,
    val linea: Color,
    val resaltador: Color,
    val estados: EstadoColores,
    val familias: List<Familia>,
) {
    /** Familia estable para un curso (mismo nombre → mismo color). */
    fun familiaDe(curso: String): Familia =
        familias[Math.floorMod(curso.trim().lowercase().hashCode(), familias.size)]
}

private val ColoresClaro = TareiaColores(GrafitoClaro, LineaClaro, Resaltador, EstadosClaro, FamiliasClaro)
private val ColoresOscuro = TareiaColores(GrafitoOscuro, LineaOscuro, Resaltador, EstadosOscuro, FamiliasOscuro)

private val EsquemaClaro = lightColorScheme(
    primary = TintaClaro, onPrimary = PapelClaro,
    primaryContainer = Color(0xFFDDE3F0), onPrimaryContainer = TintaClaro,
    secondary = EstadosClaro.sello, onSecondary = Color.White,
    secondaryContainer = Color(0xFFE4E8F1), onSecondaryContainer = TintaClaro,
    tertiary = Color(0xFF6B5300), onTertiary = Color.White,
    tertiaryContainer = Resaltador, onTertiaryContainer = Color(0xFF2B2100),
    background = PapelClaro, onBackground = TintaClaro,
    surface = HojaClaro, onSurface = TintaClaro,
    surfaceVariant = Color(0xFFEDE9DF), onSurfaceVariant = GrafitoClaro,
    surfaceContainer = HojaClaro, surfaceContainerHigh = HojaClaro,
    outline = Color(0xFFBDB8AC), outlineVariant = LineaClaro,
    error = EstadosClaro.lacre, onError = Color.White,
)

private val EsquemaOscuro = darkColorScheme(
    primary = TintaOscuro, onPrimary = PapelOscuro,
    primaryContainer = Color(0xFF2A3550), onPrimaryContainer = TintaOscuro,
    secondary = EstadosOscuro.sello, onSecondary = PapelOscuro,
    secondaryContainer = Color(0xFF2A3550), onSecondaryContainer = TintaOscuro,
    tertiary = Resaltador, onTertiary = Color(0xFF2B2100),
    tertiaryContainer = Color(0xFF4A3B00), onTertiaryContainer = Resaltador,
    background = PapelOscuro, onBackground = TintaOscuro,
    surface = HojaOscuro, onSurface = TintaOscuro,
    surfaceVariant = Color(0xFF252C3D), onSurfaceVariant = GrafitoOscuro,
    surfaceContainer = HojaOscuro, surfaceContainerHigh = Color(0xFF222939),
    outline = Color(0xFF4A5368), outlineVariant = LineaOscuro,
    error = EstadosOscuro.lacre, onError = PapelOscuro,
)

private val LocalTareiaColores = staticCompositionLocalOf { ColoresClaro }

/**
 * Tema de marca. `dynamicColor` se omite a propósito: el color de marca
 * no debe cambiar con el fondo de pantalla del usuario.
 */
@Composable
fun TareiaTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalTareiaColores provides if (darkTheme) ColoresOscuro else ColoresClaro) {
        MaterialTheme(
            colorScheme = if (darkTheme) EsquemaOscuro else EsquemaClaro,
            typography = TareiaTypography,
            shapes = TareiaShapes,
            content = content,
        )
    }
}

/** Acceso a los tokens de marca: `TareiaTheme.colores.resaltador`. */
object TareiaTheme {
    val colores: TareiaColores
        @Composable @ReadOnlyComposable get() = LocalTareiaColores.current
}
