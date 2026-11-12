package com.tareia.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

class MainActivity : ComponentActivity() {
    var incomingShare by mutableStateOf<IncomingShare?>(null)
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        incomingShare = shareFrom(intent)
        setContent { TareIAApp(this@MainActivity, incomingShare) { incomingShare = null } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingShare = shareFrom(intent)
    }

    private fun shareFrom(intent: Intent?): IncomingShare? {
        if (intent?.action != Intent.ACTION_SEND) return null
        val text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
        @Suppress("DEPRECATION")
        val uri = intent.getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM)
        return if (text.isNullOrBlank() && uri == null) null else IncomingShare(text, uri)
    }
}

private val Ink = Color(0xFF30363A)
private val Muted = Color(0xFF687177)
private val Accent = Color(0xFF59666C)
private val Canvas = Color(0xFFF3F2EF)
private val Taupe = Color(0xFF8E887E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TareIAApp(activity: ComponentActivity, incomingShare: IncomingShare?, onShareConsumed: () -> Unit) {
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
    var page by remember { mutableStateOf("Inicio") }
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
            page = "Permisos"
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

    LaunchedEffect(incomingShare) { if (incomingShare != null) page = "Importar" }

    MaterialTheme(
        colorScheme = lightColorScheme(primary = Accent, onPrimary = Color.White, secondary = Taupe,
            background = Canvas, surface = Color.White, onSurface = Ink, onBackground = Ink,
            outline = Color(0xFFE3E1DC)),
        shapes = Shapes(medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(20.dp))
    ) {
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
            val mainPages = listOf("Inicio", "Tareas", "Importar", "Plan", "Más")
            Scaffold(
                containerColor = Canvas,
                topBar = {
                    TopAppBar(
                        title = { Column { Text("TARE·IA", fontWeight = FontWeight.SemiBold); Text(data.profile.name, color = Muted, fontSize = 12.sp) } },
                        navigationIcon = { if (page !in mainPages) TextButton(onClick = { page = "Más" }) { Text("‹ Volver") } },
                        actions = { TextButton(onClick = {
                            authenticated = false
                            page = "Inicio"
                            FirebaseAuth.getInstance().signOut()
                            authScope.launch { runCatching { googleAuth.clearCredentialState() } }
                        }) { Text("Salir") } },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Canvas)
                    )
                },
                bottomBar = {
                    NavigationBar(containerColor = Color.White) {
                        mainPages.forEach { item -> NavigationBarItem(
                            selected = page == item, onClick = { page = item }, icon = {},
                            label = { Text(item, fontSize = 11.sp, maxLines = 1) }
                        ) }
                    }
                }
            ) { padding ->
                BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
                    Column(
                        Modifier.fillMaxSize().padding(horizontal = 16.dp).widthIn(max = 760.dp)
                            .align(Alignment.TopCenter).verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        when (page) {
                            "Inicio" -> DashboardScreen(data)
                            "Tareas" -> TasksScreen(
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
                                onPlan = { task -> planTaskId = task.id; page = "Plan" }
                            )
                            "Importar" -> ImportScreen(
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
                            "Plan" -> PlannerScreen(data, planTaskId,
                                onSaveTask = { task -> addTask(task) },
                                onConfirmPlan = { newSessions -> save(data.copy(sessions = data.sessions.filterNot { old -> newSessions.any { it.taskId == old.taskId } } + newSessions)) }
                            )
                            "Más" -> MoreScreen(onOpen = { page = it })
                            "Cursos" -> CoursesScreen(data.courses,
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
                            "Horario" -> ScheduleScreen(data.schedule, onAdd = { block ->
                                val overlap = data.schedule.any { it.dayOfWeek == block.dayOfWeek && block.startMinute < it.endMinute && it.startMinute < block.endMinute }
                                if (overlap) false else { save(data.copy(schedule = data.schedule + block)); true }
                            }, onDelete = { block -> save(data.copy(schedule = data.schedule.filterNot { it.id == block.id })) })
                            "Apuntes" -> NotesScreen(data, unclassifiedOnly = false,
                                onSave = { note ->
                                    val updated = if (data.notes.any { it.id == note.id }) data.notes.map { if (it.id == note.id) note else it } else listOf(note) + data.notes
                                    save(data.copy(notes = updated))
                                }, onDelete = { note ->
                                    val trash = TrashEntry(type = "note", label = note.title, payload = AppStore.encodeNote(note))
                                    save(data.copy(notes = data.notes.filterNot { it.id == note.id }, trash = listOf(trash) + data.trash))
                                }, onFolder = { folder -> if (folder !in data.folders) save(data.copy(folders = data.folders + folder)) }
                            )
                            "Sin clasificar" -> NotesScreen(data, unclassifiedOnly = true,
                                onSave = { note -> save(data.copy(notes = data.notes.map { if (it.id == note.id) note else it })) },
                                onDelete = { note ->
                                    val trash = TrashEntry(type = "note", label = note.title, payload = AppStore.encodeNote(note))
                                    save(data.copy(notes = data.notes.filterNot { it.id == note.id }, trash = listOf(trash) + data.trash))
                                }, onFolder = { folder -> if (folder !in data.folders) save(data.copy(folders = data.folders + folder)) }
                            )
                            "Papelera" -> TrashScreen(data.trash,
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
                            "Integraciones" -> IntegrationsScreen(data, onImportMoodle = { imported ->
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
                            "Calendario" -> CalendarSettingsScreen(data.selectedCalendarId) { id -> save(data.copy(selectedCalendarId = id)); syncMessage = "Calendario seleccionado." }
                            "IA" -> AiSettingsScreen()
                            "Reporte" -> ReportScreen(data.tasks)
                            "Perfil" -> ProfileScreen(data.profile, onSave = { save(data.copy(profile = it)) })
                            "Permisos" -> PermissionCenter(syncMessage) { syncMessage = it }
                            else -> DashboardScreen(data)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PageHeading(title: String, subtitle: String = "") {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, color = Ink)
        if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Muted)
    }
}

@Composable
private fun SurfaceCard(content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
private fun AppButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier.fillMaxWidth()) { Text(text) }
}

@Composable
private fun AuthScreen(
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
    MaterialTheme(colorScheme = lightColorScheme(primary = Accent, background = Canvas, surface = Color.White, onSurface = Ink)) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
            SurfaceCard {
                PageHeading("TARE·IA", "Accede con Google para administrar tus actividades académicas")
                AppButton("Continuar con Google", onGoogleLogin, enabled = !googleBusy)
                if (googleBusy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                if (googleMessage.isNotBlank()) Text(googleMessage, color = MaterialTheme.colorScheme.error)
                HorizontalDivider()
                Text("Acceso local anterior (temporal)", color = Muted, fontSize = 12.sp)
                if (!profile.configured) {
                    OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(email, { email = it }, label = { Text("Correo") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(code, { code = it }, label = { Text("Código de estudiante (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(semester, { semester = it }, label = { Text("Semestre (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                } else {
                    Text("${profile.name} · ${profile.email}", color = Muted)
                }
                OutlinedTextField(pin, { pin = it }, label = { Text(if (profile.configured) "PIN local" else "Crea un PIN de 6 o más caracteres") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
                if (message.isNotBlank()) Text(message, color = Muted)
                AppButton(if (profile.configured) "Ingresar" else "Crear perfil e ingresar", onClick = {
                    if (profile.configured) {
                        if (!onLogin(pin)) message = "PIN incorrecto."
                    } else runCatching { LocalAuth.create(name, email, code, semester, pin) }
                        .onSuccess(onCreate).onFailure { message = it.message ?: "Revisa los datos ingresados." }
                })
                Text("Perfil y PIN guardados localmente y cifrados en este dispositivo.", color = Muted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun DashboardScreen(data: AppSnapshot) {
    val today = LocalDate.now().toString()
    val pending = data.tasks.filter { it.status != "Completada" }
    PageHeading("Hola, ${data.profile.name.substringBefore(' ')}", "Resumen de tu actividad académica")
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MetricCard("Pendientes", pending.size.toString(), Modifier.weight(1f))
        MetricCard("Cursos", data.courses.size.toString(), Modifier.weight(1f))
        MetricCard("Esta semana", pending.count { it.dueDate >= today && runCatching { LocalDate.parse(it.dueDate).isBefore(LocalDate.now().plusDays(8)) }.getOrDefault(false) }.toString(), Modifier.weight(1f))
    }
    Text("Próximas entregas", fontWeight = FontWeight.SemiBold)
    val dueSoon = pending.filter { it.dueDate.isNotBlank() }.sortedBy { it.dueDate }.take(6)
    if (dueSoon.isEmpty()) SurfaceCard { Text("No hay entregas próximas. Importa un apunte o registra una actividad.", color = Muted) }
    dueSoon.forEach { TaskCard(it) }
    Text("Sesiones planificadas", fontWeight = FontWeight.SemiBold)
    data.sessions.sortedWith(compareBy<StudySession> { it.date }.thenBy { it.startMinute }).take(5).forEach { session ->
        SurfaceCard { Text(session.title, fontWeight = FontWeight.Medium); Text("${session.date} · ${formatMinute(session.startMinute)} · ${session.durationMinutes} min", color = Muted) }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(label, fontSize = 11.sp, color = Muted)
        }
    }
}

@Composable
private fun TaskCard(task: TaskItem, onToggle: (() -> Unit)? = null, onEdit: (() -> Unit)? = null,
                     onDelete: (() -> Unit)? = null, onSync: (() -> Unit)? = null, onPlan: (() -> Unit)? = null) {
    SurfaceCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(task.title, fontWeight = FontWeight.SemiBold, color = Ink)
                Text(task.course.ifBlank { "Sin curso" }, color = Muted)
                Text("Entrega: ${task.dueDate.ifBlank { "Sin fecha" }}", color = Muted, fontSize = 13.sp)
            }
            AssistChip(onClick = {}, label = { Text(task.status, fontSize = 11.sp) })
        }
        Text("Origen: ${task.source}${if (task.calendarEventId != null) " · Sincronizada" else ""}", color = Muted, fontSize = 11.sp)
        if (onToggle != null || onEdit != null || onDelete != null || onSync != null || onPlan != null) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (onToggle != null) TextButton(onClick = onToggle) { Text(if (task.status == "Completada") "Pendiente" else "Completar") }
                if (onEdit != null) TextButton(onClick = onEdit) { Text("Editar") }
                if (onPlan != null) TextButton(onClick = onPlan) { Text("Planificar") }
                if (onSync != null) TextButton(onClick = onSync) { Text("Calendario") }
                if (onDelete != null) TextButton(onClick = onDelete) { Text("Eliminar") }
            }
        }
    }
}

@Composable
private fun TasksScreen(
    data: AppSnapshot, message: String, onAdd: (TaskItem) -> Boolean,
    onEdit: (TaskItem, Boolean) -> Boolean, onDelete: (TaskItem) -> Unit,
    onToggle: (TaskItem) -> Unit, onSync: (TaskItem) -> Unit, onPlan: (TaskItem) -> Unit
) {
    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<TaskItem?>(null) }
    var feedback by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Todas") }
    PageHeading("Actividades", "Crear, validar, planificar y sincronizar tareas")
    AppButton("Nueva actividad", { editing = null; showEditor = true })
    if (message.isNotBlank()) Text(message, color = Muted)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Todas", "Pendiente", "Pendiente de validar", "Completada").forEach { filter ->
            FilterChip(selected = status == filter, onClick = { status = filter }, label = { Text(filter) })
        }
    }
    if (feedback.isNotBlank()) Text(feedback, color = Muted)
    val rows = data.tasks.filter { status == "Todas" || it.status == status }.sortedBy { it.dueDate }
    if (rows.isEmpty()) SurfaceCard { Text("No hay actividades en este filtro.", color = Muted) }
    rows.forEach { task -> TaskCard(task,
        onToggle = { onToggle(task) },
        onEdit = { editing = task; showEditor = true },
        onDelete = { onDelete(task) },
        onSync = { onSync(task) },
        onPlan = { onPlan(task) }
    ) }
    if (showEditor) TaskEditorDialog(editing, data.courses, onDismiss = { showEditor = false }, onSave = { task, replan ->
        val ok = if (editing == null) onAdd(task) else onEdit(task, replan)
        if (!ok) feedback = "Ya existe una actividad con el mismo título, curso y fecha."
        else { feedback = "Actividad guardada."; showEditor = false }
    })
}

@Composable
private fun TaskEditorDialog(task: TaskItem?, courses: List<CourseItem>, onDismiss: () -> Unit, onSave: (TaskItem, Boolean) -> Unit) {
    var title by remember(task?.id) { mutableStateOf(task?.title.orEmpty()) }
    var course by remember(task?.id) { mutableStateOf(task?.course.orEmpty()) }
    var date by remember(task?.id) { mutableStateOf(task?.dueDate ?: LocalDate.now().plusDays(7).toString()) }
    var details by remember(task?.id) { mutableStateOf(task?.description.orEmpty()) }
    var replan by remember(task?.id) { mutableStateOf(true) }
    val validDate = runCatching { LocalDate.parse(date) }.isSuccess
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (task == null) "Nueva actividad" else "Editar actividad") },
        text = { Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(title, { title = it }, label = { Text("Título") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(course, { course = it }, label = { Text("Curso") }, singleLine = true, supportingText = { Text(courses.joinToString { it.name }.ifBlank { "Configura cursos en Más > Cursos" }) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(date, { date = it }, label = { Text("Fecha límite (AAAA-MM-DD)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(details, { details = it }, label = { Text("Descripción (opcional)") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            if (task != null && task.dueDate != date) Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(replan, { replan = it }); Text("Proponer una nueva planificación al guardar", fontSize = 13.sp)
            }
            if (!validDate) Text("Escribe una fecha válida.", color = Muted)
        } },
        confirmButton = { TextButton(enabled = title.isNotBlank() && validDate, onClick = {
            onSave((task ?: TaskItem(title = title.trim(), course = course.trim(), dueDate = date.trim())).copy(
                title = title.trim(), course = course.trim(), dueDate = date.trim(), description = details.trim()
            ), replan)
        }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun ImportScreen(state: AppSnapshot, shared: IncomingShare?, onShareConsumed: () -> Unit,
    onAiExtract: (String) -> List<TaskDraft>,
                         onSaveDrafts: (List<TaskDraft>) -> Int, onSaveUnclassified: (String, String, String) -> Unit) {
    val context = LocalContext.current
    var extractedText by remember { mutableStateOf("") }
    val drafts = remember { mutableStateListOf<TaskDraft>() }
    var isWorking by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf("") }
    var selectedSource by remember { mutableStateOf("Documento") }

    fun acceptText(text: String, source: String) {
        extractedText = text
        selectedSource = source
        drafts.clear()
        drafts.addAll(TaskExtractor.extract(text, state.courses, source))
        feedback = if (text.isBlank()) "No se pudo extraer texto legible. Puedes escribir los datos manualmente." else "Revisa cada propuesta; la extracción puede contener errores."
    }
    fun runOcr(uri: android.net.Uri, source: String) {
        isWorking = true; feedback = "Procesando documento e imagen con OCR local…"
        Thread {
            val result = runCatching { DocumentProcessor.extractText(context, uri) }
            Handler(Looper.getMainLooper()).post {
                isWorking = false
                result.onSuccess { acceptText(it, source) }.onFailure { feedback = it.message ?: "No se pudo procesar el documento." }
            }
        }.start()
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) runOcr(uri, "Documento") }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            isWorking = true; feedback = "Reconociendo el texto de la captura…"
            Thread {
                val result = runCatching { DocumentProcessor.extractBitmap(bitmap) }
                Handler(Looper.getMainLooper()).post { isWorking = false; result.onSuccess { acceptText(it, "Cámara") }.onFailure { feedback = it.message ?: "No se pudo procesar la imagen." }; bitmap.recycle() }
            }.start()
        }
    }
    LaunchedEffect(shared) {
        if (shared != null) {
            if (shared.uri != null) runOcr(shared.uri, "Compartido")
            else shared.text?.let { acceptText(it, "Compartido") }
            onShareConsumed()
        }
    }

    PageHeading("Importar y reconocer", "PDF, imagen, texto o contenido compartido desde otra aplicación")
    SurfaceCard {
        Text("El OCR se ejecuta en el dispositivo. Los datos detectados siempre requieren revisión antes de guardarse.", color = Muted)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { picker.launch(arrayOf("application/pdf", "image/*", "text/plain")) }, modifier = Modifier.weight(1f)) { Text("Elegir archivo") }
            OutlinedButton(onClick = { camera.launch(null) }, modifier = Modifier.weight(1f)) { Text("Tomar foto") }
        }
        if (isWorking) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (feedback.isNotBlank()) Text(feedback, color = Muted, fontSize = 13.sp)
    }
    if (extractedText.isNotBlank()) {
        SurfaceCard {
            Text("Texto reconocido", fontWeight = FontWeight.SemiBold)
            OutlinedTextField(extractedText, { extractedText = it }, minLines = 4, modifier = Modifier.fillMaxWidth())
            OutlinedButton(onClick = {
                isWorking = true
                feedback = "Enviando el texto al proveedor de IA configurado…"
                val sourceText = extractedText
                Thread {
                    val result = runCatching { onAiExtract(sourceText) }
                    Handler(Looper.getMainLooper()).post {
                        isWorking = false
                        result.onSuccess { proposals -> drafts.clear(); drafts.addAll(proposals); feedback = "La IA propuso ${proposals.size} actividad(es). Revisa y corrige cada campo antes de guardar." }
                            .onFailure { feedback = it.message ?: "No se pudo analizar con IA." }
                    }
                }.start()
            }, enabled = !isWorking, modifier = Modifier.fillMaxWidth()) { Text("Analizar texto con IA configurada") }
            Text("Al usar esta opción, el texto reconocido se envía al proveedor que configures en Más > IA.", color = Muted, fontSize = 12.sp)
            if (drafts.isEmpty()) {
                AppButton("Guardar como contenido sin clasificar", {
                    onSaveUnclassified("Importación $selectedSource", extractedText, selectedSource)
                    feedback = "Contenido guardado en Sin clasificar."
                })
            }
        }
        drafts.forEachIndexed { index, draft ->
            DraftEditor(draft, state.courses, onChange = { drafts[index] = it })
        }
        if (drafts.isNotEmpty()) AppButton("Confirmar y guardar ${drafts.size} propuesta(s)", {
            val count = onSaveDrafts(drafts.toList())
            feedback = "$count actividad(es) guardada(s). Revisa las que quedaron sin clasificar."
            drafts.clear()
        })
        AppButton("Guardar el texto completo en Apuntes", {
            onSaveUnclassified("Apunte importado", extractedText, selectedSource)
            feedback = "Apunte guardado."
        })
    }
}

@Composable
private fun DraftEditor(draft: TaskDraft, courses: List<CourseItem>, onChange: (TaskDraft) -> Unit) {
    SurfaceCard {
        Text("Propuesta detectada", fontWeight = FontWeight.SemiBold)
        OutlinedTextField(draft.title, { onChange(draft.copy(title = it)) }, label = { Text("Actividad") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(draft.course, { onChange(draft.copy(course = it)) }, label = { Text("Curso (si no se reconoce, quedará sin clasificar)") },
            supportingText = { Text(courses.joinToString { it.name }.ifBlank { "Añade cursos en Más > Cursos" }) }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(draft.dueDate, { onChange(draft.copy(dueDate = it)) }, label = { Text("Fecha límite (AAAA-MM-DD)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Text("Fuente: ${draft.source}", fontSize = 12.sp, color = Muted)
    }
}

@Composable
private fun PlannerScreen(data: AppSnapshot, initialTaskId: String, onSaveTask: (TaskItem) -> Boolean, onConfirmPlan: (List<StudySession>) -> Unit) {
    val context = LocalContext.current
    var selectedId by remember(data.tasks, initialTaskId) { mutableStateOf(initialTaskId.takeIf { id -> data.tasks.any { it.id == id } } ?: data.tasks.firstOrNull()?.id.orEmpty()) }
    var transcript by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var course by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    var proposal by remember { mutableStateOf<List<StudySession>>(emptyList()) }
    var message by remember { mutableStateOf("") }
    var listening by remember { mutableStateOf(false) }
    var hasMic by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) }
    val recognizer = remember(context) { if (SpeechRecognizer.isRecognitionAvailable(context)) SpeechRecognizer.createSpeechRecognizer(context) else null }
    DisposableEffect(recognizer) { onDispose { recognizer?.destroy() } }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> hasMic = granted; if (!granted) message = "Permiso de micrófono denegado; puedes escribir los datos manualmente." }

    fun startVoice() {
        val engine = recognizer
        if (engine == null) { message = "El reconocimiento de voz no está disponible."; return }
        engine.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { listening = true }
            override fun onBeginningOfSpeech() { listening = true }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { listening = false }
            override fun onError(error: Int) { listening = false; message = "No se reconoció el audio. Intenta de nuevo o escribe la instrucción." }
            override fun onResults(results: Bundle?) {
                listening = false
                transcript = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                val candidate = TaskExtractor.extract(transcript, data.courses, "Audio").firstOrNull()
                if (candidate != null) { title = candidate.title; course = candidate.course; dueDate = candidate.dueDate }
                message = "Revisa la transcripción, el curso y la fecha antes de generar el plan."
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-PE")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Dicta la tarea, el curso y la fecha límite")
        }
        runCatching { engine.startListening(intent); listening = true }.onFailure { message = "No se pudo iniciar el micrófono." }
    }

    PageHeading("Planificar", "Genera sesiones con fechas, dificultad, prioridad y horario disponible")
    SurfaceCard {
        Text("Plan para una actividad existente", fontWeight = FontWeight.SemiBold)
        if (data.tasks.isEmpty()) Text("Primero registra una tarea o dicta una nueva.", color = Muted)
        data.tasks.forEach { task ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selectedId == task.id, onClick = { selectedId = task.id; proposal = emptyList() })
                Column { Text(task.title, fontWeight = FontWeight.Medium); Text("${task.course} · ${task.dueDate}", color = Muted, fontSize = 12.sp) }
            }
        }
        AppButton("Generar propuesta", {
            val task = data.tasks.firstOrNull { it.id == selectedId }
            if (task != null) {
                val sessions = TaskPlanner.propose(task, data.courses.firstOrNull { it.name == task.course }, data.schedule, data.sessions.filterNot { it.taskId == task.id })
                proposal = sessions
                message = if (sessions.isEmpty()) "No se pudo proponer sesiones. Revisa la fecha y el horario disponible." else "Propuesta lista; aún no se guardó."
            }
        }, enabled = selectedId.isNotBlank())
    }
    SurfaceCard {
        Text("Crear planificación por audio", fontWeight = FontWeight.SemiBold)
        Text("Dicta una instrucción o escribe los datos; la transcripción se puede corregir.", color = Muted)
        OutlinedButton(onClick = { if (hasMic) startVoice() else permission.launch(Manifest.permission.RECORD_AUDIO) }, enabled = !listening, modifier = Modifier.fillMaxWidth()) { Text(if (listening) "Escuchando…" else "Dictar") }
        if (listening) LinearProgressIndicator(Modifier.fillMaxWidth())
        OutlinedTextField(transcript, { transcript = it }, label = { Text("Transcripción") }, minLines = 2, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(title, { title = it }, label = { Text("Actividad") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(course, { course = it }, label = { Text("Curso") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(dueDate, { dueDate = it }, label = { Text("Fecha límite (AAAA-MM-DD)") }, modifier = Modifier.fillMaxWidth())
        AppButton("Proponer sesiones desde audio", {
            val task = TaskItem(title = title.trim(), course = course.trim(), dueDate = dueDate.trim(), source = "Audio")
            val generated = TaskPlanner.propose(task, data.courses.firstOrNull { it.name == course }, data.schedule, data.sessions)
            if (generated.isNotEmpty()) { proposal = generated; selectedId = task.id; message = "Propuesta preparada. Confirma para guardar la actividad y las sesiones." }
            else message = "Completa título y fecha futura; añade disponibilidad en Más > Horario."
        }, enabled = title.isNotBlank() && runCatching { LocalDate.parse(dueDate) }.isSuccess)
    }
    if (message.isNotBlank()) Text(message, color = Muted)
    if (proposal.isNotEmpty()) {
        SurfaceCard {
            Text("Propuesta de ${proposal.size} sesión(es)", fontWeight = FontWeight.SemiBold)
            proposal.forEach { Text("${it.date} · ${formatMinute(it.startMinute)} · ${it.durationMinutes} min · ${it.title}") }
            AppButton("Confirmar planificación", {
                val isNewAudio = proposal.any { it.taskId == selectedId } && data.tasks.none { it.id == selectedId }
                if (isNewAudio) {
                    val task = TaskItem(id = selectedId, title = title.trim(), course = course.trim(), dueDate = dueDate.trim(), source = "Audio")
                    if (onSaveTask(task)) onConfirmPlan(proposal) else message = "Ya existe una actividad con esos datos."
                } else onConfirmPlan(proposal)
                proposal = emptyList()
            })
        }
    }
}

@Composable
private fun MoreScreen(onOpen: (String) -> Unit) {
    PageHeading("Más herramientas", "Configura y administra tu espacio académico")
    listOf("Cursos", "Horario", "Apuntes", "Sin clasificar", "Papelera", "Integraciones", "Calendario", "Reporte", "IA", "Perfil", "Permisos").forEach { item ->
        SurfaceCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(item, fontWeight = FontWeight.Medium)
                TextButton(onClick = { onOpen(item) }) { Text("Abrir") }
            }
        }
    }
}

@Composable
private fun CoursesScreen(courses: List<CourseItem>, onSave: (CourseItem) -> Boolean, onDelete: (CourseItem) -> Unit) {
    var dialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<CourseItem?>(null) }
    var feedback by remember { mutableStateOf("") }
    PageHeading("Cursos", "Organiza actividades y define dificultad y prioridad")
    AppButton("Agregar curso", { editing = null; dialog = true })
    if (feedback.isNotBlank()) Text(feedback, color = Muted)
    courses.forEach { course -> SurfaceCard {
        Text(course.name, fontWeight = FontWeight.SemiBold)
        Text("Abreviatura: ${course.abbreviation} · Prioridad: ${course.priority}/5 · Dificultad: ${course.difficulty}/5", color = Muted)
        Row {
            TextButton(onClick = { editing = course; dialog = true }) { Text("Editar") }
            TextButton(onClick = { onDelete(course) }) { Text("Eliminar a papelera") }
        }
    } }
    if (dialog) CourseDialog(editing, onDismiss = { dialog = false }, onSave = {
        if (onSave(it)) { feedback = "Curso guardado."; dialog = false } else feedback = "Esa abreviatura ya está asignada a otro curso."
    })
}

@Composable
private fun CourseDialog(course: CourseItem?, onDismiss: () -> Unit, onSave: (CourseItem) -> Unit) {
    var name by remember(course?.id) { mutableStateOf(course?.name.orEmpty()) }
    var abbreviation by remember(course?.id) { mutableStateOf(course?.abbreviation.orEmpty()) }
    var priority by remember(course?.id) { mutableIntStateOf(course?.priority ?: 3) }
    var difficulty by remember(course?.id) { mutableIntStateOf(course?.difficulty ?: 3) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (course == null) "Nuevo curso" else "Editar curso") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(abbreviation, { abbreviation = it.uppercase() }, label = { Text("Abreviatura única") }, modifier = Modifier.fillMaxWidth())
            Text("Prioridad académica: $priority/5"); Slider(priority.toFloat(), { priority = it.toInt().coerceIn(1, 5) }, valueRange = 1f..5f, steps = 3)
            Text("Dificultad percibida: $difficulty/5"); Slider(difficulty.toFloat(), { difficulty = it.toInt().coerceIn(1, 5) }, valueRange = 1f..5f, steps = 3)
        } },
        confirmButton = { TextButton(enabled = name.isNotBlank() && abbreviation.isNotBlank(), onClick = {
            onSave((course ?: CourseItem(name = name.trim(), abbreviation = abbreviation.trim())).copy(name = name.trim(), abbreviation = abbreviation.trim(), priority = priority, difficulty = difficulty))
        }) { Text("Guardar") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

@Composable
private fun ScheduleScreen(blocks: List<StudyBlock>, onAdd: (StudyBlock) -> Boolean, onDelete: (StudyBlock) -> Unit) {
    val weekdays = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
    var day by remember { mutableIntStateOf(1) }
    var start by remember { mutableStateOf("18:00") }
    var end by remember { mutableStateOf("20:00") }
    var label by remember { mutableStateOf("Disponible") }
    var message by remember { mutableStateOf("") }
    PageHeading("Horario disponible", "Añade bloques libres; el planificador no usará horas ocupadas")
    SurfaceCard {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            weekdays.forEachIndexed { index, name -> FilterChip(day == index + 1, { day = index + 1 }, label = { Text(name.take(3)) }) }
        }
        OutlinedTextField(start, { start = it }, label = { Text("Inicio (HH:mm)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(end, { end = it }, label = { Text("Fin (HH:mm)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(label, { label = it }, label = { Text("Descripción") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        AppButton("Agregar bloque disponible", {
            val a = parseMinute(start); val b = parseMinute(end)
            if (a == null || b == null || b <= a) message = "Revisa el formato y el intervalo horario."
            else {
                val block = StudyBlock(dayOfWeek = day, startMinute = a, endMinute = b, label = label.ifBlank { "Disponible" })
                message = if (onAdd(block)) "Bloque guardado." else "Ese horario se cruza con otro bloque."
            }
        })
        if (message.isNotBlank()) Text(message, color = Muted)
    }
    blocks.sortedWith(compareBy<StudyBlock> { it.dayOfWeek }.thenBy { it.startMinute }).forEach { block ->
        SurfaceCard {
            Text("${weekdays.getOrElse(block.dayOfWeek - 1) { "Día" }} · ${formatMinute(block.startMinute)}–${formatMinute(block.endMinute)}", fontWeight = FontWeight.Medium)
            Text(block.label, color = Muted)
            TextButton(onClick = { onDelete(block) }) { Text("Eliminar") }
        }
    }
}

@Composable
private fun NotesScreen(data: AppSnapshot, unclassifiedOnly: Boolean, onSave: (AcademicNote) -> Unit,
                        onDelete: (AcademicNote) -> Unit, onFolder: (String) -> Unit) {
    var dialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<AcademicNote?>(null) }
    var folderName by remember { mutableStateOf("") }
    PageHeading(if (unclassifiedOnly) "Sin clasificar" else "Apuntes y carpetas", "Guarda notas breves y asígnalas a un curso o carpeta")
    if (!unclassifiedOnly) {
        AppButton("Nueva anotación", { editing = null; dialog = true })
        SurfaceCard {
            Text("Crear carpeta", fontWeight = FontWeight.Medium)
            OutlinedTextField(folderName, { folderName = it }, label = { Text("Nombre de carpeta") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            TextButton(onClick = { if (folderName.isNotBlank()) { onFolder(folderName.trim()); folderName = "" } }) { Text("Agregar carpeta") }
            Text("Carpetas: ${data.folders.joinToString()}", color = Muted, fontSize = 12.sp)
        }
    }
    val rows = if (unclassifiedOnly) data.notes.filter { it.folder == "Sin clasificar" || it.courseId.isBlank() } else data.notes
    if (rows.isEmpty()) SurfaceCard { Text(if (unclassifiedOnly) "No hay contenido pendiente de clasificar." else "Aún no hay apuntes.", color = Muted) }
    rows.forEach { note -> SurfaceCard {
        Text(note.title, fontWeight = FontWeight.SemiBold)
        Text("${data.courses.firstOrNull { it.id == note.courseId }?.name ?: note.folder} · ${note.source}", color = Muted, fontSize = 12.sp)
        Text(note.content.take(400), color = Ink)
        Row {
            TextButton(onClick = { editing = note; dialog = true }) { Text(if (unclassifiedOnly) "Clasificar" else "Editar") }
            TextButton(onClick = { onDelete(note) }) { Text("Eliminar") }
        }
    } }
    if (dialog) NoteDialog(editing, data, onDismiss = { dialog = false }, onSave = { onSave(it); dialog = false })
}

@Composable
private fun NoteDialog(note: AcademicNote?, data: AppSnapshot, onDismiss: () -> Unit, onSave: (AcademicNote) -> Unit) {
    var title by remember(note?.id) { mutableStateOf(note?.title.orEmpty()) }
    var content by remember(note?.id) { mutableStateOf(note?.content.orEmpty()) }
    var courseName by remember(note?.id) { mutableStateOf(data.courses.firstOrNull { it.id == note?.courseId }?.name.orEmpty()) }
    var folder by remember(note?.id) { mutableStateOf(note?.folder ?: "Sin clasificar") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (note == null) "Nueva anotación" else "Editar / clasificar") },
        text = { Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(title, { title = it }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(content, { content = it }, label = { Text("Contenido") }, minLines = 3, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(courseName, { courseName = it }, label = { Text("Curso (opcional)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(folder, { folder = it }, label = { Text("Carpeta") }, modifier = Modifier.fillMaxWidth())
            Text("Cursos: ${data.courses.joinToString { it.name }} · Carpetas: ${data.folders.joinToString()}", color = Muted, fontSize = 11.sp)
        } },
        confirmButton = { TextButton(enabled = title.isNotBlank() && content.isNotBlank(), onClick = {
            val course = data.courses.firstOrNull { it.name.equals(courseName.trim(), true) }
            onSave((note ?: AcademicNote(title = title.trim(), content = content.trim())).copy(
                title = title.trim(), content = content.trim(), courseId = course?.id.orEmpty(),
                folder = if (course != null) course.name else folder.ifBlank { "Sin clasificar" }
            ))
        }) { Text("Guardar") }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

@Composable
private fun TrashScreen(trash: List<TrashEntry>, onRestore: (TrashEntry) -> Unit, onPurge: (TrashEntry) -> Unit) {
    PageHeading("Papelera", "Restaura cursos, tareas y apuntes eliminados")
    if (trash.isEmpty()) SurfaceCard { Text("La papelera está vacía.", color = Muted) }
    trash.forEach { item -> SurfaceCard {
        Text(item.label, fontWeight = FontWeight.Medium)
        Text("${item.type} · eliminado ${java.time.Instant.ofEpochMilli(item.deletedAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate()}", color = Muted, fontSize = 12.sp)
        Row { TextButton(onClick = { onRestore(item) }) { Text("Restaurar") }; TextButton(onClick = { onPurge(item) }) { Text("Eliminar definitivamente") } }
    } }
}

@Composable
private fun IntegrationsScreen(data: AppSnapshot, onImportMoodle: (List<TaskItem>) -> Int) {
    val context = LocalContext.current
    var baseUrl by remember { mutableStateOf(MoodleSettings.url(context)) }
    var token by remember { mutableStateOf(MoodleSettings.token(context)) }
    var aulaUrl by remember { mutableStateOf(MoodleSettings.aulaUrl(context).ifBlank { "https://aulavirtual.upt.edu.pe/" }) }
    var message by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    PageHeading("Integraciones", "Conecta calendarios y plataformas académicas")
    SurfaceCard {
        Text("Moodle · solo lectura", fontWeight = FontWeight.SemiBold)
        Text("Importa cursos y actividades de una instancia habilitada. No se sincronizan calificaciones ni se envían entregas.", color = Muted)
        OutlinedTextField(baseUrl, { baseUrl = it }, label = { Text("URL base HTTPS") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(token, { token = it }, label = { Text("Token de servicio web") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        TextButton(onClick = { MoodleSettings.save(context, baseUrl, token); message = "URL y token guardados; el token queda cifrado en el dispositivo." }, enabled = token.isNotBlank()) { Text("Guardar conexión") }
        AppButton(if (busy) "Consultando Moodle…" else "Consultar cursos y actividades", {
            val url = baseUrl.trim(); val key = token.trim()
            MoodleSettings.save(context, url, key); busy = true
            Thread {
                val result = runCatching { MoodleClient.fetchAssignments(url, key) }
                Handler(Looper.getMainLooper()).post {
                    busy = false
                    result.onSuccess { items -> message = "${onImportMoodle(items)} actividad(es) nuevas importadas; las repeticiones se omitieron." }
                        .onFailure { message = it.message ?: "No se pudo acceder a Moodle." }
                }
            }.start()
        }, enabled = !busy && baseUrl.startsWith("https://") && token.isNotBlank())
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (message.isNotBlank()) Text(message, color = Muted, fontSize = 13.sp)
        if (token.isNotBlank()) TextButton(onClick = { SecureTokenStore.clear(context); token = ""; message = "Token eliminado." }) { Text("Desconectar") }
        Text("Requiere permisos para core_webservice_get_site_info, core_enrol_get_users_courses y mod_assign_get_assignments.", color = Muted, fontSize = 11.sp)
    }
    SurfaceCard {
        Text("Aula Virtual UPT", fontWeight = FontWeight.SemiBold)
        Text("Acceso al portal separado de Moodle, según el alcance actual.", color = Muted)
        OutlinedTextField(aulaUrl, { aulaUrl = it; MoodleSettings.saveAulaUrl(context, it) }, label = { Text("URL segura del portal") }, modifier = Modifier.fillMaxWidth())
        OutlinedButton(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(aulaUrl))) }.onFailure { message = "No se pudo abrir el portal." } }, enabled = aulaUrl.startsWith("https://"), modifier = Modifier.fillMaxWidth()) { Text("Abrir Aula Virtual") }
        Text("Para sincronizar datos automáticamente se necesitará la API del proveedor. Por ahora abre el portal sin guardar credenciales.", color = Muted, fontSize = 12.sp)
    }
    SurfaceCard {
        Text("Calendario del dispositivo", fontWeight = FontWeight.SemiBold)
        Text("La app usa el proveedor de calendarios Android. Si una cuenta Google está sincronizada en el dispositivo, el calendario seleccionado puede reflejar los eventos en Google Calendar.", color = Muted)
        Text("Calendario seleccionado: ${data.selectedCalendarId ?: "ninguno"}", color = Muted)
        TextButton(onClick = { /* settings live in Permission Center for OS grants */ }) { Text("Permisos y selección en Más > Permisos") }
    }
}

@Composable
private fun AiSettingsScreen() {
    val context = LocalContext.current
    val current = remember { AiSettings.load(context) }
    var endpoint by remember { mutableStateOf(current.endpoint) }
    var model by remember { mutableStateOf(current.model) }
    var apiKey by remember { mutableStateOf("") }
    var hasKey by remember { mutableStateOf(current.apiKey.isNotBlank()) }
    var message by remember { mutableStateOf("") }
    PageHeading("Proveedor de IA", "Opcional: extrae actividades desde texto reconocido")
    SurfaceCard {
        Text("La extracción local continúa disponible sin configurar IA. Al invocar el análisis remoto, el texto del documento se envía al endpoint indicado.", color = Muted)
        OutlinedTextField(endpoint, { endpoint = it }, label = { Text("Endpoint HTTPS compatible con chat completions") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(model, { model = it }, label = { Text("Modelo") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(apiKey, { apiKey = it }, label = { Text(if (hasKey) "Nueva API key (vacío conserva la actual)" else "API key") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        Text(if (hasKey) "API key almacenada con Android Keystore." else "No hay API key configurada.", color = Muted, fontSize = 12.sp)
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
        if (message.isNotBlank()) Text(message, color = Muted)
    }
}

@Composable
private fun CalendarSettingsScreen(selectedId: Long?, onSelect: (Long) -> Unit) {
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
        Text("TARE-IA usa el proveedor de calendarios Android. Si eliges una cuenta Google sincronizada, el proveedor del teléfono se encarga de sincronizar los eventos.", color = Muted)
        AppButton("Conceder acceso a calendarios", { permission.launch(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)) })
        if (calendars.isEmpty() && CalendarRepository.hasPermissions(context)) Text("No se encontraron calendarios editables.", color = Muted)
        calendars.forEach { calendar ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selectedId == calendar.id, onClick = { onSelect(calendar.id) })
                Column { Text(calendar.name, fontWeight = FontWeight.Medium); Text(calendar.account, color = Muted, fontSize = 12.sp) }
            }
        }
        if (message.isNotBlank()) Text(message, color = Muted)
    }
}

@Composable
private fun ReportScreen(tasks: List<TaskItem>) {
    val context = LocalContext.current
    var course by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Todas") }
    var pending by remember { mutableStateOf<List<TaskItem>>(emptyList()) }
    var message by remember { mutableStateOf("") }
    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) runCatching { PdfReport.write(context, uri, pending) }
            .onSuccess { message = "Reporte guardado." }.onFailure { message = it.message ?: "No se pudo generar el PDF." }
    }
    val filtered = tasks.filter { (course.isBlank() || it.course.contains(course, true)) && (status == "Todas" || it.status == status) }
    PageHeading("Reporte de actividades", "Filtra y exporta tareas a PDF")
    SurfaceCard {
        OutlinedTextField(course, { course = it }, label = { Text("Filtrar por curso (opcional)") }, modifier = Modifier.fillMaxWidth())
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Todas", "Pendiente", "Pendiente de validar", "Completada").forEach { FilterChip(status == it, { status = it }, label = { Text(it) }) }
        }
        Text("${filtered.size} actividad(es) incluidas", color = Muted)
        AppButton("Guardar PDF", { pending = filtered; create.launch("TARE-IA-reporte-${LocalDate.now()}.pdf") }, enabled = filtered.isNotEmpty())
        if (message.isNotBlank()) Text(message, color = Muted)
    }
    filtered.forEach { TaskCard(it) }
}

@Composable
private fun ProfileScreen(profile: UserProfile, onSave: (UserProfile) -> Unit) {
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
        if (message.isNotBlank()) Text(message, color = Muted)
        Text("La cuenta local se protege con un PIN cifrado en este dispositivo; no es una cuenta en la nube.", color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun PermissionCenter(message: String, onMessage: (String) -> Unit) {
    val context = LocalContext.current
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        onMessage(if (result.values.all { it }) "Permisos concedidos." else "Algunos permisos fueron denegados; las funciones no relacionadas siguen disponibles.")
    }
    val notificationPermission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.POST_NOTIFICATIONS else null
    PageHeading("Permisos", "TARE-IA solo solicita acceso al usar cada función")
    SurfaceCard {
        Text("Calendario", fontWeight = FontWeight.SemiBold)
        Text("Permite leer calendarios disponibles y añadir eventos al que selecciones.", color = Muted)
        AppButton("Conectar calendarios", { permissions.launch(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)) })
        if (CalendarRepository.hasPermissions(context)) {
            val calendars = remember { runCatching { CalendarRepository.calendars(context) }.getOrDefault(emptyList()) }
            if (calendars.isEmpty()) Text("No hay calendarios editables en el dispositivo.", color = Muted)
            calendars.forEach { calendar ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${calendar.name} · ${calendar.account}", Modifier.weight(1f), fontSize = 12.sp)
                    TextButton(onClick = { onMessage("Para cambiar el calendario seleccionado, usa la pantalla de integraciones después de conceder acceso.") }) { Text("Disponible") }
                }
            }
        }
    }
    SurfaceCard {
        Text("Notificaciones", fontWeight = FontWeight.SemiBold)
        Text("Los recordatorios se programan para un día antes de la fecha límite, a las 9:00 a. m.", color = Muted)
        if (notificationPermission != null) AppButton("Permitir notificaciones", { permissions.launch(arrayOf(notificationPermission)) })
        else Text("Las notificaciones están habilitadas por la versión de Android.", color = Muted)
    }
    SurfaceCard {
        Text("Micrófono", fontWeight = FontWeight.SemiBold)
        Text("Solo se solicita al iniciar dictado. El servicio de voz del dispositivo puede transmitir audio por Internet.", color = Muted)
        AppButton("Permitir micrófono", { permissions.launch(arrayOf(Manifest.permission.RECORD_AUDIO)) })
    }
    SurfaceCard {
        Text("Archivos e imágenes", fontWeight = FontWeight.SemiBold)
        Text("Los documentos se seleccionan con el selector del sistema; no se solicita acceso general al almacenamiento. Para tomar una foto se abre la cámara del dispositivo.", color = Muted)
    }
    if (message.isNotBlank()) Text(message, color = Muted)
}

private object MoodleSettings {
    private const val PREF = "tareia_moodle"
    private const val URL_KEY = "url"
    private const val AULA_KEY = "aula_url"
    fun url(context: Context) = context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(URL_KEY, "").orEmpty()
    fun aulaUrl(context: Context) = context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(AULA_KEY, "").orEmpty()
    fun token(context: Context) = SecureTokenStore.read(context)
    fun save(context: Context, url: String, token: String) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(URL_KEY, url.trim()).apply()
        if (token.isNotBlank()) SecureTokenStore.write(context, token.trim())
    }
    fun saveAulaUrl(context: Context, url: String) { context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(AULA_KEY, url.trim()).apply() }
}

private fun formatMinute(minute: Int): String = "%02d:%02d".format(minute / 60, minute % 60)

private fun parseMinute(value: String): Int? = runCatching {
    val time = LocalTime.parse(value.trim(), DateTimeFormatter.ofPattern("H:mm"))
    time.hour * 60 + time.minute
}.getOrNull()
