package com.tareia.app.dominio

import java.util.Locale

// =====================================================================================
// ENUMERACIONES («E» en el diagrama)
// Una enumeración es una lista fija de opciones posibles. Se usan como TIPO de un
// atributo (por eso en el diagrama no tienen flechas): por ejemplo, ActividadAcademica
// tiene el atributo "tipo: TipoActividad".
// =====================================================================================

/** Tipo de archivo o contenido importado. Lo usa ContenidoAcademico.tipo. */
enum class TipoContenido { PDF, IMAGEN, TEXTO }

/** Tipo de actividad académica. Lo usa ActividadAcademica.tipo. */
enum class TipoActividad {
    TAREA, EXAMEN, ENTREGABLE, PROYECTO;

    companion object {
        /** Deduce el tipo a partir de palabras del título (por ejemplo "Examen parcial" -> EXAMEN). */
        fun deducir(titulo: String): TipoActividad {
            val t = titulo.lowercase(Locale.ROOT)
            return when {
                "examen" in t || "práctica calificada" in t || "evaluación" in t -> EXAMEN
                "proyecto" in t -> PROYECTO
                "entrega" in t || "informe" in t || "entregable" in t -> ENTREGABLE
                else -> TAREA
            }
        }
    }
}

/**
 * De dónde salió una actividad. Lo usa ActividadAcademica.origen.
 * IA y MANUAL son los del diagrama. Se agregó INTEGRACION porque la app ya importaba
 * tareas desde Moodle, que no es ni IA ni registro manual.
 */
enum class OrigenActividad { IA, MANUAL, INTEGRACION }

/**
 * Estado de un plan de estudio. Lo usa PlanEstudio.estado.
 * PROPUESTA: copia (clon) que el estudiante todavía no confirma.
 * VIGENTE: el plan que se está usando.
 * REEMPLAZADO: plan anterior, se conserva como historial (trazabilidad, RNF-14).
 */
enum class EstadoPlan { PROPUESTA, VIGENTE, REEMPLAZADO }

/** Tipo de bloque del horario. Lo usa FranjaHoraria.tipo. */
enum class TipoFranja { CLASE, DISPONIBLE_ESTUDIO }
