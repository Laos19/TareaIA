package com.tareia.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/*
 * Paleta de marca TARE·IA — dirección de arte «Tablero periódico».
 * Ver docs/MARCA.md para el uso de cada token.
 */

// Núcleo — claro
internal val TintaClaro = Color(0xFF1F2A44)      // texto y acción primaria
internal val PapelClaro = Color(0xFFF6F3EC)      // fondo
internal val HojaClaro = Color(0xFFFFFFFF)       // superficies (tarjetas)
internal val GrafitoClaro = Color(0xFF5B6170)    // texto secundario
internal val LineaClaro = Color(0xFFE2DED3)      // bordes y divisores

// Núcleo — oscuro
internal val TintaOscuro = Color(0xFFF1EDE3)
internal val PapelOscuro = Color(0xFF12161F)
internal val HojaOscuro = Color(0xFF1B2130)
internal val GrafitoOscuro = Color(0xFFA3AAB8)
internal val LineaOscuro = Color(0xFF2C3446)

// Firma: el «·» del logo. Solo para énfasis y marcas, nunca como color de texto.
internal val Resaltador = Color(0xFFFFD84D)
internal val SobreResaltador = Color(0xFF2B2100)

/** Colores semánticos de estado (siempre acompañados de texto o ícono). */
@Immutable
data class EstadoColores(
    val sello: Color,     // contenido sugerido por IA / pendiente de validar
    val lacre: Color,     // vencido o ≤ 24 h
    val ambar: Color,     // ≤ 3 días
    val exito: Color,     // completada / sincronizada
)

internal val EstadosClaro = EstadoColores(
    sello = Color(0xFF1E7A6D), lacre = Color(0xFFB93A0B),
    ambar = Color(0xFF8A5A00), exito = Color(0xFF2E7D32),
)
internal val EstadosOscuro = EstadoColores(
    sello = Color(0xFF5CC2AE), lacre = Color(0xFFFF8A5B),
    ambar = Color(0xFFF2B84B), exito = Color(0xFF7BCB80),
)

/**
 * Familias del «Tablero periódico»: cada curso se asigna a una familia,
 * igual que un elemento en la tabla. [contenedor] es el fondo de la celda
 * y [contenido] el texto sobre ella (contraste AA verificado en claro).
 */
@Immutable
data class Familia(val nombre: String, val contenedor: Color, val contenido: Color)

internal val FamiliasClaro = listOf(
    Familia("Alcalinos", Color(0xFFF3C9C0), Color(0xFF5C1E12)),
    Familia("Alcalinotérreos", Color(0xFFF5D9B0), Color(0xFF5A3600)),
    Familia("Transición", Color(0xFFC4DAF0), Color(0xFF12355B)),
    Familia("Metaloides", Color(0xFFC6E5D2), Color(0xFF134A2C)),
    Familia("No metales", Color(0xFFF1E7A8), Color(0xFF4A4000)),
    Familia("Halógenos", Color(0xFFDCD0F0), Color(0xFF33215C)),
    Familia("Gases nobles", Color(0xFFC0E6E6), Color(0xFF0E4545)),
    Familia("Lantánidos", Color(0xFFF1CADA), Color(0xFF5A1530)),
)

/** En oscuro se invierte la relación: celda profunda y texto claro. */
internal val FamiliasOscuro = FamiliasClaro.map {
    Familia(it.nombre, contenedor = it.contenido, contenido = it.contenedor)
}
