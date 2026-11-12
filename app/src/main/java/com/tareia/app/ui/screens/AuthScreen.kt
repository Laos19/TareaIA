package com.tareia.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.tareia.app.*
import com.tareia.app.ui.components.*

// RF: acceso local y Google.

@Composable
internal fun AuthScreen(
    profile: UserProfile,
    onCreate: (UserProfile) -> Unit,
    onLogin: (String) -> Boolean,
    onGoogleLogin: () -> Unit,
    googleBusy: Boolean,
    googleMessage: String
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var semester by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    // Desplazable: con letra grande o teclado abierto el formulario no cabe en pantalla.
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).imePadding(), contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 520.dp).verticalScroll(rememberScrollState()).padding(20.dp)) {
            SurfaceCard {
                CeldaMarca(lado = 72.dp)
                PageHeading("TARE·IA", "Accede con Google para administrar tus actividades académicas")
                AppButton("Continuar con Google", onGoogleLogin, enabled = !googleBusy)
                if (googleBusy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                if (googleMessage.isNotBlank()) Text(googleMessage, color = MaterialTheme.colorScheme.error)
                HorizontalDivider()
                Text("Acceso local anterior (temporal)", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                if (!profile.configured) {
                    OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(email, { email = it }, label = { Text("Correo") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(code, { code = it }, label = { Text("Código de estudiante (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(semester, { semester = it }, label = { Text("Semestre (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                } else {
                    Text("${profile.name} · ${profile.email}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedTextField(pin, { pin = it }, label = { Text(if (profile.configured) "PIN local" else "Crea un PIN de 6 o más caracteres") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
                if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                AppButton(if (profile.configured) "Ingresar" else "Crear perfil e ingresar", onClick = {
                    if (profile.configured) {
                        if (!onLogin(pin)) message = "PIN incorrecto."
                    } else runCatching { LocalAuth.create(name, email, code, semester, pin) }
                        .onSuccess(onCreate).onFailure { message = it.message ?: "Revisa los datos ingresados." }
                })
                Text("Perfil y PIN guardados localmente y cifrados en este dispositivo.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
