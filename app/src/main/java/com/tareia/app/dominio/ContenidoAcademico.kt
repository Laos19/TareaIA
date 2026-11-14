package com.tareia.app.dominio

import java.time.LocalDateTime
import java.util.UUID

/**
 * Entidad abstracta ContenidoAcademico: todo material que el estudiante guarda.
 *
 * Es ABSTRACTA («A» en el diagrama): no se crean objetos "ContenidoAcademico" directamente,
 * sino de sus subclases:
 *   - Apunte:    contenido importado (PDF, foto o compartido) cuyo texto sale del OCR.
 *   - Anotacion: nota escrita a mano por el estudiante dentro de la app (RF-23).
 *
 * Es el PRODUCTO del patrón Factory Method: lo crean las subclases de ImportadorContenido.
 *
 * - carpeta = null significa "Sin clasificar" (RN13).
 * - Implementa Eliminable: puede ir a la papelera (RF-21, RN14).
 */
abstract class ContenidoAcademico(
    override val id: String,
    var titulo: String,
    val tipo: TipoContenido,
    var textoReconocido: String,
    val fechaRegistro: LocalDateTime,
    carpeta: Carpeta?,
    fechaEliminacion: Long?
) : Eliminable {

    var carpeta: Carpeta? = carpeta
        private set

    override var fechaEliminacion: Long? = fechaEliminacion
        protected set

    /**
     * Dónde está el archivo original (texto de un Uri de Android). Solo se usa mientras se
     * hace el OCR; no se guarda en la base de datos. Es un String para que el dominio no
     * dependa de clases de Android.
     */
    var ubicacionOrigen: String? = null

    /** El texto que se muestra en pantalla (cada subclase decide cuál es). */
    abstract fun texto(): String

    /** Reemplaza el texto (cuando el estudiante lo corrige). */
    abstract fun actualizarTexto(nuevo: String)

    fun estaSinClasificar(): Boolean = carpeta == null

    /** Mueve el contenido a una carpeta; con null vuelve a "Sin clasificar". */
    fun asignarCarpeta(c: Carpeta?) { carpeta = c }

    override fun etiquetaPapelera() = titulo
    override fun enviarAPapelera() { fechaEliminacion = System.currentTimeMillis() }
    override fun restaurar() { fechaEliminacion = null }
}

/** Contenido importado: su texto viene del OCR (textoReconocido). */
class Apunte(
    id: String = UUID.randomUUID().toString(),
    titulo: String,
    tipo: TipoContenido,
    textoReconocido: String = "",
    fechaRegistro: LocalDateTime = LocalDateTime.now(),
    carpeta: Carpeta? = null,
    fechaEliminacion: Long? = null
) : ContenidoAcademico(id, titulo, tipo, textoReconocido, fechaRegistro, carpeta, fechaEliminacion) {
    override fun texto() = textoReconocido
    override fun actualizarTexto(nuevo: String) { textoReconocido = nuevo }
}

/** Nota escrita por el estudiante (RF-23): su texto está en "contenido", no pasa por OCR. */
class Anotacion(
    id: String = UUID.randomUUID().toString(),
    titulo: String,
    var contenido: String,
    fechaRegistro: LocalDateTime = LocalDateTime.now(),
    carpeta: Carpeta? = null,
    fechaEliminacion: Long? = null
) : ContenidoAcademico(id, titulo, TipoContenido.TEXTO, "", fechaRegistro, carpeta, fechaEliminacion) {
    override fun texto() = contenido
    override fun actualizarTexto(nuevo: String) { contenido = nuevo }
}
