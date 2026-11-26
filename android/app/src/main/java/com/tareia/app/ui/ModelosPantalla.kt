package com.tareia.app.ui

import com.tareia.app.*
import com.tareia.app.aplicacion.Dependencias
import com.tareia.app.dominio.*

// =====================================================================================
// TRADUCCIÓN ENTRE EL DOMINIO Y LA PANTALLA
// -------------------------------------------------------------------------------------
// Las pantallas (Jetpack Compose) siguen usando sus modelos simples de AppModels.kt
// (TaskItem, CourseItem...). La capa de presentación no está en el diagrama de clases.
// Estas funciones convierten los objetos del dominio en esos modelos de pantalla.
// Las pantallas NO modifican datos: toda acción pasa por un Gestor.
// =====================================================================================

/** Arma todo lo que muestran las pantallas, leyendo a través de los gestores. */
internal fun construirSnapshot(dep: Dependencias, perfil: UserProfile): AppSnapshot {
    val cursos = dep.gestorCursos.listar()
    val estudiante = dep.gestorPlanificacion.obtenerEstudiante(perfil.name, perfil.email)
    val plan = dep.gestorPlanificacion.planVigente(estudiante)
    val carpetasLibres = dep.gestorIngesta.listarCarpetasLibres().map { it.nombre }
    return AppSnapshot(
        profile = perfil,
        tasks = dep.gestorActividades.listar().map { it.aTaskItem() },
        courses = cursos.map { it.aCourseItem() },
        schedule = estudiante.franjas.map { it.aStudyBlock() },
        sessions = plan.sesiones.map { it.aStudySession() },
        notes = dep.gestorIngesta.listarContenido().map { it.aAcademicNote(cursos) },
        folders = (listOf(Carpeta.SIN_CLASIFICAR) + cursos.map { it.nombre } + carpetasLibres).distinct(),
        trash = dep.gestorPapelera.listar().map { it.aTrashEntry() },
        selectedCalendarId = null
    )
}

internal fun ActividadAcademica.aTaskItem() = TaskItem(
    id = id,
    title = titulo,
    course = curso?.nombre.orEmpty(),
    dueDate = fechaLimite?.toString().orEmpty(),
    status = estado.nombre(),                      // nombre del estado (patrón State)
    source = when (origen) {
        OrigenActividad.IA -> "IA"
        OrigenActividad.MANUAL -> "Manual"
        OrigenActividad.INTEGRACION -> "Moodle"
    },
    externalId = idExterno,
    description = descripcion,
    calendarEventId = null,
    reminderEnabled = recordatorios.any { it.activo } || recordatorios.isEmpty(),
    googleEventId = evento?.idExterno.orEmpty(),
    googleCalendarAccount = evento?.cuenta.orEmpty()
)

internal fun Curso.aCourseItem() = CourseItem(
    id = id, name = nombre, abbreviation = abreviatura.valor, priority = prioridad.nivel, difficulty = dificultad
)

/** Un objeto de valor no tiene id: el id de pantalla se arma con sus propios valores. */
internal fun FranjaHoraria.aStudyBlock() = StudyBlock(
    id = "${dia.value}-$minutoInicio-$minutoFin", dayOfWeek = dia.value,
    startMinute = minutoInicio, endMinute = minutoFin, label = etiqueta
)

internal fun StudyBlock.aFranjaHoraria(): FranjaHoraria? = runCatching {
    FranjaHoraria(
        dia = java.time.DayOfWeek.of(dayOfWeek),
        inicio = java.time.LocalTime.of(startMinute / 60, startMinute % 60),
        fin = if (endMinute >= 24 * 60) java.time.LocalTime.MAX else java.time.LocalTime.of(endMinute / 60, endMinute % 60),
        etiqueta = label.ifBlank { "Disponible" }
    )
}.getOrNull()

internal fun SesionEstudio.aStudySession() = StudySession(
    id = id, taskId = idActividad, title = tituloActividad, date = inicio.toLocalDate().toString(),
    startMinute = inicio.hour * 60 + inicio.minute, durationMinutes = duracionMinutos
)

internal fun ContenidoAcademico.aAcademicNote(cursos: List<Curso>) = AcademicNote(
    id = id,
    title = titulo,
    content = texto(),
    courseId = cursos.firstOrNull { it.carpeta.id == carpeta?.id }?.id.orEmpty(),
    folder = carpeta?.nombre ?: Carpeta.SIN_CLASIFICAR,
    source = when {
        this is Anotacion -> "Nota"
        tipo == TipoContenido.PDF -> "PDF"
        tipo == TipoContenido.IMAGEN -> "Imagen"
        else -> "Texto"
    }
)

internal fun Eliminable.aTrashEntry() = TrashEntry(
    id = id,
    type = when (this) {
        is Curso -> "curso"
        is ActividadAcademica -> "actividad"
        else -> "apunte"
    },
    label = etiquetaPapelera(),
    payload = "",
    deletedAt = fechaEliminacion ?: System.currentTimeMillis()
)

/** Propuesta de la IA (builder a medio llenar) -> borrador editable de la pantalla Importar. */
internal fun ActividadBuilder.aTaskDraft(fuente: String) = TaskDraft(
    title = titulo,
    course = curso?.nombre ?: nombreCursoDetectado,
    dueDate = fechaLimite?.toString().orEmpty(),
    source = fuente,
    description = descripcion
)

/** Texto de la pantalla -> tipo de contenido del dominio. */
internal fun tipoContenidoDe(texto: String): TipoContenido =
    runCatching { TipoContenido.valueOf(texto) }.getOrDefault(TipoContenido.TEXTO)
