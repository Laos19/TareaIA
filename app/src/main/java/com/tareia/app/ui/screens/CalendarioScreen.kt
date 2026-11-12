package com.tareia.app.ui.screens

import android.Manifest
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

// Selección del calendario del dispositivo.

@Composable
internal fun CalendarSettingsScreen(selectedId: Long?, onSelect: (Long) -> Unit) {
    val context = LocalContext.current
    var calendars by remember { mutableStateOf(emptyList<DeviceCalendar>()) }
    var message by remember { mutableStateOf("") }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.all { it }) calendars = runCatching { CalendarRepository.calendars(context) }.getOrDefault(emptyList())
        else message = "Se denegó el acceso al calendario. Las demás funciones siguen disponibles."
    }
    LaunchedEffect(Unit) {
        if (CalendarRepository.hasPermissions(context)) calendars = runCatching { CalendarRepository.calendars(context) }.getOrDefault(emptyList())
    }
    PageHeading("Calendario del dispositivo", "Autoriza y selecciona el calendario donde crear eventos")
    SurfaceCard {
        Text("TARE-IA usa el proveedor de calendarios Android. Si eliges una cuenta Google sincronizada, el proveedor del teléfono se encarga de sincronizar los eventos.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        AppButton("Conceder acceso a calendarios", { permission.launch(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)) })
        if (calendars.isEmpty() && CalendarRepository.hasPermissions(context)) Text("No se encontraron calendarios editables.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        calendars.forEach { calendar ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selectedId == calendar.id, onClick = { onSelect(calendar.id) })
                Column { Text(calendar.name, fontWeight = FontWeight.Medium); Text(calendar.account, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) }
            }
        }
        if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
