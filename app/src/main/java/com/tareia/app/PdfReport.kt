package com.tareia.app

import android.content.Context
import android.net.Uri
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object PdfReport {
    fun write(context: Context, destination: Uri, tasks: List<TaskItem>) {
        require(tasks.isNotEmpty()) { "No hay tareas para incluir en el reporte." }
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 42f
        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(48, 54, 58); textSize = 11f }
        val muted = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(104, 113, 119); textSize = 9f }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(48, 54, 58); textSize = 22f; typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL) }
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(89, 102, 108); strokeWidth = 1.2f }
        val pages = tasks.chunked(7)
        pages.forEachIndexed { pageIndex, pageTasks ->
            val info = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
            val page = document.startPage(info)
            val canvas = page.canvas
            canvas.drawColor(android.graphics.Color.WHITE)
            var y = 56f
            canvas.drawText("TARE·IA", margin, y, muted)
            y += 34f
            canvas.drawText("Reporte de actividades", margin, y, titlePaint)
            y += 22f
            canvas.drawText("Generado: ${LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("es", "PE")))}", margin, y, muted)
            y += 16f
            canvas.drawText("Total de actividades: ${tasks.size}", margin, y, muted)
            y += 18f
            canvas.drawLine(margin, y, pageWidth - margin, y, headerPaint)
            y += 24f
            pageTasks.forEachIndexed { itemIndex, task ->
                val globalIndex = pageIndex * 7 + itemIndex + 1
                body.textSize = 12f
                canvas.drawText("$globalIndex. ${ellipsize(task.title, body, pageWidth - margin * 2 - 18)}", margin, y, body)
                y += 18f
                body.textSize = 10f
                canvas.drawText("Curso: ${ellipsize(task.course.ifBlank { "Sin curso" }, body, pageWidth - margin * 2 - 10)}", margin + 12, y, body)
                y += 15f
                canvas.drawText("Entrega: ${task.dueDate.ifBlank { "Sin fecha" }}     Estado: ${task.status}", margin + 12, y, body)
                y += 15f
                canvas.drawText("Origen: ${task.source}", margin + 12, y, muted)
                y += 18f
                canvas.drawLine(margin, y, pageWidth - margin, y, headerPaint.apply { alpha = 70 })
                headerPaint.alpha = 255
                y += 18f
            }
            val footer = "TARE·IA  ·  Página ${pageIndex + 1} de ${pages.size}"
            canvas.drawText(footer, margin, pageHeight - 28f, muted)
            document.finishPage(page)
        }
        try {
            val stream = context.contentResolver.openOutputStream(destination)
                ?: throw IllegalStateException("No se pudo abrir el destino seleccionado.")
            stream.use { document.writeTo(it) }
        } finally {
            document.close()
        }
    }

    private fun ellipsize(value: String, paint: Paint, maxWidth: Float): String =
        android.text.TextUtils.ellipsize(value, android.text.TextPaint(paint), maxWidth, android.text.TextUtils.TruncateAt.END).toString()
}
