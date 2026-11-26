package com.tareia.app.ia

import com.tareia.app.dominio.ActividadAcademica
import com.tareia.app.dominio.Curso
import com.tareia.app.dominio.EstadoPlan
import com.tareia.app.dominio.FranjaHoraria
import com.tareia.app.dominio.PlanEstudio
import com.tareia.app.dominio.SesionEstudio
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit


// INTERFAZ
interface EstrategiaPlanificacion {
    val nombre: String

    // Devuelve un PlanEstudio en estado PROPUESTA con las sesiones de las actividades
    suspend fun planificar(
        acts: List<ActividadAcademica>,
        cursos: List<Curso>,
        franjas: List<FranjaHoraria>,
        ocupadas: List<SesionEstudio> = emptyList()
    ): PlanEstudio
}

// ConcreteStrategy: planificación con IA. Usa el GeneradorPlanIA que creó la fábrica

class PlanificacionConIA(private val generador: GeneradorPlanIA) : EstrategiaPlanificacion {
    override val nombre = "Con IA"

    override suspend fun planificar(
        acts: List<ActividadAcademica>, cursos: List<Curso>,
        franjas: List<FranjaHoraria>, ocupadas: List<SesionEstudio>
    ): PlanEstudio {
        val propuestas = generador.generar(acts, franjas)
        val aceptadas = mutableListOf<SesionEstudio>()
        propuestas.forEach { s ->
            if (ocupadas.none { it.seCruzaCon(s) } && aceptadas.none { it.seCruzaCon(s) }) aceptadas += s
        }
        return PlanEstudio(estado = EstadoPlan.PROPUESTA, sesiones = aceptadas)
    }
}

// ConcreteStrategy: planificación local, sin internet
class PlanificacionLocalPorPrioridad : EstrategiaPlanificacion {
    override val nombre = "Local por prioridad"

    override suspend fun planificar(
        acts: List<ActividadAcademica>, cursos: List<Curso>,
        franjas: List<FranjaHoraria>, ocupadas: List<SesionEstudio>
    ): PlanEstudio {
        val generadas = mutableListOf<SesionEstudio>()
        // Primero las actividades de los cursos con mayor prioridad (RN04).
        acts.sortedByDescending { it.curso?.prioridad?.nivel ?: 3 }.forEach { act ->
            generadas += sesionesPara(act, franjas, ocupadas + generadas)
        }
        return PlanEstudio(estado = EstadoPlan.PROPUESTA, sesiones = generadas)
    }

    private fun sesionesPara(act: ActividadAcademica, franjas: List<FranjaHoraria>, ocupadas: List<SesionEstudio>): List<SesionEstudio> {
        val entrega = act.fechaLimite?.valor ?: return emptyList()
        val hoy = LocalDate.now()
        if (entrega.isBefore(hoy)) return emptyList()
        val prioridad = act.curso?.prioridad?.nivel ?: 3
        val dificultad = act.curso?.dificultad ?: 3
        val cantidad = (1 + (dificultad + prioridad) / 2).coerceIn(2, 6)
        val dias = generateSequence(entrega.minusDays(1)) { it.minusDays(1) }
            .takeWhile { !it.isBefore(hoy) }.toList().reversed()
        val resultado = mutableListOf<SesionEstudio>()
        val ordenadas = franjas.sortedWith(compareBy<FranjaHoraria> { it.dia }.thenBy { it.inicio })

        for (dia in dias) {
            if (resultado.size >= cantidad) break
            for (franja in ordenadas.filter { it.dia == dia.dayOfWeek }) {
                var inicio = dia.atTime(franja.inicio)
                val finFranja = dia.atTime(franja.fin)
                while (!inicio.plusMinutes(60).isAfter(finFranja) && resultado.size < cantidad) {
                    val candidata = SesionEstudio(idActividad = act.id, tituloActividad = act.titulo, inicio = inicio, fin = inicio.plusMinutes(60))
                    if ((ocupadas + resultado).none { it.seCruzaCon(candidata) }) resultado += candidata
                    inicio = inicio.plusMinutes(60)
                }
            }
        }
        // Sin horario registrado: se reparten las sesiones a las 18:00 entre hoy y la entrega.
        if (resultado.size < cantidad && franjas.isEmpty()) {
            val margen = ChronoUnit.DAYS.between(hoy, entrega).coerceAtLeast(0)
            for (i in resultado.size until cantidad) {
                val dia = hoy.plusDays((margen * (i + 1) / (cantidad + 1)).coerceAtMost(margen))
                val inicio = dia.atTime(LocalTime.of(18, 0))
                resultado += SesionEstudio(idActividad = act.id, tituloActividad = act.titulo, inicio = inicio, fin = inicio.plusMinutes(60))
            }
        }
        return resultado
    }
}
