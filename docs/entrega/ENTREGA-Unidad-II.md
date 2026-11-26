---
title: "TARE·IA — Entrega del Proyecto de Unidad II"
version: "1.0"
fecha: "2026-11-21"
autores: "Adriana Rafaela Laos González (2023077474) y Antony Piero Solorzano Zegarra (2023078696)"
---

# Datos de la entrega

| | |
|---|---|
| Curso | SI-784 Calidad y Pruebas de Software — Docente: Ing. Patrick Cuadros Quiroga |
| Integrantes | Adriana Rafaela Laos González (2023077474) · Antony Piero Solorzano Zegarra (2023078696) |
| Proyecto | **TARE·IA**: asistente académico Android que convierte apuntes y tareas del aula virtual en actividades, planes de estudio y recordatorios |
| Repositorio | https://github.com/Laos19/TareaIA |
| Asignación (Classroom) | https://classroom50.org/UPT-FAING-EPIS/si784-2026-ii/assignments/si784-2026-ii-proyecto/accept |

# Enlaces

| Recurso | Enlace |
|---|---|
| App Android (APK de cada versión) | https://github.com/Laos19/TareaIA/releases |
| Dashboard de utilización | https://tareia-dashboard.onrender.com (demo `admin@demo.tareia.pe` / `tareia2026`) |
| API REST · Swagger | https://tareia-api.onrender.com/api/health · https://tareia-api.onrender.com/api/docs |
| Documentación técnica (GitHub Pages) | https://laos19.github.io/TareaIA/ |
| Ejecuciones y reportes de las automatizaciones | https://github.com/Laos19/TareaIA/actions |

> Los enlaces de Render y Pages quedan activos cuando se configuran los secretos indicados en
> `docs/automatizaciones.md` y se ejecutan `infra.yml`, `release.yml` y `pages.yml` sobre `main`.

# Semana 1

| Indicación | Evidencia |
|---|---|
| Título | **TARE·IA** — `README.md`, `docs/investigacion/problema-y-objetivos.md` |
| Problema detallado con fuentes | `docs/investigacion/problema-y-objetivos.md` (fuentes en APA 7 con URL) y FD01 |
| Objetivos de investigación y de solución medibles | `docs/investigacion/problema-y-objetivos.md` (objetivos SMART con indicador, línea base, meta, instrumento y plazo) |
| FD01 Informe de Factibilidad | `docs/fd/FD01-Informe-Factibilidad.md` · `.docx` · `.pdf` |
| FD02 Documento de Visión | `docs/fd/FD02-Informe-Vision.md` · `.docx` · `.pdf` |
| FD03 Especificación de Requerimientos (SRS) | `docs/fd/FD03-Informe-SRS.md` · `.docx` · `.pdf` |
| FD04 Documento de Arquitectura (SAD) | `docs/fd/FD04-Informe-SAD.md` · `.docx` · `.pdf` |
| Automatización del análisis estático (Sonar, Semgrep y Snyk) | `.github/workflows/sonarqube.yml`, `semgrep.yml`, `snyk.yml` |

# Semanas 2 a 6

| Indicación | Automatización | Reporte | Resultado verificado en local |
|---|---|---|---|
| Pruebas unitarias y de mutación | `ci.yml` (unitarias con cobertura), `android.yml` (JVM), `mutacion.yml` (Stryker y Pitest) | `reporte-ci-pruebas`, `reporte-mutacion-api`, `reporte-mutacion-android` | API: 66 unitarias; app: 35 pruebas JVM nuevas del dominio y la capa remota. Mutación: 93,8 % (Stryker) y 68,4 % (Pitest) |
| Pruebas de integración y de sistema | `ci.yml`, `infra.yml` (contra PostgreSQL 16 migrado con Liquibase y servidor HTTP real) | `reporte-ci-pruebas`, `reporte-pruebas` | 22 de integración y 11 de sistema |
| Pruebas de interfaz de usuario | `infra.yml` (Playwright, escritorio y Pixel 7), `android.yml` (Compose en emulador API 34) | `reporte-pruebas` (playwright-report), `reporte-android-interfaz` | 16 Playwright; 16 pruebas Compose se ejecutan en el emulador de CI |
| Pruebas de aceptación de usuario | `infra.yml` (Cucumber, Gherkin en español) | `reporte-pruebas` (aceptacion.html) | 5 historias de usuario, 18 escenarios, 76 pasos |
| Aspectos no funcionales: sesiones, historial, caché, logs y telemetría | Implementados en la app, la API y la BD; probados en integración, sistema y aceptación | Sección «Requisitos no funcionales» del `README.md` | Ver tabla de evidencias del README |
| Reto: integración con 3 aplicaciones o plugin con skills y agente | Ambas: Firebase Auth (Google), Gemini, Google Calendar, Moodle y ML Kit; servidor MCP + skill + agente de Claude Code | `docs/integraciones.md` | 14 pruebas del servidor MCP y prueba de punta a punta por stdio |

Las pruebas encontraron tres defectos reales que se corrigieron antes de la entrega: el historial fallaba al borrar
un usuario en cascada; el correo no se normalizaba al volver a entrar con Google; y la detección de duplicados en
PostgreSQL no unificaba espacios repetidos (lo encontró un escenario de aceptación).

# Rúbrica: dónde está cada criterio

| Criterio | Puntos | Evidencia |
|---|---:|---|
| Problemática detallada y sustentada · objetivos medibles | 1 | `docs/investigacion/problema-y-objetivos.md` |
| Formatos FD01, FD02, FD03, FD04 | 2 | `docs/fd/` (Markdown, Word y PDF con portada UPT) |
| Código fuente de la aplicación y base de datos en GitHub | 2 | `android/` (app), `backend/`, `dashboard/`, `integrations/mcp/`, `db/changelog/` (11 tablas, 6 vistas, 7 funciones, 7 triggers en 20 changesets de Liquibase) |
| Contribuciones al proyecto | 1 | Historial de commits de ambos integrantes (*Insights → Contributors*) |
| Automatización Terraform + reporte de pruebas superadas + reporte de costos | 1 | `infra.yml`: fmt, validate, tflint, `terraform test`, checkov; pruebas de la aplicación en JUnit; costos de la capa gratuita frente a AWS/Azure (Infracost); plan/apply en Neon y Render |
| Automatización SonarQube: Bugs, Vulnerabilities y Security Hotspots | 2 | `sonarqube.yml` + `sonar-project.properties` |
| Automatización Semgrep y Snyk con reporte de hallazgos | 2 | `semgrep.yml` (SARIF a Code Scanning), `snyk.yml` (dependencias npm y Gradle, código, IaC) |
| Release y despliegue en la infraestructura aprovisionada | 1 | `release.yml`: release-please, APK + API + dashboard + MCP en el Release, Liquibase en producción, deploy hooks de Render, smoke test |
| README: diccionario de datos, ER, clases, componentes y despliegue | 1 | `docs.yml` regenera `README.md`, `docs/diagramas/` y `docs/database/diccionario-de-datos.md` |
| GitHub Pages con detalle de cada clase, método y propiedad | 1 | `pages.yml`: Dokka (app Kotlin) y TypeDoc (API, dashboard y MCP) |
| Presentación del proyecto | 2 | `docs/presentacion/` (HTML, PDF y PPTX con notas del orador, guion de demo y banco de preguntas) |
| Dashboard de utilización del producto | 2 | `dashboard/` sobre las vistas `mv_uso_diario`, `v_uso_por_funcion`, `v_uso_por_pantalla`, `v_origen_actividades` y `v_resumen_sesiones` |

# Pendientes del equipo antes de la exposición

1. Configurar los secretos de GitHub (`docs/automatizaciones.md`) para que SonarQube, Snyk, Terraform y el despliegue se ejecuten y generen sus reportes.
2. Fusionar la rama de trabajo en `main` para que corran `infra.yml`, `release.yml`, `docs.yml` y `pages.yml`.
3. Ejecutar la app en un emulador con Google Play y registrar la huella SHA-1 del equipo en Firebase.
4. Revisar los documentos FD y completar las firmas de revisión y aprobación.
