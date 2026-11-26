package com.tareia.app.ia

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import com.tareia.app.dominio.ActividadAcademica
import com.tareia.app.dominio.ActividadBuilder
import com.tareia.app.dominio.Curso
import com.tareia.app.dominio.FranjaHoraria
import com.tareia.app.dominio.SesionEstudio

// Proveedor A: Google Gemini
class FabricaProveedorA(private val context: Context) : FabricaServiciosIA {
    override val nombreProveedor = "Gemini (Google)"
    override fun crearReconocedorOCR(): ReconocedorOCR = ReconocedorOCRMLKit(context)
    override fun crearExtractor(): ExtractorParametros = ExtractorParametrosGemini()
    override fun crearGeneradorPlan(): GeneradorPlanIA = GeneradorPlanGemini()
}

// modelo
private const val MODELO_GEMINI = "gemini-3.5-flash-lite"

// ConcreteProduct
class ExtractorParametrosGemini : ExtractorParametros {

    private val modelo by lazy {
        val esquema = Schema.obj(
            mapOf(
                "items" to Schema.array(
                    Schema.obj(
                        mapOf(
                            "title" to Schema.string(),
                            "course" to Schema.string(),
                            "dueDate" to Schema.string()
                        )
                    )
                )
            )
        )
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = MODELO_GEMINI,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = esquema
            }
        )
    }

    override suspend fun extraer(texto: String, cursos: List<Curso>): List<ActividadBuilder> {
        require(texto.isNotBlank()) { "No existe texto para analizar." }
        val respuesta = modelo.generateContent(ComunIA.promptExtraccion(texto, cursos)).text
            ?.takeIf { it.isNotBlank() } ?: throw IllegalStateException("Gemini no devolvió actividades.")
               //    builder
        return ComunIA.leerExtraccion(respuesta, texto, cursos)
    }
}

// ConcreteProduct, Generador de plan del Proveedor A
class GeneradorPlanGemini : GeneradorPlanIA {

    private val modelo by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = MODELO_GEMINI,
            generationConfig = generationConfig { responseMimeType = "application/json" }
        )
    }

    override suspend fun generar(acts: List<ActividadAcademica>, franjas: List<FranjaHoraria>): List<SesionEstudio> {
        val prompt = ComunIA.INSTRUCCIONES_PLAN + "\n\nDATOS:\n" + ComunIA.promptPlan(acts, franjas)
        val respuesta = modelo.generateContent(prompt).text.orEmpty()
        if (respuesta.isBlank()) return emptyList()
        return ComunIA.leerPlan(respuesta, acts, franjas)
    }
}
