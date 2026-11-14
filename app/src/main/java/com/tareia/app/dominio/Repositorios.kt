package com.tareia.app.dominio

// =====================================================================================
// PATRÓN REPOSITORY (DDD / arquitectura)
// -------------------------------------------------------------------------------------
// El dominio guarda y busca objetos COMO SI FUERA UNA COLECCIÓN, sin saber nada de la
// base de datos. Estas INTERFACES están en el dominio; sus implementaciones
// (Local...Repository) están en el paquete "infraestructura".
//
// La dependencia apunta HACIA EL DOMINIO (Inversión de Dependencias, la "D" de SOLID):
// el negocio define qué necesita y la infraestructura se adapta. Si mañana se cambia
// la forma de guardar los datos, solo se reescriben las clases de infraestructura.
//
// Los métodos que están en el diagrama aparecen primero; los demás se agregaron porque
// la app los necesita (listar todo, papelera, borrar definitivamente).
// =====================================================================================

/** «Repository» Actividades académicas. */
interface ActividadRepository {
    fun guardar(a: ActividadAcademica)
    fun buscarPorId(id: String): ActividadAcademica?
    /** Para detectar duplicados (RN02): mismo título, curso y fecha. Ignora la papelera. */
    fun buscarPor(titulo: String, curso: Curso?, fecha: FechaLimite?): ActividadAcademica?
    /** Actividades no completadas y fuera de la papelera. */
    fun listarPendientes(): List<ActividadAcademica>

    fun listarTodas(): List<ActividadAcademica>
    fun listarEnPapelera(): List<ActividadAcademica>
    fun buscarPorIdExterno(idExterno: String): ActividadAcademica?
    fun eliminar(id: String)
}

/** «Repository» Cursos. */
interface CursoRepository {
    fun guardar(c: Curso)
    fun listarActivos(): List<Curso>

    fun buscarPorId(id: String): Curso?
    fun buscarPorNombre(nombre: String): Curso?
    fun listarEnPapelera(): List<Curso>
    fun eliminar(id: String)
}

/** «Repository» Contenido académico (apuntes y anotaciones) y carpetas libres. */
interface ContenidoRepository {
    fun guardar(c: ContenidoAcademico)
    fun listarSinClasificar(): List<ContenidoAcademico>

    fun listarTodos(): List<ContenidoAcademico>
    fun buscarPorId(id: String): ContenidoAcademico?
    fun listarEnPapelera(): List<ContenidoAcademico>
    fun eliminar(id: String)
    /** Carpetas libres: las que creó el estudiante y no pertenecen a un curso. */
    fun guardarCarpeta(c: Carpeta)
    fun listarCarpetas(): List<Carpeta>
}

/** «Repository» Planes de estudio. */
interface PlanRepository {
    fun guardar(p: PlanEstudio)
    fun obtenerVigente(e: Estudiante): PlanEstudio?
}

/**
 * «Repository» Estudiante. AGREGADO respecto al diagrama: hacía falta guardar el horario
 * (franjas) del estudiante, que la planificación necesita, y ninguno de los otros
 * repositorios era el lugar correcto.
 */
interface EstudianteRepository {
    fun obtener(): Estudiante?
    fun guardar(e: Estudiante)
}
