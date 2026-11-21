package com.tareia.app.ia

import com.tareia.app.dominio.ActividadAcademica
import com.tareia.app.dominio.ActividadBuilder
import com.tareia.app.dominio.ContenidoAcademico
import com.tareia.app.dominio.Curso
import com.tareia.app.dominio.FranjaHoraria
import com.tareia.app.dominio.SesionEstudio

//  PRODUCTOS ABSTRACTOS


interface ReconocedorOCR {
    suspend fun reconocer(c: ContenidoAcademico): String
}

// Devuelve BUILDERS y no actividades terminadas pq la IA puede no encontrar todos los datos
// el estudiante revisa la propuesta antes de que se construya la actividad

interface ExtractorParametros {
    suspend fun extraer(texto: String, cursos: List<Curso>): List<ActividadBuilder>
}

// Propone sesiones de estudio con IA
interface GeneradorPlanIA {
    suspend fun generar(acts: List<ActividadAcademica>, franjas: List<FranjaHoraria>): List<SesionEstudio>
}
