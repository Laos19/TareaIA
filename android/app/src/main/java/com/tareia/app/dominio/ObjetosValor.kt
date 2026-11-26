package com.tareia.app.dominio

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

// =====================================================================================
// OBJETOS DE VALOR (DDD)
// -------------------------------------------------------------------------------------
// Un objeto de valor NO tiene identidad (no tiene id): se define solo por su contenido.
// Características que cumplen todas las clases de este archivo:
//   1. Se comparan por valor  -> son "data class", Kotlin genera equals() por contenido.
//   2. Son inmutables         -> todas sus propiedades son "val".
//   3. Se validan a sí mismas -> el bloque init rechaza valores inválidos, así que
//                                nunca existe un objeto de valor inválido en la app.
// =====================================================================================

/**
 * «ValueObject» Abreviatura de un curso, por ejemplo "PS".
 * Regla RN12: la abreviatura es obligatoria y única (la unicidad la revisa GestorCursos).
 * Se guarda en mayúsculas para que "ps" y "PS" se consideren la misma abreviatura.
 */
data class Abreviatura(val valor: String) {
    init {
        require(valor.isNotBlank()) { "La abreviatura del curso es obligatoria." }
        require(valor.length <= 12) { "La abreviatura no puede tener más de 12 caracteres." }
        require(valor == valor.trim().uppercase()) { "Usa Abreviatura.de(texto) para normalizarla." }
    }

    companion object {
        /** Crea la abreviatura normalizada (sin espacios a los lados y en mayúsculas). */
        fun de(texto: String) = Abreviatura(texto.trim().uppercase())
    }
}

/**
 * «ValueObject» Prioridad académica de un curso: de 1 (baja) a 5 (alta).
 * Regla RN04: la planificación reparte más horas de estudio a los cursos con mayor prioridad.
 */
data class Prioridad(val nivel: Int) {
    init {
        require(nivel in MINIMA..MAXIMA) { "La prioridad debe estar entre $MINIMA y $MAXIMA." }
    }

    companion object {
        const val MINIMA = 1
        const val MAXIMA = 5
        val NORMAL = Prioridad(3)
    }
}

/**
 * «ValueObject» Fecha límite de una actividad académica.
 * Nota: el diagrama la muestra como LocalDateTime, pero la app trabaja con fechas de
 * entrega por día (sin hora), así que se implementó con LocalDate.
 */
data class FechaLimite(val valor: LocalDate) {

    /** true si la fecha es hoy o posterior (todavía se puede entregar). */
    fun esFutura(hoy: LocalDate = LocalDate.now()): Boolean = !valor.isBefore(hoy)

    /** Texto en formato ISO (AAAA-MM-DD), el que usa la interfaz. */
    override fun toString(): String = valor.toString()

    companion object {
        /** Convierte un texto "AAAA-MM-DD" en FechaLimite; devuelve null si el texto no es una fecha válida. */
        fun desdeTexto(texto: String?): FechaLimite? =
            texto?.trim()?.takeIf { it.isNotEmpty() }
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                ?.let { FechaLimite(it) }
    }
}

/**
 * «ValueObject» Bloque del horario semanal del estudiante (una clase o un tiempo libre para estudiar).
 * Regla RN16: la hora de inicio debe ser anterior a la hora de fin.
 */
data class FranjaHoraria(
    val dia: DayOfWeek,
    val inicio: LocalTime,
    val fin: LocalTime,
    val tipo: TipoFranja = TipoFranja.DISPONIBLE_ESTUDIO,
    val etiqueta: String = "Disponible"
) {
    init {
        require(inicio.isBefore(fin)) { "La hora de inicio debe ser anterior a la hora de fin (RN16)." }
    }

    /** true si esta franja y la otra ocurren el mismo día y sus horas se cruzan. */
    fun seSolapaCon(otra: FranjaHoraria): Boolean =
        dia == otra.dia && inicio.isBefore(otra.fin) && otra.inicio.isBefore(fin)

    /** Minutos desde las 00:00 (la interfaz guarda las horas así). */
    val minutoInicio: Int get() = inicio.hour * 60 + inicio.minute
    val minutoFin: Int get() = fin.hour * 60 + fin.minute
}
