package com.tareia.app.remoto

import android.content.Context
import com.tareia.app.SecureTokenStore
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URI
import java.util.concurrent.TimeUnit

/**
 * Descarga el calendario exportado del aula virtual y guarda su URL de forma segura.
 *
 * La URL incluye una clave privada (authtoken) del estudiante: se guarda cifrada con Android Keystore,
 * nunca se envía a la API de TARE·IA ni se escribe en los logs.
 */
object CalendarioAulaRemoto {
    private const val PREF_CIFRADA = "tareia_aula"
    private const val CLAVE_URL = "ical_url"
    private const val PREF = "tareia_aula_estado"
    private const val MAX_BYTES = 2L * 1024 * 1024
    /** Intervalo mínimo entre sincronizaciones automáticas. */
    const val INTERVALO_MS = 3L * 60 * 60 * 1000

    private val cliente = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /** Valida que sea una URL de exportación de calendario por HTTPS. Devuelve el motivo del rechazo o null. */
    fun validar(url: String): String? {
        val u = runCatching { URI(url.trim()) }.getOrNull() ?: return "La dirección no es válida."
        if (!u.scheme.equals("https", ignoreCase = true)) return "La dirección debe empezar con https://"
        if (u.host.isNullOrBlank()) return "La dirección no es válida."
        if (!u.path.orEmpty().contains("calendar/export_execute.php")) {
            return "Copia la URL que da el aula virtual en Calendario → Exportar calendario → Obtener URL del calendario."
        }
        return null
    }

    fun url(context: Context): String = SecureTokenStore.readProtected(context, PREF_CIFRADA, CLAVE_URL)

    fun guardar(context: Context, url: String) = SecureTokenStore.writeProtected(context, PREF_CIFRADA, CLAVE_URL, url.trim())

    fun desvincular(context: Context) {
        SecureTokenStore.clearProtected(context, PREF_CIFRADA, CLAVE_URL)
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().clear().apply()
    }

    fun ultimaSincronizacion(context: Context): Long = context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getLong("ultima", 0L)

    fun marcarSincronizado(context: Context, ahora: Long = System.currentTimeMillis()) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putLong("ultima", ahora).apply()

    /** Hay URL guardada y pasó el intervalo mínimo desde la última sincronización. */
    fun tocaSincronizar(context: Context, ahora: Long = System.currentTimeMillis()): Boolean =
        url(context).isNotBlank() && ahora - ultimaSincronizacion(context) >= INTERVALO_MS

    /** Descarga el .ics (bloqueante: llamar desde un hilo de fondo). */
    fun descargar(url: String): String {
        validar(url)?.let { throw IOException(it) }
        val req = Request.Builder().url(url.trim()).header("Accept", "text/calendar").build()
        cliente.newCall(req).execute().use { r ->
            if (r.code == 401 || r.code == 403 || r.code == 404) {
                throw IOException("El aula virtual rechazó la URL del calendario. Genera una nueva en Calendario → Exportar calendario.")
            }
            if (!r.isSuccessful) throw IOException("El aula virtual respondió con el error ${r.code}.")
            val cuerpo = r.body ?: throw IOException("El aula virtual no devolvió el calendario.")
            if (cuerpo.contentLength() > MAX_BYTES) throw IOException("El calendario es demasiado grande.")
            // Lee como máximo MAX_BYTES + 1 para detectar calendarios demasiado grandes sin cargarlos completos.
            val texto = cuerpo.source().use { s ->
                s.request(MAX_BYTES + 1)
                if (s.buffer.size > MAX_BYTES) throw IOException("El calendario es demasiado grande.")
                s.buffer.readUtf8()
            }
            if (!texto.contains("BEGIN:VCALENDAR")) {
                throw IOException("La URL no devolvió un calendario. Revisa que hayas copiado la dirección completa.")
            }
            return texto
        }
    }
}
