package com.tareia.app.ingesta

import android.net.Uri
import com.tareia.app.dominio.Apunte
import com.tareia.app.dominio.ContenidoAcademico
import com.tareia.app.dominio.TipoContenido
import java.util.Locale

// =====================================================================================
// PATRÓN FACTORY METHOD (creacional)
// -------------------------------------------------------------------------------------
// Problema: el contenido entra por tres vías (archivo PDF, imagen JPG/PNG y contenido
// compartido desde otra app con "Compartir"). Las tres siguen los MISMOS pasos:
// validar el formato (RN10) -> crear el contenido -> devolverlo. Solo cambia CÓMO se crea.
//
// Solución: la clase base define los pasos fijos en importar() y deja un método
// abstracto, crearContenido() (el "método fábrica"), que cada subclase implementa.
// GestorIngesta solo llama a importar(); no sabe qué subclase está trabajando.
// Para agregar un formato nuevo basta con una subclase nueva (principio Abierto/Cerrado).
//
// Roles:
//   Creator          -> ImportadorContenido (esta clase abstracta)
//   ConcreteCreator  -> ImportadorPDF, ImportadorImagen, ReceptorCompartido
//   Product          -> ContenidoAcademico (Apunte)
//   Cliente          -> GestorIngesta
// =====================================================================================

/** «FactoryMethod / Creator» Pasos comunes para importar contenido. */
abstract class ImportadorContenido {

    /**
     * Pasos fijos (no se sobrescriben):
     *   1. valida el formato (RN10),
     *   2. llama al MÉTODO FÁBRICA crearContenido(),
     *   3. recuerda dónde está el archivo para que luego se le aplique OCR.
     * El texto todavía está vacío: lo llena GestorAnalisisIA con el ReconocedorOCR.
     */
    fun importar(origen: Uri, tipoMime: String, titulo: String): ContenidoAcademico {
        require(validarFormato(origen, tipoMime)) {
            "Formato no permitido. Solo se aceptan archivos PDF, JPG/JPEG o PNG (RN10)."
        }
        val contenido = crearContenido(origen, tipoMime, titulo.ifBlank { "Apunte importado" })
        contenido.ubicacionOrigen = origen.toString()
        return contenido
    }

    /** Paso que cada subclase puede ajustar: ¿este importador acepta el archivo? */
    protected abstract fun validarFormato(origen: Uri, tipoMime: String): Boolean

    /** MÉTODO FÁBRICA: cada subclase decide qué contenido concreto crear. */
    protected abstract fun crearContenido(origen: Uri, tipoMime: String, titulo: String): ContenidoAcademico

    companion object {
        /** RN10: tipos permitidos. */
        val MIME_PDF = setOf("application/pdf")
        val MIME_IMAGEN = setOf("image/jpeg", "image/jpg", "image/png")

        fun normalizar(mime: String) = mime.lowercase(Locale.ROOT).trim()
    }
}

/** «ConcreteCreator» Archivos PDF. */
class ImportadorPDF : ImportadorContenido() {
    override fun validarFormato(origen: Uri, tipoMime: String) =
        normalizar(tipoMime) in MIME_PDF || origen.toString().lowercase(Locale.ROOT).endsWith(".pdf")

    override fun crearContenido(origen: Uri, tipoMime: String, titulo: String): ContenidoAcademico =
        Apunte(titulo = titulo, tipo = TipoContenido.PDF)
}

/** «ConcreteCreator» Imágenes JPG o PNG (archivo elegido o foto tomada con la cámara). */
class ImportadorImagen : ImportadorContenido() {
    override fun validarFormato(origen: Uri, tipoMime: String) = normalizar(tipoMime) in MIME_IMAGEN

    override fun crearContenido(origen: Uri, tipoMime: String, titulo: String): ContenidoAcademico =
        Apunte(titulo = titulo, tipo = TipoContenido.IMAGEN)
}

/**
 * «ConcreteCreator» Contenido compartido desde otra app con el botón "Compartir" (RF-15).
 * RN11: lo compartido entra al MISMO flujo de análisis y validación que lo importado.
 * Acepta PDF, imágenes JPG/PNG y texto plano (el texto no necesita OCR).
 */
class ReceptorCompartido : ImportadorContenido() {
    override fun validarFormato(origen: Uri, tipoMime: String): Boolean {
        val mime = normalizar(tipoMime)
        return mime in MIME_PDF || mime in MIME_IMAGEN || mime == "text/plain"
    }

    override fun crearContenido(origen: Uri, tipoMime: String, titulo: String): ContenidoAcademico {
        val mime = normalizar(tipoMime)
        val tipo = when {
            mime in MIME_PDF -> TipoContenido.PDF
            mime in MIME_IMAGEN -> TipoContenido.IMAGEN
            else -> TipoContenido.TEXTO
        }
        return Apunte(titulo = titulo, tipo = tipo)
    }

    /** Texto compartido (sin archivo): ya viene escrito, así que no pasa por OCR. */
    fun desdeTexto(texto: String, titulo: String): ContenidoAcademico =
        Apunte(titulo = titulo.ifBlank { "Texto compartido" }, tipo = TipoContenido.TEXTO, textoReconocido = texto)
}
