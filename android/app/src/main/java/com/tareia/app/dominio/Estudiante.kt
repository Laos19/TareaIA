package com.tareia.app.dominio

import java.util.UUID

/**
 * Entidad Estudiante (DDD): el usuario de la app.
 *
 * Es una ENTIDAD porque tiene identidad propia (id): dos estudiantes con el mismo nombre
 * siguen siendo personas distintas.
 *
 * Composición con FranjaHoraria: el estudiante es dueño de su horario (clases y tiempo
 * libre para estudiar). Ese horario lo usa la planificación (RF-07, RN16).
 *
 * Nota: los datos de acceso (PIN, Google Sign-In) siguen en UserProfile/LocalAuth,
 * porque la autenticación se dejó fuera del diagrama.
 */
class Estudiante(
    val id: String = UUID.randomUUID().toString(),
    var nombre: String,
    var correo: String,
    var calendarioVinculado: Boolean = false,
    franjas: List<FranjaHoraria> = emptyList()
) {
    private val _franjas = franjas.toMutableList()

    /** Horario del estudiante (solo lectura desde afuera). */
    val franjas: List<FranjaHoraria> get() = _franjas.toList()

    /** Franjas libres para estudiar: son las que usa la planificación. */
    fun franjasDisponibles(): List<FranjaHoraria> = _franjas.filter { it.tipo == TipoFranja.DISPONIBLE_ESTUDIO }

    /**
     * Agrega una franja al horario. Devuelve false si se cruza con otra franja existente
     * (dos bloques no pueden ocupar la misma hora del mismo día).
     */
    fun agregarFranja(franja: FranjaHoraria): Boolean {
        if (_franjas.any { it.seSolapaCon(franja) }) return false
        _franjas += franja
        return true
    }

    fun quitarFranja(franja: FranjaHoraria) {
        _franjas.remove(franja)
    }
}
