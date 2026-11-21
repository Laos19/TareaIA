---
title: "TARE·IA — Informe de Factibilidad"
version: "1.0"
fecha: "2026-11-21"
autores: "Adriana Rafaela Laos González (2023077474) y Antony Piero Solorzano Zegarra (2023078696)"
---

# Control de versiones

| Versión | Hecha por | Revisada por | Aprobada por | Fecha | Motivo |
|---|---|---|---|---|---|
| 1.0 | Adriana Laos y Antony Solorzano | Ing. Patrick Cuadros Quiroga (pendiente) | (pendiente) | 2026-11-21 | Versión inicial para la Unidad II de SI-784 Calidad y Pruebas de Software |

> **Universidad Privada de Tacna · Facultad de Ingeniería · Escuela Profesional de Ingeniería de Sistemas**
> Curso: SI-784 Calidad y Pruebas de Software · Docente: Ing. Patrick Cuadros Quiroga · Semestre 2026-II
> Repositorio: https://github.com/Laos19/TareaIA

---

# 1. Descripción del proyecto

## 1.1 Nombre del proyecto

**TARE·IA: asistente académico Android que convierte apuntes, documentos y tareas del aula virtual en
actividades con fecha, planes de estudio y recordatorios mediante inteligencia artificial.**

## 1.2 Duración del proyecto

16 semanas (un semestre académico de la UPT, 2026-II), organizadas según la rúbrica del curso:

| Fase | Semanas | Entregables |
|---|---|---|
| Problema, objetivos y documentos de gestión | S1 | Problema con fuentes, objetivos medibles, FD01–FD04, análisis estático automatizado (SonarQube, Semgrep, Snyk) |
| Pruebas automatizadas y reportes | S2–S6 | Unitarias y de mutación; integración y sistema; interfaz de usuario; aceptación (Cucumber) |
| Aspectos no funcionales | S2–S6 | Sesiones, historial, caché, logs y telemetría |
| Infraestructura, release y documentación | S6–S12 | Terraform con pruebas y reporte de costos, release, GitHub Pages, README con diagramas |
| Reto e integraciones | S8–S14 | Servidor MCP, skill y agente de Claude Code, integraciones externas |
| Piloto, dashboard y presentación | S12–S16 | Medición de objetivos SMART con el dashboard de utilización; presentación final |

## 1.3 Descripción

TARE·IA es una aplicación **Android nativa** (Kotlin, Jetpack Compose y Material 3) para estudiantes
universitarios. El estudiante toma una foto de un apunte, importa un PDF o una captura, comparte un texto
desde otra app o lee sus tareas del **aula virtual Moodle** de la UPT. La app reconoce el texto en el
dispositivo (ML Kit), usa **Gemini** (mediante Firebase AI Logic) para extraer título, curso y fecha, y
propone las actividades. **Nada se guarda como confirmado ni se envía al calendario sin que el estudiante
lo valide** (reglas RN01 y RN09). Luego la app sincroniza las actividades con **Google Calendar**, programa
recordatorios, genera planes de estudio por sesiones y un reporte PDF.

La app se conecta a una **API REST** (Node 22 + Fastify 5 + TypeScript) con base de datos **PostgreSQL 16**
(migraciones con Liquibase). La API guarda sesiones, historial de cambios, telemetría de uso y logs del
cliente. Un **dashboard web** (React 18 + Vite), solo para el rol administrador, muestra la utilización.
La infraestructura se define con **Terraform** (Neon y Render) y la calidad se controla con **GitHub
Actions** (pruebas, mutación, SonarQube, Semgrep, Snyk, release y documentación).

**Fuera de alcance (trabajo futuro):** versión iOS, publicación en Google Play Store, escritura en Moodle
(la integración es de solo lectura), sincronización bidireccional continua con Google Calendar, y acceso a
otros sistemas académicos de la UPT distintos de Moodle.

## 1.4 Objetivos

### 1.4.1 Objetivo general

Desarrollar, probar y documentar en 16 semanas TARE·IA, un asistente académico Android con API en la nube
y dashboard de utilización, que convierta apuntes, PDF, capturas y tareas de Moodle en actividades con
fecha, planes de estudio y recordatorios mediante IA y Google Calendar, logrando que al menos el 50 % de
las actividades lleguen por IA o Moodle y que al menos el 80 % de las actividades completadas se cierren
antes de su fecha límite, con calidad verificada por pruebas automatizadas (mutación ≥ 60 %) y análisis
estático sin bugs ni vulnerabilidades críticas.

### 1.4.2 Objetivos específicos (SMART)

| ID | Objetivo | Indicador | Línea base | Meta | Instrumento de medición | Plazo |
|---|---|---|---|---|---|---|
| OS-1 | Que la mayoría de actividades no se escriban a mano. | % de actividades con origen IA o Moodle | Sin medición previa | ≥ 50 % | Vista `v_origen_actividades`; dashboard | S14 |
| OS-2 | Registrar una tarea en menos tiempo. | Mediana de segundos entre `importacion_iniciada` y `actividad_confirmada` | Registro manual (OI-2, S4) | ≤ 60 s y ≥ 50 % menos que el manual | Tabla `evento` + prueba cronometrada | S12 |
| OS-3 | Completar actividades antes de su fecha. | % de COMPLETADAS con `completada_en` ≤ `fecha_limite` | Sistema nuevo | ≥ 80 % | Consulta SQL sobre `actividad`; dashboard | S15 |
| OS-4 | La IA nunca crea eventos sin validación. | Actividades SINCRONIZADAS sin pasar por CONFIRMADA | Trigger de transición | 0 | `historial_cambio`; HU-03 | S6 |
| OS-5 | Evitar duplicados. | Grupos duplicados (título + curso + fecha normalizados) | Defecto hallado y corregido | 0 | Consulta SQL; HU-02 | S6 |
| OS-6 | Uso recurrente. | Sesiones por usuario activo por semana | Sistema nuevo | ≥ 3 (mediana) | Tabla `sesion`; `v_resumen_sesiones` | S15 |
| OS-7 | Estabilidad en uso real. | % de sesiones con al menos un log ERROR | Sistema nuevo | ≤ 5 % | Tabla `log_cliente` | S15 |
| OS-8 | Pruebas que detectan defectos. | Puntaje de mutación | Stryker 93,8 %; Pitest 68,4 % | ≥ 60 % (API) y ≥ 50 % (dominio) | `mutacion.yml` | S6 |
| OS-9 | Batería de pruebas en verde. | % de pruebas que pasan | 99 API + 35 JVM + 14 Compose + 16 Playwright + 18 escenarios, en verde | 100 % en `main` | `ci.yml`, `android.yml`, reportes | S6–S16 |
| OS-10 | Sin defectos ni vulnerabilidades críticas. | Bugs/vulnerabilidades críticos (SonarQube); hallazgos críticos/altos (Semgrep, Snyk) | Pendiente de la primera ejecución | 0; quality gate aprobado; hotspots revisados | `sonarqube.yml`, `semgrep.yml`, `snyk.yml` | S2 |
| OS-11 | Operar sin costo en desarrollo y piloto. | Costo mensual de infraestructura | S/ 720/mes (estimación de la versión previa) | USD 0/mes | Reporte de costos de `infra.yml` | S16 |

El detalle (fórmulas, objetivos de investigación OI-1 a OI-4 y fuentes) está en
`docs/investigacion/problema-y-objetivos.md`.

# 2. Riesgos

| N.° | Riesgo | Probabilidad | Impacto | Plan de contingencia |
|---|---|---|---|---|
| 1 | **Equipo de dos integrantes:** enfermedad o exámenes de uno de ellos detienen el avance. | Alta | Alto | Alcance priorizado (RF de prioridad alta primero); tablero de issues; automatización de pruebas y despliegue; documentación generada por workflows (`docs.yml`). |
| 2 | **La IA extrae datos incorrectos** (fecha o curso equivocados, "alucinaciones"). | Alta | Alto | Toda actividad de IA o Moodle nace PENDIENTE_VALIDAR (RN09); el estudiante confirma con título y fecha obligatorios (RN05); el trigger impide sincronizar sin validar (RN01); estrategia de planificación local si la IA falla. |
| 3 | **Límites de la capa gratuita de Gemini** (Firebase AI Logic en plan Spark) o cambios de modelo. | Media | Medio | Patrón Abstract Factory: proveedor B compatible con OpenAI; extractor y planificador locales como respaldo; Firebase App Check contra abuso. |
| 4 | **El aula virtual Moodle no habilita los Web Services** o el token del estudiante. | Media | Medio | Integración de solo lectura configurable; importación por PDF, captura o texto compartido como alternativa (RF-15, RF-22). |
| 5 | **Arranque en frío del plan gratuito de Render:** la API se suspende tras 15 min sin tráfico y tarda cerca de 1 min en despertar (Render, s. f.-a). | Alta | Bajo | La app funciona sin conexión con su base local cifrada; la telemetría y los logs se encolan y se envían por lotes; plan de pago como referencia (sección 4.2.4 b). |
| 6 | **Límites de Neon Free** (almacenamiento por proyecto y horas de cómputo; Neon, s. f.). | Media | Medio | Telemetría sin datos libres grandes; índices BRIN por fecha; vista materializada para el dashboard; Terraform modular para migrar a AWS o Azure. |
| 7 | **Datos personales y tokens de Google** expuestos por error. | Baja | Alto | Datos mínimos; tokens cifrados con Android Keystore; logs con datos sensibles ocultos (pino *redact*) y correos enmascarados; telemetría sin datos personales; Semgrep (secretos) y Snyk en CI. |
| 8 | **Revocación o cambio de permisos OAuth de Google Calendar.** | Media | Medio | Manejo de autorización denegada o revocada; la actividad queda CONFIRMADA y se reintenta la sincronización; identificador de evento estable para no duplicar. |
| 9 | **Cambios en precios o condiciones de los servicios gratuitos.** | Media | Medio | Infraestructura como código; reporte de costos con Infracost en cada ejecución de `infra.yml`; referencia AWS/Azure ya modelada. |

# 3. Análisis de la situación actual

## 3.1 Planteamiento del problema

Entre el 80 % y el 95 % de los universitarios procrastina, según las estimaciones recogidas por Steel
(2007), y la procrastinación se asocia con menor rendimiento académico (Kim & Seo, 2015). En cambio, la
gestión del tiempo se asocia con mayor logro académico y bienestar (Aeon et al., 2021), y la planificación
de corto plazo predice el promedio mejor que el puntaje de admisión (Britton & Tesser, 1991). Lo que más
reduce la procrastinación es fijar metas y prioridades, no usar herramientas por sí solas (Garzón
Umerenkova & Gil Flores, 2017). En universitarios peruanos se reportan niveles altos de estrés académico,
con la sobrecarga de tareas entre sus estresores principales (Estrada Araoz et al., 2021).

En la EPIS-UPT, las tareas llegan por el aula virtual Moodle, sílabos en PDF, apuntes y capturas. El
estudiante copia a mano cada fecha a su calendario. El tiempo que esto toma y la frecuencia de entregas
olvidadas **no están publicados**; se medirán con los objetivos de investigación OI-1 y OI-2.

La versión previa de TARE·IA (curso Soluciones Móviles I) era una app local con PIN, sin backend, sin
pruebas automatizadas ejecutadas y con un presupuesto de S/ 66 005,00 basado en fondos semilla. La versión
1.0 de este informe corresponde al producto rehecho para SI-784: autenticación con Google, API en la nube,
telemetría, pruebas automatizadas y servicios en capa gratuita.

## 3.2 Consideraciones de hardware y software

| Componente | Tecnología | Justificación |
|---|---|---|
| App móvil | Kotlin 2.3.21, Jetpack Compose + Material 3, minSdk 28 (Android 9), targetSdk 35, AGP 8.13.2 | El 90 % de los usuarios de internet en el Perú accede por celular (INEI, 2024b). |
| Arquitectura de la app | Capas `ui`, `aplicacion` (Gestor*), `dominio` (DDD), `ia`, `infraestructura`, `remoto`; patrones State, Builder, Prototype, Strategy, Abstract Factory, Repository, Singleton, Observer y Facade | Dominio probado sin Android (proyecto `verificacion-jvm`); proveedores de IA intercambiables. |
| Persistencia local | Base local cifrada con Android Keystore | Funciona sin conexión; protege datos y tokens. |
| IA y OCR | Gemini mediante Firebase AI Logic; ML Kit Text Recognition en el dispositivo | El OCR no envía la imagen; la IA recibe solo el texto necesario. |
| Autenticación | Firebase Authentication con Google Sign-In (Credential Manager); Firebase App Check | El estudiante entra con su cuenta Google; la API verifica el ID token (jose + JWKS de Google). |
| API | Node 22 + Fastify 5 + TypeScript; JWT de 7 días con id de sesión; OpenAPI/Swagger; helmet, CORS restringido, rate limit | Un lenguaje tipado en API y dashboard; bajo consumo de memoria (apto para 512 MB). |
| Base de datos | PostgreSQL 16 (Neon); Liquibase con 11 changesets SQL con *rollback*; 11 tablas, 6 vistas, 7 funciones, 7 triggers | Reglas críticas (estado, duplicados, historial) también en la BD. |
| Dashboard | React 18 + Vite | Utilización: KPIs, sesiones, eventos, funciones, pantallas, origen × estado. |
| Infraestructura | Terraform (Neon + Render), estado en HCP Terraform, `terraform test`, Infracost | Reproducible y migrable a AWS o Azure. |
| CI/CD y calidad | GitHub Actions: `ci`, `android`, `mutacion`, `sonarqube`, `semgrep`, `snyk`, `infra`, `release`, `docs`, `pages` | Cada cambio se prueba y analiza automáticamente. |
| Equipo de desarrollo | 2 laptops personales (8–16 GB RAM), emulador Android API 34 y celular Android personal | Equipos existentes; no se compra hardware. |
| Cliente mínimo | Celular Android 9 o superior con cámara y conexión intermitente | Gama media típica de estudiantes. |

# 4. Estudio de factibilidad

Se evalúa la viabilidad técnica, económica, operativa, legal, social y ambiental del proyecto para su
desarrollo en el curso y un piloto con estudiantes de la EPIS-UPT.

## 4.1 Factibilidad técnica

- **Arquitectura comprobada:** la app, la API, la base de datos y el dashboard ya están implementados en el
  repositorio y cuentan con pruebas automatizadas que pasan localmente: **99 pruebas de la API** (66
  unitarias con Vitest, 22 de integración contra PostgreSQL real y 11 de sistema con servidor HTTP real),
  **35 pruebas JVM** del dominio y la capa remota de Android, **14 pruebas de interfaz Compose** en
  emulador, **16 pruebas Playwright** del dashboard (8 × 2 dispositivos) y **18 escenarios Cucumber
  (76 pasos)** de aceptación.
- **Calidad de las pruebas:** puntaje de mutación de **93,8 %** con Stryker en la API (umbral 60 %) y de
  **68,4 %** con Pitest en el dominio Kotlin (umbral 50 %). Las pruebas encontraron y permitieron corregir
  **3 defectos reales**: el historial fallaba al borrar un usuario en cascada; un cambio de correo con el
  mismo UID de Google chocaba (y el correo no se normalizaba a minúsculas); y la detección de duplicados en
  PostgreSQL no unificaba espacios repetidos.
- **Análisis estático automatizado:** existen los workflows `sonarqube.yml` (bugs, vulnerabilidades,
  *security hotspots* y quality gate en SonarQube Cloud), `semgrep.yml` (reglas para Kotlin, TypeScript,
  React, Node.js, secretos, Terraform e inyección SQL; SARIF a Code Scanning) y `snyk.yml` (dependencias
  npm y Gradle, Snyk Code e IaC). Producen sus reportes al ejecutarse en GitHub Actions.
- **Integraciones externas:** Firebase Authentication, Gemini (Firebase AI Logic), Google Calendar REST
  API con OAuth, Moodle Web Services (solo lectura), ML Kit OCR en el dispositivo y Firebase App Check. La
  API de Google Calendar no tiene costo adicional; tiene límites por minuto por proyecto y por usuario
  (Google for Developers, s. f.).
- **Competencias del equipo:** Kotlin y Compose (proyecto previo de Soluciones Móviles I), PostgreSQL y
  TypeScript (proyecto AtomReactChamber de Base de Datos II). El aprendizaje nuevo está en pruebas de
  mutación, análisis estático y Terraform, apoyado en la documentación oficial.

**Conclusión:** técnicamente factible con herramientas libres y servicios en capa gratuita.

## 4.2 Factibilidad económica

Supuestos generales: tipo de cambio **S/ 3,45 por US$** (supuesto de trabajo; verificar el valor del día
en BCRPData); duración **4 meses** (16 semanas); todos los montos en soles.

### 4.2.1 Costos generales (equipos)

Se usan las laptops personales existentes de los dos integrantes. No se compra equipo: se imputa la
**depreciación** del periodo con la tasa máxima de 25 % anual para equipos de procesamiento de datos
(Reglamento de la Ley del Impuesto a la Renta, art. 22; SUNAT, 2020).

Depreciación por laptop = valor × tasa anual × (meses / 12) = 3 800,00 × 0,25 × 4/12 = **S/ 316,67**

| Recurso | Cantidad | Valor unitario (S/) | Tasa anual | Meses | Costo imputado (S/) |
|---|---|---|---|---|---|
| Laptop personal (existente) | 2 | 3 800,00 | 25 % | 4 | 633,33 |
| Celular Android personal para pruebas (existente) | 1 | — | — | 4 | 0,00 *(supuesto: uso personal, no se imputa)* |
| Android Studio, VS Code, Git, Node, PostgreSQL local | — | 0,00 | — | 4 | 0,00 |
| **Total** | | | | | **633,33** |

Verificación: 2 × 316,67 = 633,33 (redondeado de 633,333…).

### 4.2.2 Costos operativos durante el desarrollo

El trabajo se hace en casa y en los laboratorios de la UPT; no hay alquiler de oficina.

| Servicio | Cálculo | Costo total (S/) |
|---|---|---|
| Electricidad | 0,065 kW × 320 h = 20,8 kWh × S/ 0,80/kWh *(tarifa supuesta; verificar recibo)* | 16,64 |
| Internet | 30 % del plan de S/ 70,00 × 4 meses × 2 integrantes | 168,00 |
| **Total** | | **184,64** |

### 4.2.3 Costos de personal

Dos integrantes con roles rotativos. Dedicación: **10 h por semana por integrante × 2 integrantes ×
16 semanas = 320 h**. Tarifa de costo de oportunidad: **S/ 15,00/h** *(supuesto de practicante de
ingeniería)*.

| Rol rotativo | Actividades | Horas | Subtotal (S/) |
|---|---|---|---|
| Analista / gestión | Problema, objetivos, FD01–FD04, cronograma, issues | 32 | 480,00 |
| Desarrollo Android | Pantallas Compose, dominio, IA, OCR, Calendar, Moodle | 80 | 1 200,00 |
| Desarrollo backend y BD | API Fastify, Liquibase, funciones, triggers, vistas | 56 | 840,00 |
| Dashboard | KPIs, gráficos, tablas, accesibilidad | 24 | 360,00 |
| QA y pruebas automatizadas | Unitarias, mutación, integración, sistema, UI, aceptación | 64 | 960,00 |
| DevOps y seguridad | GitHub Actions, Terraform, SonarQube, Semgrep, Snyk, release | 40 | 600,00 |
| Reto e integraciones | Servidor MCP, skill y agente de Claude Code | 12 | 180,00 |
| Documentación y presentación | README, diagramas, GitHub Pages, exposición | 12 | 180,00 |
| **Total** | | **320** | **4 800,00** |

### 4.2.4 Costos del ambiente

**a) Desarrollo y piloto (capas gratuitas)**

| Servicio | Uso | Plan | Límite relevante | Costo (S/) |
|---|---|---|---|---|
| Neon | PostgreSQL 16 | Free | Almacenamiento limitado por proyecto; cómputo con autosuspensión (Neon, s. f.) | 0,00 |
| Render | API (web service) y dashboard (static site) | Free | Se suspende tras 15 min sin tráfico (Render, s. f.-a) | 0,00 |
| Firebase | Authentication, App Check, AI Logic (Gemini) | Spark (sin cuenta de facturación) | AI Logic es gratuito; Gemini usa la capa gratuita de la Gemini Developer API con modelos y cuotas limitadas (Firebase, s. f.-b) | 0,00 |
| Google Calendar API | Crear, actualizar y borrar eventos | Sin costo | Límites por minuto por proyecto y por usuario (Google for Developers, s. f.) | 0,00 |
| ML Kit Text Recognition | OCR en el dispositivo | Sin costo | Se ejecuta en el celular | 0,00 |
| GitHub | Repositorio, Actions, Pages, Releases, Code Scanning | Gratis (repositorio público) | — | 0,00 |
| HCP Terraform | Estado remoto de Terraform | Free | — | 0,00 |
| SonarQube Cloud, Semgrep, Snyk | Análisis estático y de dependencias | Planes gratuitos para proyectos de código abierto | Verificar límites vigentes | 0,00 |
| Certificados TLS y dominio | HTTPS en subdominios `onrender.com` y `github.io` | Incluidos | — | 0,00 |
| **Total** | | | | **0,00** |

**b) Referencia de una arquitectura de pago — ESTIMADO, no es un gasto del proyecto**

Precios de lista aproximados (on-demand, región `us-east-1` / `eastus`, 730 h/mes) documentados en
`infra/terraform/cost-reference/README.md` y recalculados por Infracost en `infra.yml`:

| Nube | Componentes | US$/mes | US$/año | S/ /año |
|---|---|---|---|---|
| AWS | RDS PostgreSQL `db.t4g.micro` + 20 GB gp3 (13,98) + ECS Fargate 0,25 vCPU/0,5 GB (7,21) + Application Load Balancer (16,43 + uso) + S3/CloudFront (~0,03) + Secrets Manager (0,40) | ≈ 38 | ≈ 456 | **1 573,20** |
| Azure | PostgreSQL Flexible Server B1ms + 32 GB (≈ 16) + App Service B1 (≈ 13) + Static Web Apps Standard (9) | ≈ 38 | ≈ 456 | 1 573,20 |

Cálculo: 38 × 12 = 456 US$/año × 3,45 = S/ 1 573,20 por año. No incluye transferencia adicional ni
consumo de Gemini en planes de pago.

### 4.2.5 Costos totales del desarrollo del sistema

| Concepto | Costo (S/) |
|---|---|
| Costos generales (depreciación) | 633,33 |
| Costos operativos | 184,64 |
| Costos del ambiente (capas gratuitas) | 0,00 |
| Costos de personal | 4 800,00 |
| **Total general del proyecto** | **5 617,97** |

Verificación: 633,33 + 184,64 + 0,00 + 4 800,00 = 5 617,97.

**Comparación con la versión previa:** el FD01 del curso Soluciones Móviles I estimaba S/ 66 005,00 de
desarrollo y S/ 720,00 mensuales de operación. La diferencia se explica porque ahora se imputa la
depreciación (no la compra) de equipos, el equipo es de dos personas con horas reales y los servicios de
pago se sustituyeron por capas gratuitas.

## 4.3 Factibilidad operativa

- **Para el estudiante:** instala el APK (publicado en GitHub Releases por `release.yml`), entra con su
  cuenta Google y empieza a importar. El flujo de importación y confirmación está pensado para sesiones
  breves entre clases.
- **Funciona sin conexión:** los datos viven en la base local cifrada; la telemetría (cola de hasta 200
  eventos, lotes de 20) y los logs se envían cuando hay red.
- **Moodle:** el estudiante configura la URL HTTPS del aula virtual y un token de Web Services; la
  integración es de solo lectura y requiere que el administrador de Moodle habilite las funciones
  `core_webservice_get_site_info`, `core_enrol_get_users_courses` y `mod_assign_get_assignments`.
- **Para el administrador:** el dashboard (rol admin) muestra sesiones, eventos, funciones y pantallas más
  usadas, y el origen de las actividades, sin acceder al contenido de los apuntes.
- **Para el equipo:** pruebas, análisis estático, migraciones Liquibase, documentación, release y
  despliegue están automatizados en GitHub Actions; el mantenimiento manual es mínimo.

**Conclusión:** operativamente factible. Depende de que la UPT habilite los Web Services de Moodle para la
lectura de tareas; si no, la importación por PDF, captura o texto compartido cubre el caso.

## 4.4 Factibilidad legal

- **Ley N.° 29733, Ley de Protección de Datos Personales** (Congreso de la República del Perú, 2011), y su
  **Reglamento, D. S. N.° 016-2024-JUS** (Ministerio de Justicia y Derechos Humanos, 2024a), publicado el
  30 de noviembre de 2024, que reemplazó al D. S. 003-2013-JUS (Ministerio de Justicia y Derechos Humanos,
  2024b). *Aplicación en TARE·IA:*
  - **Datos mínimos:** correo, nombre y UID de Google, perfil académico básico, cursos y actividades. No se
    piden DNI, fotos de perfil propias ni datos sensibles.
  - **Consentimiento informado:** el estudiante inicia sesión con Google y autoriza por separado el acceso
    a Google Calendar (OAuth); la IA solo procesa el contenido que el estudiante decide importar.
  - **Telemetría sin datos personales:** los eventos de uso guardan tipo, pantalla y métricas (por ejemplo
    `{"origen":"IA","propuestas":3}`); no guardan títulos, apuntes, correos ni tokens.
  - **Logs:** la API oculta `authorization`, `password` e `idToken` (pino *redact*); la app enmascara
    correos y tokens antes de subir logs WARN/ERROR.
  - **Seguridad:** HTTPS, JWT con id de sesión revocable (logout y cierre por 30 min de inactividad),
    tokens cifrados con Android Keystore, rate limit y CORS restringido.
  - **Derechos ARCO:** el estudiante puede borrar actividades, cursos y apuntes (papelera y eliminación
    permanente); el borrado del usuario se propaga en cascada.
  - Los usuarios son estudiantes universitarios, normalmente mayores de edad; si se incluyera a menores de
    14 años, se requeriría el consentimiento de quienes ejercen la patria potestad (Ministerio de Justicia
    y Derechos Humanos, 2024b).
- **Términos de Google y Firebase:** el uso de Firebase Authentication, Firebase AI Logic (Gemini) y la API
  de Google Calendar está sujeto a los términos de servicio de Firebase y de las API de Google (Firebase,
  s. f.-a; Google, s. f.). Los datos de Calendar se usan solo para la función que el estudiante pide (crear
  y actualizar sus eventos). En la capa gratuita de la Gemini Developer API, Google puede usar el contenido
  enviado para mejorar sus productos según sus condiciones vigentes; por eso la app envía solo el texto
  extraído necesario y lo informa al estudiante (verificar las condiciones vigentes antes del piloto).
- **Moodle:** la lectura usa los Web Services oficiales con el token del propio estudiante; no se hace
  *scraping* ni se guardan credenciales del aula virtual en texto plano.
- **Decreto Legislativo N.° 822, Ley sobre el Derecho de Autor** (1996) y licencias de terceros (Apache 2.0
  de Android Jetpack y Firebase SDK, MIT de Fastify y React, PostgreSQL License, OFL de las fuentes
  tipográficas en `docs/licencias`).

**Conclusión:** legalmente factible con las medidas de minimización y seguridad ya implementadas; antes del
piloto se debe publicar una política de privacidad en lenguaje sencillo.

## 4.5 Factibilidad social

El público son **estudiantes de pregrado de la EPIS-UPT**. El 98,4 % de la población con educación superior
universitaria usa internet (INEI, 2024b; Andina, 2024), por lo que el acceso no es una barrera. El proyecto
ataca una causa académica controlable del estrés y de las entregas tardías: la desorganización de
actividades dispersas (Estrada Araoz et al., 2021; Kim & Seo, 2015). La app está en español, es gratuita y
mantiene al estudiante en control (valida todo lo que propone la IA). **Conclusión:** socialmente factible
y pertinente.

## 4.6 Factibilidad ambiental

TARE·IA reemplaza agendas impresas y avisos en papel por registros digitales. El OCR se ejecuta en el
dispositivo y la infraestructura usa servidores compartidos que se suspenden sin tráfico (Render Free y
autosuspensión de Neon). El impacto se limita al consumo eléctrico del desarrollo (≈ 20,8 kWh en total) y
al consumo de los servicios en la nube. **Conclusión:** factible, con impacto ambiental bajo.

# 5. Análisis financiero

## 5.1 Justificación de la inversión

### 5.1.1 Beneficios del proyecto

- **Tangibles:** tiempo ahorrado por el estudiante al no copiar fechas a mano; costo evitado de una
  suscripción a una herramienta equivalente; infraestructura en capa gratuita.
- **Intangibles:** menos olvidos y entregas tardías, menor sensación de sobrecarga, hábitos de
  planificación, datos de uso para mejorar el producto y aprendizaje aplicado de calidad y pruebas de
  software.

### 5.1.2 Criterios de inversión (proyección a 3 años)

**Supuestos (transparentes y modificables):**

| Supuesto | Valor | Fuente / justificación |
|---|---|---|
| Inversión inicial (año 0) | S/ 5 617,97 | Sección 4.2.5 |
| Costo de operación anual (años 1–3) | S/ 1 573,20 | Referencia de pago AWS (sección 4.2.4 b). Es **conservador**: en capa gratuita el costo sería S/ 0 |
| Tiempo ahorrado por estudiante | 10 min/semana × 32 semanas lectivas = 320 min = 5,33 h/año | *Supuesto* a validar con OI-2 y OS-2 |
| Valor de la hora del estudiante | S/ 5,40/h | Referencia: RMV de S/ 1 130 (D. S. 006-2024-TR) ÷ ≈ 208 h/mes; verificar la RMV vigente |
| Costo evitado de suscripción | S/ 10,00/mes × 10 meses = S/ 100,00/año por estudiante | Precio de referencia del modelo premium previsto en el Documento de Visión previo del proyecto (no es un precio de mercado verificado) |
| Beneficio anual por estudiante | 5,33 h × 5,40 + 100,00 = 28,80 + 100,00 = **S/ 128,80** | 320/60 × 5,40 = 28,80 |
| Tasa de descuento | 10 % anual | Costo de oportunidad referencial |
| Horizonte | 3 años | Vida útil razonable de una versión de app móvil |

**Escenarios** (varía el número de estudiantes que usan la app durante el año):

- **Optimista:** 300 estudiantes (una parte importante de la EPIS).
- **Base** — escenario recomendado: 100 estudiantes.
- **Pesimista:** 30 estudiantes (tamaño del piloto).

**Fórmulas**

- Beneficio anual: Bₜ = n × 128,80
- Flujo neto anual: FNₜ = Bₜ − Cₜ, con Cₜ = 1 573,20
- VAN = −I₀ + Σₜ₌₁³ FNₜ / (1 + i)ᵗ, con i = 0,10
- B/C = [Σₜ₌₁³ Bₜ / (1 + i)ᵗ] / [I₀ + Σₜ₌₁³ Cₜ / (1 + i)ᵗ]
- TIR: tasa r tal que −I₀ + Σₜ₌₁³ FNₜ / (1 + r)ᵗ = 0 (resuelta numéricamente por bisección)
- Factores de actualización al 10 %: 1/1,1 = 0,9091; 1/1,1² = 0,8264; 1/1,1³ = 0,7513; suma = 2,4869

**Escenario base (100 estudiantes), paso a paso**

- B = 100 × 128,80 = S/ 12 880,00 por año; C = S/ 1 573,20; FN = S/ 11 306,80
- VAN = −5 617,97 + 11 306,80/1,1 + 11 306,80/1,1² + 11 306,80/1,1³
  = −5 617,97 + 10 278,91 + 9 344,46 + 8 494,97 = **S/ 22 500,37**
- VA beneficios = 11 709,09 + 10 644,63 + 9 676,93 = 32 030,65
- VA costos = 5 617,97 + (1 430,18 + 1 300,17 + 1 181,97) = 5 617,97 + 3 912,32 = 9 530,29
- B/C = 32 030,65 / 9 530,29 = **3,361**
- TIR = **193,28 %**

**Escenario pesimista (30 estudiantes), paso a paso**

- B = 30 × 128,80 = S/ 3 864,00; FN = 3 864,00 − 1 573,20 = S/ 2 290,80
- VAN = −5 617,97 + 2 082,55 + 1 893,22 + 1 721,11 = **S/ 78,91**
- VA beneficios = 3 512,73 + 3 193,39 + 2 903,08 = 9 609,20; B/C = 9 609,20 / 9 530,29 = **1,008**
- TIR = **10,80 %** (apenas sobre la tasa de descuento)

**Resumen de escenarios**

| Escenario | Estudiantes | Beneficio anual (S/) | Flujo neto anual (S/) | VAN 10 % (S/) | B/C | TIR | Decisión |
|---|---|---|---|---|---|---|---|
| Optimista | 300 | 38 640,00 | 37 066,80 | 86 561,68 | 10,083 | 658,28 % | Viable |
| **Base** | **100** | **12 880,00** | **11 306,80** | **22 500,37** | **3,361** | **193,28 %** | **Viable** |
| Pesimista | 30 | 3 864,00 | 2 290,80 | 78,91 | 1,008 | 10,80 % | Viable en el límite |

**Punto de equilibrio:** el beneficio anual que hace VAN = 0 es
B* = I₀ / 2,4869 + C = 5 617,97 / 2,4869 + 1 573,20 = 2 259,07 + 1 573,20 = **S/ 3 832,27 por año**, es
decir, **≈ 30 estudiantes** (3 832,27 / 128,80 = 29,75).

**Interpretación:** con al menos 30 estudiantes activos durante tres años, el proyecto recupera la
inversión aun pagando una infraestructura de referencia en AWS. Si se mantiene en capa gratuita (C = 0),
el punto de equilibrio baja a 5 617,97 / 2,4869 / 128,80 ≈ 18 estudiantes. La viabilidad depende sobre
todo de dos supuestos que el piloto debe validar: el **tiempo ahorrado** (OS-2) y la **disposición a pagar**
por una herramienta equivalente. Si el costo evitado de suscripción fuera cero, el beneficio sería solo
S/ 28,80 por estudiante y se necesitarían ≈ 133 estudiantes en capa de pago (3 832,27 / 28,80); en ese
caso el proyecto se justifica sobre todo por su valor académico y social.

# 6. Conclusiones

1. TARE·IA es **técnicamente factible**: la app Android, la API, la base de datos y el dashboard están
   implementados y verificados con 99 pruebas de API, 35 pruebas JVM y 14 de interfaz en Android, 16
   pruebas Playwright y 18 escenarios de aceptación en verde, y puntajes de mutación de 93,8 % (API) y
   68,4 % (dominio).
2. El costo de desarrollo es **S/ 5 617,97** (dos integrantes, 320 h, depreciación de equipos) y el costo
   de infraestructura durante el desarrollo y el piloto es **S/ 0** (Neon, Render, Firebase Spark, GitHub).
3. En el escenario base (100 estudiantes) el proyecto tiene **VAN = S/ 22 500,37, B/C = 3,361 y TIR =
   193,28 %**; el punto de equilibrio es de unos 30 estudiantes con infraestructura de pago.
4. Es **operativa, legal, social y ambientalmente factible**, con datos mínimos, telemetría sin datos
   personales y validación humana obligatoria de lo que propone la IA (Ley 29733 y D. S. 016-2024-JUS).
5. Los objetivos son medibles con lo que la app ya registra (actividad, evento, sesión, log_cliente) y con
   los reportes de los workflows de calidad; el piloto (S12–S15) confirmará las metas.

# Referencias

Aeon, B., Faber, A., & Panaccio, A. (2021). Does time management work? A meta-analysis. *PLOS ONE, 16*(1), e0245066. https://doi.org/10.1371/journal.pone.0245066

Andina. (2024). *INEI: uso de internet alcanzó al 80.2% de la población de 6 y más años de edad*. https://andina.pe/agencia/noticia-inei-uso-internet-alcanzo-al-802-de-poblacion-6-y-mas-anos-edad-1012997.aspx

Britton, B. K., & Tesser, A. (1991). Effects of time-management practices on college grades. *Journal of Educational Psychology, 83*(3), 405–410. https://doi.org/10.1037/0022-0663.83.3.405

Congreso de la República del Perú. (2011). *Ley N.° 29733, Ley de Protección de Datos Personales*. https://www.gob.pe/institucion/congreso-de-la-republica/normas-legales/243470-29733

Estrada Araoz, E. G., Mamani Roque, M., Gallegos Ramos, N. A., Mamani Uchasara, H. J., & Zuloaga Araoz, M. C. (2021). Estrés académico en estudiantes universitarios peruanos en tiempos de la pandemia del COVID-19. *Archivos Venezolanos de Farmacología y Terapéutica, 40*(1). https://saber.ucv.ve/ojs/index.php/rev_aavft/article/view/22351

Firebase. (s. f.-a). *Firebase terms of service*. Recuperado el 21 de noviembre de 2026, de https://firebase.google.com/terms

Firebase. (s. f.-b). *Pricing for Firebase AI Logic*. Recuperado el 21 de noviembre de 2026, de https://firebase.google.com/docs/ai-logic/pricing

Garzón Umerenkova, A., & Gil Flores, J. (2017). Gestión del tiempo y procrastinación en la educación superior. *Universitas Psychologica, 16*(3), 1–13. https://doi.org/10.11144/Javeriana.upsy16-3.gtpe

Google. (s. f.). *Google APIs terms of service*. Recuperado el 21 de noviembre de 2026, de https://developers.google.com/terms

Google for Developers. (s. f.). *Manage quotas — Google Calendar API*. Recuperado el 21 de noviembre de 2026, de https://developers.google.com/workspace/calendar/api/guides/quota

Instituto Nacional de Estadística e Informática. (2024b). *Uso de internet alcanzó al 80,2% de la población de 6 y más años de edad en el tercer trimestre de 2024*. https://www.gob.pe/institucion/inei/noticias/1081936-uso-de-internet-alcanzo-al-80-2-de-la-poblacion-de-6-y-mas-anos-de-edad-en-el-tercer-trimestre-de-2024

Kim, K. R., & Seo, E. H. (2015). The relationship between procrastination and academic performance: A meta-analysis. *Personality and Individual Differences, 82*, 26–33. https://doi.org/10.1016/j.paid.2015.02.038

Ministerio de Justicia y Derechos Humanos. (2024a). *Decreto Supremo N.° 016-2024-JUS que aprueba el Reglamento de la Ley N.° 29733*. https://www.congreso.gob.pe/Docs/DGP/DIDP/files/ds_016-2024-jus.pdf

Ministerio de Justicia y Derechos Humanos. (2024b). *Ejecutivo aprueba nuevo reglamento de la Ley de Protección de Datos Personales*. https://www.gob.pe/institucion/minjus/noticias/1067368-ejecutivo-aprueba-nuevo-reglamento-de-la-ley-de-proteccion-de-datos-personales

Ministerio de Trabajo y Promoción del Empleo. (2024). *Decreto Supremo N.° 006-2024-TR* [Remuneración mínima vital de S/ 1 130]. https://img.lpderecho.pe/wp-content/uploads/2024/12/Decreto-Supremo-006-2024-TR-LPDerecho.pdf

Neon. (s. f.). *Neon pricing*. Recuperado el 21 de noviembre de 2026, de https://neon.com/pricing

Presidencia del Consejo de Ministros. (1996). *Decreto Legislativo N.° 822, Ley sobre el Derecho de Autor*. https://www.gob.pe/institucion/indecopi/normas-legales/1670023-822

Render. (s. f.-a). *Deploy for free*. https://render.com/docs/free

Render. (s. f.-b). *Pricing*. https://render.com/pricing

Steel, P. (2007). The nature of procrastination: A meta-analytic and theoretical review of quintessential self-regulatory failure. *Psychological Bulletin, 133*(1), 65–94. https://doi.org/10.1037/0033-2909.133.1.65

Superintendencia Nacional de Aduanas y de Administración Tributaria. (2020). *Informe N.° 151-2020-SUNAT/7T0000* [Tasa máxima de depreciación de equipos de procesamiento de datos]. https://www.sunat.gob.pe/legislacion/oficios/2020/informe-oficios/i151-2020-7T0000.pdf
