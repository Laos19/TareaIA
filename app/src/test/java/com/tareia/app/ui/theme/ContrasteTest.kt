package com.tareia.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifica el contraste WCAG 2.1 de la paleta de marca.
 * Texto normal ≥ 4.5:1 (AA). Si alguien cambia un token y rompe el contraste, falla aquí.
 */
class ContrasteTest {
    private fun ratio(a: Color, b: Color): Double {
        val (l1, l2) = listOf(a.luminance().toDouble(), b.luminance().toDouble()).sortedDescending()
        return (l1 + 0.05) / (l2 + 0.05)
    }

    private fun aa(nombre: String, texto: Color, fondo: Color, minimo: Double = 4.5) {
        val r = ratio(texto, fondo)
        assertTrue("$nombre: contraste %.2f:1 < %.1f:1".format(r, minimo), r >= minimo)
    }

    @Test fun textoBaseClaro() {
        aa("tinta/papel", TintaClaro, PapelClaro)
        aa("tinta/hoja", TintaClaro, HojaClaro)
        aa("grafito/papel", GrafitoClaro, PapelClaro)
        aa("grafito/hoja", GrafitoClaro, HojaClaro)
        aa("botón primario", PapelClaro, TintaClaro)
    }

    @Test fun textoBaseOscuro() {
        aa("tinta/papel (oscuro)", TintaOscuro, PapelOscuro)
        aa("tinta/hoja (oscuro)", TintaOscuro, HojaOscuro)
        aa("grafito/papel (oscuro)", GrafitoOscuro, PapelOscuro)
        aa("grafito/hoja (oscuro)", GrafitoOscuro, HojaOscuro)
        aa("botón primario (oscuro)", PapelOscuro, TintaOscuro)
    }

    @Test fun estadosSobreTarjeta() {
        with(EstadosClaro) { listOf("sello" to sello, "lacre" to lacre, "ámbar" to ambar, "éxito" to exito).forEach { (n, c) -> aa("$n/hoja", c, HojaClaro) } }
        with(EstadosOscuro) { listOf("sello" to sello, "lacre" to lacre, "ámbar" to ambar, "éxito" to exito).forEach { (n, c) -> aa("$n/hoja (oscuro)", c, HojaOscuro) } }
    }

    @Test fun textoSobreResaltador() = aa("sobreResaltador/resaltador", SobreResaltador, Resaltador)

    @Test fun familiasDeCursos() {
        (FamiliasClaro + FamiliasOscuro).forEach { aa("familia ${it.nombre}", it.contenido, it.contenedor) }
    }
}
