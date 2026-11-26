package com.tareia.app.ia

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tareia.app.dominio.ContenidoAcademico
import com.tareia.app.dominio.TipoContenido
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

// ConcreteProduct de ReconocedorOCR, OCR de Google ML Kit

// Lo crean las 2 fábricas: las APIs compatibles con OpenAI no ofrecen un OCR estándar,
//así que el Proveedor B también usa el OCR del dispositivo (además funciona sin internet).

class ReconocedorOCRMLKit(private val context: Context) : ReconocedorOCR {

    override suspend fun reconocer(c: ContenidoAcademico): String = withContext(Dispatchers.IO) {
        val ubicacion = c.ubicacionOrigen
        // Texto compartido desde otra app: ya es texto, no hace falta OCR.
        if (ubicacion == null) return@withContext c.texto()
        val uri = Uri.parse(ubicacion)
        val reconocedor = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            when (c.tipo) {
                TipoContenido.PDF -> reconocerPdf(uri, reconocedor)
                TipoContenido.IMAGEN -> Tasks.await(reconocedor.process(InputImage.fromFilePath(context, uri))).text
                TipoContenido.TEXTO -> leerTexto(uri)
            }
        } finally {
            reconocedor.close()
        }
    }

    private fun leerTexto(uri: Uri): String =
        context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { lector ->
            val texto = lector.readText()
            require(texto.length <= 1_000_000) { "El texto supera el tamaño permitido para esta importación." }
            texto
        }.orEmpty()

    /** Convierte cada página del PDF en imagen y le aplica OCR (máximo 30 páginas). */
    private fun reconocerPdf(uri: Uri, reconocedor: TextRecognizer): String {
        val descriptor = context.contentResolver.openFileDescriptor(uri, "r")
            ?: throw IllegalArgumentException("No se pudo abrir el PDF seleccionado.")
        descriptor.use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                require(renderer.pageCount <= 30) { "El PDF tiene más de 30 páginas. Divide el documento e inténtalo de nuevo." }
                val salida = StringBuilder()
                for (indice in 0 until renderer.pageCount) {
                    renderer.openPage(indice).use { pagina ->
                        val ancho = (pagina.width * 1.6f).toInt().coerceAtLeast(800)
                        val alto = (pagina.height * 1.6f).toInt().coerceAtLeast(1000)
                        val bitmap = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
                        val matriz = Matrix().apply { postScale(ancho.toFloat() / pagina.width, alto.toFloat() / pagina.height) }
                        pagina.render(bitmap, null, matriz, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        try {
                            salida.append("\n[Página ${indice + 1}]\n")
                            salida.append(Tasks.await(reconocedor.process(InputImage.fromBitmap(bitmap, 0))).text)
                        } finally {
                            bitmap.recycle()
                        }
                    }
                }
                return salida.toString()
            }
        }
    }

    companion object {
        /** Ayuda para deducir el tipo de contenido por el tipo MIME del archivo. */
        fun tipoDesdeMime(mime: String): TipoContenido = when {
            mime.lowercase(Locale.ROOT) == "application/pdf" -> TipoContenido.PDF
            mime.lowercase(Locale.ROOT).startsWith("image/") -> TipoContenido.IMAGEN
            else -> TipoContenido.TEXTO
        }
    }
}
