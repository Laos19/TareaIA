package com.tareia.app.dominio

import java.util.UUID

class ActividadBuilder {

    // Datos que se van cargando, para que la pantalla
    // pueda mostrar la propuesta de la IA antes de construir la actividad.
    var id: String = UUID.randomUUID().toString(); private set
    var titulo: String = ""; private set
    var fechaLimite: FechaLimite? = null; private set
    var curso: Curso? = null; private set
    var tipo: TipoActividad? = null; private set
    var origen: OrigenActividad = OrigenActividad.MANUAL; private set
    var descripcion: String = ""; private set
    var idExterno: String = ""; private set
    var idContenidoOrigen: String? = null; private set
    var nombreCursoDetectado: String = ""; private set

    // pasos que devuelven this

    fun conTitulo(t: String): ActividadBuilder { titulo = t.trim(); return this }

    fun conFechaLimite(f: FechaLimite?): ActividadBuilder { fechaLimite = f; return this }

    fun conCurso(c: Curso?): ActividadBuilder { curso = c; return this }

    fun conTipo(t: TipoActividad): ActividadBuilder { tipo = t; return this }

    // Marca que la actividad la propuso la IA
    fun desdeIA(): ActividadBuilder { origen = OrigenActividad.IA; return this }
    fun conId(nuevoId: String): ActividadBuilder { id = nuevoId; return this }
    fun conDescripcion(d: String): ActividadBuilder { descripcion = d.trim(); return this }
    fun conNombreCursoDetectado(n: String): ActividadBuilder { nombreCursoDetectado = n.trim(); return this }
    fun conContenidoOrigen(idContenido: String?): ActividadBuilder { idContenidoOrigen = idContenido; return this }

    // Actividad importada de Moodle
    fun desdeIntegracion(idEnMoodle: String): ActividadBuilder {
        origen = OrigenActividad.INTEGRACION
        idExterno = idEnMoodle
        return this
    }

    fun build(): ActividadAcademica {
        require(titulo.isNotBlank()) { "La actividad necesita un título (RN05)." }
        if (origen == OrigenActividad.MANUAL) {
            require(fechaLimite != null) { "La actividad necesita una fecha límite (RN05)." }
        }
        val faltanDatos = fechaLimite == null
        val estadoInicial: EstadoActividad =            //queda en pendiente validar
            if (origen != OrigenActividad.MANUAL || faltanDatos) PendienteValidar() else Confirmada()

        return ActividadAcademica(     // devuelve
            id = id,
            titulo = titulo,
            fechaLimite = fechaLimite,
            tipo = tipo ?: TipoActividad.deducir(titulo),
            origen = origen,
            estado = estadoInicial,
            curso = curso,
            descripcion = descripcion,
            idExterno = idExterno,
            idContenidoOrigen = idContenidoOrigen
        )
    }
}
