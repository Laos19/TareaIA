package com.tareia.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.tareia.app.*
import com.tareia.app.ui.components.*
import kotlinx.coroutines.launch

// Centro de permisos.

@Composable
internal fun PermissionCenter(message: String, onMessage: (String) -> Unit) {
    val context = LocalContext.current
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        onMessage(if (result.values.all { it }) "Permisos concedidos." else "Algunos permisos fueron denegados; las funciones no relacionadas siguen disponibles.")
    }
    val notificationPermission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.POST_NOTIFICATIONS else null
    PageHeading("Permisos", "TARE-IA solo solicita acceso al usar cada función")
    SurfaceCard {
        Text("Calendario", fontWeight = FontWeight.SemiBold)
        Text("Permite leer calendarios disponibles y añadir eventos al que selecciones.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        AppButton("Conectar calendarios", { permissions.launch(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)) })
        if (CalendarRepository.hasPermissions(context)) {
            val calendars = remember { runCatching { CalendarRepository.calendars(context) }.getOrDefault(emptyList()) }
            if (calendars.isEmpty()) Text("No hay calendarios editables en el dispositivo.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            calendars.forEach { calendar ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${calendar.name} · ${calendar.account}", Modifier.weight(1f), fontSize = 12.sp)
                    TextButton(onClick = { onMessage("Para cambiar el calendario seleccionado, usa la pantalla de integraciones después de conceder acceso.") }) { Text("Disponible") }
                }
            }
        }
    }
    SurfaceCard {
        Text("Notificaciones", fontWeight = FontWeight.SemiBold)
        Text("Los recordatorios se programan para un día antes de la fecha límite, a las 9:00 a. m.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (notificationPermission != null) AppButton("Permitir notificaciones", { permissions.launch(arrayOf(notificationPermission)) })
        else Text("Las notificaciones están habilitadas por la versión de Android.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    SurfaceCard {
        Text("Micrófono", fontWeight = FontWeight.SemiBold)
        Text("Solo se solicita al iniciar dictado. El servicio de voz del dispositivo puede transmitir audio por Internet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        AppButton("Permitir micrófono", { permissions.launch(arrayOf(Manifest.permission.RECORD_AUDIO)) })
    }
    SurfaceCard {
        Text("Archivos e imágenes", fontWeight = FontWeight.SemiBold)
        Text("Los documentos se seleccionan con el selector del sistema; no se solicita acceso general al almacenamiento. Para tomar una foto se abre la cámara del dispositivo.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
