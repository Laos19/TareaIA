# Skills nativas para TARE·IA (Android / Kotlin / Compose)

Esta entrega contiene el proyecto fuente original **sin modificaciones de `app/src/main`** y adapta el material de planificación y las guías de interfaz de la distribución previa a **Android nativo**, no a React o CSS.

## Qué son las skills
Archivos `SKILL.md` con instrucciones para asistentes compatibles (como Codex); **no son dependencias de Gradle, plugins que Android Studio cargue automáticamente ni nuevas pantallas implementadas**. Para que un agente las utilice, debe abrir `TAREIA_v4` como raíz y leer `AGENTS.md`.

## Skills móviles activas
| Skill | Enfoque Android | RF asociados |
|---|---|---|
| android-compose-ux | Pantallas, estados, diseño responsivo Android | 05–07, 10, 13–25 |
| android-compose-design-system | Material 3, tokens Kotlin, componentes Compose | Transversal |
| android-compose-state-navigation | Navegación, estado UI, callbacks seguros | 05, 07, 10, 19 |
| android-compose-forms-validation | Validaciones, IME, fechas, confirmación | 05–07, 11, 14, 17–19 |
| android-compose-accessibility | TalkBack, semantics, touch target y font scale | Transversal |
| android-native-intents-permissions | ContentResolver, picker, SAF, cámara, voz | 03, 12, 15, 22, 24, 25 |
| android-compose-calendar-planning | Plan IA, conflictos, duplicados, calendario | 02, 05, 07–09, 11, 17–19 |
| android-compose-dashboards-reports | Seguimiento, clasificados, papelera, reporte | 10, 16, 20, 21, 25 |
| android-compose-performance-testing | Compose testing, Gradle, emulador, rendimiento | Transversal |

Además se mantienen activas las skills genéricas `planning-with-files`, `writing-plans`, `systematic-debugging` y `verification-before-completion`. Las otras 33 skills preexistentes están **archivadas sin modificar** en `docs/skills_originales_no_android/` para evitar que una guía de React o Web Vitals se aplique por error a la app.

## Documentos integrados
- `PLAN_ADOPCION_SKILLS.md`: plan original del usuario, intacto.
- `AUDITORIA_RF01_RF27.md`: auditoría original, intacta (cobertura estática/histórica).
- `PLAN_ADOPCION_ANDROID_COMPOSE.md`: plan adaptado al desarrollo móvil.
- `MATRIZ_RF01_RF26.md`: alcance vigente y estados declarados/pendientes.
- `docs/referencias/FD02_DOCUMENTO_VISION.md`: Visión histórica como contexto de producto.

## Antes de ejecutar
Abrir `TAREIA_v4`, elegir Gradle JDK 17, instalar SDK Platform 35, sincronizar y probar `./gradlew.bat :app:assembleDebug` en Windows. Se excluyeron caches, builds antiguos, archivos `.idea` y `local.properties` específicos de una computadora. Android Studio regenerará lo necesario; **no se garantiza una compilación realizada aquí**.
