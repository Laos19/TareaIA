package com.tareia.app.dominio

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

/** Servicios de dominio: detección de duplicados (RN02) y clasificación de contenido (RN13). */
class ServiciosDominioTest {
    private class RepoFalso(val existente: ActividadAcademica?) : ActividadRepository {
        override fun guardar(a: ActividadAcademica) = Unit
        override fun buscarPorId(id: String) = null
        override fun buscarPor(titulo: String, curso: Curso?, fecha: FechaLimite?) = existente
        override fun listarPendientes() = emptyList<ActividadAcademica>()
        override fun listarTodas() = emptyList<ActividadAcademica>()
        override fun listarEnPapelera() = emptyList<ActividadAcademica>()
        override fun buscarPorIdExterno(idExterno: String) = null
        override fun eliminar(id: String) = Unit
    }

    private val fecha = FechaLimite(LocalDate.of(2030, 12, 1))

    @Test fun duplicadaSoloSiExisteOtraConDistintoId() {
        val a = ActividadBuilder().conId("a").conTitulo("Informe").conFechaLimite(fecha).build()
        val otra = ActividadBuilder().conId("b").conTitulo("Informe").conFechaLimite(fecha).build()
        assertFalse(DetectorDuplicados(RepoFalso(null)).esDuplicada(a))
        assertFalse(DetectorDuplicados(RepoFalso(a)).esDuplicada(a))
        assertTrue(DetectorDuplicados(RepoFalso(otra)).esDuplicada(a))
    }

    @Test fun clasificaPorAbreviaturaONombreConPalabraCompleta() {
        val ps = Curso(nombre = "Pruebas de Software", abreviatura = Abreviatura.de("ps"))
        val bd = Curso(nombre = "Base de Datos", abreviatura = Abreviatura.de("bd2"))
        val cursos = listOf(ps, bd)
        val c = ClasificadorContenido()
        assertEquals(ps.carpeta, c.clasificar(Apunte(titulo = "Clase PS 3", tipo = TipoContenido.TEXTO), cursos))
        assertEquals(bd.carpeta, c.clasificar(Anotacion(titulo = "Notas", contenido = "repaso de base de datos"), cursos))
        assertNull(c.clasificar(Apunte(titulo = "Un lapso", tipo = TipoContenido.PDF, textoReconocido = "sin curso"), cursos))
        assertNull(c.clasificar(Apunte(titulo = "Algo", tipo = TipoContenido.IMAGEN), emptyList()))
    }

    @Test fun contenidoSinCarpetaQuedaSinClasificarYVaALaPapelera() {
        val ap = Apunte(titulo = "Foto", tipo = TipoContenido.IMAGEN, textoReconocido = "ocr")
        assertTrue(ap.estaSinClasificar())
        assertEquals("ocr", ap.texto())
        ap.actualizarTexto("corregido"); assertEquals("corregido", ap.texto())
        ap.asignarCarpeta(Carpeta(nombre = "Física")); assertFalse(ap.estaSinClasificar())
        assertEquals("Foto", ap.etiquetaPapelera())
        ap.enviarAPapelera(); assertTrue(ap.estaEnPapelera())
        ap.restaurar(); assertFalse(ap.estaEnPapelera())
        val nota = Anotacion(titulo = "N", contenido = "a")
        nota.actualizarTexto("b"); assertEquals("b", nota.texto())
        assertEquals(TipoContenido.TEXTO, nota.tipo)
        assertEquals("Sin clasificar", Carpeta.SIN_CLASIFICAR)
    }

    @Test fun cursoEnPapelera() {
        val c = Curso(nombre = "Calidad", abreviatura = Abreviatura.de("si784"))
        assertEquals("Calidad", c.nombre)
        assertFalse(c.estaEnPapelera())
        c.enviarAPapelera(); assertTrue(c.estaEnPapelera())
        c.restaurar(); assertFalse(c.estaEnPapelera())
    }
}
