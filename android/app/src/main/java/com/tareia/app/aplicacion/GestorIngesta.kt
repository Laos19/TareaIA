package com.tareia.app.aplicacion

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.tareia.app.dominio.*
import com.tareia.app.ingesta.ImportadorContenido
import com.tareia.app.ingesta.ImportadorImagen
import com.tareia.app.ingesta.ImportadorPDF
import com.tareia.app.ingesta.ReceptorCompartido
import java.io.File
import java.io.FileOutputStream

/**
 * GestorIngesta (RF-15, RF-22, RF-23): recibe el contenido que entra a la app y lo guarda.
 *
 * Es el CLIENTE del patrón Factory Method: elige el importador según el tipo de archivo
 * y solo llama a importar(); no sabe cómo crea el contenido cada importador.
 * Al guardar un apunte usa ClasificadorContenido para ubicarlo en la carpeta de su curso (RN13).
 */
class GestorIngesta(
    private val context: Context,
    private val contenidos: ContenidoRepository,
    private val cursos: CursoRepository,
    private val clasificador: ClasificadorContenido
) {
    private val importadorPDF = ImportadorPDF()
    private val importadorImagen = ImportadorImagen()
    private val receptorCompartido = ReceptorCompartido()

    /** Archivo elegido con "Elegir archivo" (RF-22). */
    fun importarArchivo(uri: Uri, titulo: String): ContenidoAcademico {
        val mime = tipoMime(uri)
        // Se elige el importador; el resto del trabajo lo hace el patrón Factory Method.
        val importador: ImportadorContenido = when {
            mime in ImportadorContenido.MIME_PDF || uri.toString().endsWith(".pdf", true) -> importadorPDF
            else -> importadorImagen   // si no es JPG/PNG, su validarFormato() lo rechaza (RN10)
        }
        return importador.importar(uri, mime, titulo)
    }

    /** Foto tomada con la cámara: se guarda como PNG temporal y se importa como imagen. */
    fun importarFoto(foto: Bitmap): ContenidoAcademico {
        val archivo = File(context.cacheDir, "foto_apunte_${System.currentTimeMillis()}.png")
        FileOutputStream(archivo).use { foto.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return importadorImagen.importar(Uri.fromFile(archivo), "image/png", "Apunte fotografiado")
    }

    /**
     * Contenido compartido desde otra app (RF-15). El diagrama recibe un Intent; aquí se
     * recibe lo que MainActivity ya leyó de ese Intent (texto o archivo).
     */
    fun recibirCompartido(texto: String?, uri: Uri?): ContenidoAcademico {
        if (uri != null) return receptorCompartido.importar(uri, tipoMime(uri), "Contenido compartido")
        return receptorCompartido.desdeTexto(texto.orEmpty(), "Texto compartido")
    }

    // ------------------------- Guardar y organizar contenido -------------------------

    /**
     * Guarda un apunte importado (o lo actualiza si ya existía) y lo clasifica (RN13).
     * Devuelve el apunte guardado.
     */
    fun guardarApunte(idExistente: String?, titulo: String, texto: String, tipo: TipoContenido): ContenidoAcademico {
        require(titulo.isNotBlank() && texto.isNotBlank()) { "Escribe un título y agrega texto antes de guardar el apunte." }
        val existente = idExistente?.let { contenidos.buscarPorId(it) }
        val apunte = existente ?: Apunte(titulo = titulo.trim(), tipo = tipo, textoReconocido = texto)
        apunte.titulo = titulo.trim()
        apunte.actualizarTexto(texto)
        if (apunte.estaSinClasificar()) apunte.asignarCarpeta(clasificador.clasificar(apunte, cursos.listarActivos()))
        contenidos.guardar(apunte)
        return apunte
    }

    /** Nota escrita por el estudiante (RF-23) o edición/clasificación manual de un contenido. */
    fun guardarAnotacion(id: String?, titulo: String, texto: String, idCurso: String?, nombreCarpeta: String): ContenidoAcademico {
        val existente = id?.let { contenidos.buscarPorId(it) }
        val contenido = existente ?: Anotacion(titulo = titulo.trim(), contenido = texto)
        contenido.titulo = titulo.trim()
        contenido.actualizarTexto(texto)
        contenido.asignarCarpeta(resolverCarpeta(idCurso, nombreCarpeta))
        contenidos.guardar(contenido)
        return contenido
    }

    /** Carpeta libre creada por el estudiante (sin curso). */
    fun crearCarpeta(nombre: String) {
        val limpio = nombre.trim()
        if (limpio.isBlank() || limpio.equals(Carpeta.SIN_CLASIFICAR, true)) return
        if (contenidos.listarCarpetas().any { it.nombre.equals(limpio, true) }) return
        contenidos.guardarCarpeta(Carpeta(nombre = limpio))
    }

    fun listarContenido(): List<ContenidoAcademico> = contenidos.listarTodos()
    fun listarCarpetasLibres(): List<Carpeta> = contenidos.listarCarpetas()
    fun buscar(id: String): ContenidoAcademico? = contenidos.buscarPorId(id)

    /**
     * Apunte que el estudiante está revisando en la pantalla Importar (todavía no guardado).
     * Se usa para pedirle a la IA que analice el texto, quizá ya corregido por el estudiante.
     */
    fun contenidoEnRevision(titulo: String, texto: String, tipo: TipoContenido): ContenidoAcademico =
        Apunte(titulo = titulo.ifBlank { "Apunte importado" }, tipo = tipo, textoReconocido = texto)

    /** Curso elegido -> carpeta del curso; nombre de carpeta libre -> esa carpeta; si no, Sin clasificar (null). */
    private fun resolverCarpeta(idCurso: String?, nombreCarpeta: String): Carpeta? {
        idCurso?.takeIf { it.isNotBlank() }?.let { id -> cursos.buscarPorId(id)?.let { return it.carpeta } }
        val nombre = nombreCarpeta.trim()
        if (nombre.isBlank() || nombre.equals(Carpeta.SIN_CLASIFICAR, true)) return null
        cursos.buscarPorNombre(nombre)?.let { return it.carpeta }
        return contenidos.listarCarpetas().firstOrNull { it.nombre.equals(nombre, true) }
            ?: Carpeta(nombre = nombre).also { contenidos.guardarCarpeta(it) }
    }

    private fun tipoMime(uri: Uri): String =
        context.contentResolver.getType(uri)
            ?: when {
                uri.toString().endsWith(".pdf", true) -> "application/pdf"
                uri.toString().endsWith(".png", true) -> "image/png"
                uri.toString().endsWith(".jpg", true) || uri.toString().endsWith(".jpeg", true) -> "image/jpeg"
                else -> "text/plain"
            }
}
