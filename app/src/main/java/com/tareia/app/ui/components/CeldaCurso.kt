package com.tareia.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tareia.app.CourseItem
import com.tareia.app.TaskItem
import com.tareia.app.ui.theme.Archivo
import com.tareia.app.ui.theme.EstiloCifras
import com.tareia.app.ui.theme.Familia
import com.tareia.app.ui.theme.FormaCelda
import com.tareia.app.ui.theme.TareiaTheme

// «Tablero periódico»: cada curso es un elemento con número, símbolo, nombre y familia (color).

/**
 * Celda de curso. [numero] es su posición en la lista de cursos («número atómico»),
 * [pendientes] se muestra arriba a la derecha como la «masa» del elemento.
 * El tamaño crece con la escala de fuente (hasta 1,6×) para que el texto quepa.
 */
@Composable
internal fun CeldaCurso(
    numero: Int, simbolo: String, nombre: String, pendientes: Int, familia: Familia,
    modifier: Modifier = Modifier, lado: Dp = 92.dp, onClick: (() -> Unit)? = null,
) {
    val escalaFuente = LocalDensity.current.fontScale.coerceIn(1f, 1.6f)
    val descripcion = "Curso $numero, $nombre, ${if (pendientes == 1) "1 pendiente" else "$pendientes pendientes"}"
    Box(
        modifier
            .size(lado * escalaFuente)
            .background(familia.contenedor, FormaCelda)
            .then(if (onClick != null) Modifier.clickable(onClickLabel = "Editar $nombre", onClick = onClick) else Modifier)
            .clearAndSetSemantics { contentDescription = descripcion; if (onClick != null) role = Role.Button }
            .padding(8.dp)
    ) {
        Text(numero.toString(), style = EstiloCifras, fontSize = 11.sp, color = familia.contenido, modifier = Modifier.align(Alignment.TopStart))
        if (pendientes > 0) Text(pendientes.toString(), style = EstiloCifras, fontSize = 11.sp, color = familia.contenido, modifier = Modifier.align(Alignment.TopEnd))
        Text(
            simbolo, fontFamily = Archivo, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 30.sp,
            color = familia.contenido, maxLines = 1, modifier = Modifier.align(Alignment.Center),
        )
        Text(
            nombre, style = MaterialTheme.typography.labelSmall, color = familia.contenido, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/** Símbolo pequeño del curso para tarjetas y listas (decorativo: el nombre ya se lee al lado). */
@Composable
internal fun SimboloCurso(simbolo: String, familia: Familia, modifier: Modifier = Modifier) {
    val escala = LocalDensity.current.fontScale.coerceIn(1f, 1.6f)
    Box(
        modifier.size(34.dp * escala).background(familia.contenedor, FormaCelda).clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Text(simbolo, fontFamily = Archivo, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = familia.contenido, maxLines = 1)
    }
}

/** La «tabla» de cursos: celdas en filas que se acomodan al ancho disponible. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TablaCursos(cursos: List<CourseItem>, tareas: List<TaskItem>, modifier: Modifier = Modifier, onCurso: ((CourseItem) -> Unit)? = null) {
    val colores = TareiaTheme.colores
    FlowRow(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        cursos.forEachIndexed { index, curso ->
            CeldaCurso(
                numero = index + 1,
                simbolo = simboloCurso(curso.name, cursos),
                nombre = curso.name,
                pendientes = tareas.count { it.status != "Completada" && it.course.trim().equals(curso.name.trim(), true) },
                familia = colores.familiaDe(curso.name),
                onClick = onCurso?.let { { it(curso) } },
            )
        }
    }
}
