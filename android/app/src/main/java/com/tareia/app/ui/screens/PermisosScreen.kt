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
internal fun PermissionCenter(
    message: String,
    onMessage: (String) -> Unit,
    googleEmail: String?,
    googleConnected: Boolean,
    googleBusy: Boolean,
    onConnectGoogle: () -> Unit
) {
    val context = LocalContext.current
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        onMessage(if (result.values.all { it }) "Permisos concedidos." else "Algunos permisos fueron denegados; las funciones no relacionadas siguen disponibles.")
    }
    val notificationPermission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.POST_NOTIFICATIONS else null
    PageHeading("Permisos", "TARE-IA solo solicita acceso al usar cada función")
    SurfaceCard {
        Text("Google Calendar", fontWeight = FontWeight.SemiBold)
        Text("Conecta directamente tu cuenta a Google Calendar mediante OAuth 2.0. No requiere permisos del calendario local ni tener la app Calendario instalada.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (googleEmail.isNullOrBlank()) {
            Text("Inicia sesión con Google (no con PIN local) para poder autorizar el calendario.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text("Cuenta: $googleEmail", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AppButton(
            text = if (googleBusy) "Conectando…" else if (googleConnected) "Comprobar conexión" else "Conectar Google Calendar",
            onClick = onConnectGoogle,
            enabled = !googleBusy && !googleEmail.isNullOrBlank()
        )
        if (googleConnected) Text("Cuenta vinculada. Los eventos confirmados se guardan en el calendario principal de Google.", color = MaterialTheme.colorScheme.primary)
        Text("La primera conexión puede solicitar tu consentimiento. Solo se pide permiso para administrar eventos (calendar.events).", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
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
