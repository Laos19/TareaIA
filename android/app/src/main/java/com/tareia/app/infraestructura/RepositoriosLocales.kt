package com.tareia.app.infraestructura

import com.tareia.app.dominio.*
import com.tareia.app.infraestructura.AppDatabase.Tablas
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

// =====================================================================================
// PATRÓN REPOSITORY — IMPLEMENTACIONES (capa de infraestructura)
// -------------------------------------------------------------------------------------
// Cada clase implementa una interfaz del dominio (ActividadRepository, CursoRepository...).
// Aquí, y solo aquí, se sabe CÓMO se guardan los datos (filas JSON en AppDatabase).
// El resto de la app solo conoce las interfaces.
//
// Todas reciben la MISMA instancia de AppDatabase (patrón Singleton).
// En el diagrama se llamaban Room...Repository; ver la nota de AppDatabase.
// =====================================================================================

/** «Repository» Implementación de CursoRepository. */
class LocalCursoRepository(private val db: AppDatabase) : CursoRepository {

    override fun guardar(c: Curso) = db.guardarFila(Tablas.CURSOS, aJson(c))

    override fun listarActivos(): List<Curso> = todos().filter { !it.estaEnPapelera() }

    override fun buscarPorId(id: String): Curso? = todos().firstOrNull { it.id == id }

    override fun buscarPorNombre(nombre: String): Curso? =
        listarActivos().firstOrNull { it.nombre.equals(nombre.trim(), ignoreCase = true) }

    override fun listarEnPapelera(): List<Curso> = todos().filter { it.estaEnPapelera() }

    override fun eliminar(id: String) = db.borrarFila(Tablas.CURSOS, id)

    private fun todos(): List<Curso> = db.leerTabla(Tablas.CURSOS).mapNotNull { runCatching { desdeJson(it) }.getOrNull() }

    private fun aJson(c: Curso) = JSONObject()
        .put("id", c.id).put("nombre", c.nombre).put("abreviatura", c.abreviatura.valor)
        .put("prioridad", c.prioridad.nivel).put("dificultad", c.dificultad).put("activo", c.activo)
        .put("carpetaId", c.carpeta.id)
        .put("fechaEliminacion", c.fechaEliminacion ?: -1L)

    private fun desdeJson(j: JSONObject) = Curso(
        id = j.getString("id"),
        nombre = j.getString("nombre"),
        abreviatura = Abreviatura.de(j.getString("abreviatura")),
        prioridad = Prioridad(j.optInt("prioridad", 3).coerceIn(Prioridad.MINIMA, Prioridad.MAXIMA)),
        dificultad = j.optInt("dificultad", 3).coerceIn(1, 5),
        activo = j.optBoolean("activo", true),
        carpeta = Carpeta(id = j.optString("carpetaId").ifBlank { java.util.UUID.randomUUID().toString() }, nombre = j.getString("nombre")),
        fechaEliminacion = j.optLong("fechaEliminacion", -1L).takeIf { it >= 0 }
    )
}

/** «Repository» Implementación de ContenidoRepository (apuntes, anotaciones y carpetas libres). */
class LocalContenidoRepository(
    private val db: AppDatabase,
    private val cursos: CursoRepository
) : ContenidoRepository {

    override fun guardar(c: ContenidoAcademico) = db.guardarFila(Tablas.CONTENIDOS, aJson(c))

    override fun listarSinClasificar(): List<ContenidoAcademico> = listarTodos().filter { it.estaSinClasificar() }

    override fun listarTodos(): List<ContenidoAcademico> = todos().filter { !it.estaEnPapelera() }

    override fun buscarPorId(id: String): ContenidoAcademico? = todos().firstOrNull { it.id == id }

    override fun listarEnPapelera(): List<ContenidoAcademico> = todos().filter { it.estaEnPapelera() }

    override fun eliminar(id: String) = db.borrarFila(Tablas.CONTENIDOS, id)

    override fun guardarCarpeta(c: Carpeta) =
        db.guardarFila(Tablas.CARPETAS, JSONObject().put("id", c.id).put("nombre", c.nombre))

    override fun listarCarpetas(): List<Carpeta> =
        db.leerTabla(Tablas.CARPETAS).map { Carpeta(id = it.getString("id"), nombre = it.optString("nombre")) }

    /** Todas las carpetas que existen: las de los cursos (incluso en papelera) y las libres. */
    private fun carpetasConocidas(): Map<String, Carpeta> {
        val deCursos = (cursos.listarActivos() + cursos.listarEnPapelera()).map { it.carpeta }
        return (deCursos + listarCarpetas()).associateBy { it.id }
    }

    private fun todos(): List<ContenidoAcademico> {
        val carpetas = carpetasConocidas()
        return db.leerTabla(Tablas.CONTENIDOS).mapNotNull { runCatching { desdeJson(it, carpetas) }.getOrNull() }
    }

    private fun aJson(c: ContenidoAcademico) = JSONObject()
        .put("id", c.id).put("clase", if (c is Anotacion) "Anotacion" else "Apunte")
        .put("titulo", c.titulo).put("tipo", c.tipo.name)
        .put("textoReconocido", c.textoReconocido)
        .put("contenido", (c as? Anotacion)?.contenido.orEmpty())
        .put("fechaRegistro", c.fechaRegistro.toString())
        .put("carpetaId", c.carpeta?.id.orEmpty())
        .put("carpetaNombre", c.carpeta?.nombre.orEmpty())
        .put("fechaEliminacion", c.fechaEliminacion ?: -1L)

    private fun desdeJson(j: JSONObject, carpetas: Map<String, Carpeta>): ContenidoAcademico {
        val carpetaId = j.optString("carpetaId")
        val carpeta = if (carpetaId.isBlank()) null
            else carpetas[carpetaId] ?: Carpeta(id = carpetaId, nombre = j.optString("carpetaNombre", "Carpeta"))
        val eliminacion = j.optLong("fechaEliminacion", -1L).takeIf { it >= 0 }
        val registro = runCatching { LocalDateTime.parse(j.optString("fechaRegistro")) }.getOrDefault(LocalDateTime.now())
        return if (j.optString("clase") == "Anotacion") Anotacion(
            id = j.getString("id"), titulo = j.optString("titulo"), contenido = j.optString("contenido"),
            fechaRegistro = registro, carpeta = carpeta, fechaEliminacion = eliminacion
        ) else Apunte(
            id = j.getString("id"), titulo = j.optString("titulo"),
            tipo = runCatching { TipoContenido.valueOf(j.optString("tipo")) }.getOrDefault(TipoContenido.TEXTO),
            textoReconocido = j.optString("textoReconocido"),
            fechaRegistro = registro, carpeta = carpeta, fechaEliminacion = eliminacion
        )
    }
}

/** «Repository» Implementación de ActividadRepository. */
class LocalActividadRepository(
    private val db: AppDatabase,
    private val cursos: CursoRepository
) : ActividadRepository {

    override fun guardar(a: ActividadAcademica) = db.guardarFila(Tablas.ACTIVIDADES, aJson(a))

    override fun buscarPorId(id: String): ActividadAcademica? = todas().firstOrNull { it.id == id }

    override fun buscarPor(titulo: String, curso: Curso?, fecha: FechaLimite?): ActividadAcademica? =
        listarTodas().firstOrNull {
            it.titulo.trim().equals(titulo.trim(), ignoreCase = true) &&
                it.curso?.id == curso?.id && it.fechaLimite == fecha
        }

    override fun listarPendientes(): List<ActividadAcademica> = listarTodas().filter { !it.estaCompletada() }

    override fun listarTodas(): List<ActividadAcademica> = todas().filter { !it.estaEnPapelera() }

    override fun listarEnPapelera(): List<ActividadAcademica> = todas().filter { it.estaEnPapelera() }

    override fun buscarPorIdExterno(idExterno: String): ActividadAcademica? =
        if (idExterno.isBlank()) null else todas().firstOrNull { it.idExterno == idExterno }

    override fun eliminar(id: String) = db.borrarFila(Tablas.ACTIVIDADES, id)

    private fun todas(): List<ActividadAcademica> {
        val porId = (cursos.listarActivos() + cursos.listarEnPapelera()).associateBy { it.id }
        return db.leerTabla(Tablas.ACTIVIDADES).mapNotNull { runCatching { desdeJson(it, porId) }.getOrNull() }
    }

    private fun aJson(a: ActividadAcademica) = JSONObject()
        .put("id", a.id).put("titulo", a.titulo)
        .put("fechaLimite", a.fechaLimite?.toString().orEmpty())
        .put("tipo", a.tipo.name).put("origen", a.origen.name)
        .put("estado", a.estado.nombre())
        .put("cursoId", a.curso?.id.orEmpty())
        .put("descripcion", a.descripcion).put("idExterno", a.idExterno)
        .put("idContenidoOrigen", a.idContenidoOrigen.orEmpty())
        .put("recordatorios", JSONArray().apply {
            a.recordatorios.forEach { r ->
                put(JSONObject().put("id", r.id).put("fechaHora", r.fechaHora.toString()).put("activo", r.activo))
            }
        })
        .put("evento", a.evento?.let { e ->
            JSONObject().put("idExterno", e.idExterno).put("ultimaSincronizacion", e.ultimaSincronizacion.toString()).put("cuenta", e.cuenta)
        } ?: JSONObject.NULL)
        .put("fechaEliminacion", a.fechaEliminacion ?: -1L)

    private fun desdeJson(j: JSONObject, cursosPorId: Map<String, Curso>): ActividadAcademica {
        val id = j.getString("id")
        val recordatorios = j.optJSONArray("recordatorios")?.let { arr ->
            (0 until arr.length()).mapNotNull { i ->
                val r = arr.optJSONObject(i) ?: return@mapNotNull null
                runCatching {
                    Recordatorio(id = r.getString("id"), idActividad = id, fechaHora = LocalDateTime.parse(r.getString("fechaHora")), activo = r.optBoolean("activo", true))
                }.getOrNull()
            }
        }.orEmpty()
        val evento = j.optJSONObject("evento")?.let { e ->
            EventoCalendario(
                idExterno = e.optString("idExterno"),
                ultimaSincronizacion = runCatching { LocalDateTime.parse(e.optString("ultimaSincronizacion")) }.getOrDefault(LocalDateTime.now()),
                cuenta = e.optString("cuenta")
            )
        }
        return ActividadAcademica(
            id = id,
            titulo = j.getString("titulo"),
            fechaLimite = FechaLimite.desdeTexto(j.optString("fechaLimite")),
            tipo = runCatching { TipoActividad.valueOf(j.optString("tipo")) }.getOrDefault(TipoActividad.TAREA),
            origen = runCatching { OrigenActividad.valueOf(j.optString("origen")) }.getOrDefault(OrigenActividad.MANUAL),
            estado = EstadoActividad.desdeNombre(j.optString("estado")),
            curso = cursosPorId[j.optString("cursoId")],
            descripcion = j.optString("descripcion"),
            idExterno = j.optString("idExterno"),
            idContenidoOrigen = j.optString("idContenidoOrigen").ifBlank { null },
            recordatorios = recordatorios,
            evento = evento,
            fechaEliminacion = j.optLong("fechaEliminacion", -1L).takeIf { it >= 0 }
        )
    }
}

/** «Repository» Implementación de PlanRepository. Guarda el plan vigente y un historial corto. */
class LocalPlanRepository(private val db: AppDatabase) : PlanRepository {

    override fun guardar(p: PlanEstudio) {
        db.guardarFila(Tablas.PLANES, aJson(p))
        // Se conservan como historial solo los 10 planes REEMPLAZADOS más recientes.
        val filas = db.leerTabla(Tablas.PLANES)
        val reemplazados = filas.filter { it.optString("estado") == EstadoPlan.REEMPLAZADO.name }
            .sortedByDescending { it.optString("fechaGeneracion") }
        if (reemplazados.size > 10) {
            val sobran = reemplazados.drop(10).map { it.optString("id") }.toSet()
            db.escribirTabla(Tablas.PLANES, filas.filterNot { it.optString("id") in sobran })
        }
    }

    /** Hay un solo estudiante por dispositivo, así que el plan vigente es el único VIGENTE. */
    override fun obtenerVigente(e: Estudiante): PlanEstudio? =
        db.leerTabla(Tablas.PLANES).firstOrNull { it.optString("estado") == EstadoPlan.VIGENTE.name }
            ?.let { runCatching { desdeJson(it) }.getOrNull() }

    private fun aJson(p: PlanEstudio) = JSONObject()
        .put("id", p.id).put("estado", p.estado.name).put("fechaGeneracion", p.fechaGeneracion.toString())
        .put("sesiones", JSONArray().apply {
            p.sesiones.forEach { s ->
                put(JSONObject().put("id", s.id).put("idActividad", s.idActividad).put("titulo", s.tituloActividad)
                    .put("inicio", s.inicio.toString()).put("fin", s.fin.toString()))
            }
        })

    private fun desdeJson(j: JSONObject): PlanEstudio {
        val arr = j.optJSONArray("sesiones") ?: JSONArray()
        val sesiones = (0 until arr.length()).mapNotNull { i ->
            val s = arr.optJSONObject(i) ?: return@mapNotNull null
            runCatching {
                SesionEstudio(id = s.getString("id"), idActividad = s.getString("idActividad"), tituloActividad = s.optString("titulo"),
                    inicio = LocalDateTime.parse(s.getString("inicio")), fin = LocalDateTime.parse(s.getString("fin")))
            }.getOrNull()
        }
        return PlanEstudio(
            id = j.getString("id"),
            estado = EstadoPlan.valueOf(j.getString("estado")),
            fechaGeneracion = runCatching { LocalDateTime.parse(j.getString("fechaGeneracion")) }.getOrDefault(LocalDateTime.now()),
            sesiones = sesiones
        )
    }
}

/** «Repository» Implementación de EstudianteRepository (agregado respecto al diagrama). */
class LocalEstudianteRepository(private val db: AppDatabase) : EstudianteRepository {

    override fun obtener(): Estudiante? = db.leerTabla(Tablas.ESTUDIANTE).firstOrNull()?.let { j ->
        val arr = j.optJSONArray("franjas") ?: JSONArray()
        val franjas = (0 until arr.length()).mapNotNull { i ->
            val f = arr.optJSONObject(i) ?: return@mapNotNull null
            runCatching {
                FranjaHoraria(
                    dia = DayOfWeek.of(f.getInt("dia")),
                    inicio = LocalTime.parse(f.getString("inicio")),
                    fin = LocalTime.parse(f.getString("fin")),
                    tipo = runCatching { TipoFranja.valueOf(f.optString("tipo")) }.getOrDefault(TipoFranja.DISPONIBLE_ESTUDIO),
                    etiqueta = f.optString("etiqueta", "Disponible")
                )
            }.getOrNull()
        }
        Estudiante(id = j.getString("id"), nombre = j.optString("nombre"), correo = j.optString("correo"),
            calendarioVinculado = j.optBoolean("calendarioVinculado"), franjas = franjas)
    }

    override fun guardar(e: Estudiante) {
        val fila = JSONObject().put("id", e.id).put("nombre", e.nombre).put("correo", e.correo)
            .put("calendarioVinculado", e.calendarioVinculado)
            .put("franjas", JSONArray().apply {
                e.franjas.forEach { f ->
                    put(JSONObject().put("dia", f.dia.value).put("inicio", f.inicio.toString()).put("fin", f.fin.toString())
                        .put("tipo", f.tipo.name).put("etiqueta", f.etiqueta))
                }
            })
        db.escribirTabla(Tablas.ESTUDIANTE, listOf(fila))
    }
}
