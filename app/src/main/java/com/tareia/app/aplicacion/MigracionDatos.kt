package com.tareia.app.aplicacion

import android.content.Context
import com.tareia.app.AppStore
import com.tareia.app.dominio.*
import com.tareia.app.infraestructura.AppDatabase
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * Migración ÚNICA de los datos guardados por la versión anterior de la app (un solo JSON en
 * AppStore) al nuevo modelo del diagrama. Se ejecuta una sola vez: deja una marca en la
 * tabla "meta" para no repetirse aunque el estudiante luego borre todo.
 * Las entradas antiguas de la papelera no se migran.
 */
internal class MigracionDatos(
    private val context: Context,
    private val db: AppDatabase,
    private val dep: Dependencias
) {
    fun ejecutar() {
        if (db.leerTabla(TABLA_META).any { it.optString("id") == MARCA }) return
        if (db.estaVacia()) runCatching { migrar() }
        db.guardarFila(TABLA_META, JSONObject().put("id", MARCA).put("fecha", System.currentTimeMillis()))
    }

    private fun migrar() {
        val antes = AppStore.cargarDatosAnteriores(context)

        // Cursos (se conserva el id para no perder la relación con los apuntes).
        antes.courses.forEach { c ->
            dep.gestorCursos.guardar(c.id, c.name, c.abbreviation.ifBlank { c.name.take(4) }, c.priority.coerceIn(1, 5), c.difficulty.coerceIn(1, 5))
        }

        // Horario.
        antes.schedule.forEach { b ->
            runCatching {
                FranjaHoraria(DayOfWeek.of(b.dayOfWeek), LocalTime.of(b.startMinute / 60, b.startMinute % 60),
                    LocalTime.of(b.endMinute / 60, b.endMinute % 60), etiqueta = b.label)
            }.getOrNull()?.let { dep.gestorPlanificacion.agregarFranja(it) }
        }

        // Apuntes y carpetas.
        antes.folders.forEach { dep.gestorIngesta.crearCarpeta(it) }
        antes.notes.forEach { n ->
            dep.gestorIngesta.guardarAnotacion(n.id, n.title, n.content, n.courseId.ifBlank { null }, n.folder)
        }

        // Actividades: se reconstruyen con el Builder y se respeta su estado anterior.
        val actividades = mutableListOf<ActividadAcademica>()
        antes.tasks.forEach { t ->
            val b = ActividadBuilder().conId(t.id).conTitulo(t.title)
                .conFechaLimite(FechaLimite.desdeTexto(t.dueDate))
                .conCurso(dep.gestorActividades.resolverCurso(t.course))
                .conDescripcion(t.description)
            if (t.externalId.isNotBlank()) b.desdeIntegracion(t.externalId)
            else if (t.status == PendienteValidar.NOMBRE) b.desdeIA()
            val a = runCatching { b.build() }.getOrNull() ?: return@forEach
            if (t.status != PendienteValidar.NOMBRE && a.esConfirmable()) a.confirmar()
            if (t.status == Completada.NOMBRE) runCatching { a.completar() }
            dep.gestorAlertas.programarPorDefecto(a)
            dep.gestorActividades.guardar(a)
            actividades += a
        }

        // Sesiones de estudio: pasan a ser el plan vigente.
        val sesiones = antes.sessions.mapNotNull { s ->
            runCatching {
                val inicio = LocalDate.parse(s.date).atTime(LocalTime.of(s.startMinute / 60, s.startMinute % 60))
                SesionEstudio(id = s.id, idActividad = s.taskId, tituloActividad = s.title, inicio = inicio, fin = inicio.plusMinutes(s.durationMinutes.toLong()))
            }.getOrNull()
        }
        if (sesiones.isNotEmpty()) {
            val e = dep.gestorPlanificacion.obtenerEstudiante(antes.profile.name, antes.profile.email)
            dep.gestorPlanificacion.confirmarPlan(PlanEstudio(estado = EstadoPlan.PROPUESTA, sesiones = sesiones), e)
        }
    }

    private companion object {
        const val TABLA_META = "meta"
        const val MARCA = "migracion_v1"
    }
}
