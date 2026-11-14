package com.tareia.app.aplicacion

import com.tareia.app.dominio.*

//gestorActividades crear, validar, editar y completar actividades.

class GestorActividades(
    private val actividades: ActividadRepository,
    private val detector: DetectorDuplicados,
    private val alertas: GestorAlertas,
    private val gestorCursos: GestorCursos
) {
    // Resultado de guardar varias propuestas a la vez
    data class ResultadoRegistro(val guardadas: List<ActividadAcademica>, val duplicadas: Int)

    fun listar(): List<ActividadAcademica> = actividades.listarTodas()
    fun buscar(id: String): ActividadAcademica? = actividades.buscarPorId(id)

    //Camino MANUAL del Builder
    fun crearManual(b: ActividadBuilder): ActividadAcademica? {
        val a = b.build()                       // BUILDER
        if (detector.esDuplicada(a)) return null
        alertas.programarPorDefecto(a)
        actividades.guardar(a)
        return a
    }

    // Guarda las propuestas que el estudiante ya revisó en la pantalla Importar
    fun registrarPropuestas(builders: List<ActividadBuilder>): ResultadoRegistro {
        val guardadas = mutableListOf<ActividadAcademica>()
        var duplicadas = 0
        builders.forEach { b ->

            // build
            val a = b.build()
            val repetidaEnLote = guardadas.any { it.titulo.equals(a.titulo, true) && it.curso?.id == a.curso?.id && it.fechaLimite == a.fechaLimite }
            if (detector.esDuplicada(a) || repetidaEnLote) { duplicadas++; return@forEach }
            if (a.curso != null && a.esConfirmable()) a.confirmar()      // STATE: Pendiente -> Confirmada
            alertas.programarPorDefecto(a)
            actividades.guardar(a)
            guardadas += a
        }
        return ResultadoRegistro(guardadas, duplicadas)
    }

    /**
     * Actividades de Moodle: se omiten las que ya existen (por id de Moodle o por RN02)
     * y quedan "Pendiente de validar" (el Builder lo decide). Devuelve cuántas se agregaron.
     */
    fun importarDeIntegracion(builders: List<ActividadBuilder>): Int {
        var nuevas = 0
        builders.forEach { b ->
            if (actividades.buscarPorIdExterno(b.idExterno) != null) return@forEach
            val a = b.build()
            if (detector.esDuplicada(a)) return@forEach
            alertas.programarPorDefecto(a)
            actividades.guardar(a)
            nuevas++
        }
        return nuevas
    }

    // Edición / validación de una actividad. Devuelve false si con los nuevos
    //  datos quedaría duplicada
    fun editar(a: ActividadAcademica, titulo: String, fecha: FechaLimite?, curso: Curso?, descripcion: String): Boolean {
        val choca = actividades.buscarPor(titulo, curso, fecha)?.let { it.id != a.id } ?: false
        if (choca) return false
        a.editar(titulo, fecha, curso, descripcion)
        if (a.estaPendienteDeValidar() && a.esConfirmable() && a.curso != null) a.confirmar()
        if (!a.estaCompletada()) alertas.reprogramar(a)
        actividades.guardar(a)
        return true
    }

    // STATE - solo se confirma si el estado actual lo permite
    fun confirmar(a: ActividadAcademica) {
        a.confirmar()
        actividades.guardar(a)
    }

    // STATE - una actividad pendiente de validar no se puede completar
    fun marcarCompletada(a: ActividadAcademica) {
        a.completar()
        alertas.cancelar(a)
        actividades.guardar(a)
    }

    // Deshace "Completar"
    fun reabrir(a: ActividadAcademica) {
        a.reabrir()
        alertas.programarPorDefecto(a)
        actividades.guardar(a)
    }

    // Guarda cambios hechos por otro gestor
    fun guardar(a: ActividadAcademica) = actividades.guardar(a)

    // Curso escrito en un formulario: el existente con ese nombre o uno nuevo (ver GestorCursos)
    fun resolverCurso(nombre: String): Curso? = gestorCursos.obtenerOCrear(nombre)
}
