---
name: android-compose-accessibility
description: Revisar accesibilidad Android nativa: TalkBack, semantics Compose, fuente escalada, foco y controles táctiles.
---

# android-compose-accessibility

> Guía local para agentes de programación en TARE·IA. Es una skill creada específicamente para Android nativo; no instala por sí misma un complemento de Android Studio.


## Cuándo usar
Cada cambio de UI de TARE·IA, incluyendo dashboard, listas, formularios, calendarios, PDF e importación.

## Auditoría nativa
- Usar `Modifier.semantics`, roles, `contentDescription` de iconos interactivos y `stateDescription` en estados personalizados.
- Los encabezados importantes deben ser navegables como encabezados y los cambios de estado relevantes comunicarse sin anuncios excesivos.
- Revisar orden de foco de TalkBack, navegación por teclado, toque objetivo adecuado y contrastes.
- Probar fuentes aumentadas, modo oscuro, contenido en orientación horizontal, IME visible y elementos con texto largo.
- Los estados no deben expresarse solo con color; adjuntar etiquetas como «Pendiente de validar».
- Comparar manualmente la experiencia en dispositivo/emulador con las pruebas automáticas disponibles.

## No usar como sustitutos
`axe-core`, `AccessLint` web, Lighthouse o auditorías DOM no verifican semánticas Android nativas.

## Referencia
https://developer.android.com/develop/ui/compose/accessibility/semantics


## Regla de cierre
No declarar un requisito completo sin una prueba específica. Si no hay Android SDK/emulador o credenciales autorizadas, registrar «pendiente de verificación», nunca «probado».
