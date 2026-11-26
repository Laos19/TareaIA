package com.tareia.app.dominio

import java.util.UUID

/**
 * Entidad Curso (DDD).
 *
 * - abreviatura y prioridad son OBJETOS DE VALOR (Abreviatura, Prioridad): se validan solos.
 * - Cada curso tiene SU carpeta (relación 1 a 1 del diagrama): ahí se guardan sus apuntes.
 * - Implementa Eliminable: un curso puede ir a la papelera y restaurarse (RF-21, RN14).
 *
 * Se agregó el atributo "dificultad" (1 a 5) porque la app ya lo pedía en el formulario
 * de cursos y la planificación local lo usa para calcular cuántas sesiones proponer.
 */
class Curso(
    override val id: String = UUID.randomUUID().toString(),
    nombre: String,
    var abreviatura: Abreviatura,
    var prioridad: Prioridad = Prioridad.NORMAL,
    var dificultad: Int = 3,
    var activo: Boolean = true,
    val carpeta: Carpeta = Carpeta(nombre = nombre),
    fechaEliminacion: Long? = null
) : Eliminable {

    var nombre: String = nombre
        private set

    override var fechaEliminacion: Long? = fechaEliminacion
        private set

    init {
        require(nombre.isNotBlank()) { "El nombre del curso es obligatorio." }
        require(dificultad in 1..5) { "La dificultad debe estar entre 1 y 5." }
    }

    /** Cambia el nombre del curso y el de su carpeta, para que sigan coincidiendo. */
    fun renombrar(nuevoNombre: String) {
        require(nuevoNombre.isNotBlank()) { "El nombre del curso es obligatorio." }
        nombre = nuevoNombre.trim()
        carpeta.nombre = nombre
    }

    override fun etiquetaPapelera() = nombre
    override fun enviarAPapelera() { fechaEliminacion = System.currentTimeMillis(); activo = false }
    override fun restaurar() { fechaEliminacion = null; activo = true }
}
