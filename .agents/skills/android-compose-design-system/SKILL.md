---
name: android-compose-design-system
description: Definir y reutilizar tokens, componentes y patrones visuales nativos en Jetpack Compose Material 3.
---

# android-compose-design-system

> Guía local para agentes de programación en TARE·IA. Es una skill creada específicamente para Android nativo; no instala por sí misma un complemento de Android Studio.


## Cuándo usar
La app necesita diseño coherente en tarjetas de tarea, chips de curso, badges de prioridad, botones, diálogos, filtros y formularios.

## Reglas
- Respetar el look «Tablero periódico» SOLO como dirección preliminar citada en el plan de origen, no como mockup recuperado o paleta aprobada sin los archivos originales.
- Mapear colores a `ColorScheme` de Material 3, estilos a `Typography`, formas a `Shapes`, y espaciados/tamaños a tokens Kotlin en Compose; no Tailwind ni CSS variables.
- Diferenciar estado pendiente, validado, sincronizado, conflicto y error con texto/iconos además de color.
- Mantener consistencia de componentes en diferentes pantallas, contraste legible y áreas táctiles adecuadas.
- Evitar rediseños masivos cuando el pedido sea corregir compilación; proponer en un diff separado y pedir permiso si se requiere modificar frontend.

## Lista de comprobación
- [ ] Variantes clara/oscura y tamaños de texto grandes revisados.
- [ ] Vacío, carga, error, éxito, sin conexión y permiso denegado.
- [ ] Iconos descriptivos y etiquetas de acción.
- [ ] Screenshot o prueba visual por componente y por flujo crítico.

## Referencia
https://developer.android.com/develop/ui/compose/designsystems/material3


## Regla de cierre
No declarar un requisito completo sin una prueba específica. Si no hay Android SDK/emulador o credenciales autorizadas, registrar «pendiente de verificación», nunca «probado».
