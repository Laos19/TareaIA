package com.tareia.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Destinos de la app. Reemplaza la navegación por cadenas (`page: String`):
 * un destino inexistente ya no compila, y el enum es Serializable, así que
 * sobrevive a rotaciones y a la muerte del proceso con `rememberSaveable`.
 */
enum class Destino(val etiqueta: String, val icono: ImageVector, val principal: Boolean = false) {
    // Barra inferior
    Inicio("Inicio", Icons.Outlined.Home, principal = true),
    Tareas("Tareas", Icons.Outlined.TaskAlt, principal = true),
    Importar("Importar", Icons.Outlined.DocumentScanner, principal = true),
    Plan("Plan", Icons.Outlined.CalendarMonth, principal = true),
    Mas("Más", Icons.Outlined.GridView, principal = true),

    // Herramientas (desde «Más»)
    Cursos("Cursos", Icons.Outlined.School),
    Horario("Horario", Icons.Outlined.Schedule),
    Apuntes("Apuntes", Icons.AutoMirrored.Outlined.StickyNote2),
    SinClasificar("Sin clasificar", Icons.Outlined.Inbox),
    Papelera("Papelera", Icons.Outlined.DeleteOutline),
    Integraciones("Integraciones", Icons.Outlined.Link),
    Calendario("Calendario", Icons.Outlined.Event),
    Reporte("Reporte", Icons.Outlined.PictureAsPdf),
    IA("IA", Icons.Outlined.AutoAwesome),
    Perfil("Perfil", Icons.Outlined.AccountCircle),
    Permisos("Permisos", Icons.Outlined.Security);

    companion object {
        val principales = entries.filter { it.principal }
        val herramientas = entries.filterNot { it.principal }
    }
}
