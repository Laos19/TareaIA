package com.tareia.app.dominio

import java.util.UUID

/**
 * Entidad Carpeta: agrupa contenido académico (apuntes y anotaciones).
 *
 * - Cada Curso tiene su propia carpeta (relación 1 a 1 del diagrama).
 * - Un contenido SIN carpeta (carpeta = null) es contenido "Sin clasificar" (RN13).
 * - La app ya permitía crear carpetas libres (sin curso); se conservan: son carpetas que
 *   no pertenecen a ningún curso y las guarda ContenidoRepository.
 */
class Carpeta(
    val id: String = UUID.randomUUID().toString(),
    var nombre: String
) {
    companion object {
        /** Nombre que la interfaz muestra para el contenido sin carpeta. */
        const val SIN_CLASIFICAR = "Sin clasificar"
    }
}
