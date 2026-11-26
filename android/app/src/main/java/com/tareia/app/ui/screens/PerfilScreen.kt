package com.tareia.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.tareia.app.*
import com.tareia.app.ui.components.*

// Perfil local del estudiante.

@Composable
internal fun ProfileScreen(profile: UserProfile, onSave: (UserProfile) -> Unit) {
    var name by remember(profile.name) { mutableStateOf(profile.name) }
    var email by remember(profile.email) { mutableStateOf(profile.email) }
    var code by remember(profile.studentCode) { mutableStateOf(profile.studentCode) }
    var semester by remember(profile.semester) { mutableStateOf(profile.semester) }
    var message by remember { mutableStateOf("") }
    PageHeading("Perfil", "Administra los datos locales del estudiante")
    SurfaceCard {
        OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(email, { email = it }, label = { Text("Correo") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(code, { code = it }, label = { Text("Código de estudiante") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(semester, { semester = it }, label = { Text("Semestre") }, modifier = Modifier.fillMaxWidth())
        AppButton("Guardar perfil", { onSave(profile.copy(name = name.trim(), email = email.trim(), studentCode = code.trim(), semester = semester.trim())); message = "Perfil actualizado." })
        if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("La cuenta local se protege con un PIN cifrado en este dispositivo; no es una cuenta en la nube.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
}
