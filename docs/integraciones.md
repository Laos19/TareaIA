# Integraciones de TARE·IA (reto del curso)

El reto pide integrar la aplicación con **tres aplicaciones externas** o crear un **plugin con skills y agente**.
TARE·IA cumple ambas opciones.

## 1. Aplicaciones externas integradas en la app Android

| # | Aplicación | Para qué se usa | Dónde está en el código | Cómo se prueba |
|---|---|---|---|---|
| 1 | **Google (Firebase Authentication)** | Inicio de sesión con la cuenta institucional de Google (Credential Manager). La API verifica el mismo ID token y abre una sesión. | `android/.../GoogleAuthManager.kt`, `backend/src/auth/VerificadorGoogle.ts` | Integración: `backend/test/integracion/api.test.ts` (verificador sustituido) · aceptación HU-01 |
| 2 | **Gemini (Firebase AI Logic)** | Extrae tareas y fechas de apuntes, PDF y capturas; propone planes de estudio (patrón Abstract Factory, proveedor A). | `android/.../ia/FabricaProveedorA.kt`, `ia/ComunIA.kt` | Interfaz Compose `ImportarFlowTest.kt` · aceptación HU-03 |
| 3 | **Google Calendar (API REST)** | Envía las actividades validadas como eventos (RN01: nunca sin validar). | `android/.../GoogleCalendarApi.kt`, `aplicacion/GestorSincronizacion.kt` | Unitarias `GoogleCalendarIdTest.kt` · trigger de estado en la BD |
| 4 | **Moodle — Aula Virtual UPT (Web Services)** | Importa cursos y tareas con fecha; llegan *pendientes de validar* y sin duplicados. | `android/.../MoodleClient.kt`, `GestorActividades.importarDeIntegracion` | Aceptación HU-04 (`idExterno` moodle) |
| 5 | **ML Kit (Google)** | OCR de imágenes y PDF en el propio teléfono. | `android/.../ia/ReconocedorOCRMLKit.kt` | Manual en emulador |

## 2. Plugin para Claude Code: servidor MCP + skill + agente

| Pieza | Ubicación | Qué hace |
|---|---|---|
| **Servidor MCP** `tareia` | `integrations/mcp/` (registrado en `.mcp.json`) | 7 herramientas sobre la API: pendientes, registrar, cambiar estado, historial, plan semanal, dashboard, estado. 14 pruebas automáticas. |
| **Skill** `tareia` | `.claude/skills/tareia/` | Instrucciones y script `tareia.sh` (curl) para consultar la API cuando el MCP no está conectado. |
| **Agente** `coach-academico` | `.claude/agents/coach-academico.md` | Coach de organización: diagnostica pendientes, guía la validación de lo que propuso la IA y arma el plan semanal. |

Cada uso desde Claude Code abre una sesión con origen `mcp` o `claude` y se registra como telemetría, así que
aparece en el **dashboard de utilización** junto al uso desde la app.

## 3. Servicios de plataforma

GitHub (Actions, Releases, Pages, Code Scanning), SonarQube Cloud, Semgrep, Snyk, Neon, Render y HCP Terraform
(ver [automatizaciones.md](automatizaciones.md)).
