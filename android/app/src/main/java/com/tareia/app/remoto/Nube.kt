package com.tareia.app.remoto

import android.content.Context
import android.os.Build
import android.util.Log
import com.google.firebase.auth.FirebaseUser
import com.tareia.app.BuildConfig
import com.tareia.app.SecureTokenStore
import com.tareia.app.aplicacion.Dependencias
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Punto único de conexión con la nube de TARE·IA (Facade).
 *
 * - **Sesión:** al entrar con Google se canjea el ID token de Firebase por un JWT de la API; el JWT se
 *   guarda cifrado con Android Keystore (`SecureTokenStore`). Al salir se cierra la sesión en el servidor.
 * - **Sincronización:** respaldo de cursos y actividades en PostgreSQL (como mucho cada 2 minutos).
 * - **Telemetría y logs:** eventos de uso y errores en lotes, sin datos personales.
 *
 * Todo ocurre en segundo plano y es tolerante a fallos: sin conexión la app sigue funcionando igual
 * con sus datos locales; los eventos esperan en la cola hasta el siguiente envío.
 */
object Nube {
    private const val PREF = "tareia_nube"
    private const val TAG = "TareiaNube"
    private const val INTERVALO_SYNC_MS = 2 * 60_000L

    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val eventos = ColaTelemetria()
    private val logs = ColaTelemetria(capacidad = 50, tamanoLote = 10)
    private var api: ApiTareia? = null
    private var appContext: Context? = null
    @Volatile private var ultimaSync = 0L

    /** Hay una URL de API configurada en la compilación (BuildConfig.API_URL). */
    val habilitada: Boolean get() = BuildConfig.API_URL.isNotBlank()

    /** Inicializa el cliente (llamar una vez desde la Activity). */
    fun iniciar(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        if (habilitada) api = ApiTareia(BuildConfig.API_URL, context.cacheDir)
    }

    private fun token(): String? = appContext?.let { SecureTokenStore.readProtected(it, PREF, "token") }?.ifBlank { null }

    /** Canjea la sesión de Firebase por una sesión de la API y hace la primera sincronización. */
    fun iniciarSesion(usuario: FirebaseUser, dep: Dependencias) {
        val cliente = api ?: return
        val ctx = appContext ?: return
        alcance.launch {
            try {
                val idToken = obtenerIdToken(usuario)
                val (jwt, _) = cliente.iniciarSesionGoogle(idToken, "${Build.MANUFACTURER} ${Build.MODEL} (API ${Build.VERSION.SDK_INT})", BuildConfig.VERSION_NAME)
                SecureTokenStore.writeProtected(ctx, PREF, "token", jwt)
                registrar("app_abierta")
                sincronizar(dep, forzar = true)
            } catch (e: Exception) {
                advertir("Sesion", "No se pudo abrir la sesión en la nube: ${e.message}")
            }
        }
    }

    private suspend fun obtenerIdToken(usuario: FirebaseUser): String = suspendCancellableCoroutine { cont ->
        usuario.getIdToken(false)
            .addOnSuccessListener { r -> r.token?.let { cont.resume(it) } ?: cont.resumeWithException(IllegalStateException("Firebase no devolvió token")) }
            .addOnFailureListener { cont.resumeWithException(it) }
    }

    /** Envía lo pendiente y cierra la sesión en el servidor; borra el token local. */
    fun cerrarSesion() {
        val ctx = appContext ?: return
        val jwt = token() ?: return
        SecureTokenStore.clearProtected(ctx, PREF, "token")
        alcance.launch {
            vaciar(jwt)
            runCatching { api?.cerrarSesion(jwt) }
        }
    }

    /** Respaldo de cursos y actividades en la nube (como mucho cada 2 minutos salvo que se fuerce). */
    fun sincronizar(dep: Dependencias, forzar: Boolean = false) {
        val cliente = api ?: return
        val jwt = token() ?: return
        val ahora = System.currentTimeMillis()
        if (!forzar && ahora - ultimaSync < INTERVALO_SYNC_MS) return
        ultimaSync = ahora
        alcance.launch {
            try {
                val lote = withContext(Dispatchers.Default) { MapeoNube.lote(dep.gestorCursos.listar(), dep.gestorActividades.listar(), null) }
                val r = cliente.sincronizar(jwt, lote)
                val rechazados = r.optJSONArray("rechazados")?.length() ?: 0
                if (rechazados > 0) advertir("Sync", "$rechazados registro(s) rechazados por el servidor")
            } catch (e: ErrorApi) {
                if (e.codigo == 401) appContext?.let { SecureTokenStore.clearProtected(it, PREF, "token") }
                advertir("Sync", "Sincronización fallida (${e.codigo}): ${e.message}")
            } catch (e: Exception) {
                Log.i(TAG, "Sin conexión para sincronizar: ${e.message}")
            }
        }
    }

    /** Registra un evento de uso (pantalla vista, actividad creada, plan generado…). */
    fun registrar(tipo: String, pantalla: String? = null, datos: Map<String, Any> = emptyMap()) {
        if (!habilitada) return
        if (eventos.agregar(ColaTelemetria.evento(tipo, pantalla, datos))) token()?.let { jwt -> alcance.launch { vaciar(jwt) } }
    }

    /** Log de advertencia: va a Logcat y, en lote, al servidor. */
    fun advertir(etiqueta: String, mensaje: String) = log("WARN", etiqueta, mensaje)

    /** Log de error: va a Logcat y, en lote, al servidor. */
    fun error(etiqueta: String, mensaje: String, e: Throwable? = null) = log("ERROR", etiqueta, mensaje + (e?.let { " · ${it.javaClass.simpleName}" } ?: ""))

    private fun log(nivel: String, etiqueta: String, mensaje: String) {
        if (nivel == "ERROR") Log.e(TAG, "[$etiqueta] $mensaje") else Log.w(TAG, "[$etiqueta] $mensaje")
        if (habilitada) logs.agregar(ColaTelemetria.log(nivel, etiqueta, mensaje, BuildConfig.VERSION_NAME))
    }

    /** Envía todo lo pendiente (al salir de la app o al cerrar sesión). */
    fun vaciarEnSegundoPlano() {
        val jwt = token() ?: return
        alcance.launch { vaciar(jwt) }
    }

    private fun vaciar(jwt: String) {
        val cliente = api ?: return
        var lote = eventos.tomarLote()
        while (lote.isNotEmpty()) {
            try { cliente.enviarEventos(jwt, lote) } catch (e: Exception) { eventos.devolver(lote); break }
            lote = eventos.tomarLote()
        }
        val l = logs.tomarLote()
        if (l.isNotEmpty()) runCatching { cliente.enviarLogs(jwt, l) }.onFailure { logs.devolver(l) }
    }

    /** Datos de diagnóstico para la pantalla de perfil. */
    fun estado(): JSONObject = JSONObject()
        .put("habilitada", habilitada)
        .put("conectada", token() != null)
        .put("eventosPendientes", eventos.tamano)
}
