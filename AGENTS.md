# Instrucciones del repositorio para asistentes de programación — TARE·IA

## Stack y alcance
Android nativo, Kotlin 2.3.21, Jetpack Compose con Material 3, Gradle Wrapper 8.13, AGP 8.13.2, JDK 17 o 21, `compileSdk=35`, `minSdk=28`. **No es una aplicación React, Next.js ni una webview.**

## Orden de lectura obligatorio
1. `MATRIZ_RF01_RF26.md` — alcance vigente RF-01 a RF-26 (el RF-27 aparece solo en documentación histórica).
2. `PLAN_ADOPCION_ANDROID_COMPOSE.md` — ruta de trabajo móvil y adaptación de las dos referencias originales.
3. `PLAN_ADOPCION_SKILLS.md` y `AUDITORIA_RF01_RF27.md` — originales suministrados y conservados.
4. `README_SKILLS_ANDROID.md` y `.agents/skills/android-*/SKILL.md` — instrucciones operativas para frontend nativo.
5. `README_IMPLEMENTACION.md` — declaración de implementación previa; **no equivale a tests aprobados**.
6. `docs/MARCA.md` — identidad visual «Tablero periódico» y tokens de `ui/theme`; toda UI nueva usa `TareiaTheme`, no colores literales.

## Estructura de la UI (`app/src/main/java/com/tareia/app/`)
- `MainActivity.kt` — solo la Activity: splash, intents compartidos y `setContent`.
- `ui/TareiaApp.kt` — shell: estado de la app, reglas de negocio por destino, barra superior/inferior y botón Atrás.
- `ui/navigation/Destino.kt` — enum con todos los destinos (etiqueta, ícono, si va en la barra inferior). Para añadir una pantalla: nuevo valor aquí + rama en el `when` del shell.
- `ui/screens/` — una pantalla por archivo (`*Screen.kt`), sin acceso directo a `AppStore`: reciben datos y callbacks.
- `ui/components/` — piezas reutilizables (`PageHeading`, `SectionTitle`, `SurfaceCard`, `AppButton`, `StatusBadge`, `TaskCard`, `CeldaMarca`).
- `ui/theme/` — tokens de marca (ver `docs/MARCA.md`). No usar colores literales en pantallas: `MaterialTheme.colorScheme.*` o `TareiaTheme.colores.*`.
- Lógica de dominio y datos (`AppStore`, `TaskPlanner`, `CalendarRepository`, etc.) sigue en el paquete raíz.
- Pruebas: `app/src/test` (JVM, incluye `ContrasteTest`: si cambias un color de la paleta y rompe WCAG AA, falla) y `app/src/androidTest` (Compose UI, semántica y áreas táctiles). Auditoría: `docs/AUDITORIA_UI.md`.

## Reglas de modificación
- Si el pedido es de documentación, adopción de skills o compilación, no modificar archivos `app/src/main/**` (frontend, backend ni recursos) sin autorización expresa.
- No implementar funciones de red o integrar credenciales sin consentimiento y configuración segura. No añadir keys/tokens reales al repositorio.
- Distinguir UI propuesta vs funcionalidad implementada vs funcionalidad comprobada.
- No alterar versiones de Gradle/Kotlin/Compose sin motivo verificado.
- Para cambios en app: documentar RF, archivos tocados, prueba y posibles regresiones. Preservar confirmación humana previa a evento externo y control de duplicados.

## Skills prioritarias
`android-compose-ux`, `android-compose-design-system`, `android-compose-state-navigation`, `android-compose-forms-validation`, `android-compose-accessibility`, `android-native-intents-permissions`, `android-compose-calendar-planning`, `android-compose-dashboards-reports`, `android-compose-performance-testing`.

## Skills generales disponibles
`planning-with-files`, `writing-plans`, `systematic-debugging`, `verification-before-completion`.
Las skills antiguas para web/React/GSAP/SEO están archivadas en `docs/skills_originales_no_android/` y no se deben ejecutar sobre el frontend Android.

## Condición de cierre
Verificación en Android Studio/emulador; `BUILD SUCCESSFUL` obtenido en la máquina correcta, captura o pruebas trazables por RF. Documentar dependencias de IA, Google Calendar y Moodle que permanezcan pendientes.
