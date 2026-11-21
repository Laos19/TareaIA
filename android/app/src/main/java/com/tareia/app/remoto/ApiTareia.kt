package com.tareia.app.remoto

import okhttp3.Cache
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Error de la API con el código HTTP y el mensaje en español del servidor. */
class ErrorApi(val codigo: Int, mensaje: String) : IOException(mensaje)

/**
 * Cliente HTTP de la API de TARE·IA (OkHttp).
 *
 * - Caché HTTP en disco de 5 MB: respeta `Cache-Control` del servidor para las consultas GET.
 * - Tiempos de espera cortos: la app nunca se bloquea esperando a la nube (todo es en segundo plano).
 * - Solo HTTPS en producción (`usesCleartextTraffic=false` en el manifiesto).
 */
class ApiTareia(private val baseUrl: String, carpetaCache: File?) {

    private val cliente: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .apply { if (carpetaCache != null) cache(Cache(File(carpetaCache, "http-tareia"), 5L * 1024 * 1024)) }
        .build()

    private val json = "application/json; charset=utf-8".toMediaType()

    private fun pedir(metodo: String, ruta: String, cuerpo: JSONObject?, token: String?): String {
        val req = Request.Builder()
            .url(baseUrl.trimEnd('/') + ruta)
            .apply { if (token != null) header("Authorization", "Bearer $token") }
            .method(metodo, cuerpo?.toString()?.toRequestBody(json) ?: if (metodo == "POST") "".toRequestBody(json) else null)
            .build()
        cliente.newCall(req).execute().use { r ->
            val texto = r.body?.string().orEmpty()
            if (!r.isSuccessful) {
                val mensaje = runCatching { JSONObject(texto).optString("mensaje") }.getOrNull().orEmpty()
                throw ErrorApi(r.code, mensaje.ifBlank { "Error ${r.code}" })
            }
            return texto
        }
    }

    /** Inicia sesión con el ID token de Firebase. Devuelve (token JWT, id de sesión). */
    fun iniciarSesionGoogle(idToken: String, dispositivo: String, versionApp: String): Pair<String, String> {
        val r = JSONObject(pedir("POST", "/api/auth/google", JSONObject()
            .put("idToken", idToken).put("origen", "android")
            .put("dispositivo", dispositivo.take(80)).put("versionApp", versionApp.take(20)), null))
        return r.getString("token") to r.getString("sesionId")
    }

    /** Cierra la sesión en el servidor. */
    fun cerrarSesion(token: String) { pedir("POST", "/api/auth/logout", null, token) }

    /** Envía cursos y actividades; devuelve la respuesta del servidor (aplicados, conservados, rechazados…). */
    fun sincronizar(token: String, lote: JSONObject): JSONObject = JSONObject(pedir("POST", "/api/sync", lote, token))

    /** Envía un lote de eventos de uso. */
    fun enviarEventos(token: String, eventos: List<JSONObject>) {
        pedir("POST", "/api/eventos", JSONObject().put("eventos", JSONArray(eventos)), token)
    }

    /** Envía un lote de logs. */
    fun enviarLogs(token: String, logs: List<JSONObject>) {
        pedir("POST", "/api/logs", JSONObject().put("logs", JSONArray(logs)), token)
    }
}
