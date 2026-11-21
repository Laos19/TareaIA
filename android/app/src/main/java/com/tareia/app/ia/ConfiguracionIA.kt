package com.tareia.app.ia

import android.content.Context
import com.tareia.app.SecureTokenStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.net.URL
import javax.net.ssl.HttpsURLConnection

// Configuración del Proveedor B (API compatible con OpenAI) que el estudiante escribe en
// Más > IA. Antes estaba en AiClient.kt; se movió aquí sin cambiar su funcionamiento.

data class AiConfig(val endpoint: String, val model: String, val apiKey: String) {
    /** true si están los tres datos: solo entonces se puede usar el Proveedor B. */
    fun estaCompleta() = endpoint.isNotBlank() && model.isNotBlank() && apiKey.isNotBlank()
}

object AiSettings {
    private const val PREF = "tareia_ai"
    fun load(context: Context) = AiConfig(
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString("endpoint", "").orEmpty(),
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString("model", "").orEmpty(),
        SecureTokenStore.readProtected(context, PREF, "api_key")
    )
    fun save(context: Context, endpoint: String, model: String, apiKey: String) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString("endpoint", endpoint.trim()).putString("model", model.trim()).apply()
        if (apiKey.isNotBlank()) SecureTokenStore.writeProtected(context, PREF, "api_key", apiKey.trim())
    }
    fun clearKey(context: Context) = SecureTokenStore.clearProtected(context, PREF, "api_key")
}

/**
 * Cliente HTTP mínimo para APIs compatibles con OpenAI (/v1/chat/completions).
 * Lo usan los productos del Proveedor B. Exige HTTPS y no acepta credenciales en la URL.
 */
internal object ClienteCompatibleOpenAI {

    suspend fun completar(config: AiConfig, sistema: String, usuario: String): String = withContext(Dispatchers.IO) {
        require(config.estaCompleta()) { "Configura proveedor, modelo y API key en Más > IA." }
        val uri = URI(config.endpoint.trim())
        require(uri.scheme.equals("https", true) && uri.userInfo == null) { "El endpoint de IA debe usar HTTPS y no incluir credenciales en la URL." }
        val raiz = config.endpoint.trimEnd('/')
        val endpoint = when {
            raiz.endsWith("/chat/completions") -> raiz
            raiz.endsWith("/v1") -> "$raiz/chat/completions"
            else -> "$raiz/v1/chat/completions"
        }
        val payload = JSONObject().apply {
            put("model", config.model)
            put("temperature", 0.1)
            put("messages", JSONArray()
                .put(JSONObject().put("role", "system").put("content", sistema))
                .put(JSONObject().put("role", "user").put("content", usuario)))
        }.toString()
        val conexion = (URL(endpoint).openConnection() as? HttpsURLConnection)
            ?: throw IllegalStateException("El endpoint de IA debe usar HTTPS.")
        conexion.apply {
            requestMethod = "POST"; connectTimeout = 15000; readTimeout = 60000; doOutput = true; instanceFollowRedirects = false
            setRequestProperty("Authorization", "Bearer ${config.apiKey}")
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("Accept", "application/json")
        }
        try {
            conexion.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            val estado = conexion.responseCode
            val flujo = if (estado in 200..299) conexion.inputStream else conexion.errorStream
            val cuerpo = flujo?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (estado !in 200..299) throw IllegalStateException("El proveedor de IA respondió HTTP $estado.")
            JSONObject(cuerpo).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
        } finally { conexion.disconnect() }
    }
}
