# Plan de adopción móvil — TARE·IA (Kotlin + Jetpack Compose)

**Estado**: plan de trabajo, no implementación concluida. **Fuente**: `PLAN_ADOPCION_SKILLS.md` y `AUDITORIA_RF01_RF27.md` recibidos. **Alcance vigente**: RF-01 a RF-26 descritos por el usuario. La versión anterior auditaba también RF-27, que queda fuera del alcance vigente hasta confirmación formal.

## 0. Inventario y línea base (sin tocar `app/src`)
- [x] Localizar app Kotlin/Compose, Gradle Wrapper, versiones y 37 skills heredadas; preservar fuentes originales.
- [x] Incorporar los dos documentos originales al proyecto.
- [x] Crear skills nativas Android y archivar skills web fuera de `.agents/skills/`.
- [ ] Ejecutar `gradlew.bat :app:assembleDebug`, `:app:lintDebug` y `:app:testDebugUnitTest` con SDK 35/JDK 17; archivar salida real.
- [ ] Probar la app en emulador y un teléfono API 28+; registrar resultados sin inferir que `BUILD SUCCESSFUL` equivale a RF aprobado.

## 1. Experiencia móvil y arquitectura de pantallas
Skills: `android-compose-ux`, `android-compose-design-system`, `android-compose-state-navigation`, `android-compose-forms-validation`.
- Inventariar pantallas **reales** Compose y sus rutas: acceso, inicio, importación, validar, actividades, cursos, horarios, notas, sin clasificar, papelera, perfil, reportes, integraciones.
- Priorizar acceso (RF-01), importación → extracción → revisión (RF-03/04/05/22), alta manual (RF-06), planificación (RF-07/17/18/24) y calendario (RF-02/08/09/11/19).
- Formular cada historia con estados: vacío, cargando, éxito, error, sin conexión, sin permiso, cancelar, restaurar y reintentar.
- Diseñar con **Compose Material 3**, semánticas Android y espaciado de dispositivo; nunca código React, GSAP ni DOM.
- «Tablero periódico» está citado en plan fuente como dirección de arte; faltan mockups originales para tratarlo como diseño cerrado.

## 2. Accesibilidad, medios y reportes
Skills: `android-compose-accessibility`, `android-native-intents-permissions`, `android-compose-dashboards-reports`.
- Probar TalkBack y fuente 1.3x/2x, IME, foco y rotación; no reemplazar estas pruebas por Lighthouse web.
- RF-12/15/22: validar picker PDF/imágenes y Android Sharesheet, sin pedir permisos de almacenamiento innecesarios.
- RF-10/16/20/21: filtros por curso, clasificación, papelera y recuperación.
- RF-25: vista previa y exportación PDF por selector del sistema con datos filtrados.

## 3. Flujos sensibles y decisiones explícitas
Skill: `android-compose-calendar-planning`.
- RF-02/08: definir si Google Calendar se usará mediante Calendar Provider del teléfono (actual) o Google Calendar API/OAuth directo; mostrar cuenta/destino.
- RF-05/07/11/19: confirmar siempre antes de crear/reprogramar sesiones y eventos. Opción Sobrescribir/Cancelar frente a duplicados.
- RF-09: configurar aviso por actividad y reprogramar al editar. Manejar permisos denegados.
- RF-03/04/07/24: definir proveedor IA, modalidad multimodal/voz y consentimiento para enviar contenido. No declarar IA multimodal completa si el código solo envía texto OCR.
- RF-26 Moodle: probar con instancia y token autorizados, sin hardcodear credenciales, con documentos de prueba.

## 4. Verificación por requisitos y seguridad
Skill: `android-compose-performance-testing` y generales `systematic-debugging` / `verification-before-completion`.
- Crear evidencia de test reproducible por RF: caso, pasos, datos, esperado, obtenido, captura, fecha y entorno.
- Asegurar que acciones de IA, sincronización y borrado definitivo requieren los consentimientos correspondientes.
- Validar privacidad, almacenamiento seguro de credenciales, no duplicados, recuperación de errores y modo sin conexión limitado.
- No certificar RF-01 a RF-26 desde código estático solamente. Actualizar matriz conforme pruebas.

## 5. Entregables opcionales del plan original, separados del frontend Android
- Demo web, React, GSAP, SEO, Core Web Vitals, AccessLint DOM y Terraform NO intervienen en la compilación de la app Android. Ejecutarlos solo para una demo web o infraestructura que exista de verdad y se solicite.
- DOCX/PPTX/reveal.js pueden emplearse para documentos y presentación una vez estén los archivos fuente disponibles.
- Snyk y revisión de PR solo con herramientas/cuentas instaladas y permisos válidos.

## Política de cambios
**Esta entrega no modifica frontend/backend ni agrega RF nuevos funcionales**; instala rutas de trabajo, documentación y skills. Toda modificación de código funcional posterior requiere tarea explícita, diff y pruebas.

## Insumos aún necesarios para una solución completa
- Elección de autenticación: PIN local actual o Google Sign-In, según la Visión y RF-01.
- Decisión de Google Calendar Provider frente a API OAuth; acceso autorizado a cuenta de prueba.
- Proveedor IA multimodal/voz, modelo, endpoint y política de privacidad/costos (credenciales a configurar localmente, NO en el ZIP).
- URL y token de un Moodle de pruebas permitido; detalle de funciones habilitadas.
- Mockups/paleta del «Tablero periódico» o libertad para proponer diseño.
- Datos de prueba anónimos (PDF, imágenes, audio, horario) y prioridades de RF.

## Referencias técnicas Android
- https://developer.android.com/develop/ui/compose/state-hoisting
- https://developer.android.com/develop/ui/compose/accessibility/semantics
- https://developer.android.com/training/data-storage/shared/photopicker
- https://developer.android.com/identity/providers/calendar-provider
- https://developer.android.com/develop/ui/compose/testing
