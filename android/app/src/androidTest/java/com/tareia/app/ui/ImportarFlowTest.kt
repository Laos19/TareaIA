package com.tareia.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tareia.app.AppSnapshot
import com.tareia.app.ImportWorkspace
import com.tareia.app.TaskDraft
import com.tareia.app.ui.screens.ImportScreen
import com.tareia.app.ui.theme.TareiaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Pruebas de UI: las propuestas se pueden rechazar sin perder el texto del apunte. */
@RunWith(AndroidJUnit4::class)
class ImportarFlowTest {
    @get:Rule val rule = createComposeRule()

    private fun montar(vararg suggestions: TaskDraft): () -> ImportWorkspace {
        var workspace by mutableStateOf(ImportWorkspace(
            extractedText = "Examen de Gestión de Proyectos, 2026-11-25",
            documentTitle = "Apunte clase",
            drafts = suggestions.toList()
        ))
        rule.setContent {
            TareiaTheme(darkTheme = false) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    ImportScreen(
                        state = AppSnapshot(),
                        workspace = workspace,
                        isWorking = false,
                        onWorkspaceChange = { workspace = it },
                        onDocument = {},
                        onCamera = {},
                        onAnalyze = {},
                        onSaveDrafts = {},
                        onSaveNote = {},
                        onOpenNotes = {},
                        onClear = { workspace = ImportWorkspace() }
                    )
                }
            }
        }
        return { workspace }
    }

    @Test fun descartarUnaPropuestaMantieneElDocumento() {
        val estado = montar(TaskDraft("Examen final", "", "2026-11-25", "Local"))
        rule.onNodeWithText("Descartar esta propuesta").performScrollTo().performClick()
        rule.onNodeWithText("Sin propuestas pendientes.", substring = true).assertExists()
        rule.onNodeWithText("Guardar texto completo en Apuntes").assertExists()
        assertEquals("Examen de Gestión de Proyectos, 2026-11-25", estado().extractedText)
        assertEquals(0, estado().drafts.size)
    }

    @Test fun descartarTodasRequiereConfirmacion() {
        val estado = montar(
            TaskDraft("Examen", "", "2026-11-25", "Local"),
            TaskDraft("Informe", "", "2026-11-27", "Gemini")
        )
        rule.onNodeWithText("Descartar todas").performScrollTo().performClick()
        assertEquals(2, estado().drafts.size)
        rule.onNodeWithText("Descartar propuestas").performClick()
        assertEquals(0, estado().drafts.size)
        assertEquals("Apunte clase", estado().documentTitle)
    }
}
