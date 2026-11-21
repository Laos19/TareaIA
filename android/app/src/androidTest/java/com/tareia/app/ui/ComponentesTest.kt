package com.tareia.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tareia.app.CourseItem
import com.tareia.app.TaskItem
import com.tareia.app.ui.components.CintaEntregas
import com.tareia.app.ui.components.PuntoPensando
import com.tareia.app.ui.components.StatusBadge
import com.tareia.app.ui.components.TablaCursos
import com.tareia.app.ui.components.TaskCard
import com.tareia.app.ui.navigation.Destino
import com.tareia.app.ui.screens.MoreScreen
import com.tareia.app.ui.theme.TareiaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** Pruebas de interfaz de los componentes de marca: comportamiento y semántica para TalkBack. */
@RunWith(AndroidJUnit4::class)
class ComponentesTest {
    @get:Rule val rule = createComposeRule()

    private fun conTema(contenido: @Composable () -> Unit) = rule.setContent {
        TareiaTheme(darkTheme = false) { Column(Modifier.verticalScroll(rememberScrollState())) { contenido() } }
    }

    /** Todo nodo pulsable debe medir al menos 48 × 48 dp (guía de accesibilidad de Android). */
    private fun assertAreasTactilesMinimas() {
        val minimo = with(rule.density) { 48.dp.toPx() } - 1f
        rule.onAllNodes(hasClickAction()).fetchSemanticsNodes().forEach { nodo ->
            val t = nodo.touchBoundsInRoot
            val nombre = nodo.config.getOrNull(SemanticsProperties.Text)?.joinToString() ?: nodo.config.toString()
            assertTrue("Área táctil ${t.width}×${t.height}px < 48dp en «$nombre»", t.width >= minimo && t.height >= minimo)
        }
    }

    @Test fun estadoPorValidarLlevaSelloIA() {
        conTema { StatusBadge("Pendiente de validar") }
        rule.onNodeWithContentDescription("Sugerido automáticamente: Por validar").assertIsDisplayed()
    }

    @Test fun estadoNoEsUnBotonFalso() {
        conTema { StatusBadge("Completada") }
        rule.onNodeWithContentDescription("Estado: Completada").assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
        assertEquals(0, rule.onAllNodes(hasClickAction()).fetchSemanticsNodes().size)
    }

    @Test fun cintaResumeEntregasParaTalkBack() {
        val hoy = LocalDate.of(2026, 11, 12)
        val tareas = listOf(
            TaskItem(title = "Quiz", dueDate = "2026-11-12"),
            TaskItem(title = "Ensayo", dueDate = "2026-11-14"),
            TaskItem(title = "Atrasada", dueDate = "2026-11-01"),
            TaskItem(title = "Hecha", dueDate = "2026-11-13", status = "Completada"),
        )
        conTema { CintaEntregas(tareas, hoy = hoy) }
        rule.onNodeWithContentDescription(
            "Próximos 14 días: 2 entregas. jueves 12: Quiz. sábado 14: Ensayo. 1 vencida", substring = false,
        ).assertExists()
    }

    @Test fun completarLlamaAlCallbackYCambiaLaAccion() {
        var tarea by mutableStateOf(TaskItem(title = "Laboratorio", course = "Base de Datos", dueDate = LocalDate.now().plusDays(5).toString()))
        var llamadas = 0
        conTema {
            TaskCard(tarea, onToggle = { llamadas++; tarea = tarea.copy(status = "Completada") }, onEdit = {}, onDelete = {})
        }
        rule.onNodeWithText("Completar").performClick()
        assertEquals(1, llamadas)
        rule.onNodeWithText("Marcar pendiente").assertIsDisplayed()
        rule.onNodeWithContentDescription("Estado: Completada").assertExists()
    }

    @Test fun accionesDeTarjetaCumplenAreaTactil() {
        conTema { TaskCard(TaskItem(title = "T", dueDate = "2026-12-01"), onToggle = {}, onEdit = {}, onDelete = {}, onSync = {}, onPlan = {}) }
        assertAreasTactilesMinimas()
    }

    @Test fun celdaDeCursoSeAnunciaCompleta() {
        val cursos = listOf(CourseItem(name = "Inteligencia Artificial", abbreviation = "IA"))
        val tareas = listOf(TaskItem(title = "A", course = "inteligencia artificial", dueDate = "2026-12-01"))
        var abierto: CourseItem? = null
        conTema { TablaCursos(cursos, tareas, onCurso = { abierto = it }) }
        rule.onNodeWithContentDescription("Curso 1, Inteligencia Artificial, 1 pendiente").performClick()
        assertEquals("IA", abierto?.abbreviation)
        assertAreasTactilesMinimas()
    }

    @Test fun masAbreCadaHerramientaConToqueEnLaFila() {
        val abiertos = mutableListOf<Destino>()
        conTema { MoreScreen(onOpen = { abiertos += it }) }
        rule.onNodeWithText("Cursos").performClick()
        rule.onNodeWithText("Papelera").performClick()
        assertEquals(listOf(Destino.Cursos, Destino.Papelera), abiertos)
        assertAreasTactilesMinimas()
    }

    @Test fun puntoPensandoEsProgresoIndeterminado() {
        conTema { PuntoPensando("Procesando documento…") }
        rule.onNodeWithContentDescription("Procesando documento…")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo.Indeterminate))
    }
}
