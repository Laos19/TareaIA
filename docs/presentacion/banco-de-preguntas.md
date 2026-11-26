# Banco de preguntas: exposición de TARE·IA

> SI-784 Calidad y Pruebas de Software · UPT · Ing. Patrick Cuadros Quiroga · Exponen: Adriana Rafaela Laos
> González (2023077474) y Antony Piero Solorzano Zegarra (2023078696)
>
> Preguntas probables del docente con respuesta corta, agrupadas por criterio de la rúbrica. Cada respuesta
> nombra el archivo, el workflow o la tabla, para abrirlo en vivo si hay repregunta.
> Cómo responder: **(1)** repetir la pregunta, **(2)** responder en 30–45 s, **(3)** nombrar el archivo o el
> reporte, **(4)** si hay un límite, decirlo antes de que lo pregunten.
>
> Regla de oro: si no se midió, decir «no lo medimos todavía; lo anotamos como trabajo futuro». Nunca inventar
> una cifra.

## Datos rápidos para memorizar

| Dato | Valor | Dónde |
|---|---|---|
| Pruebas de la API | 99 = 66 unitarias (Vitest) + 22 integración (PostgreSQL real) + 11 sistema | `backend/test`, `ci.yml` |
| Mutación | Stryker 93,8 % (umbral 60 %) · Pitest 68,4 % (umbral 50 %) | `backend/stryker.config.json`, `android/verificacion-jvm`, `mutacion.yml` |
| App Android | 35 pruebas JVM + 16 pruebas Compose en emulador | `android.yml` |
| Interfaz web | 8 pruebas Playwright × 2 dispositivos = 16 | `dashboard`, `ci.yml` |
| Aceptación | 5 historias, 18 escenarios, 76 pasos (Cucumber en español) | `acceptance/features` |
| MCP | 7 herramientas, 14 pruebas | `integrations/mcp` |
| Base de datos | 11 tablas · 6 vistas · 7 funciones · 7 triggers · 20 changesets en 11 archivos SQL | `db/` |
| Costo | USD 0/mes hoy · ≈ USD 38/mes de referencia en AWS o Azure | `infra/README.md §6` |
| Rate limit | 300/min general · 10/min en `/auth/login` · 20/min en `/auth/google` | `backend/src` |
| Defectos reales | 3 (historial en cascada, correo/UID de Google, espacios en duplicados) | diapositiva 9 |

---

## 1. Problemática y objetivos medibles (1 punto)

**1. ¿Cuál es el problema exactamente?**
Las tareas llegan dispersas (Moodle, PDF, apuntes, capturas) y el estudiante copia cada fecha a mano: olvida
entregas y estudia a última hora. Está en `docs/investigacion/problema-y-objetivos.md §2`.

**2. ¿De dónde sale el 80–95 % de procrastinación?**
Del metaanálisis de Steel (2007), que lo toma de estudios previos (Ellis y Knaus, 1977; O'Brien, 2002). Lo
marcamos como fuente secundaria; por eso no lo presentamos como medición propia.

**3. ¿Tienen algún dato peruano?**
Sí: Dominguez-Lara (2017), 517 estudiantes de Lima; interrupción de estudios de 11,5 % en 2021-1 (Minedu vía El
Peruano, 2021) y 98,4 % de uso de internet entre universitarios (INEI, 2024b). No comparamos prevalencias
porque usan instrumentos distintos.

**4. ¿Por qué una app móvil y no una web?**
El 90 % de los usuarios de internet en el Perú entra desde un celular (INEI, 2024b). Además el teléfono tiene
cámara para los apuntes y OCR en el dispositivo.

**5. ¿Hay algún dato que no pudieron verificar?**
Sí: cuántos estudiantes de la EPIS usan calendario y cuánto tardan en registrar una tarea a mano. No lo
inventamos: son los objetivos de investigación OI-1 (encuesta) y OI-2 (prueba cronometrada).

**6. ¿Por qué sus objetivos son SMART?**
Cada uno tiene indicador, línea base, meta, instrumento y semana de plazo. Ejemplo: OS-4, «0 actividades
sincronizadas sin validar», medido con `historial_cambio` y la HU-03, plazo S6.

**7. ¿Cuál es el objetivo general?**
En 16 semanas, desarrollar, probar y documentar TARE·IA con ≥ 50 % de actividades por IA o Moodle, ≥ 80 %
completadas antes de su fecha, mutación ≥ 60 % y análisis estático sin críticos.

**8. ¿Qué objetivos ya cumplieron y cuáles no?**
Cumplidos: OS-4, OS-5, OS-8, OS-9 y OS-11. Pendientes del piloto: OS-1, OS-2, OS-3, OS-6, OS-7 y los OI. La
instrumentación (tablas `evento`, `sesion`, vistas) ya está.

**9. ¿Van a medir si mejora el rendimiento académico?**
No. Eso necesita un estudio longitudinal; lo decimos en §2.2. Medimos uso y puntualidad de entregas.

## 2. FD01–FD04 (2 puntos)

**10. ¿Qué contiene cada FD?**
FD01 factibilidad (técnica, económica, operativa, legal), FD02 visión, FD03 SRS con RF y RNF, FD04 arquitectura
(SAD). Están en `docs/fd/`.

**11. ¿Cómo justifican la factibilidad económica?**
Con el reporte de costos de `infra.yml`: USD 0/mes en capas gratuitas y ≈ USD 38/mes de referencia en AWS o
Azure calculado con Infracost.

**12. ¿Qué vistas de arquitectura tiene el FD04?**
Componentes, despliegue, clases y datos (ER). Los diagramas Mermaid se generan con `docs.yml`.

**13. ¿Cuántos requisitos funcionales tienen?**
RF-01 a RF-26 (la matriz está en `docs/historico/MATRIZ_RF01_RF26.md` y en el FD03).

**14. ¿Qué reglas de negocio son las más importantes?**
RN01 (nada a Calendar sin validar), RN02 (sin duplicados), RN05 (confirmar exige título y fecha) y RN09 (lo de
IA o Moodle nace pendiente de validar).

## 3. Código fuente, base de datos y arquitectura (2 puntos)

**15. ¿Qué arquitectura usaron?**
Capas en las tres piezas. App: `ui/` → `aplicacion/` → `dominio/` ← `infraestructura/`, `ia/`, `remoto/`.
API: rutas → servicios → repositorios → PostgreSQL. Inyección de dependencias manual.

**16. ¿Por qué el dominio no depende de Android?**
Para probarlo en la JVM sin emulador y correr Pitest sobre él (`android/verificacion-jvm`).

**17. Explique el patrón State.**
`EstadoActividad`: Pendiente de validar → Confirmada → Sincronizada → Completada, con «reabrir». Cada estado
decide qué transiciones permite. En la BD, `fn_actividad_transicion` repite la regla en un trigger.

**18. ¿Para qué el Builder?**
`ActividadBuilder` decide el estado inicial: manual con fecha nace Confirmada; IA o Moodle nace Pendiente de
validar (RN09). Así la regla no se repite en cada pantalla.

**19. ¿Dónde está el Prototype?**
`PlanEstudio.clonar()` crea una propuesta a partir del plan vigente; al confirmarla, el anterior queda
REEMPLAZADO y conserva su historial (`fn_confirmar_plan`).

**20. ¿Strategy y Abstract Factory no son lo mismo?**
No. Strategy elige *cómo planificar* en ejecución (con IA o local por prioridad, con respaldo si la IA falla).
Abstract Factory crea la *familia de servicios de IA* (Gemini o un proveedor compatible con OpenAI).

**21. ¿Dónde usan Observer?**
`GestorActividades` avisa cada evento de uso a `ObservadorUso`, que lo encola para la telemetría. El gestor no
sabe nada de la telemetría.

**22. ¿Por qué Singleton en la base local? ¿No es un antipatrón?**
La base local cifrada (`AppDatabase`) debe tener una sola instancia para no abrir conexiones en paralelo ni
duplicar la clave. Se obtiene desde `Dependencias`, y las pruebas del dominio usan repositorios en memoria.

**23. ¿Qué hace la Facade «Nube»?**
Esconde la API REST, el JWT, la cola de eventos y la de logs detrás de métodos simples para la capa de
aplicación.

**24. ¿Por qué poner reglas en triggers si ya están en el código?**
Defensa en profundidad: la BD es la última barrera si alguien usa otro cliente (MCP, SQL directo). El costo es
duplicar la regla; por eso ambas tienen pruebas.

**25. ¿Cómo evitan duplicados en la BD?**
Antes de insertar, el repositorio busca una actividad con igual `fn_normalizar(titulo)`, curso y fecha
(`AcademicoRepository.ts`); la API responde 409 si existe. `fn_normalizar` pone minúsculas, quita tildes y unifica
espacios: las pruebas encontraron que no unificaba espacios repetidos y se corrigió.

**26. ¿Cómo versionan la base de datos?**
Liquibase: 20 changesets en 11 archivos SQL, cada uno con rollback. `release.yml` los aplica en producción.

**27. ¿Qué pasa si dos dispositivos editan la misma actividad?**
`fn_tocar_version` sube la versión en cada cambio y la sincronización rechaza un cambio más antiguo (escenario
«Un cambio antiguo no pisa uno más reciente», HU-04).

**28. ¿Cómo protegen los datos en el teléfono?**
Base local cifrada con claves del Android Keystore; el JWT también se guarda cifrado.

## 4. Requisitos no funcionales (semanas 2–6)

**29. ¿Cómo manejan las sesiones?**
El ID token de Firebase se verifica con las llaves públicas de Google (jose + JWKS) y se canjea por un JWT de 7
días con id de sesión. Cada inicio crea una fila en `sesion`; se cierra con logout o tras 30 min sin actividad
(`fn_cerrar_sesiones_inactivas`).

**30. Si roban el JWT, ¿qué pasa?**
Al hacer logout la sesión queda cerrada en la BD y el token deja de servir, aunque no haya vencido. Además el
token va cifrado en el teléfono y solo por HTTPS.

**31. ¿Cómo funciona el historial?**
`fn_registrar_historial` guarda antes y después en JSONB de cada alta, cambio o borrado de cursos, actividades y
apuntes en `historial_cambio`. Se consulta en `GET /actividades/{id}/historial`.

**32. ¿Qué cachean y cómo lo comprueban?**
El dashboard en memoria con TTL (`MemoryCache`), la vista materializada `mv_uso_diario` y una caché HTTP de 5 MB
en OkHttp. La respuesta trae `x-cache: HIT/MISS` y `/api/health` muestra aciertos y fallos.

**33. ¿Y si la caché muestra datos viejos?**
El TTL es corto y el botón «Recalcular métricas» (`/dashboard/refrescar`) refresca la vista materializada y
vacía la caché.

**34. ¿Cómo evitan guardar datos sensibles en los logs?**
pino oculta `authorization`, `password` e `idToken`. Los logs que sube la app los enmascara la API al recibirlos
(correos y tokens) antes de guardarlos en `log_cliente`.

**35. ¿Qué telemetría guardan y es legal?**
Eventos de uso (pantalla vista, actividad creada, importación IA, Moodle…) sin datos personales, en lotes de 20
con un máximo de 200 en cola. Va en línea con la Ley 29733: minimizar datos. La HU-05 lo prueba.

**36. ¿Qué otras medidas de seguridad tiene la API?**
helmet, CORS restringido, rate limit (300/min general, 10/min en login, 20/min en `/auth/google`) y Firebase
App Check en la app. Las pruebas de sistema verifican CORS, helmet y rate limit.

## 5. Pruebas automatizadas y sus reportes (semanas 2–6)

**37. ¿Qué diferencia hay entre sus pruebas de integración y de sistema?**
Integración: servicios y repositorios contra un PostgreSQL real (22). Sistema: el servidor HTTP completo
levantado, probando CORS, helmet, rate limit y Swagger (11).

**38. ¿Por qué no usan mocks de la base de datos?**
Porque las reglas viven también en triggers; un mock no las ejecutaría. Los 3 defectos se encontraron gracias a
la BD real.

**39. ¿Qué es la prueba de mutación?**
La herramienta cambia el código a propósito (por ejemplo `>` por `>=`) y vuelve a correr las pruebas. Si alguna
falla, el mutante «muere». El puntaje es el % de mutantes muertos.

**40. ¿Por qué Pitest da menos que Stryker?**
El dominio Kotlin tiene más ramas de interfaz y fechas; quedan mutantes en mensajes y casos borde. Superamos el
umbral (50 %) y la meta futura es ≥ 75 %.

**41. ¿Qué umbrales configuraron?**
Stryker: high 85, low 70, break 60 (`stryker.config.json`). Pitest: `mutationThreshold = 50`. Si baja del
umbral, el workflow falla.

**42. ¿Qué cubren las pruebas de interfaz?**
Playwright: login, control por rol, KPIs, gráficos con vista de tabla, en escritorio y Pixel 7 (16). Compose: 16
pruebas en emulador de pantallas, semántica para TalkBack y áreas táctiles de 48 dp.

**43. ¿Cómo hicieron las pruebas de aceptación?**
Cucumber con Gherkin en español: 5 historias (HU-01 a HU-05), 18 escenarios y 76 pasos, contra la API real.

**44. Lea un escenario.**
HU-03: «Dado que la IA propuso una actividad, cuando intento enviarla al calendario sin validar, entonces el
sistema lo rechaza (RN01)». Archivo `acceptance/features/hu03-validacion-ia.feature`.

**45. Cuénteme uno de los defectos que encontraron.**
Al borrar un usuario, el borrado en cascada disparaba el trigger de historial, que intentaba insertar con un
usuario que ya no existía. Se corrigió y quedó una prueba de regresión.

**46. ¿Y el del correo?**
Un mismo UID de Google con otro correo, o el correo con mayúsculas, chocaba al iniciar sesión. Ahora se
normaliza a minúsculas y se identifica por UID.

**47. ¿Dónde están los reportes?**
Cobertura y resultados en los artefactos de `ci.yml` y `android.yml`; mutación en `mutacion.yml`; aceptación
en su reporte HTML. Todos se publican en GitHub Pages.

**48. ¿Qué cobertura de código tienen?**
La genera `ci.yml` y la muestra SonarQube. Respuesta honesta: preferimos mostrar el puntaje de mutación porque
mide si las pruebas detectan errores, no solo si pasan por las líneas. [Abrir el reporte para leer el número
del día.]

## 6. Análisis estático: SonarQube, Semgrep y Snyk (4 puntos)

**49. ¿Qué reporta SonarQube?**
Bugs, vulnerabilidades, *security hotspots*, código duplicado, *code smells* y cobertura, con un quality gate en
cada PR (`sonarqube.yml`, SonarQube Cloud).

**50. ¿Qué es un security hotspot?**
Código sensible que no es necesariamente una vulnerabilidad pero que una persona debe revisar (por ejemplo,
CORS o una expresión regular). La meta OS-10 es 100 % revisados.

**51. ¿Por qué usar Semgrep si ya tienen SonarQube?**
Semgrep tiene reglas específicas (p/default, kotlin, typescript, react, nodejs, secrets, terraform,
sql-injection) y publica SARIF en GitHub Code Scanning. Detecta patrones que Sonar no cubre, como secretos.

**52. ¿Qué analiza Snyk?**
Dependencias vulnerables de npm y Gradle (CVE conocidas), Snyk Code sobre el código propio e IaC de Terraform
(`snyk.yml`).

**53. ¿Cuántos hallazgos tienen ahora?**
No lo decimos de memoria: lo mostramos en la pestaña Security y en SonarCloud con los números del día. Lo
crítico se corrige antes de fusionar.

**54. ¿Qué hacen con un falso positivo?**
Se revisa, se documenta la razón y se marca en la herramienta (o en `.semgrepignore`), nunca se desactiva una
regla entera sin justificar.

**55. ¿Cuándo corren estos análisis?**
En cada push y pull request a `main`; Semgrep y Snyk también corren de forma programada para detectar nuevas CVE.

## 7. Terraform: reporte de tests y de costos (1 punto)

**56. ¿Qué crea Terraform?**
La base PostgreSQL en Neon y en Render la API (web service) y el dashboard (sitio estático). Estado en HCP
Terraform. Módulos `database-neon` y `app-render`.

**57. ¿Cómo prueban Terraform?**
`terraform test` con proveedores simulados (no crea nada real), más fmt, validate, tflint y checkov en
`infra.yml`.

**58. ¿Cómo calcularon los costos?**
Producción real: USD 0 (capas gratuitas). Referencia de pago en AWS y Azure (≈ USD 38/mes) con Infracost; el
reporte queda en el *Job summary* y el artefacto `reporte-costos`.

**59. ¿Qué límites tiene la capa gratuita?**
0,5 GB de base de datos y la API se duerme tras 15 min sin tráfico (primer arranque ≈ 1 min).

**60. ¿Cómo migrarían a AWS o Azure?**
`envs/prod` solo depende del contrato de salidas de los módulos; se cambia el módulo por uno de AWS o Azure
(`infra/README.md §7`).

## 8. Release, despliegue y contribuciones (2 puntos)

**61. ¿Cómo se publica una versión?**
Commits convencionales → release-please crea versión y CHANGELOG → `release.yml` construye APK, API, dashboard y
paquete MCP → GitHub Releases → Liquibase en producción → deploy hooks de Render → smoke test.

**62. ¿Qué pasa si el smoke test falla?**
El workflow queda en rojo y nos enteramos. Para volver atrás se redepliega el release anterior y, si hubo cambio de
esquema, se usa el rollback de Liquibase.

**63. ¿Cómo se ve la contribución de cada integrante?**
En el historial de commits y en *Insights → Contributors* de GitHub; los commits convencionales dicen qué hizo
cada uno.

## 9. README y GitHub Pages (2 puntos)

**64. ¿El diccionario de datos lo escribieron a mano?**
No: `docs.yml` lo genera desde la base real, junto con los diagramas Mermaid (ER, clases, componentes,
despliegue), y actualiza el README.

**65. ¿Qué hay en GitHub Pages?**
Dokka (cada clase, método y propiedad de Kotlin), TypeDoc (API y dashboard), diccionario, diagramas, reportes
de pruebas y mutación, esta presentación y los PDF (`pages.yml`).

## 10. Dashboard de utilización (2 puntos)

**66. ¿De dónde salen los datos del dashboard?**
De la telemetría de la app (`evento`), `sesion` y `actividad`, a través de las vistas `mv_uso_diario`,
`v_uso_por_funcion`, `v_uso_por_pantalla`, `v_origen_actividades` y `v_resumen_sesiones`.

**67. ¿Los números de la captura son reales?**
No, son datos de demostración. El piloto con estudiantes todavía no se hizo.

**68. ¿Quién puede ver el dashboard?**
Solo el rol admin; un estudiante recibe acceso denegado (escenario de la HU-05).

**69. ¿Es accesible?**
Cada gráfico tiene vista de tabla, contraste AA y funciona en móvil, claro y oscuro.

## 11. Reto: integraciones

**70. ¿Cuáles son las integraciones?**
Firebase Authentication, Gemini (Firebase AI Logic), Google Calendar, Moodle UPT y el servidor MCP con la skill
`tareia` y el agente `coach-academico` de Claude Code. Pedían 3; hicimos 5.

**71. ¿Qué hace el servidor MCP?**
Expone 7 herramientas sobre la API: `estado_api`, `actividades_pendientes`, `registrar_actividad`,
`cambiar_estado_actividad`, `historial_actividad`, `plan_semanal` y `resumen_dashboard`. Tiene 14 pruebas.

**72. ¿El agente puede saltarse la validación?**
No. Lo que registra nace PENDIENTE_VALIDAR (RN09) y la API responde 409 si intenta sincronizar sin validar
(RN01). Su sesión queda con origen `mcp`.

**73. ¿Cómo se conectan con Moodle?**
Con los Web Services de Moodle del aula virtual de la UPT; las tareas importadas nacen pendientes de validar.

**74. ¿Qué pasa si Gemini falla o cambia de precio?**
Strategy usa la planificación local; Abstract Factory permite cambiar a otro proveedor compatible sin tocar la
app.

## 12. Presentación y cierre (2 puntos)

**75. ¿Qué harían distinto?**
Empezar antes el piloto con estudiantes para medir los objetivos de uso, y escribir las pruebas de mutación del
dominio desde el inicio.

**76. ¿Cuál fue la mayor dificultad?**
Mantener la misma regla de negocio en tres lugares (app, API y BD) sin que se contradigan; lo resolvimos con
pruebas en cada capa y escenarios de aceptación que cruzan todo.

---

## Preguntas para lanzar al público (si sobra tiempo)

1. ¿Ustedes cómo anotan hoy las fechas de entrega?
2. ¿Confiarían en que una IA mande eventos a su calendario sin preguntarles?
3. ¿Qué preferirían: menos pruebas y más funciones, o al revés?
4. Si tuvieran que elegir una sola herramienta de análisis estático, ¿cuál y por qué?
