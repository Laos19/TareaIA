package com.tareia.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp
import com.tareia.app.CourseItem
import com.tareia.app.ui.theme.TareiaTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

// Campos de formulario que previenen errores en lugar de validarlos después.

private val ES = Locale.forLanguageTag("es-PE")
private val FORMATO_LARGO: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(ES)
private val FORMATO_CORTO: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d 'de' MMM yyyy", ES)

/** «2026-11-19» → «jueves, 19 de noviembre de 2026»; vacío o inválido → null. */
fun fechaLegible(iso: String): String? = runCatching { LocalDate.parse(iso).format(FORMATO_LARGO) }.getOrNull()

/** «2026-11-19» → «jue 19 de nov. 2026» (cabe en el campo); vacío o inválido → null. */
fun fechaCorta(iso: String): String? = runCatching { LocalDate.parse(iso).format(FORMATO_CORTO) }.getOrNull()

/**
 * Fecha elegida con el selector de Material en vez de escribir «AAAA-MM-DD».
 * El valor sigue siendo ISO-8601 para no cambiar el modelo ni la sincronización con el calendario.
 * El selector trabaja en UTC: se convierte sin zona horaria para no correr la fecha un día.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CampoFecha(valor: String, onCambio: (String) -> Unit, etiqueta: String = "Fecha límite", modifier: Modifier = Modifier) {
    var abierto by remember { mutableStateOf(false) }
    val legible = fechaLegible(valor)
    Box(modifier.fillMaxWidth()) {
        OutlinedTextField(
            // Visible: forma corta (cabe a cualquier escala). TalkBack: forma larga (ver capa táctil).
            value = fechaCorta(valor) ?: "", onValueChange = {}, readOnly = true, maxLines = 2,
            label = { Text(etiqueta) },
            placeholder = { Text("Elegir fecha") },
            trailingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
        )
        // Capa táctil: un campo de solo lectura no recibe el toque por sí mismo.
        Box(
            Modifier.matchParentSize()
                .clickable(onClickLabel = "Elegir fecha") { abierto = true }
                .clearAndSetSemantics {
                    contentDescription = "$etiqueta: ${legible ?: "sin elegir"}"
                    role = Role.Button
                    onClick(label = "Elegir fecha") { abierto = true; true }
                }
        )
    }
    if (abierto) {
        val inicial = runCatching { LocalDate.parse(valor) }.getOrNull() ?: LocalDate.now()
        val estado = rememberDatePickerState(initialSelectedDateMillis = inicial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { abierto = false },
            confirmButton = {
                TextButton(enabled = estado.selectedDateMillis != null, onClick = {
                    estado.selectedDateMillis?.let { onCambio(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()) }
                    abierto = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { abierto = false }) { Text("Cancelar") } },
        ) {
            // Título y fecha grande en español; meses y días siguen el idioma del dispositivo.
            DatePicker(
                estado,
                title = { Text(etiqueta, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp)) },
                headline = {
                    val elegida = estado.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().format(FORMATO_CORTO) }
                    Text(elegida ?: "Sin elegir", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 24.dp, end = 12.dp, bottom = 12.dp))
                },
            )
        }
    }
}

/**
 * Curso con lista desplegable de los cursos existentes (con su símbolo del tablero).
 * Sigue siendo editable: escribir filtra la lista y permite un curso nuevo
 * (útil cuando la IA o el OCR detectan uno que aún no está registrado).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CampoCurso(
    valor: String, onCambio: (String) -> Unit, cursos: List<CourseItem>,
    modifier: Modifier = Modifier, etiqueta: String = "Curso", permitirVacio: Boolean = true,
) {
    var expandido by remember { mutableStateOf(false) }
    val exacto = cursos.any { it.name.equals(valor.trim(), ignoreCase = true) }
    // Con un curso ya elegido se muestran todos; mientras se escribe, se filtra.
    val opciones = if (valor.isBlank() || exacto) cursos else cursos.filter { it.name.contains(valor.trim(), ignoreCase = true) }
    val colores = TareiaTheme.colores
    ExposedDropdownMenuBox(expanded = expandido, onExpandedChange = { expandido = it }, modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = valor, onValueChange = { onCambio(it); expandido = true }, singleLine = true,
            label = { Text(etiqueta) },
            supportingText = when {
                cursos.isEmpty() -> ({ Text("Aún no hay cursos. Agrégalos en Más › Cursos.") })
                valor.isNotBlank() && !exacto -> ({ Text("Curso nuevo: no está en tu tabla") })
                else -> null
            },
            leadingIcon = if (exacto) ({ SimboloCurso(simboloCurso(valor, cursos), colores.familiaDe(valor), fijo = true) }) else null,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable),
        )
        if (cursos.isNotEmpty()) ExposedDropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
            if (permitirVacio) DropdownMenuItem(text = { Text("Sin curso") }, onClick = { onCambio(""); expandido = false })
            opciones.forEach { curso ->
                DropdownMenuItem(
                    text = { Text(curso.name) },
                    leadingIcon = { SimboloCurso(simboloCurso(curso.name, cursos), colores.familiaDe(curso.name), fijo = true) },
                    onClick = { onCambio(curso.name); expandido = false },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}
