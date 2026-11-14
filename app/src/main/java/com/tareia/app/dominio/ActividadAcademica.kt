package com.tareia.app.dominio

import java.time.LocalDateTime
import java.util.UUID

/**
 * Entidad ActividadAcademica: una tarea, examen, entregable o proyecto.
 *
 * PATRÓN STATE — esta clase es el CONTEXTO:
 *   guarda su estado actual en "estado" y sus métodos confirmar(), sincronizar(),
 *   completar() y reabrir() NO deciden nada: delegan en el objeto estado.
 *   El estado es quien sabe si la acción está permitida y a qué estado pasar.
 *
 * PATRÓN BUILDER — esta clase es el PRODUCTO:
 *   no se construye con "ActividadAcademica(...)" desde la app, sino con ActividadBuilder,
 *   que valida las reglas y fija el estado inicial (por eso el constructor es "internal").
 *
 * Relaciones del diagrama:
 *   - curso: Curso?            -> "pertenece a"
 *   - idContenidoOrigen        -> "origen (si IA)". Se guarda solo el id del apunte (en DDD
 *                                 se referencia a otro agregado por su identidad, así no se
 *                                 carga el texto completo del apunte con cada actividad).
 *   - recordatorios            -> composición 1 a 0..* con Recordatorio
 *   - evento: EventoCalendario -> "sincronizada como" (0..1)
 *   - fechaLimite              -> objeto de valor FechaLimite (null si la IA no la encontró)
 *
 * Atributos agregados porque la app ya los usaba: descripcion, idExterno (Moodle).
 */
class ActividadAcademica internal constructor(
    override val id: String = UUID.randomUUID().toString(),
    titulo: String,
    fechaLimite: FechaLimite?,
    var tipo: TipoActividad,
    val origen: OrigenActividad,
    estado: EstadoActividad,
    curso: Curso?,
    descripcion: String = "",
    val idExterno: String = "",
    val idContenidoOrigen: String? = null,
    recordatorios: List<Recordatorio> = emptyList(),
    evento: EventoCalendario? = null,
    fechaEliminacion: Long? = null
) : Eliminable {

    var titulo: String = titulo
        private set
    var fechaLimite: FechaLimite? = fechaLimite
        private set
    var curso: Curso? = curso
        private set
    var descripcion: String = descripcion
        private set

    /** Estado actual (patrón State). Solo los estados lo cambian, usando cambiarEstado(). */
    var estado: EstadoActividad = estado
        private set

    val recordatorios: MutableList<Recordatorio> = recordatorios.toMutableList()

    var evento: EventoCalendario? = evento
        private set

    override var fechaEliminacion: Long? = fechaEliminacion
        private set

    // ----------------------- Patrón State: el contexto delega -----------------------
    fun confirmar() = estado.confirmar(this)
    fun sincronizar() = estado.sincronizar(this)
    fun completar() = estado.completar(this)
    fun reabrir() = estado.reabrir(this)

    /** Lo usan las clases de estado para pasar al siguiente estado. */
    fun cambiarEstado(e: EstadoActividad) { estado = e }

    /** RN05: una actividad se puede confirmar si tiene título y fecha límite. */
    fun esConfirmable(): Boolean = titulo.isNotBlank() && fechaLimite != null

    fun estaCompletada(): Boolean = estado is Completada
    fun estaPendienteDeValidar(): Boolean = estado is PendienteValidar

    // ----------------------------------- Edición -------------------------------------
    /** El estudiante corrige los datos (formulario de validación, RF-05 / edición, RF-11). */
    fun editar(nuevoTitulo: String, nuevaFecha: FechaLimite?, nuevoCurso: Curso?, nuevaDescripcion: String) {
        require(nuevoTitulo.isNotBlank()) { "El título es obligatorio (RN05)." }
        titulo = nuevoTitulo.trim()
        fechaLimite = nuevaFecha
        curso = nuevoCurso
        descripcion = nuevaDescripcion.trim()
        tipo = TipoActividad.deducir(titulo)
    }

    /** Guarda el vínculo con el evento de Google Calendar (lo usa GestorSincronizacion). */
    fun vincularEvento(e: EventoCalendario?) { evento = e }

    // -------------------------------- Eliminable -------------------------------------
    override fun etiquetaPapelera() = titulo
    override fun enviarAPapelera() { fechaEliminacion = System.currentTimeMillis() }
    override fun restaurar() { fechaEliminacion = null }
}

/**
 * Recordatorio de una actividad (RF-09). Composición: pertenece a una sola actividad.
 * Por defecto se crea uno para el día anterior a la entrega a las 9:00 (lo hace GestorAlertas).
 */
data class Recordatorio(
    val id: String = UUID.randomUUID().toString(),
    val idActividad: String,
    val fechaHora: LocalDateTime,
    val activo: Boolean = true
)

/**
 * Evento de Google Calendar asociado a una actividad (RF-08).
 * idExterno: id del evento en Google. cuenta: correo de la cuenta Google donde se creó
 * (la app no sobrescribe eventos de otra cuenta).
 */
data class EventoCalendario(
    val idExterno: String,
    val ultimaSincronizacion: LocalDateTime,
    val cuenta: String
)
