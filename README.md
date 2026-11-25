# TARE·IA

**Asistente académico Android que convierte apuntes, PDF, capturas y tareas del aula virtual en actividades con
fecha, planes de estudio y recordatorios.** La IA (Gemini) propone; el estudiante valida; nada llega a Google
Calendar sin su confirmación.

> Proyecto del curso **SI-784 Calidad y Pruebas de Software** — Universidad Privada de Tacna, Facultad de Ingeniería, EPIS.
> Docente: Ing. Patrick Cuadros Quiroga · Autores: **Adriana Rafaela Laos González** (2023077474) · **Antony Piero Solorzano Zegarra** (2023078696) · 2026.

| | Enlace |
|---|---|
| App Android (APK) | [GitHub Releases](https://github.com/Laos19/TareaIA/releases) |
| Dashboard de utilización | https://tareia-dashboard.onrender.com — demo `admin@demo.tareia.pe` / `tareia2026` |
| API REST y Swagger | https://tareia-api.onrender.com/api/health · https://tareia-api.onrender.com/api/docs |
| Documentación técnica (Dokka + TypeDoc, diagramas, reportes) | https://laos19.github.io/TareaIA/ |
| Problema y objetivos · FD01–FD04 · presentación | [docs/investigacion](docs/investigacion/) · [docs/fd](docs/fd/) · [docs/presentacion](docs/presentacion/) |
| Entrega por criterio de la rúbrica | [docs/entrega](docs/entrega/) |

> El plan gratuito de Render suspende la API tras 15 minutos sin uso: la primera petición puede tardar alrededor de un minuto.

## Qué hace

- **Importar**: PDF, imágenes, capturas o texto compartido desde otra app → OCR en el teléfono (ML Kit) → Gemini propone tareas con fecha.
- **Validar**: lo que propone la IA o llega de Moodle queda *Pendiente de validar* (RN09); para confirmarlo hace falta título y fecha (RN05).
- **Planificar**: plan de estudio según prioridad, dificultad y horario libre; con IA o local (patrón Strategy). Cada propuesta es un clon del plan vigente (Prototype).
- **Sincronizar**: Google Calendar solo recibe actividades validadas (RN01). Recordatorios locales el día anterior.
- **Aula virtual**: con la URL del calendario exportado de Moodle (se copia una sola vez), la app trae sola las entregas con fecha, «Por validar», sin duplicados (RN02) y avisa si el docente cambia una fecha.
- **Nube**: respaldo de cursos y actividades, sesiones, historial de cambios, telemetría y logs en PostgreSQL; **dashboard de utilización** para el equipo.
- **Claude Code**: servidor MCP, skill y agente para consultar y organizar las actividades desde el editor.

## Requisitos no funcionales: sesiones, historial, caché, logs y telemetría

| Requisito | Cómo se implementa | Evidencia |
|---|---|---|
| **Sesiones** | La app canjea el ID token de Firebase (Google) por un JWT de la API (7 días) que lleva el id de una fila de `sesion` (origen, dispositivo, versión). Cada petición renueva `ultima_actividad`; logout la cierra y `fn_cerrar_sesiones_inactivas` cierra las abandonadas (30 min). El JWT se guarda cifrado con Android Keystore. | `backend/src/auth/AuthService.ts`, `backend/src/app.ts`, `db/changelog/sql/002-seguridad.sql`, `android/.../remoto/Nube.kt` |
| **Historial** | Trigger `fn_registrar_historial`: antes/después en JSONB de cada alta, cambio o borrado de cursos, actividades y apuntes (`GET /api/actividades/{id}/historial`). Los planes reemplazados se conservan. Liquibase guarda el historial del esquema. | `005-historial.sql`, `007-funciones.sql`, `backend/test/integracion/api.test.ts` |
| **Caché** | Caché en memoria con TTL para el dashboard (cabecera `x-cache: HIT/MISS`, aciertos y fallos en `/api/health`); vista materializada `mv_uso_diario`; caché HTTP de 5 MB (OkHttp) en la app; recursos del dashboard con caché inmutable en el CDN. | `backend/src/cache/MemoryCache.ts`, `009-vistas.sql`, `android/.../remoto/ApiTareia.kt`, `infra/terraform/modules/app-render/main.tf` |
| **Logs** | Logs JSON estructurados (pino) con `authorization`, `password` e `idToken` ocultos; errores 500 sin detalles al cliente. La app sube logs WARN/ERROR por lotes y la API los guarda en `log_cliente` con correos y tokens enmascarados. | `backend/src/app.ts`, `backend/src/dominio/privacidad.ts`, `android/.../remoto/Nube.kt` |
| **Telemetría** | Patrón Observer: los gestores avisan eventos (actividad creada, validada, completada, importación IA, Moodle, plan, pantalla vista) → cola en lotes → `POST /api/eventos` sin datos personales → dashboard. | `android/.../aplicacion/ObservadorUso.kt`, `remoto/ColaTelemetria.kt`, `backend/src/services/TelemetriaService.ts`, `dashboard/` |

## Arquitectura

```
android/           App Kotlin + Jetpack Compose (ui · aplicacion · dominio · ia · infraestructura · remoto)
  verificacion-jvm/  Dominio Kotlin compilado en JVM para pruebas y Pitest
backend/           API REST Node 22 + Fastify + TypeScript · JWT · OpenAPI · pg
db/changelog/      Liquibase: tablas, triggers (patrón State en la BD), vistas, datos demo
dashboard/         Dashboard de utilización (React + Vite) + pruebas de interfaz Playwright
acceptance/        Pruebas de aceptación en Gherkin (Cucumber)
integrations/mcp/  Servidor MCP  ·  .claude/  skill y agente de Claude Code
infra/terraform/   Neon (PostgreSQL) + Render (API + dashboard) · pruebas terraform test · referencia de costos
.github/workflows/ 10 automatizaciones (ver docs/automatizaciones.md)
docs/              Problema y objetivos, FD01–FD04, diagramas, diccionario, presentación, entrega
```

Patrones de diseño: **State** (`EstadoActividad` y trigger `tg_actividad_transicion`), **Builder** (`ActividadBuilder`),
**Prototype** (`PlanEstudio.clonar`), **Strategy** (`EstrategiaPlanificacion`), **Abstract Factory** (`FabricaServiciosIA`),
**Repository**, **Singleton** (`AppDatabase`), **Observer** (`ObservadorUso`) y **Facade** (`Nube`).

### Métricas

<!-- AUTO:metricas:inicio -->
| Métrica | Valor |
|---|---|
| Archivos Kotlin de la app / líneas | 86 / 7394 |
| Tablas PostgreSQL | 11 |
| Vistas / vistas materializadas | 5 / 1 |
| Funciones / triggers | 7 / 7 |
| Changesets de Liquibase | 20 |
| Endpoints REST | 18 |
| Pruebas unitarias declaradas (API · app Android) | 47 · 69 |
| Pruebas de integración / sistema (API) | 22 / 11 |
| Pruebas de interfaz (dashboard Playwright · Compose) | 8 × 2 dispositivos · 16 |
| Escenarios de aceptación (Gherkin) | 18 |
<!-- AUTO:metricas:fin -->

## Calidad y pruebas

| Tipo | Herramienta | Alcance | Resultado local verificado |
|---|---|---|---|
| Unitarias | Vitest · JUnit | Dominio, caché, privacidad, servicios (API); dominio y capa remota (app) | API 66 ✅ · app (JVM) 35 ✅ |
| Mutación | Stryker · Pitest | `backend/src/dominio`, caché y servicios · dominio Kotlin | 93,8 % · 68,4 % (umbrales 60 % y 50 %) |
| Integración | Vitest + PostgreSQL 16 | Rutas → servicios → repositorios → triggers | 22 ✅ |
| Sistema | Vitest + servidor HTTP real | Flujos completos, CORS, helmet, rate limit, Swagger | 11 ✅ |
| Interfaz | Playwright · Compose UI Test | Dashboard (escritorio y Pixel 7) · pantallas de la app | 16 ✅ · 16 en emulador (CI) |
| Aceptación | Cucumber (Gherkin en español) | 5 historias de usuario | 18 escenarios / 76 pasos ✅ |
| Estático | SonarQube · Semgrep · Snyk | Código, dependencias, secretos, Terraform | Reportes en cada ejecución de GitHub Actions |

Las pruebas encontraron y ayudaron a corregir tres defectos reales: el historial fallaba al borrar un usuario en
cascada, el correo no se normalizaba al volver a entrar con Google y la detección de duplicados en PostgreSQL no
unificaba espacios repetidos.

## Base de datos

### Diccionario de datos (resumen)

<!-- AUTO:diccionario:inicio -->
| Tabla | Descripción |
|---|---|
| `usuario` | Estudiante (entra con Google mediante Firebase Auth) o administrador del dashboard (correo y contraseña). |
| `sesion` | Sesión de uso: se abre al iniciar sesión, se renueva con cada petición y se cierra con logout o inactividad. |
| `curso` | Curso del estudiante. El id (UUID) lo genera la app para poder crear sin conexión. |
| `actividad` | Actividad académica (tarea, examen, entregable o proyecto). Refleja ActividadAcademica del dominio Android. |
| `apunte` | Contenido académico importado (texto OCR de PDF/imagen o nota escrita). Sin curso = bandeja Sin clasificar (RF-20). |
| `franja_horaria` | Bloque semanal del horario. Solo DISPONIBLE_ESTUDIO se usa para planificar (RN16). |
| `plan_estudio` | Plan de estudio. Una PROPUESTA es un clon (Prototype) del plan VIGENTE; al confirmarla el anterior queda REEMPLAZADO (RNF-14). |
| `sesion_estudio` | Bloque de estudio propuesto para una actividad dentro de un plan. |
| `historial_cambio` | Historial de cada alta, cambio o borrado de cursos, actividades y apuntes (trigger fn_registrar_historial). |
| `evento` | Eventos de uso enviados en lotes por la app (append-only). Alimentan el dashboard de utilización. |
| `log_cliente` | Logs WARN/ERROR que la app Android sube para diagnosticar fallos (sin datos personales). |

Diccionario completo (columnas, tipos, restricciones, índices y diagrama por tabla): [docs/database](docs/database/).
<!-- AUTO:diccionario:fin -->

### Diagrama entidad-relación

<!-- AUTO:er:inicio -->
```mermaid
erDiagram
  actividad {
    uuid id PK
    bigint usuario_id FK
    uuid curso_id FK
    character titulo
    text descripcion
    character tipo
    character origen
    character estado
    date fecha_limite
    character id_externo
    character evento_calendario_id
    timestamp completada_en
    timestamp eliminado_en
    integer version
    timestamp creado_en
    timestamp actualizado_en
  }
  apunte {
    uuid id PK
    bigint usuario_id FK
    uuid curso_id FK
    character carpeta
    character titulo
    character tipo
    text contenido
    timestamp eliminado_en
    integer version
    timestamp creado_en
    timestamp actualizado_en
  }
  curso {
    uuid id PK
    bigint usuario_id FK
    character nombre
    character abreviatura
    smallint prioridad
    smallint dificultad
    timestamp eliminado_en
    integer version
    timestamp creado_en
    timestamp actualizado_en
  }
  evento {
    bigint id PK
    uuid sesion_id FK
    bigint usuario_id FK
    character tipo
    character pantalla
    jsonb datos
    timestamp creado_en
  }
  franja_horaria {
    bigint id PK
    bigint usuario_id FK
    smallint dia_semana
    time hora_inicio
    time hora_fin
    character tipo
  }
  historial_cambio {
    bigint id PK
    character tabla
    text registro_id
    bigint usuario_id FK
    character operacion
    jsonb antes
    jsonb despues
    timestamp cambiado_en
  }
  log_cliente {
    bigint id PK
    uuid sesion_id FK
    character nivel
    character etiqueta
    character mensaje
    character version_app
    timestamp creado_en
  }
  plan_estudio {
    uuid id PK
    bigint usuario_id FK
    character estado
    character estrategia
    uuid origen_id FK
    timestamp creado_en
  }
  sesion {
    uuid id PK
    bigint usuario_id FK
    character origen
    character dispositivo
    character version_app
    timestamp iniciada_en
    timestamp ultima_actividad
    timestamp cerrada_en
    character motivo_cierre
  }
  sesion_estudio {
    bigint id PK
    uuid plan_id FK
    uuid actividad_id FK
    date fecha
    smallint minuto_inicio
    smallint duracion_min
  }
  usuario {
    bigint id PK
    character firebase_uid
    character email
    character nombre
    character rol
    character password_hash
    boolean activo
    timestamp creado_en
    timestamp ultimo_acceso
  }
  usuario ||--o{ actividad : "usuario_id"
  curso ||--o{ actividad : "curso_id"
  usuario ||--o{ apunte : "usuario_id"
  curso ||--o{ apunte : "curso_id"
  usuario ||--o{ curso : "usuario_id"
  sesion ||--o{ evento : "sesion_id"
  usuario ||--o{ evento : "usuario_id"
  usuario ||--o{ franja_horaria : "usuario_id"
  usuario ||--o{ historial_cambio : "usuario_id"
  sesion ||--o{ log_cliente : "sesion_id"
  usuario ||--o{ plan_estudio : "usuario_id"
  plan_estudio ||--o{ plan_estudio : "origen_id"
  usuario ||--o{ sesion : "usuario_id"
  plan_estudio ||--o{ sesion_estudio : "plan_id"
  actividad ||--o{ sesion_estudio : "actividad_id"
```

<!-- AUTO:er:fin -->

## Diagramas

### Clases — app Android

<!-- AUTO:clasesAndroid:inicio -->
```mermaid
classDiagram
  direction LR
  namespace dominio {
    class ActividadAcademica {
      +id : String
      +tipo : TipoActividad
      +origen : OrigenActividad
      +idExterno : String
      +idContenidoOrigen : String?
      +confirmar()
      +sincronizar()
      +completar()
      +reabrir()
      +cambiarEstado()
      +esConfirmable()
      +estaCompletada()
      +estaPendienteDeValidar()
      +editar()
    }
    class Recordatorio {
      +id : String
      +idActividad : String
      +fechaHora : LocalDateTime
      +activo : Boolean
    }
    class EventoCalendario {
      +idExterno : String
      +ultimaSincronizacion : LocalDateTime
      +cuenta : String
    }
    class ActividadBuilder {
      +conTitulo()
      +conFechaLimite()
      +conCurso()
      +conTipo()
      +desdeIA()
      +conId()
      +conDescripcion()
      +conNombreCursoDetectado()
      +conContenidoOrigen()
      +desdeIntegracion()
      +build()
    }
    class Carpeta {
      +id : String
      +nombre : String
    }
    class ContenidoAcademico {
      +id : String
      +titulo : String
      +tipo : TipoContenido
      +textoReconocido : String
      +fechaRegistro : LocalDateTime
      +estaSinClasificar()
      +asignarCarpeta()
      +etiquetaPapelera()
      +enviarAPapelera()
      +restaurar()
    }
    class Apunte {
      +texto()
      +actualizarTexto()
    }
    class Anotacion {
      +contenido : String
      +texto()
      +actualizarTexto()
    }
    class Curso {
      +id : String
      +abreviatura : Abreviatura
      +prioridad : Prioridad
      +dificultad : Int
      +activo : Boolean
      +carpeta : Carpeta
      +renombrar()
      +etiquetaPapelera()
      +enviarAPapelera()
      +restaurar()
    }
    class Eliminable {
      <<interface>>
      +etiquetaPapelera()
      +enviarAPapelera()
      +restaurar()
      +estaEnPapelera()
    }
    class TipoContenido {
      <<enumeration>>
    }
    class TipoActividad {
      <<enumeration>>
    }
    class OrigenActividad {
      <<enumeration>>
    }
    class EstadoPlan {
      <<enumeration>>
    }
    class TipoFranja {
      <<enumeration>>
    }
    class EstadoActividad {
      <<interface>>
      +confirmar()
      +sincronizar()
      +completar()
      +reabrir()
      +nombre()
    }
    class PendienteValidar {
      +confirmar()
      +sincronizar()
      +completar()
      +reabrir()
      +nombre()
    }
    class Confirmada {
      +confirmar()
      +sincronizar()
      +completar()
      +reabrir()
      +nombre()
    }
    class Sincronizada {
      +confirmar()
      +sincronizar()
      +completar()
      +reabrir()
      +nombre()
    }
    class Completada {
      +confirmar()
      +sincronizar()
      +completar()
      +reabrir()
      +nombre()
    }
    class Estudiante {
      +id : String
      +nombre : String
      +correo : String
      +calendarioVinculado : Boolean
      +franjasDisponibles()
      +agregarFranja()
      +quitarFranja()
    }
    class Abreviatura {
      +valor : String
    }
    class Prioridad {
      +nivel : Int
    }
    class FechaLimite {
      +valor : LocalDate
      +esFutura()
      +toString()
    }
    class FranjaHoraria {
      +dia : DayOfWeek
      +inicio : LocalTime
      +fin : LocalTime
      +tipo : TipoFranja
      +etiqueta : String
      +seSolapaCon()
    }
    class PlanEstudio {
      +id : String
      +estado : EstadoPlan
      +fechaGeneracion : LocalDateTime
      +clonar()
      +sesionesDe()
      +reemplazarSesionesDe()
      +quitarSesionesDe()
    }
    class SesionEstudio {
      +id : String
      +idActividad : String
      +tituloActividad : String
      +inicio : LocalDateTime
      +fin : LocalDateTime
      +clonar()
      +seCruzaCon()
    }
    class ActividadRepository {
      <<interface>>
      +guardar()
      +buscarPorId()
      +buscarPor()
      +listarPendientes()
      +listarTodas()
      +listarEnPapelera()
      +buscarPorIdExterno()
      +eliminar()
    }
    class CursoRepository {
      <<interface>>
      +guardar()
      +listarActivos()
      +buscarPorId()
      +buscarPorNombre()
      +listarEnPapelera()
      +eliminar()
    }
    class ContenidoRepository {
      <<interface>>
      +guardar()
      +listarSinClasificar()
      +listarTodos()
      +buscarPorId()
      +listarEnPapelera()
      +eliminar()
      +guardarCarpeta()
      +listarCarpetas()
    }
    class PlanRepository {
      <<interface>>
      +guardar()
      +obtenerVigente()
    }
    class EstudianteRepository {
      <<interface>>
      +obtener()
      +guardar()
    }
    class DetectorDuplicados {
      -repositorio : ActividadRepository
      +esDuplicada()
    }
    class ClasificadorContenido {
      +clasificar()
      -contienePalabra()
    }
  }
  namespace aplicacion {
    class Dependencias {
      +estrategiaConIA()
      +estrategiaLocal()
      +gestorSincronizacion()
      +recargarFabricaIA()
      +migrarDatosAnteriores()
    }
    class GestorActividades {
      -actividades : ActividadRepository
      -detector : DetectorDuplicados
      -alertas : GestorAlertas
      -gestorCursos : GestorCursos
      -avisar()
      +listar()
      +buscar()
      +crearManual()
      +registrarPropuestas()
      +importarDeIntegracion()
      +editar()
      +confirmar()
      +marcarCompletada()
      +reabrir()
    }
    class GestorAlertas {
      -programador : ProgramadorNotificaciones
      +programarPorDefecto()
      +reprogramar()
      +cancelar()
      +reprogramarTodas()
    }
    class GestorAnalisisIA {
      -fabrica : FabricaServiciosIA
      -cursos : CursoRepository
      -extractorSinIA : ExtractorLocal
      +procesarOCR()
      +extraerParametros()
      +sugerirSinIA()
    }
    class GestorCursos {
      -cursos : CursoRepository
      +listar()
      +buscar()
      +guardar()
      +obtenerOCrear()
      -iniciales()
    }
    class GestorIngesta {
      -context : Context
      -contenidos : ContenidoRepository
      -cursos : CursoRepository
      -clasificador : ClasificadorContenido
      +importarArchivo()
      +importarFoto()
      +recibirCompartido()
      +guardarApunte()
      +guardarAnotacion()
      +crearCarpeta()
      +listarContenido()
      +listarCarpetasLibres()
      +buscar()
      +contenidoEnRevision()
    }
    class GestorPapelera {
      -actividades : ActividadRepository
      -cursos : CursoRepository
      -contenidos : ContenidoRepository
      -alertas : GestorAlertas
      -planificacion : GestorPlanificacion
      +listar()
      +buscar()
      +enviar()
      +restaurar()
      +eliminarDefinitivo()
      -guardar()
    }
    class GestorPlanificacion {
      -planes : PlanRepository
      -cursos : CursoRepository
      -estudiantes : EstudianteRepository
      +setEstrategia()
      +obtenerEstudiante()
      +actualizarDatosEstudiante()
      +planVigente()
      +generarPropuesta()
      +reorganizar()
      +confirmarPlan()
      +quitarSesionesDe()
      +agregarFranja()
      +quitarFranja()
    }
    class GestorSincronizacion {
      -calendario : CalendarioGateway
      -actividades : ActividadRepository
      +sincronizar()
      +eliminarEvento()
      +enlace()
    }
    class MigracionDatos {
      -context : Context
      -db : AppDatabase
      -dep : Dependencias
      +ejecutar()
      -migrar()
    }
    class ObservadorUso {
      <<interface>>
      +alOcurrir()
    }
  }
  namespace ia {
    class ComunIA {
      <<object>>
      +promptExtraccion()
      +leerExtraccion()
      +promptPlan()
      +leerPlan()
      -limpiarJson()
    }
    class AiConfig {
      +endpoint : String
      +model : String
      +apiKey : String
      +estaCompleta()
    }
    class AiSettings {
      <<object>>
      +load()
      +save()
      +clearKey()
    }
    class ClienteCompatibleOpenAI {
      <<object>>
      +completar()
    }
    class EstrategiaPlanificacion {
      <<interface>>
      +planificar()
    }
    class PlanificacionConIA {
      -generador : GeneradorPlanIA
      +planificar()
    }
    class PlanificacionLocalPorPrioridad {
      +planificar()
      -sesionesPara()
    }
    class ExtractorLocal {
      +extraer()
      +extraerAhora()
      -leerFecha()
    }
    class FabricaProveedorA {
      -context : Context
      +crearReconocedorOCR()
      +crearExtractor()
      +crearGeneradorPlan()
    }
    class ExtractorParametrosGemini {
      +extraer()
    }
    class GeneradorPlanGemini {
      +generar()
    }
    class FabricaProveedorB {
      -context : Context
      -config : AiConfig
      +crearReconocedorOCR()
      +crearExtractor()
      +crearGeneradorPlan()
    }
    class ExtractorParametrosCompatibleOpenAI {
      -config : AiConfig
      +extraer()
    }
    class GeneradorPlanCompatibleOpenAI {
      -config : AiConfig
      +generar()
    }
    class FabricaServiciosIA {
      <<interface>>
      +crearReconocedorOCR()
      +crearExtractor()
      +crearGeneradorPlan()
    }
    class ReconocedorOCRMLKit {
      -context : Context
      +reconocer()
      -leerTexto()
      -reconocerPdf()
    }
    class ReconocedorOCR {
      <<interface>>
      +reconocer()
    }
    class ExtractorParametros {
      <<interface>>
      +extraer()
    }
    class GeneradorPlanIA {
      <<interface>>
      +generar()
    }
  }
  namespace infraestructura {
    class AppDatabase {
      -context : Context
      +leerTabla()
      +escribirTabla()
      +guardarFila()
      +borrarFila()
      +estaVacia()
    }
    class CalendarioGateway {
      <<interface>>
      +crearEvento()
      +actualizarEvento()
      +eliminarEvento()
      +enlaceEvento()
    }
    class GoogleCalendarAdapter {
      -token : String
      -uid : String
      +cuenta : String
      +crearEvento()
      +actualizarEvento()
      +eliminarEvento()
      +enlaceEvento()
      -datos()
    }
    class ProgramadorNotificaciones {
      <<interface>>
      +programar()
      +cancelar()
    }
    class AlarmManagerNotificaciones {
      -context : Context
      +programar()
      +cancelar()
      -intentPendiente()
    }
    class LocalCursoRepository {
      -db : AppDatabase
      +guardar()
      +listarActivos()
      +buscarPorId()
      +buscarPorNombre()
      +listarEnPapelera()
      +eliminar()
      -todos()
      -aJson()
      -desdeJson()
    }
    class LocalContenidoRepository {
      -db : AppDatabase
      -cursos : CursoRepository
      +guardar()
      +listarSinClasificar()
      +listarTodos()
      +buscarPorId()
      +listarEnPapelera()
      +eliminar()
      +guardarCarpeta()
      +listarCarpetas()
      -carpetasConocidas()
      -todos()
      -aJson()
      -desdeJson()
    }
    class LocalActividadRepository {
      -db : AppDatabase
      -cursos : CursoRepository
      +guardar()
      +buscarPorId()
      +buscarPor()
      +listarPendientes()
      +listarTodas()
      +listarEnPapelera()
      +buscarPorIdExterno()
      +eliminar()
      -todas()
      -aJson()
      -desdeJson()
    }
    class LocalPlanRepository {
      -db : AppDatabase
      +guardar()
      +obtenerVigente()
      -aJson()
      -desdeJson()
    }
    class LocalEstudianteRepository {
      -db : AppDatabase
      +obtener()
      +guardar()
    }
  }
  namespace remoto {
    class ErrorApi {
      +codigo : Int
      -baseUrl : String
      -pedir()
      +iniciarSesionGoogle()
      +cerrarSesion()
      +sincronizar()
      +enviarEventos()
      +enviarLogs()
    }
    class ApiTareia {
      -baseUrl : String
      -pedir()
      +iniciarSesionGoogle()
      +cerrarSesion()
      +sincronizar()
      +enviarEventos()
      +enviarLogs()
    }
    class ColaTelemetria {
      -capacidad : Int
      -tamanoLote : Int
      +agregar()
      +tomarLote()
      +devolver()
    }
    class MapeoNube {
      <<object>>
      +codigoEstado()
      +instante()
      +curso()
      +actividad()
      +lote()
    }
    class Nube {
      <<object>>
      +iniciar()
      -token()
      +iniciarSesion()
      -obtenerIdToken()
      +cerrarSesion()
      +sincronizar()
      +registrar()
      +advertir()
      +error()
      -log()
      +vaciarEnSegundoPlano()
      -vaciar()
      +estado()
    }
  }
  Eliminable <|-- ActividadAcademica
  ActividadAcademica --> TipoActividad
  ActividadAcademica --> OrigenActividad
  Eliminable <|-- ContenidoAcademico
  ContenidoAcademico --> TipoContenido
  ContenidoAcademico <|-- Apunte
  Eliminable <|-- Curso
  Curso --> Abreviatura
  Curso --> Prioridad
  Curso --> Carpeta
  EstadoActividad <|-- PendienteValidar
  EstadoActividad <|-- Confirmada
  EstadoActividad <|-- Sincronizada
  EstadoActividad <|-- Completada
  FranjaHoraria --> TipoFranja
  EstadoPlan <|-- PlanEstudio
  DetectorDuplicados --> ActividadRepository
  GestorCursos <|-- GestorActividades
  GestorActividades --> ActividadRepository
  GestorActividades --> DetectorDuplicados
  GestorActividades --> GestorAlertas
  GestorAlertas --> ProgramadorNotificaciones
  CursoRepository <|-- GestorAnalisisIA
  GestorAnalisisIA --> FabricaServiciosIA
  GestorAnalisisIA --> ExtractorLocal
  GestorCursos --> CursoRepository
  ClasificadorContenido <|-- GestorIngesta
  GestorIngesta --> ContenidoRepository
  GestorIngesta --> CursoRepository
  GestorPlanificacion <|-- GestorPapelera
  GestorPapelera --> ActividadRepository
  GestorPapelera --> CursoRepository
  GestorPapelera --> ContenidoRepository
  GestorPapelera --> GestorAlertas
  EstudianteRepository <|-- GestorPlanificacion
  GestorPlanificacion --> PlanRepository
  GestorPlanificacion --> CursoRepository
  ActividadRepository <|-- GestorSincronizacion
  GestorSincronizacion --> CalendarioGateway
  Dependencias <|-- MigracionDatos
  MigracionDatos --> AppDatabase
  EstrategiaPlanificacion <|-- PlanificacionConIA
  PlanificacionConIA --> GeneradorPlanIA
  EstrategiaPlanificacion <|-- PlanificacionLocalPorPrioridad
  ExtractorParametros <|-- ExtractorLocal
  FabricaServiciosIA <|-- FabricaProveedorA
  ExtractorParametros <|-- ExtractorParametrosGemini
  GeneradorPlanIA <|-- GeneradorPlanGemini
  FabricaServiciosIA <|-- FabricaProveedorB
  FabricaProveedorB --> AiConfig
  ExtractorParametros <|-- ExtractorParametrosCompatibleOpenAI
  ExtractorParametrosCompatibleOpenAI --> AiConfig
  GeneradorPlanIA <|-- GeneradorPlanCompatibleOpenAI
  GeneradorPlanCompatibleOpenAI --> AiConfig
  ReconocedorOCR <|-- ReconocedorOCRMLKit
  CalendarioGateway <|-- GoogleCalendarAdapter
  ProgramadorNotificaciones <|-- AlarmManagerNotificaciones
  CursoRepository <|-- LocalCursoRepository
  LocalCursoRepository --> AppDatabase
  ContenidoRepository <|-- LocalContenidoRepository
  LocalContenidoRepository --> AppDatabase
  LocalContenidoRepository --> CursoRepository
  ActividadRepository <|-- LocalActividadRepository
  LocalActividadRepository --> AppDatabase
  LocalActividadRepository --> CursoRepository
  PlanRepository <|-- LocalPlanRepository
  LocalPlanRepository --> AppDatabase
  EstudianteRepository <|-- LocalEstudianteRepository
  LocalEstudianteRepository --> AppDatabase
```

<!-- AUTO:clasesAndroid:fin -->

### Clases — API

<!-- AUTO:clases:inicio -->
```mermaid
classDiagram
  direction LR
  class AuthService {
    -db : Database
    -verificador : VerificadorIdentidad
    +login(email, password, datos)
    +loginGoogle(idToken, datos)
    -abrirSesion(usuarioId, datos)
    +tocarSesion(sid, usuarioId, minutosInactividad)
    +cerrarSesion(sid, usuarioId)
    +perfil(usuarioId)
  }
  class VerificadorFirebase {
    -projectId : string
    +verificar(idToken)
  }
  class MemoryCache {
    +aciertos
    +fallos
    -ttlMs : number
    -ahora : () = number
    +get(clave)
    +set(clave, valor)
    +obtener(clave, cargar)
    +invalidar(clave)
  }
  class Database {
    +pool : pg.Pool
    +query(text, params)
    +tx(fn)
    +ping()
    +close()
  }
  class HttpError {
    +status : number
    +code : 
  }
  class AcademicoRepository {
    -db : Database
    +listarCursos(usuarioId, desde)
    +buscarCurso(usuarioId, id)
    +guardarCurso(usuarioId, c)
    +listarActividades(usuarioId, desde)
    +buscarActividad(usuarioId, id)
    +buscarDuplicado(usuarioId, a)
    +guardarActividad(usuarioId, a)
    +cambiarEstado(usuarioId, id, estado)
    +marcarPapelera(usuarioId, tabla, id, enPapelera)
    +pendientes(usuarioId)
    +historial(usuarioId, tabla, id)
  }
  class DashboardRepository {
    -db : Database
    +obtener()
    +refrescar(minutosInactividad)
  }
  class TelemetriaRepository {
    -db : Database
    +insertarEventos(sid, usuarioId, eventos)
    +insertarLogs(sid, logs)
  }
  class ActividadService {
    -repo : AcademicoRepository
    +guardar(usuarioId, entrada)
    +accion(usuarioId, id, accion)
    +sincronizar(usuarioId, cursos, actividades, desde)
  }
  class DashboardService {
    +cache : MemoryCache Dashboard
    -repo : DashboardRepository
    -minutosInactividad : number
    +obtener()
    +refrescar()
  }
  class TelemetriaService {
    -repo : TelemetriaRepository
    -ahora : () = number
    +depurarEventos(entrada)
    +fechaValida(fecha)
    +registrarEventos(sid, usuarioId, entrada)
    +registrarLogs(sid, entrada)
  }
  AuthService --> Database : usa
  class HttpError:::error
  AcademicoRepository --> Database : usa
  DashboardRepository --> Database : usa
  TelemetriaRepository --> Database : usa
  ActividadService --> AcademicoRepository : usa
  DashboardService --> DashboardRepository : usa
  TelemetriaService --> TelemetriaRepository : usa
```

<!-- AUTO:clases:fin -->

### Componentes

<!-- AUTO:componentes:inicio -->
```mermaid
flowchart LR
  subgraph Movil["App Android (Kotlin + Jetpack Compose)"]
    ui["ui/<br/><small>pantallas Compose</small>"]
    apl["aplicacion/<br/><small>Gestor* (casos de uso)</small>"]
    dom["dominio/<br/><small>entidades · State · Builder</small>"]
    inf["infraestructura/<br/><small>AppDatabase cifrada · alarmas · calendario</small>"]
    ia["ia/<br/><small>Abstract Factory · Strategy</small>"]
    rem["remoto/<br/><small>Nube · ApiTareia (OkHttp)</small>"]
  end
  subgraph Externos["Servicios externos"]
    fb["Firebase Auth<br/><small>Google Sign-In</small>"]
    gem["Gemini<br/><small>Firebase AI Logic</small>"]
    gcal["Google Calendar API"]
    moodle["Moodle (Aula Virtual)<br/><small>Web Services</small>"]
    mlkit["ML Kit OCR<br/><small>en el dispositivo</small>"]
  end
  subgraph Nube["Nube TARE·IA"]
    api["@tareia/backend<br/><small>Fastify · JWT · OpenAPI</small>"]
    dash["@tareia/dashboard<br/><small>React · métricas de uso</small>"]
    db[("PostgreSQL 16<br/><small>triggers · vistas · historial</small>")]
    lb["Liquibase<br/><small>db/changelog</small>"]
  end
  mcp["@tareia/mcp<br/><small>servidor MCP</small>"]
  claude["Claude Code<br/><small>skill + agente</small>"]
  ui --> apl --> dom
  apl --> inf
  apl --> ia
  apl -. "Observer" .-> rem
  ia --> gem
  ia --> mlkit
  inf --> gcal
  apl --> moodle
  ui --> fb
  rem -- "HTTPS /api (JWT)" --> api
  dash -- "HTTPS /api" --> api
  mcp -- "HTTPS /api" --> api
  claude -- "stdio" --> mcp
  api -- "verifica ID token" --> fb
  api -- "SQL (pg)" --> db
  lb -- "migra" --> db
```

<!-- AUTO:componentes:fin -->

### Despliegue

<!-- AUTO:despliegue:inicio -->
```mermaid
flowchart TB
  estudiante(["Estudiante<br/>teléfono Android"])
  admin(["Administración<br/>navegador"])
  dev(["Equipo<br/>git push"])
  subgraph GitHub
    repo["Repositorio Laos19/TareaIA"]
    gha["GitHub Actions<br/>ci · android · mutación · sonar · semgrep · snyk · infra · release · docs · pages"]
    pages["GitHub Pages<br/>documentación técnica"]
    rel["Releases<br/>APK · API · dashboard · MCP"]
  end
  subgraph Render["Render (free)"]
    dash["Static Site<br/>dashboard/dist"]
    api["Web Service Node 22<br/>backend/dist/server.js"]
  end
  subgraph Neon["Neon (free)"]
    pg[("PostgreSQL 16")]
  end
  subgraph Google["Google / Firebase"]
    fb["Firebase Auth + AI Logic"]
    gcal["Google Calendar"]
  end
  hcp["HCP Terraform<br/>estado remoto"]
  dev --> repo --> gha
  gha -- "terraform apply" --> Render
  gha -- "terraform apply" --> Neon
  gha -. "estado" .-> hcp
  gha -- "liquibase update" --> pg
  gha -- "APK + artefactos" --> rel
  gha -- "deploy pages" --> pages
  gha -- "deploy hook" --> api
  estudiante -- "descarga APK" --> rel
  estudiante -- "HTTPS /api" --> api
  estudiante -- "Google Sign-In · Gemini" --> fb
  estudiante -- "eventos" --> gcal
  admin -- HTTPS --> dash
  dash -- "HTTPS /api" --> api
  api -- "TLS 5432" --> pg
```

<!-- AUTO:despliegue:fin -->

<!-- AUTO:fecha:inicio -->
_Secciones generadas automáticamente el 2026-11-21 por `scripts/docs/build-readme.mjs`._
<!-- AUTO:fecha:fin -->

## Ejecutar en local

App Android: abrir la carpeta `android/` en Android Studio (JDK 17 o 21, SDK 35) y ejecutar `app` en un emulador **con Google Play**.
La URL de la API se cambia con `-PapiUrl=...` (vacía = solo datos locales).

API y dashboard (requiere Node 22 y PostgreSQL 16):

```bash
npm ci
```

```bash
DATABASE_URL=postgres://postgres:postgres@localhost:5432/tareia JWT_SECRET=una-cadena-de-al-menos-32-caracteres npm run dev:api
```

```bash
VITE_API_URL=http://localhost:8080 npm run dev:dashboard
```

Las migraciones se aplican con Liquibase (`.github/scripts/liquibase.sh update`). Más detalles y la configuración de
secretos de GitHub en [docs/automatizaciones.md](docs/automatizaciones.md).

## Licencia

MIT.
