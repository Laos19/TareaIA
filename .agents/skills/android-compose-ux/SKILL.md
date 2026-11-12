---
name: android-compose-ux
description: Diseñar interfaces Android en Jetpack Compose y Material 3 centradas en los flujos de TARE·IA.
---

# android-compose-ux

> Guía local para agentes de programación en TARE·IA. Es una skill creada específicamente para Android nativo; no instala por sí misma un complemento de Android Studio.


## Cuándo usar
Diseñar o revisar pantallas de Inicio, Actividades, Importar, Validación, Cursos, Horario, Papelera, Perfil, Reportes y Ajustes de TARE·IA (RF-05, RF-06, RF-10, RF-13 a RF-25).

## Flujo de trabajo
1. Leer `MATRIZ_RF01_RF26.md` y `PLAN_ADOPCION_ANDROID_COMPOSE.md`. Determinar RF y flujo; no empezar por modificar pantallas.
2. Identificar entrada, resultado, vacíos, carga, error, cancelación y recuperación. Priorizar importación → vista previa OCR → validación → confirmación → sincronización.
3. Diseñar para teléfonos Android de ancho compacto y pantallas más grandes; manejar teclado IME, scroll, insets, orientación y fuente ampliada. No portar HTML, CSS ni componentes React.
4. Reusar `Scaffold`, `TopAppBar`, `NavigationBar`, `LazyColumn`, `Card`, `SnackbarHost` y Material 3 cuando sea útil y compatible con la versión del proyecto.
5. Marcar acciones que envían datos externos (IA, Calendar, Moodle) con feedback claro; nunca sincronizar ni eliminar sin consentimiento apropiado.
6. Comprobar con capturas de emulador real y distinguir siempre prototipo visual de función comprobada.

## Entrega
Mapa del flujo, estados, pantalla/composable propuesto, RF afectados y evidencia de revisión con pantalla pequeña y escalado de texto.


## Regla de cierre
No declarar un requisito completo sin una prueba específica. Si no hay Android SDK/emulador o credenciales autorizadas, registrar «pendiente de verificación», nunca «probado».
