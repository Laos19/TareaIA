package com.tareia.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.time.LocalDate
import java.util.Locale

object DocumentProcessor {
    fun extractBitmap(bitmap: Bitmap): String {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try { Tasks.await(recognizer.process(InputImage.fromBitmap(bitmap, 0))).text }
        finally { recognizer.close() }
    }

    fun extractText(context: Context, uri: Uri): String {
        val mime = context.contentResolver.getType(uri).orEmpty().lowercase(Locale.ROOT)
        if (mime == "text/plain" || mime == "text/csv") {
            return context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { reader ->
                val text = reader.readText()
                require(text.length <= 1_000_000) { "El texto supera el tamaño permitido para esta importación." }
                text
            }.orEmpty()
        }
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            if (mime == "application/pdf" || uri.toString().lowercase(Locale.ROOT).endsWith(".pdf")) {
                return recognizePdf(context, uri, recognizer)
            }
            val image = InputImage.fromFilePath(context, uri)
            return Tasks.await(recognizer.process(image)).text
        } finally {
            recognizer.close()
        }
    }

    private fun recognizePdf(context: Context, uri: Uri, recognizer: com.google.mlkit.vision.text.TextRecognizer): String {
        val descriptor: ParcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
            ?: throw IllegalArgumentException("No se pudo abrir el PDF seleccionado.")
        descriptor.use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                require(renderer.pageCount <= 30) { "El PDF tiene más de 30 páginas. Divide el documento e inténtalo de nuevo." }
                val output = StringBuilder()
                for (index in 0 until renderer.pageCount) {
                    renderer.openPage(index).use { page ->
                        val width = (page.width * 1.6f).toInt().coerceAtLeast(800)
                        val height = (page.height * 1.6f).toInt().coerceAtLeast(1000)
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        val matrix = Matrix().apply { postScale(width.toFloat() / page.width, height.toFloat() / page.height) }
                        page.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        try {
                            output.append("\n[Página ${index + 1}]\n")
                            output.append(Tasks.await(recognizer.process(InputImage.fromBitmap(bitmap, 0))).text)
                        } finally {
                            bitmap.recycle()
                        }
                    }
                }
                return output.toString()
            }
        }
    }
}

object TaskExtractor {
    private val numericDate = Regex("\\b(\\d{1,2})[/-](\\d{1,2})[/-](\\d{4})\\b")
    private val isoDate = Regex("\\b(\\d{4})-(\\d{2})-(\\d{2})\\b")
    private val spanishDate = Regex("(?i)\\b(\\d{1,2})\\s+de\\s+(enero|febrero|marzo|abril|mayo|junio|julio|agosto|septiembre|setiembre|octubre|noviembre|diciembre)(?:\\s+de\\s+(\\d{4}))?\\b")
    private val months = mapOf("enero" to 1, "febrero" to 2, "marzo" to 3, "abril" to 4, "mayo" to 5, "junio" to 6, "julio" to 7, "agosto" to 8, "septiembre" to 9, "setiembre" to 9, "octubre" to 10, "noviembre" to 11, "diciembre" to 12)

    fun extract(text: String, courses: List<CourseItem>, source: String): List<TaskDraft> {
        val lines = text.lineSequence().map { it.trim().trimStart('-', '•', '*', '·', ' ') }
            .filter { it.length >= 8 }.distinct().toList()
        val candidates = lines.filter { line ->
            val l = line.lowercase(Locale.ROOT)
            (numericDate.containsMatchIn(line) || isoDate.containsMatchIn(line) || spanishDate.containsMatchIn(line) ||
                listOf("entrega", "tarea", "examen", "proyecto", "informe", "evaluación", "fecha límite", "vence").any(l::contains))
        }.ifEmpty { lines.take(1) }.take(10)
        return candidates.map { line ->
            val date = parseDate(line)
            val course = courses.firstOrNull { c ->
                line.contains(c.name, true) || (c.abbreviation.isNotBlank() && line.contains(c.abbreviation, true))
            }
            val cleaned = line.replace(numericDate, " ").replace(isoDate, " ").replace(spanishDate, " ")
                .replace(Regex("(?i)\\b(entregar|entrega|vence|fecha límite|fecha de entrega|tarea|examen|proyecto|evaluación)\\b"), " ")
                .replace(Regex("\\s+"), " ").trim(' ', ':', ',', '.', ';', '-')
            TaskDraft(
                title = cleaned.take(120).ifBlank { line.take(120) }, course = course?.name.orEmpty(),
                dueDate = date, source = source, description = line
            )
        }
    }

    private fun parseDate(text: String): String {
        runCatching {
            isoDate.find(text)?.let { m -> return LocalDate.of(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt()).toString() }
            numericDate.find(text)?.let { m -> return LocalDate.of(m.groupValues[3].toInt(), m.groupValues[2].toInt(), m.groupValues[1].toInt()).toString() }
            spanishDate.find(text)?.let { m ->
                val month = months[m.groupValues[2].lowercase(Locale.ROOT)] ?: return ""
                val year = m.groupValues[3].toIntOrNull() ?: LocalDate.now().year
                return LocalDate.of(year, month, m.groupValues[1].toInt()).toString()
            }
        }
        return ""
    }
}
