package com.tareia.app.aplicacion

import com.tareia.app.dominio.ActividadBuilder
import com.tareia.app.dominio.FechaLimite
import com.tareia.app.ingesta.CambioFechaAula
import com.tareia.app.ingesta.EventoAula
import com.tareia.app.ingesta.LectorIcs
import com.tareia.app.ingesta.PlanSincronizacionAula
import java.time.LocalDate
import java.time.ZoneId

/** Resultado de una sincronización con el calendario del aula virtual. */
data class ResultadoAula(
    val leidos: Int,
    val nuevas: Int,
    val cambiosDeFecha: List<CambioFechaAula>,
    val pasados: Int,
)

/**
 * Sincroniza las entregas del aula virtual (Moodle) a partir de su calendario exportado (.ics).
 *
 * - Cada entrega nueva entra con el Builder por el camino de integración: queda PENDIENTE DE VALIDAR (RN09)
 *   y no se duplica porque guarda el UID del evento como id externo (RN02).
 * - Si el docente cambia una fecha, se propone el cambio; el estudiante decide si aplicarlo.
 */
class GestorAulaVirtual(private val actividades: GestorActividades) {

    /** Procesa el texto del calendario descargado. */
    fun sincronizar(textoIcs: String, zona: ZoneId = ZoneId.of("America/Lima"), hoy: LocalDate = LocalDate.now(zona)): ResultadoAula {
        val eventos = LectorIcs.leer(textoIcs, zona)
        val existentes = actividades.listar()
            .filter { it.idExterno.startsWith("aula:") }
            .associate { it.idExterno to it.fechaLimite?.valor }
        val plan = PlanSincronizacionAula.calcular(eventos, existentes, hoy)
        val nuevas = actividades.importarDeIntegracion(plan.nuevos.mapNotNull { aBuilder(it) })
        return ResultadoAula(eventos.size, nuevas, plan.cambiosDeFecha, plan.pasados)
    }

    private fun aBuilder(e: EventoAula): ActividadBuilder? = runCatching {
        ActividadBuilder()
            .conTitulo(e.titulo)
            .conFechaLimite(FechaLimite(e.fecha))
            .conCurso(e.curso?.let { actividades.resolverCurso(it) })
            .conDescripcion(e.descripcion)
            .desdeIntegracion(e.idExterno)
    }.getOrNull()

    /** Aplica un cambio de fecha que el estudiante aceptó (sin validar la entrega). False si ya no existe o quedaría duplicada. */
    fun aplicarCambio(cambio: CambioFechaAula): Boolean {
        val a = actividades.listar().firstOrNull { it.idExterno == cambio.idExterno } ?: return false
        return actividades.cambiarFecha(a, FechaLimite(cambio.fechaNueva))
    }
}
