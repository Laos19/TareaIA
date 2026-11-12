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
import com.tareia.app.ui.screens.*
import com.tareia.app.ui.navigation.*
import com.tareia.app.ui.theme.*
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.launch

// Shell de la app: estado, navegación y reglas de negocio de cada destino.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TareIAApp(activity: ComponentActivity, incomingShare: IncomingShare?, onShareConsumed: () -> Unit) {
    val context = LocalContext.current
    var data by remember { mutableStateOf(AppStore.load(context)) }
    val googleAuth = remember(activity) { GoogleAuthManager(activity) }
    val authScope = rememberCoroutineScope()
    var googleLoginBusy by remember { mutableStateOf(false) }
    var googleLoginMessage by remember { mutableStateOf("") }
    // El almacenamiento actual es local, no está separado por cuentas Google.
    // Nunca mostrar el contenido de un perfil local bajo una cuenta diferente.
    var authenticated by remember {
        mutableStateOf(FirebaseAuth.getInstance().currentUser?.let { user ->
            data.profile.email.isNotBlank() && data.profile.email.equals(user.email, ignoreCase = true)
        } ?: false)
    }
    var page by rememberSaveable { mutableStateOf(Destino.Inicio) }
    var syncMessage by remember { mutableStateOf("") }
    var planTaskId by remember { mutableStateOf("") }

    fun save(next: AppSnapshot) {
        data = next
        AppStore.save(context, next)
    }
    fun duplicate(task: TaskItem, exceptId: String = ""): Boolean = data.tasks.any {
        it.id != exceptId && it.title.trim().equals(task.title.trim(), true) &&
            it.course.trim().equals(task.course.trim(), true) && it.dueDate == task.dueDate
    }
    fun addTask(task: TaskItem): Boolean {
        if (duplicate(task)) return false
        save(data.copy(tasks = listOf(task) + data.tasks))
        ReminderScheduler.schedule(context, task)
        return true
    }
    fun syncTask(task: TaskItem) {
        if (!CalendarRepository.hasPermissions(context)) {
            page = Destino.Permisos
            syncMessage = "Concede permisos de calendario y selecciona un calendario antes de sincronizar."
            return
        }
        runCatching {
            val calendarId = data.selectedCalendarId ?: CalendarRepository.calendars(context).firstOrNull()?.id
                ?: throw IllegalStateException("No hay un calendario con permiso de escritura disponible.")
            val eventId = CalendarRepository.upsert(context, task, calendarId)
            val updated = task.copy(calendarEventId = eventId)
            save(data.copy(selectedCalendarId = calendarId, tasks = data.tasks.map { if (it.id == task.id) updated else it }))
            syncMessage = "Evento sincronizado con el calendario del dispositivo."
        }.onFailure { syncMessage = it.message ?: "No se pudo sincronizar el evento." }
    }

    LaunchedEffect(incomingShare) { if (incomingShare != null) page = Destino.Importar }
    // Atrás del sistema: herramienta → Más → Inicio → salir de la app.
    BackHandler(enabled = authenticated && page != Destino.Inicio) {
        page = if (page.principal) Destino.Inicio else Destino.Mas
    }

    TareiaTheme {
        if (!authenticated) {
            AuthScreen(data.profile,
                onCreate = { profile -> save(data.copy(profile = profile)); authenticated = true },
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
                                    save(data.copy(profile = profile))
                                    authenticated = true
                                }
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
                                onAdd = { addTask(it) },
                                onEdit = { edited, replan ->
                                    if (duplicate(edited, edited.id)) false else {
                                        val oldTask = data.tasks.firstOrNull { it.id == edited.id }
                                        val shouldReplan = replan && oldTask?.dueDate != edited.dueDate
                                        val sessions = if (shouldReplan) data.sessions.filterNot { it.taskId == edited.id } +
                                            TaskPlanner.propose(edited, data.courses.firstOrNull { it.name == edited.course }, data.schedule, data.sessions.filterNot { it.taskId == edited.id })
                                        else data.sessions
                                        save(data.copy(tasks = data.tasks.map { if (it.id == edited.id) edited else it }, sessions = sessions))
                                        ReminderScheduler.schedule(context, edited)
                                        if (edited.calendarEventId != null && CalendarRepository.hasPermissions(context) && data.selectedCalendarId != null) syncTask(edited)
                                        true
                                    }
                                },
                                onDelete = { task ->
                                    if (task.calendarEventId != null) runCatching { CalendarRepository.delete(context, task.calendarEventId) }
                                    ReminderScheduler.cancel(context, task.id)
                                    val trash = TrashEntry(type = "task", label = task.title, payload = AppStore.encodeTask(task))
                                    save(data.copy(tasks = data.tasks.filterNot { it.id == task.id }, sessions = data.sessions.filterNot { it.taskId == task.id }, trash = listOf(trash) + data.trash))
                                },
                                onToggle = { task ->
                                    val changed = task.copy(status = if (task.status == "Completada") "Pendiente" else "Completada")
                                    save(data.copy(tasks = data.tasks.map { if (it.id == task.id) changed else it }))
                                    if (changed.status == "Completada") ReminderScheduler.cancel(context, task.id) else ReminderScheduler.schedule(context, changed)
                                },
                                onSync = ::syncTask,
                                onPlan = { task -> planTaskId = task.id; page = Destino.Plan }
                            )
                            Destino.Importar -> ImportScreen(
                                state = data,
                                shared = incomingShare,
                                onShareConsumed = onShareConsumed,
                                onAiExtract = { text ->
                                    val config = AiSettings.load(context)
                                    require(config.apiKey.isNotBlank()) { "Configura la IA en Más > IA antes de usarla." }
                                    AiClient.extractTasks(config, text, data.courses.map { it.name })
                                },
                                onSaveDrafts = { drafts ->
                                    val tasks = mutableListOf<TaskItem>()
                                    val notes = mutableListOf<AcademicNote>()
                                    val batchKeys = mutableSetOf<String>()
                                    var duplicates = 0
                                    drafts.forEach { draft ->
                                        if (draft.title.isBlank() || runCatching { LocalDate.parse(draft.dueDate) }.isFailure) return@forEach
                                        val task = TaskItem(title = draft.title.trim(), course = draft.course.trim(), dueDate = draft.dueDate,
                                            status = if (draft.course.isBlank()) "Pendiente de validar" else "Pendiente", source = draft.source, description = draft.description)
                                        val key = "${task.title.trim().lowercase(Locale.ROOT)}|${task.course.trim().lowercase(Locale.ROOT)}|${task.dueDate}"
                                        if (duplicate(task) || !batchKeys.add(key)) { duplicates++; return@forEach }
                                        tasks += task
                                        if (draft.course.isBlank()) notes += AcademicNote(title = draft.title, content = draft.description.ifBlank { draft.title }, folder = "Sin clasificar", source = draft.source)
                                    }
                                    if (tasks.isNotEmpty()) {
                                        save(data.copy(tasks = tasks + data.tasks, notes = notes + data.notes))
                                        tasks.forEach { ReminderScheduler.schedule(context, it) }
                                    }
                                    if (duplicates > 0) syncMessage = "$duplicates actividad(es) duplicadas fueron omitidas."
                                    tasks.size
                                },
                                onSaveUnclassified = { title, body, source ->
                                    val note = AcademicNote(title = title, content = body, folder = "Sin clasificar", source = source)
                                    save(data.copy(notes = listOf(note) + data.notes))
                                }
                            )
                            Destino.Plan -> PlannerScreen(data, planTaskId,
                                onSaveTask = { task -> addTask(task) },
                                onConfirmPlan = { newSessions -> save(data.copy(sessions = data.sessions.filterNot { old -> newSessions.any { it.taskId == old.taskId } } + newSessions)) }
                            )
                            Destino.Mas -> MoreScreen(onOpen = { page = it })
                            Destino.Cursos -> CoursesScreen(data.courses,
                                onSave = { course ->
                                    val duplicateCode = data.courses.any { it.id != course.id && it.abbreviation.equals(course.abbreviation, true) }
                                    if (duplicateCode) false else {
                                        val updated = if (data.courses.any { it.id == course.id }) data.courses.map { if (it.id == course.id) course else it } else listOf(course) + data.courses
                                        save(data.copy(courses = updated)); true
                                    }
                                },
                                onDelete = { course ->
                                    val relatedTasks = data.tasks.filter { it.course.equals(course.name, true) }
                                    val relatedNotes = data.notes.filter { it.courseId == course.id }
                                    val trash = mutableListOf(TrashEntry(type = "course", label = course.name, payload = AppStore.encodeCourse(course)))
                                    relatedTasks.forEach { trash += TrashEntry(type = "task", label = it.title, payload = AppStore.encodeTask(it)) }
                                    relatedNotes.forEach { trash += TrashEntry(type = "note", label = it.title, payload = AppStore.encodeNote(it)) }
                                    relatedTasks.forEach { task ->
                                        ReminderScheduler.cancel(context, task.id)
                                        task.calendarEventId?.let { eventId -> if (CalendarRepository.hasPermissions(context)) runCatching { CalendarRepository.delete(context, eventId) } }
                                    }
                                    save(data.copy(courses = data.courses.filterNot { it.id == course.id }, tasks = data.tasks - relatedTasks.toSet(), notes = data.notes - relatedNotes.toSet(), trash = trash + data.trash))
                                }
                            )
                            Destino.Horario -> ScheduleScreen(data.schedule, onAdd = { block ->
                                val overlap = data.schedule.any { it.dayOfWeek == block.dayOfWeek && block.startMinute < it.endMinute && it.startMinute < block.endMinute }
                                if (overlap) false else { save(data.copy(schedule = data.schedule + block)); true }
                            }, onDelete = { block -> save(data.copy(schedule = data.schedule.filterNot { it.id == block.id })) })
                            Destino.Apuntes -> NotesScreen(data, unclassifiedOnly = false,
                                onSave = { note ->
                                    val updated = if (data.notes.any { it.id == note.id }) data.notes.map { if (it.id == note.id) note else it } else listOf(note) + data.notes
                                    save(data.copy(notes = updated))
                                }, onDelete = { note ->
                                    val trash = TrashEntry(type = "note", label = note.title, payload = AppStore.encodeNote(note))
                                    save(data.copy(notes = data.notes.filterNot { it.id == note.id }, trash = listOf(trash) + data.trash))
                                }, onFolder = { folder -> if (folder !in data.folders) save(data.copy(folders = data.folders + folder)) }
                            )
                            Destino.SinClasificar -> NotesScreen(data, unclassifiedOnly = true,
                                onSave = { note -> save(data.copy(notes = data.notes.map { if (it.id == note.id) note else it })) },
                                onDelete = { note ->
                                    val trash = TrashEntry(type = "note", label = note.title, payload = AppStore.encodeNote(note))
                                    save(data.copy(notes = data.notes.filterNot { it.id == note.id }, trash = listOf(trash) + data.trash))
                                }, onFolder = { folder -> if (folder !in data.folders) save(data.copy(folders = data.folders + folder)) }
                            )
                            Destino.Papelera -> TrashScreen(data.trash,
                                onRestore = { item ->
                                    val remaining = data.trash.filterNot { it.id == item.id }
                                    runCatching {
                                        when (item.type) {
                                            "task" -> { val task = AppStore.decodeTask(item.payload); save(data.copy(tasks = listOf(task) + data.tasks, trash = remaining)); ReminderScheduler.schedule(context, task) }
                                            "course" -> save(data.copy(courses = listOf(AppStore.decodeCourse(item.payload)) + data.courses, trash = remaining))
                                            "note" -> save(data.copy(notes = listOf(AppStore.decodeNote(item.payload)) + data.notes, trash = remaining))
                                        }
                                    }
                                }, onPurge = { item -> save(data.copy(trash = data.trash.filterNot { it.id == item.id })) }
                            )
                            Destino.Integraciones -> IntegrationsScreen(data, onImportMoodle = { imported ->
                                val knownIds = data.tasks.map { it.externalId }.filter { it.isNotBlank() }.toMutableSet()
                                val fresh = imported.filter { candidate ->
                                    val sameExternal = candidate.externalId.isNotBlank() && !knownIds.add(candidate.externalId)
                                    val sameFields = data.tasks.any { it.title.equals(candidate.title, true) && it.course.equals(candidate.course, true) && it.dueDate == candidate.dueDate }
                                    !sameExternal && !sameFields
                                }.map { it.copy(status = "Pendiente de validar") }
                                if (fresh.isNotEmpty()) {
                                    save(data.copy(tasks = fresh + data.tasks)); fresh.forEach { ReminderScheduler.schedule(context, it) }
                                }
                                fresh.size
                            })
                            Destino.Calendario -> CalendarSettingsScreen(data.selectedCalendarId) { id -> save(data.copy(selectedCalendarId = id)); syncMessage = "Calendario seleccionado." }
                            Destino.IA -> AiSettingsScreen()
                            Destino.Reporte -> ReportScreen(data.tasks)
                            Destino.Perfil -> ProfileScreen(data.profile, onSave = { save(data.copy(profile = it)) })
                            Destino.Permisos -> PermissionCenter(syncMessage) { syncMessage = it }
                        }
                    }
                }
            }
        }
    }
}
