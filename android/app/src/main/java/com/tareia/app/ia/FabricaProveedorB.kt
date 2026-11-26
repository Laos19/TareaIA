package com.tareia.app.ia

import android.content.Context
import com.tareia.app.dominio.ActividadAcademica
import com.tareia.app.dominio.ActividadBuilder
import com.tareia.app.dominio.Curso
import com.tareia.app.dominio.FranjaHoraria
import com.tareia.app.dominio.SesionEstudio

/**
 * «ConcreteFactory» PATRÓN ABSTRACT FACTORY — Proveedor B: API compatible con OpenAI
 * (endpoint, modelo y API key que el estudiante configura en Más > IA).
 *
 * Crea la familia de servicios del Proveedor B:
 *   crearReconocedorOCR() -> ReconocedorOCRMLKit                   (OCR del celular, ver nota)
 *   crearExtractor()      -> ExtractorParametrosCompatibleOpenAI
 *   crearGeneradorPlan()  -> GeneradorPlanCompatibleOpenAI
 *
 * Nota: las APIs compatibles con OpenAI no tienen un servicio de OCR estándar, por eso
 * esta familia reutiliza el OCR del dispositivo.
 */
class FabricaProveedorB(private val context: Context, private val config: AiConfig) : FabricaServiciosIA {
    override val nombreProveedor = "Compatible con OpenAI (${config.model})"
    override fun crearReconocedorOCR(): ReconocedorOCR = ReconocedorOCRMLKit(context)
    override fun crearExtractor(): ExtractorParametros = ExtractorParametrosCompatibleOpenAI(config)
    override fun crearGeneradorPlan(): GeneradorPlanIA = GeneradorPlanCompatibleOpenAI(config)
}

/** «ConcreteProduct» Extractor del Proveedor B. */
class ExtractorParametrosCompatibleOpenAI(private val config: AiConfig) : ExtractorParametros {
    override suspend fun extraer(texto: String, cursos: List<Curso>): List<ActividadBuilder> {
        require(texto.isNotBlank()) { "No existe texto para analizar." }
        val respuesta = ClienteCompatibleOpenAI.completar(
            config,
            sistema = "Extrae actividades académicas y responde solo JSON.",
            usuario = ComunIA.promptExtraccion(texto, cursos)
        )
        return ComunIA.leerExtraccion(respuesta, texto, cursos)
    }
}

/** «ConcreteProduct» Generador de plan del Proveedor B (es el antiguo AiClient.plan()). */
class GeneradorPlanCompatibleOpenAI(private val config: AiConfig) : GeneradorPlanIA {
    override suspend fun generar(acts: List<ActividadAcademica>, franjas: List<FranjaHoraria>): List<SesionEstudio> {
        val respuesta = ClienteCompatibleOpenAI.completar(config, ComunIA.INSTRUCCIONES_PLAN, ComunIA.promptPlan(acts, franjas))
        return ComunIA.leerPlan(respuesta, acts, franjas)
    }
}
