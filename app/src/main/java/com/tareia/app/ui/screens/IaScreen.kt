package com.tareia.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.sp
import com.tareia.app.*
import com.tareia.app.ui.components.*

// Configuración del proveedor de IA.

@Composable
internal fun AiSettingsScreen() {
    val context = LocalContext.current
    val current = remember { AiSettings.load(context) }
    var endpoint by remember { mutableStateOf(current.endpoint) }
    var model by remember { mutableStateOf(current.model) }
    var apiKey by remember { mutableStateOf("") }
    var hasKey by remember { mutableStateOf(current.apiKey.isNotBlank()) }
    var message by remember { mutableStateOf("") }
    PageHeading("Proveedor de IA", "Opcional: extrae actividades desde texto reconocido")
    SurfaceCard {
        Text("La extracción local continúa disponible sin configurar IA. Al invocar el análisis remoto, el texto del documento se envía al endpoint indicado.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(endpoint, { endpoint = it }, label = { Text("Endpoint HTTPS compatible con chat completions") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(model, { model = it }, label = { Text("Modelo") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(apiKey, { apiKey = it }, label = { Text(if (hasKey) "Nueva API key (vacío conserva la actual)" else "API key") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        Text(if (hasKey) "API key almacenada con Android Keystore." else "No hay API key configurada.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        AppButton("Guardar configuración", {
            val uri = runCatching { java.net.URI(endpoint.trim()) }.getOrNull()
            if (uri?.scheme != "https" || uri.host.isNullOrBlank() || model.isBlank()) message = "Indica un endpoint HTTPS válido y un modelo."
            else {
                AiSettings.save(context, endpoint.trim(), model.trim(), apiKey)
                hasKey = AiSettings.load(context).apiKey.isNotBlank()
                apiKey = ""
                message = "Configuración guardada."
            }
        })
        OutlinedButton(onClick = { AiSettings.clearKey(context); hasKey = false; apiKey = ""; message = "API key eliminada." }, modifier = Modifier.fillMaxWidth(), enabled = hasKey) { Text("Eliminar API key") }
        if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
