package com.tareia.app.aplicacion

import android.content.Context
import com.tareia.app.dominio.ClasificadorContenido
import com.tareia.app.dominio.DetectorDuplicados
import com.tareia.app.ia.EstrategiaPlanificacion
import com.tareia.app.ia.FabricaServiciosIA
import com.tareia.app.ia.PlanificacionConIA
import com.tareia.app.ia.PlanificacionLocalPorPrioridad
import com.tareia.app.infraestructura.AlarmManagerNotificaciones
import com.tareia.app.infraestructura.AppDatabase
import com.tareia.app.infraestructura.GoogleCalendarAdapter
import com.tareia.app.infraestructura.LocalActividadRepository
import com.tareia.app.infraestructura.LocalContenidoRepository
import com.tareia.app.infraestructura.LocalCursoRepository
import com.tareia.app.infraestructura.LocalEstudianteRepository
import com.tareia.app.infraestructura.LocalPlanRepository

class Dependencias(context: Context) {

    private val appContext = context.applicationContext

    // PIDE LA BASE
    private val db = AppDatabase.getInstance(appContext)

    // REPOSITORY: implementaciones locales detrás de las interfaces del dominio.
    private val cursoRepo = LocalCursoRepository(db)
    private val actividadRepo = LocalActividadRepository(db, cursoRepo)
    private val contenidoRepo = LocalContenidoRepository(db, cursoRepo)
    private val planRepo = LocalPlanRepository(db)
    private val estudianteRepo = LocalEstudianteRepository(db)

    // ABSTRACT FACTORY, la fábrica se elige UNA vez
    var fabricaIA: FabricaServiciosIA = FabricaServiciosIA.seleccionar(appContext)
        private set

    // Gestores
    val gestorAlertas = GestorAlertas(AlarmManagerNotificaciones(appContext))
    val gestorCursos = GestorCursos(cursoRepo)
    val gestorActividades = GestorActividades(actividadRepo, DetectorDuplicados(actividadRepo), gestorAlertas, gestorCursos)
    val gestorIngesta = GestorIngesta(appContext, contenidoRepo, cursoRepo, ClasificadorContenido())
    var gestorAnalisisIA = GestorAnalisisIA(fabricaIA, cursoRepo)
        private set

    // planificación local STRATEGY
    val gestorPlanificacion = GestorPlanificacion(PlanificacionLocalPorPrioridad(), planRepo, cursoRepo, estudianteRepo)
    val gestorPapelera = GestorPapelera(actividadRepo, cursoRepo, contenidoRepo, gestorAlertas, gestorPlanificacion)

    // Estrategias disponibles para que el estudiante elija
    fun estrategiaConIA(): EstrategiaPlanificacion = PlanificacionConIA(fabricaIA.crearGeneradorPlan())
    fun estrategiaLocal(): EstrategiaPlanificacion = PlanificacionLocalPorPrioridad()


    fun gestorSincronizacion(token: String, uid: String, correo: String) =
        GestorSincronizacion(GoogleCalendarAdapter(token, uid, correo), actividadRepo)

    // Si el estudiante cambia el proveedor se vuelve a elegir la fábrica
    fun recargarFabricaIA() {
        fabricaIA = FabricaServiciosIA.seleccionar(appContext)
        gestorAnalisisIA = GestorAnalisisIA(fabricaIA, cursoRepo)
    }

    // Pasa los datos de la versión anterior de la app al nuevo modelo
    fun migrarDatosAnteriores() = MigracionDatos(appContext, db, this).ejecutar()
}
