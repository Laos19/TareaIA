package com.tareia.app.aplicacion

import com.tareia.app.dominio.*
import com.tareia.app.ia.EstrategiaPlanificacion

// GestorPlanificacion, genera y confirma el plan de estudio
class GestorPlanificacion(
    estrategiaInicial: EstrategiaPlanificacion,
    private val planes: PlanRepository,
    private val cursos: CursoRepository,
    private val estudiantes: EstudianteRepository
) {
    private var estrategia: EstrategiaPlanificacion = estrategiaInicial

    // cambia la estrategia en tiempo de ejecucion
    fun setEstrategia(e: EstrategiaPlanificacion) { estrategia = e }

    val estrategiaActual: EstrategiaPlanificacion get() = estrategia

    // Hay un estudiante por dispositivo
    // Si todavía no existe, se crea
    fun obtenerEstudiante(nombre: String = "Estudiante", correo: String = ""): Estudiante =
        estudiantes.obtener() ?: Estudiante(nombre = nombre, correo = correo).also { estudiantes.guardar(it) }

    fun actualizarDatosEstudiante(nombre: String, correo: String) {
        val e = obtenerEstudiante(nombre, correo)
        e.nombre = nombre; e.correo = correo
        estudiantes.guardar(e)
    }

    // Plan vigente, la primera vez es un plan vacio
    fun planVigente(e: Estudiante): PlanEstudio = planes.obtenerVigente(e) ?: PlanEstudio.vacio()

    /** RF-07: propuesta de plan para una actividad, partiendo del plan vigente. */
    suspend fun generarPropuesta(e: Estudiante, a: ActividadAcademica): PlanEstudio =
        reorganizar(planVigente(e), e, a)

    //  PROPUESTA
    suspend fun reorganizar(vigente: PlanEstudio, e: Estudiante, a: ActividadAcademica): PlanEstudio {
        val propuesta = vigente.clonar()
        val ocupadas = propuesta.sesiones.filter { it.idActividad != a.id }
        val nuevas = estrategia.planificar(listOf(a), cursos.listarActivos(), e.franjasDisponibles(), ocupadas).sesiones
        propuesta.reemplazarSesionesDe(a.id, nuevas)
        return propuesta
    }

    /** El estudiante acepta la propuesta: pasa a VIGENTE y el plan anterior a REEMPLAZADO. */
    fun confirmarPlan(p: PlanEstudio, e: Estudiante) {
        val anterior = planes.obtenerVigente(e)
        if (anterior != null && anterior.id != p.id) {
            anterior.estado = EstadoPlan.REEMPLAZADO
            planes.guardar(anterior)
        }
        p.estado = EstadoPlan.VIGENTE
        planes.guardar(p)
    }

    /** Al eliminar una actividad se quitan sus sesiones del plan vigente. */
    fun quitarSesionesDe(idActividad: String) {
        val e = obtenerEstudiante()
        val vigente = planes.obtenerVigente(e) ?: return
        vigente.quitarSesionesDe(idActividad)
        planes.guardar(vigente)
    }

    // ------------------------------- Horario (RN16) ----------------------------------

    /** Agrega una franja al horario. false si se cruza con otra. */
    fun agregarFranja(f: FranjaHoraria): Boolean {
        val e = obtenerEstudiante()
        if (!e.agregarFranja(f)) return false
        estudiantes.guardar(e)
        return true
    }

    fun quitarFranja(f: FranjaHoraria) {
        val e = obtenerEstudiante()
        e.quitarFranja(f)
        estudiantes.guardar(e)
    }
}
