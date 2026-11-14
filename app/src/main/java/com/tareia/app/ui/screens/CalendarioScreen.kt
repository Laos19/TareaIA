package com.tareia.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.tareia.app.ui.components.*

/** Calendar REST usa el calendario principal (primary) de la cuenta autorizada. */
@Composable
internal fun CalendarSettingsScreen(
    googleEmail: String?, connected: Boolean, busy: Boolean, message: String,
    onConnect: () -> Unit
) {
    PageHeading("Google Calendar", "Conecta tu calendario de Google con OAuth 2.0")
    SurfaceCard {
        Text("Google Calendar API", fontWeight = FontWeight.SemiBold)
        Text("TARE·IA sincroniza directamente los eventos con el calendario principal de tu cuenta Google, sin depender del calendario del dispositivo.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (googleEmail.isNullOrBlank()) Text("Debes iniciar sesión con Google. El acceso por PIN no autoriza Google Calendar.")
        else Text("Cuenta: $googleEmail", fontSize = 13.sp)
        AppButton(
            if (busy) "Conectando…" else if (connected) "Comprobar conexión" else "Conectar Google Calendar",
            onConnect, enabled = !busy && !googleEmail.isNullOrBlank()
        )
        if (connected) Text("Conexión comprobada para esta sesión.", color = MaterialTheme.colorScheme.primary)
        if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Para añadir una tarea: entra a Tareas, confirma los datos y pulsa Sincronizar. Las modificaciones de tareas vinculadas se reflejan también en Google Calendar.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
}
