package com.tareia.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.auth.FirebaseUser
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.tareia.app.*
import com.tareia.app.aplicacion.Dependencias
import com.tareia.app.dominio.ActividadAcademica
import com.tareia.app.dominio.ActividadBuilder
import com.tareia.app.dominio.Carpeta
import com.tareia.app.dominio.ContenidoAcademico
import com.tareia.app.dominio.FechaLimite
import com.tareia.app.dominio.PlanEstudio
import com.tareia.app.dominio.TipoContenido
import com.tareia.app.ui.screens.*
import com.tareia.app.ui.navigation.*
import com.tareia.app.ui.theme.*
import java.time.LocalDate
import java.util.Locale
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import kotlinx.coroutines.launch

import android.content.Intent
import android.net.Uri
import android.graphics.Bitmap
import android.provider.OpenableColumns
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Shell de la app: estado de la pantalla y navegación
// cada acción llama a un GESTOR
// Después de cada acción se llama a refrescar() que vuelve a leer los datos a través de los gestores

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TareIAApp(activity: ComponentActivity, incomingShare: IncomingShare?, onShareConsumed: () -> Unit) {
    val context = LocalContext.current
    // Se crean una sola vez todos los gestores, repositorios y la fábrica de IA
    val dep = remember { Dependencias(context).also { it.migrarDatosAnteriores() } }
    var perfil by remember { mutableStateOf(AppStore.cargarPerfil(context)) }
    var data by remember { mutableStateOf(construirSnapshot(dep, perfil)) }
    val googleAuth = remember(activity) { GoogleAuthManager(activity) }
    val authScope = rememberCoroutineScope()
    var googleLoginBusy by remember { mutableStateOf(false) }
    var googleLoginMessage by remember { mutableStateOf("") }
    // El almacenamiento local
    var authenticated by remember {
        mutableStateOf(FirebaseAuth.getInstance().currentUser?.let { user ->
            data.profile.email.isNotBlank() && data.profile.email.equals(user.email, ignoreCase = true)
        } ?: false)
    }
    var page by rememberSaveable { mutableStateOf(Destino.Inicio) }
    var syncMessage by remember { mutableStateOf("") }
    var planTaskId by remember { mutableStateOf("") }
    // El estado vive en la raíz: cambiar de pestaña no cancela la importacion
    var importWorkspace by remember(data.profile.email) {
        mutableStateOf(ImportWorkspaceStore.load(context, data.profile.email))
    }
    var importBusy by remember { mutableStateOf(false) }
    var nombreProveedorIA by remember { mutableStateOf(dep.gestorAnalisisIA.nombreProveedor) }

    // Estado de la planificación
    var planPropuesto by remember { mutableStateOf<PlanEstudio?>(null) }
    var idActividadPlan by remember { mutableStateOf("") }
    var actividadNuevaPlan by remember { mutableStateOf<TaskItem?>(null) }
    var mensajePlan by remember { mutableStateOf("") }
    var planBusy by remember { mutableStateOf(false) }
    var planificarConIA by remember { mutableStateOf(false) }

    fun refrescar() { data = construirSnapshot(dep, perfil) }

    fun guardarPerfil(nuevo: UserProfile) {
        perfil = nuevo
        AppStore.guardarPerfil(context, nuevo)
        dep.gestorPlanificacion.actualizarDatosEstudiante(nuevo.name, nuevo.email)
        refrescar()
    }

    fun updateImport(next: ImportWorkspace) {
        ImportWorkspaceStore.save(context, data.profile.email, next)
        importWorkspace = next
    }

    /** Texto ya reconocido: se muestran de inmediato las sugerencias sin IA (ExtractorLocal). */
    fun acceptImportedText(text: String, source: String, title: String, tipo: TipoContenido) {
        val clean = text.take(120_000)
        updateImport(ImportWorkspace(
            extractedText = clean,
            source = source,
            documentTitle = title.ifBlank { "Apunte importado" },
            drafts = dep.gestorAnalisisIA.sugerirSinIA(clean).map { it.aTaskDraft(source) },
            proposalOrigin = "Local",
            feedback = if (clean.isBlank()) "No se encontró texto legible. Prueba con otro archivo."
                       else "Texto reconocido. Puedes revisar las sugerencias locales o analizarlo con IA ($nombreProveedorIA).",
            tipoContenido = tipo.name
        ))
    }

    //1. GestorIngesta crea el contenido (patrón FACTORY METHOD: importar() -> crearContenido()).
    //2. GestorAnalisisIA le aplica OCR con el reconocedor que crea la fábrica (ABSTRACT FACTORY).

    fun procesarContenido(mensaje: String, source: String, crear: () -> ContenidoAcademico) {
        if (importBusy) return
        importBusy = true
        updateImport(importWorkspace.copy(feedback = mensaje))
        authScope.launch {
            try {
                val contenido = withContext(Dispatchers.IO) { crear() }
                val texto = dep.gestorAnalisisIA.procesarOCR(contenido)
                acceptImportedText(texto, source, contenido.titulo, contenido.tipo)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (e: Exception) {
                updateImport(importWorkspace.copy(feedback = "No se pudo leer el contenido: ${e.localizedMessage ?: "Error desconocido"}"))
            } finally { importBusy = false }
        }
    }

    fun nombreArchivo(uri: Uri): String = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull().orEmpty().ifBlank { "Apunte importado" }

    fun importDocument(uri: Uri) {
        val title = nombreArchivo(uri)
        procesarContenido("Extrayendo texto del documento...", "Documento") { dep.gestorIngesta.importarArchivo(uri, title) }
    }

    fun importCamera(bitmap: Bitmap) {
        if (importBusy) { bitmap.recycle(); return }
        procesarContenido("Reconociendo el texto de la fotografía...", "Cámara") {
            try { dep.gestorIngesta.importarFoto(bitmap) } finally { bitmap.recycle() }
        }
    }

    fun importShared(share: IncomingShare) {
        procesarContenido("Leyendo el contenido compartido...", "Compartido") {
            dep.gestorIngesta.recibirCompartido(share.text, share.uri)
        }
    }

    // IA de la fábrica elegida propone actividades (builders) para revisar
    fun analyzeImportWithAI() {
        if (importBusy || importWorkspace.extractedText.isBlank()) return
        val textToAnalyze = importWorkspace.extractedText
        importBusy = true
        updateImport(importWorkspace.copy(feedback = "$nombreProveedorIA está analizando el apunte..."))
        authScope.launch {
            try {
                val contenido = dep.gestorIngesta.contenidoEnRevision(importWorkspace.documentTitle, textToAnalyze, tipoContenidoDe(importWorkspace.tipoContenido))


                val proposals = dep.gestorAnalisisIA.extraerParametros(contenido)
                if (importWorkspace.extractedText == textToAnalyze) {
                    updateImport(importWorkspace.copy(


                        //    estudiante revisa
                        drafts = proposals.map { it.aTaskDraft("IA") },    // borrador editable
                        proposalOrigin = nombreProveedorIA,
                        feedback = if (proposals.isEmpty()) "La IA no detectó actividades explícitas. Puedes conservar el apunte sin crear tareas."
                                   else "La IA propuso ${proposals.size} actividad(es). Revisa, corrige o descarta cada una."
                    ))
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (e: Exception) {
                updateImport(importWorkspace.copy(feedback = "No se pudo analizar con IA: ${e.localizedMessage ?: "Error desconocido"}. Tus propuestas anteriores se conservaron."))
            } finally { importBusy = false }
        }
    }

    // Al pulsar guardar
    fun saveImportedNote() {
        try {
            val apunte = dep.gestorIngesta.guardarApunte(
                idExistente = importWorkspace.savedNoteId.ifBlank { null },
                titulo = importWorkspace.documentTitle,
                texto = importWorkspace.extractedText.trim(),
                tipo = tipoContenidoDe(importWorkspace.tipoContenido)
            )
            refrescar()
            updateImport(importWorkspace.copy(savedNoteId = apunte.id,
                feedback = "Apunte «${apunte.titulo}» guardado en Más → Apuntes → ${apunte.carpeta?.nombre ?: Carpeta.SIN_CLASIFICAR}."))
        } catch (e: Exception) {
            updateImport(importWorkspace.copy(feedback = "No se pudo guardar el apunte: ${e.localizedMessage ?: "Error desconocido"}"))
        }
    }

    // recien aqui cada borrador revisado se convierte en un ActividadBuilder nuevos
    fun saveImportedDrafts() {
        val drafts = importWorkspace.drafts
        val invalid = drafts.count { draft ->
            draft.title.isBlank() || runCatching { LocalDate.parse(draft.dueDate) }.isFailure
        }
        if (invalid > 0) {
            updateImport(importWorkspace.copy(feedback =
                "Corrige o descarta $invalid actividad(es) sin título o fecha antes de guardar."))
            return
        }
        if (drafts.isEmpty()) return
        try {


            val builders = drafts.map { draft ->
                ActividadBuilder()
                    .conTitulo(draft.title)
                    .conFechaLimite(FechaLimite.desdeTexto(draft.dueDate))
                    .conCurso(if (draft.course.isBlank()) null else dep.gestorActividades.resolverCurso(draft.course))
                    .conDescripcion(draft.description)
                    .conContenidoOrigen(importWorkspace.savedNoteId.ifBlank { null })
                    .desdeIA()
            }

                                       // los pasa a gestorActividades

            val resultado = dep.gestorActividades.registrarPropuestas(builders)
            // Como antes: lo que no tiene curso también queda como contenido "Sin clasificar" (RN13).
            drafts.filter { it.course.isBlank() }.forEach { draft ->
                if (resultado.guardadas.any { it.titulo == draft.title.trim() }) {
                    dep.gestorIngesta.guardarAnotacion(null, draft.title, draft.description.ifBlank { draft.title }, null, Carpeta.SIN_CLASIFICAR)
                }
            }
            refrescar()
            val n = resultado.guardadas.size
            updateImport(importWorkspace.copy(
                drafts = if (n > 0) emptyList() else drafts,
                feedback = if (n == 0) "No se guardó ninguna tarea: las propuestas ya existen. Puedes descartarlas."
                           else "$n actividad(es) guardada(s) en Tareas." +
                                if (resultado.duplicadas > 0) " ${resultado.duplicadas} duplicada(s) omitida(s)." else ""
            ))
        } catch (e: Exception) {
            updateImport(importWorkspace.copy(feedback =
                "No se pudo guardar las actividades: ${e.localizedMessage ?: "Error desconocido"}"))
        }
    }

    // OAuth Calendar está separado de Google Sign-In. Token solo en memoria.
    val googleCalendarAccess = remember(activity) { GoogleCalendarAccess(activity) }
    var googleCalendarConnectedEmail by remember { mutableStateOf("") }
    var calendarBusy by remember { mutableStateOf(false) }
    val calendarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        googleCalendarAccess.finishResolution(result.resultCode, result.data)
    }
    val googleCalendarUser = FirebaseAuth.getInstance().currentUser?.takeIf { user ->
        authenticated && !user.email.isNullOrBlank() &&
            user.email.equals(data.profile.email, ignoreCase = true)
    }
    val googleCalendarEmail = googleCalendarUser?.email
    val googleCalendarConnected = googleCalendarEmail != null &&
        googleCalendarConnectedEmail.equals(googleCalendarEmail, ignoreCase = true)

    /** El PIN local no basta: la cuenta tiene que estar autenticada en Firebase. */
    fun withCalendarToken(onToken: (String, FirebaseUser) -> Unit) {
        if (calendarBusy) { syncMessage = "Ya hay una operación de Google Calendar en curso."; return }
        val user = FirebaseAuth.getInstance().currentUser
        val email = user?.email
        if (!authenticated || user == null || email.isNullOrBlank() || !email.equals(data.profile.email, true)) {
            syncMessage = "Inicia sesión con Google para conectar tu calendario. El PIN no concede acceso a Google."
            page = Destino.Permisos
            return
        }
        calendarBusy = true
        googleCalendarAccess.request(
            email = email,
            launch = { intent -> calendarLauncher.launch(IntentSenderRequest.Builder(intent.intentSender).build()) },
            onComplete = { result ->
                result.onSuccess { token -> onToken(token, user) }
                    .onFailure { error ->
                        calendarBusy = false
                        syncMessage = error.localizedMessage ?: "No fue posible autorizar Google Calendar."
                    }
            }
        )
    }

    fun connectGoogleCalendar() {
        withCalendarToken { token, user ->
            authScope.launch {
                try {
                    GoogleCalendarApi.verify(token)
                    googleCalendarConnectedEmail = user.email.orEmpty()
                    syncMessage = "Google Calendar conectado: ${user.email}."
                } catch (e: Exception) {
                    googleCalendarConnectedEmail = ""
                    syncMessage = e.localizedMessage ?: "No se pudo comprobar la conexión con Google Calendar."
                } finally { calendarBusy = false }
            }
        }
    }

    /** RF-08: GestorSincronizacion + patrón STATE (una actividad pendiente no se sincroniza, RN01). */
    fun syncTask(taskId: String, openAfterSync: Boolean = false) {
        val actividad = dep.gestorActividades.buscar(taskId) ?: return
        if (actividad.estaPendienteDeValidar()) {
            syncMessage = "Confirma o corrige primero esta tarea antes de enviarla a Google Calendar."
            return
        }
        withCalendarToken { token, user ->
            authScope.launch {
                try {
                    val email = user.email.orEmpty()
                    val sincronizacion = dep.gestorSincronizacion(token, user.uid, email)
                    sincronizacion.sincronizar(actividad)
                    refrescar()
                    googleCalendarConnectedEmail = email
                    syncMessage = "Evento sincronizado directamente con Google Calendar ($email)."
                    if (openAfterSync) {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(sincronizacion.enlace(actividad))))
                            syncMessage = "Abriendo evento en Google Calendar..."
                        } catch (e: Exception) {
                            syncMessage = "Evento sincronizado, pero no se pudo abrir: " + (e.localizedMessage ?: "Error desconocido")
                        }
                    }
                } catch (e: Exception) {
                    syncMessage = e.localizedMessage ?: "Falló la sincronización con Google Calendar."
                } finally { calendarBusy = false }
            }
        }
    }

    /** Envía a la papelera (GestorPapelera). Si tenía evento en Google, primero lo borra. */
    fun deleteTask(task: TaskItem) {
        val actividad = dep.gestorActividades.buscar(task.id) ?: return
        val evento = actividad.evento
        if (evento == null) { dep.gestorPapelera.enviar(actividad); refrescar(); return }
        if (!evento.cuenta.equals(data.profile.email, ignoreCase = true)) {
            syncMessage = "No puedes eliminar un evento vinculado a otra cuenta Google."
            return
        }
        withCalendarToken { token, user ->
            authScope.launch {
                try {
                    dep.gestorSincronizacion(token, user.uid, user.email.orEmpty()).eliminarEvento(actividad)
                    dep.gestorPapelera.enviar(actividad)
                    refrescar()
                    syncMessage = "Actividad eliminada también de Google Calendar."
                } catch (e: Exception) {
                    syncMessage = "No se eliminó la actividad: ${e.localizedMessage ?: "falló Google Calendar"}."
                } finally { calendarBusy = false }
            }
        }
    }

    /** Al eliminar un curso, GestorPapelera también envía sus actividades y apuntes (RN14). */
    fun deleteCourse(course: CourseItem) {
        val curso = dep.gestorCursos.buscar(course.id) ?: return
        val remotas = dep.gestorActividades.listar().filter { it.curso?.id == curso.id && it.evento != null }
        if (remotas.isEmpty()) { dep.gestorPapelera.enviar(curso); refrescar(); return }
        if (remotas.any { !it.evento!!.cuenta.equals(data.profile.email, true) }) {
            syncMessage = "El curso contiene eventos asociados a otra cuenta Google."; return
        }
        withCalendarToken { token, user ->
            authScope.launch {
                try {
                    val sincronizacion = dep.gestorSincronizacion(token, user.uid, user.email.orEmpty())
                    remotas.forEach { sincronizacion.eliminarEvento(it) }
                    dep.gestorPapelera.enviar(dep.gestorCursos.buscar(curso.id) ?: curso)
                    refrescar()
                    syncMessage = "Curso y eventos vinculados eliminados de Google Calendar."
                } catch (e: Exception) {
                    syncMessage = "El curso se conservó: no se eliminaron todos los eventos de Google (${e.localizedMessage})."
                } finally { calendarBusy = false }
            }
        }
    }

    // STRATEGY: se elige la estrategia según el interruptor de la pantalla
    //
    // si la IA falla, se cambia a la local en tiempo de ejecución

    // PROTOTYPE: la propuesta es un clon del plan vigente, no se guarda hasta confirmar

    fun proponerPlan(actividad: ActividadAcademica, nueva: TaskItem?) {
        if (planBusy) return
        planBusy = true
        mensajePlan = ""
        authScope.launch {
            try {
                val estudiante = dep.gestorPlanificacion.obtenerEstudiante()
                dep.gestorPlanificacion.setEstrategia(if (planificarConIA) dep.estrategiaConIA() else dep.estrategiaLocal())
                var aviso = ""
                var propuesta = runCatching { dep.gestorPlanificacion.generarPropuesta(estudiante, actividad) }.getOrNull()
                if (planificarConIA && (propuesta == null || propuesta.sesionesDe(actividad.id).isEmpty())) {
                    dep.gestorPlanificacion.setEstrategia(dep.estrategiaLocal())
                    propuesta = dep.gestorPlanificacion.generarPropuesta(estudiante, actividad)
                    aviso = " La IA no devolvió sesiones válidas: se usó la planificación local."
                }
                val p = requireNotNull(propuesta)
                if (p.sesionesDe(actividad.id).isEmpty()) {
                    planPropuesto = null
                    mensajePlan = "No se pudo proponer sesiones. Revisa la fecha y el horario disponible (Más > Horario)."
                } else {
                    planPropuesto = p
                    idActividadPlan = actividad.id
                    actividadNuevaPlan = nueva
                    mensajePlan = "Propuesta lista (${dep.gestorPlanificacion.estrategiaActual.nombre}); aún no se guardó.$aviso"
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (e: Exception) {
                mensajePlan = "No se pudo generar la propuesta: ${e.localizedMessage ?: "Error desconocido"}"
            } finally { planBusy = false }
        }
    }

    fun confirmarPlan() {
        val propuesta = planPropuesto ?: return
        val nueva = actividadNuevaPlan
        if (nueva != null) {
            // Actividad dictada por voz: recién ahora se registra (camino manual del Builder).
            val creada = runCatching {
                dep.gestorActividades.crearManual(ActividadBuilder().conId(nueva.id).conTitulo(nueva.title)
                    .conCurso(dep.gestorActividades.resolverCurso(nueva.course))
                    .conFechaLimite(FechaLimite.desdeTexto(nueva.dueDate)))
            }.getOrNull()
            if (creada == null) { mensajePlan = "Ya existe una actividad con esos datos."; return }
        }
        dep.gestorPlanificacion.confirmarPlan(propuesta, dep.gestorPlanificacion.obtenerEstudiante())
        planPropuesto = null
        actividadNuevaPlan = null
        mensajePlan = "Planificación guardada."
        refrescar()
    }

    // Procesar el contenido compartido desde la raíz: no se pierde si se cambia de pestaña.
    LaunchedEffect(incomingShare, authenticated) {
        if (incomingShare != null && authenticated) {
            page = Destino.Importar
            importShared(incomingShare)
            onShareConsumed()
        }
    }

    // Atrás del sistema: herramienta → Más → Inicio → salir de la app.
    BackHandler(enabled = authenticated && page != Destino.Inicio) {
        page = if (page.principal) Destino.Inicio else Destino.Mas
    }

    TareiaTheme {
        if (!authenticated) {
            AuthScreen(data.profile,
                onCreate = { profile -> guardarPerfil(profile); authenticated = true },
                onLogin = { pin ->
                    val valid = LocalAuth.verify(data.profile, pin)
                    if (valid) authenticated = true
                    valid
                },
                onGoogleLogin = {
                    if (!googleLoginBusy) {
                        googleLoginBusy = true
                        googleLoginMessage = ""
                        authScope.launch {
                            try {
                                val googleUser = googleAuth.signIn()
                                val accountEmail = googleUser.email.orEmpty()
                                if (accountEmail.isBlank()) {
                                    FirebaseAuth.getInstance().signOut()
                                    googleLoginMessage = "Google no devolvió un correo válido."
                                } else if (data.profile.email.isNotBlank() &&
                                    !data.profile.email.equals(accountEmail, ignoreCase = true)
                                ) {
                                    FirebaseAuth.getInstance().signOut()
                                    googleLoginMessage = "Este dispositivo tiene datos de otro correo. Para protegerlos, entra con el mismo correo del perfil local."
                                } else {
                                    val profile = data.profile.copy(
                                        name = googleUser.displayName?.takeIf { it.isNotBlank() }
                                            ?: data.profile.name.ifBlank { "Estudiante" },
                                        email = accountEmail
                                    )
                                    guardarPerfil(profile)
                                    authenticated = true
                                    googleCalendarConnectedEmail = ""
                                }
                            } catch (e: GetCredentialCancellationException) {
                                // Antes se mostraba el texto técnico «[16] Cancelled by user.»
                                googleLoginMessage = "Cancelaste el inicio de sesión con Google."
                            } catch (e: NoCredentialException) {
                                googleLoginMessage = "No hay una cuenta de Google en este dispositivo. Agrégala en Ajustes o usa el acceso local."
                            } catch (e: Exception) {
                                googleLoginMessage = e.localizedMessage ?: "No se pudo iniciar sesión con Google."
                            } finally {
                                googleLoginBusy = false
                            }
                        }
                    }
                },
                googleBusy = googleLoginBusy,
                googleMessage = googleLoginMessage
            )
        } else {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    TopAppBar(
                        title = { Column { Text("TARE·IA", fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 22.sp); Text(data.profile.name, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, lineHeight = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
                        navigationIcon = { if (!page.principal) IconButton(onClick = { page = Destino.Mas }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver a Más") } },
                        actions = { TextButton(onClick = {
                            authenticated = false
                            page = Destino.Inicio
                            FirebaseAuth.getInstance().signOut()
                            googleCalendarConnectedEmail = ""
                            authScope.launch { runCatching { googleAuth.clearCredentialState() } }
                        }) { Text("Salir") } },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                    )
                },
                bottomBar = {
                    // Con letra muy grande (≥ 130 %) las etiquetas no caben en 5 destinos:
                    // se muestran solo íconos y el nombre pasa a contentDescription (TalkBack).
                    val soloIconos = LocalDensity.current.fontScale >= 1.3f
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        Destino.principales.forEach { destino -> NavigationBarItem(
                            selected = page == destino, onClick = { page = destino },
                            icon = { Icon(destino.icono, contentDescription = if (soloIconos) destino.etiqueta else null) },
                            label = if (soloIconos) null else ({ Text(destino.etiqueta, maxLines = 1, softWrap = false) }),
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = TareiaTheme.colores.resaltador,
                                selectedIconColor = TareiaTheme.colores.sobreResaltador,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            )
                        ) }
                    }
                }
            ) { padding ->
                // Cada destino empieza desde arriba (antes compartían la posición de scroll).
                val scroll = remember(page) { ScrollState(0) }
                BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
                    Column(
                            Modifier.fillMaxSize().padding(horizontal = Espacio.lg).widthIn(max = Espacio.anchoMax)
                            .align(Alignment.TopCenter).verticalScroll(scroll).padding(bottom = Espacio.xl),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                            when (page) {
                            Destino.Inicio -> DashboardScreen(data)
                            Destino.Tareas -> TasksScreen(
                                data = data,
                                message = syncMessage,



                                // Camino MANUAL del patrón BUILDER.
                                onAdd = { task ->
                                    val creada = runCatching {
                                        dep.gestorActividades.crearManual(ActividadBuilder().conId(task.id).conTitulo(task.title)
                                            .conCurso(dep.gestorActividades.resolverCurso(task.course))
                                            .conFechaLimite(FechaLimite.desdeTexto(task.dueDate))
                                            .conDescripcion(task.description))
                                    }.getOrNull()
                                    refrescar()
                                    creada != null
                                },
                                onEdit = { edited, replan ->
                                    val actividad = dep.gestorActividades.buscar(edited.id)
                                    if (actividad == null) false else {
                                        val fechaAnterior = actividad.fechaLimite
                                        val ok = runCatching {
                                            dep.gestorActividades.editar(actividad, edited.title, FechaLimite.desdeTexto(edited.dueDate),
                                                dep.gestorActividades.resolverCurso(edited.course), edited.description)
                                        }.getOrDefault(false)
                                        if (ok) {
                                            // RF-19: cambió la fecha y el estudiante marcó "proponer nueva planificación".
                                            // Esa casilla es su confirmación, así que el clon se confirma de inmediato.
                                            if (replan && fechaAnterior != actividad.fechaLimite) authScope.launch {
                                                runCatching {
                                                    val e = dep.gestorPlanificacion.obtenerEstudiante()
                                                    dep.gestorPlanificacion.setEstrategia(dep.estrategiaLocal())
                                                    val propuesta = dep.gestorPlanificacion.reorganizar(dep.gestorPlanificacion.planVigente(e), e, actividad)
                                                    dep.gestorPlanificacion.confirmarPlan(propuesta, e)
                                                }
                                                refrescar()
                                            }
                                            // Un evento Google existente se actualiza cuando se edita la tarea.
                                            if (actividad.evento != null) syncTask(actividad.id)
                                        }
                                        refrescar()
                                        ok
                                    }
                                },
                                onDelete = { task -> deleteTask(task) },
                                // Patrón STATE: completar / reabrir. Una actividad pendiente de validar no se completa.
                                onToggle = { task ->
                                    dep.gestorActividades.buscar(task.id)?.let { actividad ->
                                        runCatching {
                                            if (actividad.estaCompletada()) dep.gestorActividades.reabrir(actividad)
                                            else dep.gestorActividades.marcarCompletada(actividad)
                                        }.onFailure { syncMessage = it.localizedMessage ?: "No se pudo cambiar el estado." }
                                    }
                                    refrescar()
                                },
                                onSync = { task -> syncTask(task.id, openAfterSync = true) },
                                onPlan = { task -> planTaskId = task.id; page = Destino.Plan }
                            )
                            Destino.Importar -> ImportScreen(
                                state = data,
                                workspace = importWorkspace,
                                isWorking = importBusy,
                                onWorkspaceChange = { next -> updateImport(next) },
                                onDocument = { uri -> importDocument(uri) },
                                onCamera = { bitmap -> importCamera(bitmap) },
                                onAnalyze = { analyzeImportWithAI() },
                                onSaveDrafts = { saveImportedDrafts() },
                                onSaveNote = { saveImportedNote() },
                                onOpenNotes = { page = Destino.Apuntes },
                                onClear = {
                                    ImportWorkspaceStore.clear(context)
                                    importWorkspace = ImportWorkspace()
                                },
                                nombreProveedorIA = nombreProveedorIA,
                                onLocalSuggestions = {
                                    val proposals = dep.gestorAnalisisIA.sugerirSinIA(importWorkspace.extractedText)
                                    updateImport(importWorkspace.copy(
                                        drafts = proposals.map { it.aTaskDraft(importWorkspace.source) },
                                        proposalOrigin = "Local",
                                        feedback = "Se generaron ${proposals.size} propuesta(s) locales. Puedes descartarlas o corregirlas."
                                    ))
                                }
                            )
                            Destino.Plan -> PlannerScreen(
                                data = data,
                                initialTaskId = planTaskId,
                                usarIA = planificarConIA,
                                onUsarIA = { planificarConIA = it; planPropuesto = null },
                                propuesta = planPropuesto?.sesionesDe(idActividadPlan)?.map { it.aStudySession() }.orEmpty(),
                                mensaje = mensajePlan,
                                ocupado = planBusy,
                                onProponer = { id -> dep.gestorActividades.buscar(id)?.let { proponerPlan(it, null) } },
                                onProponerNueva = { nueva ->
                                    // Actividad dictada que aún no existe: se arma con el Builder (sin guardar) para planificarla.
                                    runCatching {
                                        ActividadBuilder().conId(nueva.id).conTitulo(nueva.title)
                                            .conCurso(dep.gestorCursos.listar().firstOrNull { it.nombre.equals(nueva.course.trim(), true) })
                                            .conFechaLimite(FechaLimite.desdeTexto(nueva.dueDate)).build()
                                    }.onSuccess { proponerPlan(it, nueva) }
                                        .onFailure { mensajePlan = "Completa título y fecha futura; añade disponibilidad en Más > Horario." }
                                },
                                onConfirmar = { confirmarPlan() },
                                onInterpretarVoz = { texto -> dep.gestorAnalisisIA.sugerirSinIA(texto).firstOrNull()?.aTaskDraft("Audio") }
                            )
                            Destino.Mas -> MoreScreen(onOpen = { page = it })
                            Destino.Cursos -> CoursesScreen(data.courses,
                                onSave = { course ->
                                    // RN12 (abreviatura única) la revisa GestorCursos.
                                    val ok = dep.gestorCursos.guardar(course.id, course.name, course.abbreviation, course.priority, course.difficulty)
                                    refrescar()
                                    ok
                                },
                                onDelete = { course -> deleteCourse(course) }
                            )
                            Destino.Horario -> ScheduleScreen(data.schedule, onAdd = { block ->
                                // RN16: FranjaHoraria (objeto de valor) rechaza horas inválidas; el estudiante rechaza cruces.
                                val ok = block.aFranjaHoraria()?.let { dep.gestorPlanificacion.agregarFranja(it) } ?: false
                                refrescar()
                                ok
                            }, onDelete = { block ->
                                dep.gestorPlanificacion.obtenerEstudiante().franjas
                                    .firstOrNull { it.aStudyBlock().id == block.id }
                                    ?.let { dep.gestorPlanificacion.quitarFranja(it) }
                                refrescar()
                            })
                            Destino.Apuntes, Destino.SinClasificar -> NotesScreen(data, unclassifiedOnly = page == Destino.SinClasificar,
                                onSave = { note ->
                                    runCatching {
                                        dep.gestorIngesta.guardarAnotacion(note.id, note.title, note.content, note.courseId.ifBlank { null }, note.folder)
                                    }
                                    refrescar()
                                }, onDelete = { note ->
                                    dep.gestorIngesta.buscar(note.id)?.let { dep.gestorPapelera.enviar(it) }
                                    refrescar()
                                }, onFolder = { folder -> dep.gestorIngesta.crearCarpeta(folder); refrescar() }
                            )
                            Destino.Papelera -> TrashScreen(data.trash,
                                onRestore = { item ->
                                    dep.gestorPapelera.buscar(item.id)?.let { dep.gestorPapelera.restaurar(it) }
                                    refrescar()
                                }, onPurge = { item ->
                                    dep.gestorPapelera.buscar(item.id)?.let { dep.gestorPapelera.eliminarDefinitivo(it) }
                                    refrescar()
                                }
                            )
                            Destino.Integraciones -> IntegrationsScreen(data, onImportMoodle = { imported ->
                                // Moodle entra con el Builder (desdeIntegracion) y queda pendiente de validar.
                                val builders = imported.mapNotNull { t ->
                                    runCatching {
                                        ActividadBuilder().conTitulo(t.title)
                                            .conFechaLimite(FechaLimite.desdeTexto(t.dueDate))
                                            .conCurso(dep.gestorActividades.resolverCurso(t.course))
                                            .conDescripcion(t.description)
                                            .desdeIntegracion(t.externalId)
                                    }.getOrNull()
                                }
                                val nuevas = dep.gestorActividades.importarDeIntegracion(builders)
                                refrescar()
                                nuevas
                            }, onOpenCalendar = { page = Destino.Calendario })
                            Destino.Calendario -> CalendarSettingsScreen(googleCalendarEmail,
                                googleCalendarConnected, calendarBusy, syncMessage, { connectGoogleCalendar() })
                            Destino.IA -> AiSettingsScreen(onCambioProveedor = {
                                // ABSTRACT FACTORY, cambia el proveedor y se elige otra fábrica.
                                dep.recargarFabricaIA()
                                nombreProveedorIA = dep.gestorAnalisisIA.nombreProveedor
                            })
                            Destino.Reporte -> ReportScreen(data.tasks)
                            Destino.Perfil -> ProfileScreen(data.profile, onSave = { guardarPerfil(it) })
                            Destino.Permisos -> PermissionCenter(syncMessage, { syncMessage = it },
                                googleCalendarEmail, googleCalendarConnected, calendarBusy, { connectGoogleCalendar() })
                        }
                    }
                }
            }
        }
    }
}
