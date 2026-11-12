---
name: android-compose-performance-testing
description: Probar UI Compose, rendimiento, compilación y flujos RF en emulador Android real.
---

# android-compose-performance-testing

> Guía local para agentes de programación en TARE·IA. Es una skill creada específicamente para Android nativo; no instala por sí misma un complemento de Android Studio.


## Línea base reproducible
En Windows desde `TAREIA_v4` y con JDK 17/SDK 35:
```powershell
.\gradlew.bat --version
.\gradlew.bat :app:assembleDebug --stacktrace
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:testDebugUnitTest
```
Registrar comando, fecha, entorno, error inicial y captura. **`NO-SOURCE` no significa que existan pruebas aprobadas**. Para UI tests instrumentados agregar dependencias de test compatibles solo cuando el cambio esté autorizado, y ejecutar en emulador.

## Rendimiento Compose
- Evitar operaciones OCR, PDF, red o parsing pesado en el hilo principal.
- Revisar recomposiciones innecesarias, `remember`/`derivedStateOf` cuando correspondan, claves estables en listas, paginación si fuera necesaria y estados de carga.
- Medir arranque, scrolling y flujos críticos en dispositivo. Lighthouse y métricas LCP/CLS/INP no son métricas de Android nativo.
- Mantener soporte de minSdk 28, y reproducir en pantallas compactas con font scale aumentado.

## Pruebas mínimas de interfaz
Autenticación local, importación y OCR con documento de prueba, validación y cancelación, creación manual, cambio de curso, generación/confirmación plan, duplicados, permisos y reporte PDF.

## Referencias
https://developer.android.com/develop/ui/compose/testing
https://developer.android.com/develop/ui/compose/performance


## Regla de cierre
No declarar un requisito completo sin una prueba específica. Si no hay Android SDK/emulador o credenciales autorizadas, registrar «pendiente de verificación», nunca «probado».
