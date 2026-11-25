package com.tareia.app.ui.screens

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tareia.app.*
import com.tareia.app.ui.components.*
import com.tareia.app.ingesta.CambioFechaAula
import java.text.DateFormat
import java.util.Date

/** Estado de la conexión con el calendario del aula virtual que muestra la pantalla. */
internal data class EstadoAulaUi(
    val conectada: Boolean,
    val ultimaSincronizacion: Long,
    val ocupada: Boolean,
    val mensaje: String,
    val cambios: List<CambioFechaAula>,
)

// Aula virtual (calendario exportado de Moodle), Moodle por token (solo lectura) y Google Calendar.

@Composable
internal fun IntegrationsScreen(
    data: AppSnapshot,
    aula: EstadoAulaUi,
    onGuardarAula: (String) -> Unit,
    onSincronizarAula: () -> Unit,
    onAplicarCambio: (CambioFechaAula) -> Unit,
    onDescartarCambio: (CambioFechaAula) -> Unit,
    onDesvincularAula: () -> Unit,
    onImportMoodle: (List<TaskItem>) -> Int,
    onOpenCalendar: () -> Unit,
) {
    val context = LocalContext.current
    var baseUrl by remember { mutableStateOf(MoodleSettings.url(context)) }
    var token by remember { mutableStateOf(MoodleSettings.token(context)) }
    var aulaUrl by remember { mutableStateOf(MoodleSettings.aulaUrl(context).ifBlank { "https://aulavirtual.upt.edu.pe/" }) }
    var message by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var urlCalendario by remember { mutableStateOf("") }
    PageHeading("Integraciones", "Conecta calendarios y plataformas académicas")
    SurfaceCard {
        Text("Aula Virtual UPT · entregas automáticas", fontWeight = FontWeight.SemiBold)
        if (!aula.conectada) {
            Text("Trae solas las tareas, cuestionarios y entregas con fecha de tus cursos. Solo tienes que copiar una vez la URL de tu calendario:", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("1. Entra al aula virtual y abre Calendario.\n2. Pulsa «Exportar calendario».\n3. Elige «Todos los cursos» y «Eventos recientes y próximos».\n4. Pulsa «Obtener URL del calendario», cópiala y pégala aquí.", fontSize = 13.sp)
            OutlinedTextField(
                urlCalendario, { urlCalendario = it.trim() },
                label = { Text("URL del calendario") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            AppButton(if (aula.ocupada) "Conectando…" else "Conectar y traer entregas", { onGuardarAula(urlCalendario); urlCalendario = "" },
                enabled = !aula.ocupada && urlCalendario.startsWith("https://"))
            Text("La URL es privada: se guarda cifrada en este teléfono y solo se usa para leer tu calendario.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        } else {
            val ultima = if (aula.ultimaSincronizacion > 0) DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(aula.ultimaSincronizacion)) else "todavía no"
            Text("Conectada. Se revisa sola al abrir la app (como mucho cada 3 horas). Última revisión: $ultima.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Las entregas nuevas llegan «Por validar»: revísalas en Tareas antes de enviarlas a Google Calendar.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            AppButton(if (aula.ocupada) "Revisando…" else "Revisar ahora", onSincronizarAula, enabled = !aula.ocupada)
        }
        if (aula.ocupada) PuntoPensando("Leyendo tu calendario del aula virtual…")
        if (aula.mensaje.isNotBlank()) Text(aula.mensaje, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        aula.cambios.forEach { c ->
            Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text("«${c.titulo}» cambió de fecha: ${c.fechaAnterior ?: "sin fecha"} → ${c.fechaNueva}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { onAplicarCambio(c) }) { Text("Actualizar fecha") }
                    TextButton(onClick = { onDescartarCambio(c) }) { Text("Mantener") }
                }
            }
        }
        if (aula.conectada) TextButton(onClick = onDesvincularAula) { Text("Desvincular aula virtual") }
    }
    SurfaceCard {
        Text("Moodle · solo lectura", fontWeight = FontWeight.SemiBold)
        Text("Importa cursos y actividades de una instancia habilitada. No se sincronizan calificaciones ni se envían entregas.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(baseUrl, { baseUrl = it }, label = { Text("URL base HTTPS") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(token, { token = it }, label = { Text("Token de servicio web") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        TextButton(onClick = { MoodleSettings.save(context, baseUrl, token); message = "URL y token guardados; el token queda cifrado en el dispositivo." }, enabled = token.isNotBlank()) { Text("Guardar conexión") }
        AppButton(if (busy) "Consultando Moodle…" else "Consultar cursos y actividades", {
            val url = baseUrl.trim(); val key = token.trim()
            MoodleSettings.save(context, url, key); busy = true
            Thread {
                val result = runCatching { MoodleClient.fetchAssignments(url, key) }
                Handler(Looper.getMainLooper()).post {
                    busy = false
                    result.onSuccess { items -> message = "${onImportMoodle(items)} actividad(es) nuevas importadas; las repeticiones se omitieron." }
                        .onFailure { message = it.message ?: "No se pudo acceder a Moodle." }
                }
            }.start()
        }, enabled = !busy && baseUrl.startsWith("https://") && token.isNotBlank())
        if (busy) PuntoPensando("Consultando Moodle…")
        if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        if (token.isNotBlank()) TextButton(onClick = { SecureTokenStore.clear(context); token = ""; message = "Token eliminado." }) { Text("Desconectar") }
        Text("Requiere permisos para core_webservice_get_site_info, core_enrol_get_users_courses y mod_assign_get_assignments.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
    }
    SurfaceCard {
        Text("Portal del aula virtual", fontWeight = FontWeight.SemiBold)
        OutlinedTextField(aulaUrl, { aulaUrl = it; MoodleSettings.saveAulaUrl(context, it) }, label = { Text("URL segura del portal") }, modifier = Modifier.fillMaxWidth())
        OutlinedButton(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(aulaUrl))) }.onFailure { message = "No se pudo abrir el portal." } }, enabled = aulaUrl.startsWith("https://"), modifier = Modifier.fillMaxWidth()) { Text("Abrir Aula Virtual") }
    }
    SurfaceCard {
        Text("Google Calendar · API REST", fontWeight = FontWeight.SemiBold)
        Text("OAuth 2.0 conecta directamente la cuenta Google del estudiante para crear, actualizar y eliminar eventos. No depende de calendarios locales.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        AppButton("Configurar Google Calendar", onOpenCalendar)
    }
}

private object MoodleSettings {
    private const val PREF = "tareia_moodle"
    private const val URL_KEY = "url"
    private const val AULA_KEY = "aula_url"
    fun url(context: Context) = context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(URL_KEY, "").orEmpty()
    fun aulaUrl(context: Context) = context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(AULA_KEY, "").orEmpty()
    fun token(context: Context) = SecureTokenStore.read(context)
    fun save(context: Context, url: String, token: String) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(URL_KEY, url.trim()).apply()
        if (token.isNotBlank()) SecureTokenStore.write(context, token.trim())
    }
    fun saveAulaUrl(context: Context, url: String) { context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(AULA_KEY, url.trim()).apply() }
}
