---
name: android-compose-state-navigation
description: Modelar estado y navegación Compose sin mezclar lógica de negocio, red ni persistencia dentro de composables.
---

# android-compose-state-navigation

> Guía local para agentes de programación en TARE·IA. Es una skill creada específicamente para Android nativo; no instala por sí misma un complemento de Android Studio.


## Cuándo usar
Un flujo debe conservar borradores, filtrar actividades, cambiar pantallas o manejar respuestas asíncronas (RF-05, RF-07, RF-10, RF-19).

## Reglas
1. Conservar una sola fuente de verdad para el estado de pantalla; usar `remember` para estado transitorio y `rememberSaveable` para UI que deba sobrevivir a recreación, cuando el valor sea guardable.
2. Elevar estado al ancestro común mínimo (`state hoisting`); separar `UiState` inmutable y eventos (`onConfirm`, `onCancel`, `onRetry`) de efectos secundarios.
3. Mantener flujos de permisos, lanzamiento de pickers y navegación fuera de composición pura; no crear ni duplicar eventos por recomposición.
4. Si se añade arquitectura, introducir `ViewModel`/Flujos solo con dependencia y pruebas justificadas; no reescribir todo `MainActivity.kt` sin autorización.
5. Mantener navegación atrás coherente, avisar de datos no guardados y preservar estado cuando rote la pantalla.

## Caso TARE·IA
Importar → resultado OCR → validar/editar → resolver duplicado → confirmar → elegir calendario → indicar resultado. Ninguna pantalla debe asumir que el paso anterior ya ocurrió.

## Referencia
https://developer.android.com/develop/ui/compose/state-hoisting


## Regla de cierre
No declarar un requisito completo sin una prueba específica. Si no hay Android SDK/emulador o credenciales autorizadas, registrar «pendiente de verificación», nunca «probado».
