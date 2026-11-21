package com.tareia.app.dominio

/**
 * Interfaz Eliminable (Papelera, RF-21 / RN14).
 *
 * La implementan Curso, ContenidoAcademico y ActividadAcademica. Gracias a esta interfaz,
 * GestorPapelera puede enviar a la papelera y restaurar CUALQUIERA de esas clases sin
 * preguntar de qué tipo es (polimorfismo).
 *
 * Un elemento en la papelera NO se borra: solo se marca. Así se puede restaurar.
 */
interface Eliminable {
    /** Identificador del elemento (para encontrarlo en la papelera). */
    val id: String

    /** Texto que se muestra en la pantalla de Papelera. */
    fun etiquetaPapelera(): String

    /** Momento en que se envió a la papelera (milisegundos) o null si no está en la papelera. */
    val fechaEliminacion: Long?

    fun enviarAPapelera()
    fun restaurar()
    fun estaEnPapelera(): Boolean = fechaEliminacion != null
}
