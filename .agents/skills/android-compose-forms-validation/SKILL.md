---
name: android-compose-forms-validation
description: Crear formularios accesibles de tareas, cursos, horarios, prioridades y confirmación de IA en Compose.
---

# android-compose-forms-validation

> Guía local para agentes de programación en TARE·IA. Es una skill creada específicamente para Android nativo; no instala por sí misma un complemento de Android Studio.


## Casos
RF-05, RF-06, RF-07, RF-11, RF-14, RF-17, RF-18, RF-19 y RF-23.

## Procedimiento
- Especificar para cada campo: obligatorio/opcional, formato, rangos, reglas, ejemplo, mensaje de error y restauración.
- Usar controles nativos Compose (`OutlinedTextField`, `ExposedDropdownMenuBox` si lo soporta la versión, selectores de fecha/hora adecuados) con label visible y errores junto al campo.
- Revisar formato de fecha, zona horaria, hora local, límites del semestre y colisión de intervalos. No asumir que una fecha extraída por OCR es correcta.
- Separar `Validar` de `Confirmar` y de `Sincronizar`; nunca guardar la propuesta de IA automáticamente antes de aprobación del usuario.
- Para RF-11, mostrar datos del evento existente y opciones **Sobrescribir / Cancelar** sin perder vínculo de evento y recordatorios.
- Probar teclado, foco, scroll hasta el error, botones de cancelar y validaciones fallidas.

## Salida
Tabla de campos, estados visuales, pruebas de validación y rutas de cancelación por RF.


## Regla de cierre
No declarar un requisito completo sin una prueba específica. Si no hay Android SDK/emulador o credenciales autorizadas, registrar «pendiente de verificación», nunca «probado».
