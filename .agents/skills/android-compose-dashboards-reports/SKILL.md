---
name: android-compose-dashboards-reports
description: Crear tableros académicos Android y una vista previa de reportes PDF sin patrones web.
---

# android-compose-dashboards-reports

> Guía local para agentes de programación en TARE·IA. Es una skill creada específicamente para Android nativo; no instala por sí misma un complemento de Android Studio.


## Alcance
RF-10, RF-13, RF-14, RF-16, RF-20, RF-21 y RF-25.

## Orientación
- Dashboard móvil de un vistazo: próximas tareas, pendientes de validación, alertas, filtros por curso y estado.
- Usar listas virtualizadas (`LazyColumn`) cuando haya muchas actividades; conservar filtros entre navegación y cambios de configuración.
- Distinguir sin clasificar, papelera y contenido activo. Evitar acciones destructivas por toques accidentales; confirmación para borrado definitivo.
- Vista del reporte antes de exportar: rango de fechas, cursos y estados incluidos, fecha del informe y opción «Guardar/Compartir» por intents/SAF.
- No simular un PDF con HTML/CSS: el generador actual es Kotlin (`PdfReport.kt`). Los cambios UI deben seguir el modelo de datos real.

## Pruebas
Reporte vacío, tareas con fechas límite, filtros combinados, exportación cancelada, contraste, fuente grande y apertura del PDF guardado.


## Regla de cierre
No declarar un requisito completo sin una prueba específica. Si no hay Android SDK/emulador o credenciales autorizadas, registrar «pendiente de verificación», nunca «probado».
