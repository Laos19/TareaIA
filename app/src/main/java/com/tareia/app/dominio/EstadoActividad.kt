package com.tareia.app.dominio

// =====================================================================================
// PATRÓN STATE (comportamiento)
// -------------------------------------------------------------------------------------
// Problema: lo que se puede hacer con una actividad depende de su estado.
//   - RN01: no se puede sincronizar con el calendario sin antes confirmarla.
//   - RN05: solo se confirma si tiene título y fecha límite.
//   - RN09: lo que viene de la IA empieza "Pendiente de validar".
// Solución: cada estado es una clase. ActividadAcademica (el Contexto) guarda su estado
// actual y le DELEGA las acciones. Así no hay "if (estado == ...)" repartidos por la app:
// cada regla vive en la clase de su estado.
//
// Roles:
//   Context        -> ActividadAcademica
//   State          -> EstadoActividad (esta interfaz)
//   ConcreteState  -> PendienteValidar, Confirmada, Sincronizada, Completada
//
// Transiciones:
//   PendienteValidar --confirmar()--> Confirmada --sincronizar()--> Sincronizada
//   Confirmada / Sincronizada --completar()--> Completada --reabrir()--> Confirmada/Sincronizada
// =====================================================================================

/** «State» Interfaz común de todos los estados de una actividad. */
interface EstadoActividad {
    fun confirmar(a: ActividadAcademica)
    fun sincronizar(a: ActividadAcademica)
    fun completar(a: ActividadAcademica)

    /**
     * Agregado respecto al diagrama: la app ya permitía "Marcar pendiente" una actividad
     * completada por error. reabrir() devuelve la actividad al estado anterior a completarla.
     */
    fun reabrir(a: ActividadAcademica)

    /** Nombre que se muestra en pantalla y que se guarda en la base de datos. */
    fun nombre(): String

    companion object {
        /** Reconstruye el estado a partir del nombre guardado en la base de datos. */
        fun desdeNombre(nombre: String): EstadoActividad = when (nombre) {
            Confirmada.NOMBRE -> Confirmada()
            Sincronizada.NOMBRE -> Sincronizada()
            Completada.NOMBRE -> Completada()
            else -> PendienteValidar()
        }
    }
}

/** «State» La actividad vino de la IA, de una integración o le faltan datos (RN05, RN09). */
class PendienteValidar : EstadoActividad {
    override fun confirmar(a: ActividadAcademica) {
        // RN05: solo se confirma si tiene título y fecha límite.
        check(a.esConfirmable()) { "Para confirmar la actividad debe tener título y fecha límite (RN05)." }
        a.cambiarEstado(Confirmada())
    }

    override fun sincronizar(a: ActividadAcademica) {
        // RN01: no se sincroniza nada que el estudiante no haya validado.
        throw IllegalStateException("Confirma o corrige primero esta actividad antes de enviarla a Google Calendar (RN01).")
    }

    override fun completar(a: ActividadAcademica) {
        throw IllegalStateException("Valida la actividad antes de marcarla como completada.")
    }

    override fun reabrir(a: ActividadAcademica) = Unit // no está completada: no hace nada

    override fun nombre() = NOMBRE

    companion object { const val NOMBRE = "Pendiente de validar" }
}

/** «State» El estudiante validó la actividad. Desde aquí recién se puede sincronizar (RN01). */
class Confirmada : EstadoActividad {
    override fun confirmar(a: ActividadAcademica) = Unit // ya está confirmada

    override fun sincronizar(a: ActividadAcademica) {
        a.cambiarEstado(Sincronizada())
    }

    override fun completar(a: ActividadAcademica) {
        a.cambiarEstado(Completada())
    }

    override fun reabrir(a: ActividadAcademica) = Unit

    override fun nombre() = NOMBRE

    companion object { const val NOMBRE = "Confirmada" }
}

/** «State» La actividad ya tiene su evento en Google Calendar (RF-08). */
class Sincronizada : EstadoActividad {
    override fun confirmar(a: ActividadAcademica) = Unit

    /** Volver a sincronizar solo actualiza el evento: el estado sigue igual. */
    override fun sincronizar(a: ActividadAcademica) = Unit

    override fun completar(a: ActividadAcademica) {
        a.cambiarEstado(Completada())
    }

    override fun reabrir(a: ActividadAcademica) = Unit

    override fun nombre() = NOMBRE

    companion object { const val NOMBRE = "Sincronizada" }
}

/** «State» Estado final: el estudiante marcó la actividad como hecha (RF-10). */
class Completada : EstadoActividad {
    override fun confirmar(a: ActividadAcademica) = Unit
    override fun sincronizar(a: ActividadAcademica) = Unit
    override fun completar(a: ActividadAcademica) = Unit

    /** Vuelve a Sincronizada si tiene evento en el calendario; si no, a Confirmada. */
    override fun reabrir(a: ActividadAcademica) {
        a.cambiarEstado(if (a.evento != null) Sincronizada() else Confirmada())
    }

    override fun nombre() = NOMBRE

    companion object { const val NOMBRE = "Completada" }
}
