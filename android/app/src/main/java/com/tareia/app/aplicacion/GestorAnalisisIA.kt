package com.tareia.app.aplicacion

import com.tareia.app.dominio.ActividadBuilder
import com.tareia.app.dominio.ContenidoAcademico
import com.tareia.app.dominio.CursoRepository
import com.tareia.app.ia.ExtractorLocal
import com.tareia.app.ia.FabricaServiciosIA
class GestorAnalisisIA(
    private val fabrica: FabricaServiciosIA,
    private val cursos: CursoRepository,
    private val extractorSinIA: ExtractorLocal = ExtractorLocal()
) {
    /** Nombre del proveedor de IA en uso (para la pantalla). */
    val nombreProveedor: String get() = fabrica.nombreProveedor

    /** RF-03: reconoce el texto del contenido con el OCR de la familia elegida. */
    suspend fun procesarOCR(c: ContenidoAcademico): String {
        val ocr = fabrica.crearReconocedorOCR()      // le pide el producto a la fábrica
        return ocr.reconocer(c)                      // se usa el producto
    }

    /** RF-04: la IA detecta tareas, fechas y cursos. Devuelve propuestas para validar. */
    suspend fun extraerParametros(c: ContenidoAcademico): List<ActividadBuilder> {
        val extractor = fabrica.crearExtractor()
        return extractor.extraer(c.texto(), cursos.listarActivos())
    }

    /** Sugerencias sin IA (regex): instantáneas y sin internet (respaldo, RNF-04). */
    fun sugerirSinIA(texto: String): List<ActividadBuilder> =
        if (texto.isBlank()) emptyList() else extractorSinIA.extraerAhora(texto, cursos.listarActivos())
}
