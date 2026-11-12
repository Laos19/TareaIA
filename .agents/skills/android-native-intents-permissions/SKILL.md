---
name: android-native-intents-permissions
description: Diseñar la interacción móvil con archivos, OCR, capturas, compartir, voz y permisos de Android.
---

# android-native-intents-permissions

> Guía local para agentes de programación en TARE·IA. Es una skill creada específicamente para Android nativo; no instala por sí misma un complemento de Android Studio.


## Alcance
RF-03, RF-12, RF-15, RF-20, RF-22, RF-24 y RF-25.

## Reglas
1. Preferir selectores del sistema (`ActivityResultContracts.OpenDocument`, Photo Picker) y URIs de `ContentResolver` antes que pedir acceso general a almacenamiento.
2. Tratar URIs como referencias, no rutas de archivo; controlar cancelación, MIME, archivo ilegible, permisos temporales y tamaño.
3. Compartir desde otras apps mediante intents Android, no arrastrar/soltar HTML; validar `ACTION_SEND`, tipo de contenido y origen.
4. Pedir permisos de micrófono, cámara, notificaciones y calendario solo cuando realmente se necesiten; algunas rutas de selector del sistema no requieren permiso de almacenamiento.
5. Para audio/imagen/PDF, mostrar consentimiento antes de enviar información académica a servicios externos; manejar sin conexión, reintento y errores.
6. No imprimir tokens de Moodle, claves de IA ni archivos sensibles en logs.

## Pruebas
PDF válido/dañado, foto, imagen compartida, micrófono denegado, picker cancelado, archivo sin permiso, app en segundo plano y sin conexión.

## Referencias
https://developer.android.com/training/data-storage/shared/photopicker
https://developer.android.com/training/sharing/receive


## Regla de cierre
No declarar un requisito completo sin una prueba específica. Si no hay Android SDK/emulador o credenciales autorizadas, registrar «pendiente de verificación», nunca «probado».
