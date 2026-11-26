package com.tareia.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.tareia.app.ui.components.PageHeading
import com.tareia.app.ui.navigation.Destino

// Menú de herramientas secundarias.

/**
 * Antes cada fila tenía un botón «Abrir» y tocar el nombre no hacía nada.
 * Ahora toda la fila es el objetivo táctil, con ícono y rol de botón.
 */
@Composable
internal fun MoreScreen(onOpen: (Destino) -> Unit) {
    PageHeading("Más herramientas", "Configura y administra tu espacio académico")
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column {
            Destino.herramientas.forEachIndexed { index, destino ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ListItem(
                    headlineContent = { Text(destino.etiqueta, style = MaterialTheme.typography.titleSmall) },
                    leadingContent = { Icon(destino.icono, contentDescription = null) },
                    trailingContent = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.clickable(role = Role.Button, onClickLabel = "Abrir ${destino.etiqueta}") { onOpen(destino) },
                )
            }
        }
    }
}
