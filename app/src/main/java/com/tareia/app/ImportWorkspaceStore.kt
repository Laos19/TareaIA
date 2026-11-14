package com.tareia.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Borrador de importación independiente de la pantalla visible. No contiene tokens ni URIs. */
data class ImportWorkspace(
    val extractedText: String = "",
    val source: String = "Documento",
    val documentTitle: String = "Apunte importado",
    val drafts: List<TaskDraft> = emptyList(),
    val proposalOrigin: String = "Local",
    val feedback: String = "",
    val savedNoteId: String = "",
    /** Tipo del contenido importado (PDF, IMAGEN o TEXTO): lo necesita el dominio al guardar el apunte. */
    val tipoContenido: String = "TEXTO"
)

/** Los apuntes OCR y las correcciones permanecen al navegar y al reiniciar la aplicación.
 *  El estado se liga al correo del perfil local para no mostrarlo a otro usuario.
 */
object ImportWorkspaceStore {
    private const val PREF = "tareia_import_workspace"
    private const val KEY = "draft_v1"
    private const val MAX_TEXT_LENGTH = 120_000

    fun load(context: Context, owner: String): ImportWorkspace = runCatching {
        val raw = SecureTokenStore.readProtected(context, PREF, KEY)
        if (raw.isBlank()) return@runCatching ImportWorkspace()
        val root = JSONObject(raw)
        if (!root.optString("owner").equals(owner.trim(), ignoreCase = true)) {
            return@runCatching ImportWorkspace()
        }
        val drafts = root.optJSONArray("drafts") ?: JSONArray()
        ImportWorkspace(
            extractedText = root.optString("text").take(MAX_TEXT_LENGTH),
            source = root.optString("source", "Documento"),
            documentTitle = root.optString("title", "Apunte importado"),
            drafts = (0 until drafts.length()).mapNotNull { i ->
                drafts.optJSONObject(i)?.let {
                    TaskDraft(
                        title = it.optString("title"),
                        course = it.optString("course"),
                        dueDate = it.optString("dueDate"),
                        source = it.optString("source", "Documento"),
                        description = it.optString("description")
                    )
                }
            }.take(30),
            proposalOrigin = root.optString("proposalOrigin", "Local"),
            feedback = root.optString("feedback"),
            savedNoteId = root.optString("savedNoteId"),
            tipoContenido = root.optString("tipoContenido", "TEXTO")
        )
    }.getOrDefault(ImportWorkspace())

    fun save(context: Context, owner: String, workspace: ImportWorkspace) {
        val root = JSONObject().apply {
            put("owner", owner.trim())
            put("text", workspace.extractedText.take(MAX_TEXT_LENGTH))
            put("source", workspace.source)
            put("title", workspace.documentTitle)
            put("proposalOrigin", workspace.proposalOrigin)
            put("feedback", workspace.feedback)
            put("savedNoteId", workspace.savedNoteId)
            put("tipoContenido", workspace.tipoContenido)
            put("drafts", JSONArray().apply {
                workspace.drafts.take(30).forEach { draft ->
                    put(JSONObject().apply {
                        put("title", draft.title)
                        put("course", draft.course)
                        put("dueDate", draft.dueDate)
                        put("source", draft.source)
                        put("description", draft.description)
                    })
                }
            })
        }
        SecureTokenStore.writeProtected(context, PREF, KEY, root.toString())
    }

    fun clear(context: Context) {
        SecureTokenStore.clearProtected(context, PREF, KEY)
    }
}
