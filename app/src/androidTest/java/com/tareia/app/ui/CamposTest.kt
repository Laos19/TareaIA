package com.tareia.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tareia.app.CourseItem
import com.tareia.app.ui.components.CampoCurso
import com.tareia.app.ui.components.CampoFecha
import com.tareia.app.ui.components.fechaLegible
import com.tareia.app.ui.theme.TareiaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Campos que previenen errores: selector de fecha y lista de cursos. */
@RunWith(AndroidJUnit4::class)
class CamposTest {
    @get:Rule val rule = createComposeRule()

    @Test fun fechaSeMuestraLegibleYSeAnuncia() {
        rule.setContent { TareiaTheme(darkTheme = false) { CampoFecha("2026-11-19", {}) } }
        rule.onNodeWithContentDescription("Fecha límite: ${fechaLegible("2026-11-19")}").assertIsDisplayed()
    }

    @Test fun fechaVaciaPideElegir() {
        rule.setContent { TareiaTheme(darkTheme = false) { CampoFecha("", {}) } }
        rule.onNodeWithContentDescription("Fecha límite: sin elegir").assertIsDisplayed()
    }

    /** Abrir y aceptar sin cambiar no debe correr la fecha un día (error típico de UTC en DatePicker). */
    @Test fun aceptarConservaLaMismaFecha() {
        var valor by mutableStateOf("2026-11-19")
        rule.setContent { TareiaTheme(darkTheme = false) { CampoFecha(valor, { valor = it }) } }
        rule.onNodeWithContentDescription("Fecha límite", substring = true).performClick()
        rule.onNodeWithText("Aceptar").assertIsDisplayed().performClick()
        assertEquals("2026-11-19", valor)
    }

    @Test fun cancelarNoCambiaLaFecha() {
        var valor by mutableStateOf("2026-11-19")
        rule.setContent { TareiaTheme(darkTheme = false) { CampoFecha(valor, { valor = "cambiada" }) } }
        rule.onNodeWithContentDescription("Fecha límite", substring = true).performClick()
        rule.onNodeWithText("Cancelar").performClick()
        assertEquals("2026-11-19", valor)
    }

    @Test fun cursoSeFiltraYSeElige() {
        val cursos = listOf(CourseItem(name = "Inteligencia Artificial", abbreviation = "IA"), CourseItem(name = "Base de Datos", abbreviation = "BD"))
        var valor by mutableStateOf("")
        rule.setContent { TareiaTheme(darkTheme = false) { Column { CampoCurso(valor, { valor = it }, cursos) } } }
        rule.onNodeWithText("Curso").performTextInput("base")
        rule.onNodeWithText("Base de Datos").assertIsDisplayed().performClick()
        assertEquals("Base de Datos", valor)
    }

    @Test fun cursoNuevoSeAvisa() {
        var valor by mutableStateOf("")
        rule.setContent { TareiaTheme(darkTheme = false) { CampoCurso(valor, { valor = it }, listOf(CourseItem(name = "Química", abbreviation = "QU"))) } }
        rule.onNodeWithText("Curso").performTextInput("Física")
        rule.onNodeWithText("Curso nuevo: no está en tu tabla").assertIsDisplayed()
    }
}
