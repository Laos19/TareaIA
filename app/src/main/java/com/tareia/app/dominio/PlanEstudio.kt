package com.tareia.app.dominio

import java.time.LocalDateTime
import java.util.UUID

// =====================================================================================
// PATRÓN PROTOTYPE (creacional)
// -------------------------------------------------------------------------------------
// Problema (RF-19 y regla "RF19" del SRS): cuando cambia algo (fecha, prioridad,
// disponibilidad) se propone un plan nuevo, pero el plan VIGENTE no se puede tocar hasta
// que el estudiante confirme.
//
// Solución: se CLONA el plan vigente con clonar(), se modifica la copia (PROPUESTA) y,
// solo si el estudiante confirma, la copia pasa a VIGENTE y el original a REEMPLAZADO.
// Si no confirma, la copia se descarta y el original queda intacto.
//
// Roles:
//   Prototype      -> PlanEstudio (método clonar())
//   Parte clonada  -> SesionEstudio (también tiene clonar(): la copia es PROFUNDA)
//   Cliente        -> GestorPlanificacion (generarPropuesta / reorganizar)
//
// ¿Por qué copia profunda? Si solo se copiara la lista, los dos planes compartirían los
// mismos objetos SesionEstudio y modificar la propuesta dañaría el plan vigente.
// =====================================================================================

/** «Prototype» Plan de estudio del estudiante (RF-07). Composición 1..* con SesionEstudio. */
class PlanEstudio(
    val id: String = UUID.randomUUID().toString(),
    var estado: EstadoPlan,
    val fechaGeneracion: LocalDateTime = LocalDateTime.now(),
    sesiones: List<SesionEstudio> = emptyList()
) {
    private val _sesiones = sesiones.toMutableList()
    val sesiones: List<SesionEstudio> get() = _sesiones.toList()

    /**
     * PROTOTYPE: devuelve una copia nueva e independiente de este plan.
     * - La copia tiene otro id y queda en estado PROPUESTA.
     * - Cada sesión también se clona (copia profunda).
     */
    fun clonar(): PlanEstudio = PlanEstudio(
        id = UUID.randomUUID().toString(),
        estado = EstadoPlan.PROPUESTA,
        fechaGeneracion = LocalDateTime.now(),
        sesiones = _sesiones.map { it.clonar() }
    )

    /** Sesiones que avanzan una actividad concreta. */
    fun sesionesDe(idActividad: String): List<SesionEstudio> = _sesiones.filter { it.idActividad == idActividad }

    /** Cambia las sesiones de UNA actividad por otras (las demás actividades no se tocan). */
    fun reemplazarSesionesDe(idActividad: String, nuevas: List<SesionEstudio>) {
        _sesiones.removeAll { it.idActividad == idActividad }
        _sesiones += nuevas
    }

    fun quitarSesionesDe(idActividad: String) {
        _sesiones.removeAll { it.idActividad == idActividad }
    }

    companion object {
        /** Plan vacío que se usa la primera vez, cuando todavía no existe un plan vigente. */
        fun vacio() = PlanEstudio(estado = EstadoPlan.VIGENTE)
    }
}

/**
 * Sesión de estudio: un bloque de tiempo para avanzar una actividad ("avanza" en el diagrama).
 * Se guarda el id y el título de la actividad (referencia por identidad, DDD).
 */
data class SesionEstudio(
    val id: String = UUID.randomUUID().toString(),
    val idActividad: String,
    val tituloActividad: String,
    val inicio: LocalDateTime,
    val fin: LocalDateTime
) {
    init {
        require(inicio.isBefore(fin)) { "Una sesión debe terminar después de empezar." }
    }

    /** PROTOTYPE: copia de esta sesión (copy() de Kotlin crea un objeto nuevo con los mismos datos). */
    fun clonar(): SesionEstudio = copy()

    fun seCruzaCon(otra: SesionEstudio): Boolean = inicio.isBefore(otra.fin) && otra.inicio.isBefore(fin)

    val duracionMinutos: Int get() = java.time.Duration.between(inicio, fin).toMinutes().toInt()
}
