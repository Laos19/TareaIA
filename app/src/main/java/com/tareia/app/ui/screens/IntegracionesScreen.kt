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
import androidx.compose.ui.unit.sp
import com.tareia.app.*
import com.tareia.app.ui.components.*

// Moodle (solo lectura), Aula Virtual y calendario.

@Composable
internal fun IntegrationsScreen(data: AppSnapshot, onImportMoodle: (List<TaskItem>) -> Int) {
    val context = LocalContext.current
    var baseUrl by remember { mutableStateOf(MoodleSettings.url(context)) }
    var token by remember { mutableStateOf(MoodleSettings.token(context)) }
    var aulaUrl by remember { mutableStateOf(MoodleSettings.aulaUrl(context).ifBlank { "https://aulavirtual.upt.edu.pe/" }) }
    var message by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    PageHeading("Integraciones", "Conecta calendarios y plataformas académicas")
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
        Text("Aula Virtual UPT", fontWeight = FontWeight.SemiBold)
        Text("Acceso al portal separado de Moodle, según el alcance actual.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(aulaUrl, { aulaUrl = it; MoodleSettings.saveAulaUrl(context, it) }, label = { Text("URL segura del portal") }, modifier = Modifier.fillMaxWidth())
        OutlinedButton(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(aulaUrl))) }.onFailure { message = "No se pudo abrir el portal." } }, enabled = aulaUrl.startsWith("https://"), modifier = Modifier.fillMaxWidth()) { Text("Abrir Aula Virtual") }
        Text("Para sincronizar datos automáticamente se necesitará la API del proveedor. Por ahora abre el portal sin guardar credenciales.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
    SurfaceCard {
        Text("Calendario del dispositivo", fontWeight = FontWeight.SemiBold)
        Text("La app usa el proveedor de calendarios Android. Si una cuenta Google está sincronizada en el dispositivo, el calendario seleccionado puede reflejar los eventos en Google Calendar.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Calendario seleccionado: ${data.selectedCalendarId ?: "ninguno"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = { /* settings live in Permission Center for OS grants */ }) { Text("Permisos y selección en Más > Permisos") }
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
