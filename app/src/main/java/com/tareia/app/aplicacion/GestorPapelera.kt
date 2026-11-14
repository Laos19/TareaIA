package com.tareia.app.aplicacion

import com.tareia.app.dominio.*

/**
 * GestorPapelera (RF-21, RN14): envía a la papelera, restaura y elimina definitivamente.
 *
 * Trabaja con la interfaz Eliminable: el mismo método enviar() sirve para un Curso, un
 * ContenidoAcademico o una ActividadAcademica (polimorfismo). Solo al guardar se fija en
 * el tipo, para usar el repositorio que corresponde.
 */
class GestorPapelera(
    private val actividades: ActividadRepository,
    private val cursos: CursoRepository,
    private val contenidos: ContenidoRepository,
    private val alertas: GestorAlertas,
    private val planificacion: GestorPlanificacion
) {
    /** Todo lo que está en la papelera, lo más reciente primero. */
    fun listar(): List<Eliminable> =
        (cursos.listarEnPapelera() + actividades.listarEnPapelera() + contenidos.listarEnPapelera())
            .sortedByDescending { it.fechaEliminacion ?: 0L }

    fun buscar(id: String): Eliminable? = listar().firstOrNull { it.id == id }

    fun enviar(e: Eliminable) {
        when (e) {
            is ActividadAcademica -> {
                alertas.cancelar(e)
                planificacion.quitarSesionesDe(e.id)
            }
            is Curso -> {
                // Al eliminar un curso también van a la papelera sus actividades y su contenido.
                actividades.listarTodas().filter { it.curso?.id == e.id }.forEach { enviar(it) }
                contenidos.listarTodos().filter { it.carpeta?.id == e.carpeta.id }.forEach { enviar(it) }
            }
        }
        e.enviarAPapelera()
        guardar(e)
    }

    fun restaurar(e: Eliminable) {
        e.restaurar()
        if (e is ActividadAcademica) alertas.programarPorDefecto(e)
        guardar(e)
    }

    fun eliminarDefinitivo(e: Eliminable) {
        when (e) {
            is ActividadAcademica -> actividades.eliminar(e.id)
            is Curso -> cursos.eliminar(e.id)
            is ContenidoAcademico -> contenidos.eliminar(e.id)
        }
    }

    private fun guardar(e: Eliminable) {
        when (e) {
            is ActividadAcademica -> actividades.guardar(e)
            is Curso -> cursos.guardar(e)
            is ContenidoAcademico -> contenidos.guardar(e)
        }
    }
}
