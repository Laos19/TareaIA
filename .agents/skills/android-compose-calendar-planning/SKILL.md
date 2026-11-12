---
name: android-compose-calendar-planning
description: Diseñar UI segura para planificación, validación y calendario, evitando eventos y recordatorios duplicados.
---

# android-compose-calendar-planning

> Guía local para agentes de programación en TARE·IA. Es una skill creada específicamente para Android nativo; no instala por sí misma un complemento de Android Studio.


## Alcance
RF-02, RF-05, RF-07, RF-08, RF-09, RF-11, RF-17, RF-18 y RF-19.

## Secuencia mínima
1. El usuario introduce o importa una actividad y visualiza título, curso, fecha y confianza/origen de extracción cuando exista.
2. La app presenta propuesta de sesiones con conflictos horarios, prioridades y disponibilidad; se puede modificar o cancelar.
3. Validación explícita antes de crear/actualizar eventos; comprobación de duplicados con opción **Sobrescribir / Cancelar**.
4. Mostrar calendario de destino, permiso y estado (local / pendiente / sincronizado / error) sin afirmar que el evento ya está en la nube por haberlo insertado localmente.
5. Si cambia la fecha, mostrar comparación «antes/después» y solicitar confirmación para actualizar calendario, recordatorios y sesiones vinculadas.
6. Permitir ajustar/desactivar recordatorios por actividad, y explicar si se requiere permiso de notificaciones.

## Evidencias de aceptación
Probar repetición de importación, revocación de calendario, denegación, conflicto, cancelación y edición; contar eventos reales antes/después en la cuenta elegida cuando haya acceso autorizado.

## Referencia
https://developer.android.com/identity/providers/calendar-provider


## Regla de cierre
No declarar un requisito completo sin una prueba específica. Si no hay Android SDK/emulador o credenciales autorizadas, registrar «pendiente de verificación», nunca «probado».
