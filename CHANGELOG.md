# Changelog

## [2.1.0](https://github.com/Laos19/TareaIA/compare/v2.0.0...v2.1.0) (2026-11-26)


### ✨ Funcionalidades

* **android:** conexión con la API (sesión, respaldo, telemetría y logs) y pruebas del dominio ([3532f0d](https://github.com/Laos19/TareaIA/commit/3532f0dbf84d9e79f6f8db91cfc43e499edf214f))
* **android:** sincronización automática de entregas desde el calendario del aula virtual (Moodle) ([55a165c](https://github.com/Laos19/TareaIA/commit/55a165c001846c3f9fd2e2b4400ab21eb8f7e326))
* **android:** telemetría de análisis IA, planes, Google Calendar y reporte PDF; FD03 y correcciones de cifras ([cd3f814](https://github.com/Laos19/TareaIA/commit/cd3f8147d3b23e24133dac69b1f6f3a5ced9ec83))
* **ui:** fase 0 del rediseño — sistema de diseño «Tablero periódico» ([a9374c9](https://github.com/Laos19/TareaIA/commit/a9374c982e6048572e130ed380cf188b6b1438a8))
* **ui:** fase 2 — componentes firma del «Tablero periódico» ([eaa2143](https://github.com/Laos19/TareaIA/commit/eaa2143af6aeafbb461b250e6df868361be6ef6c))
* **ui:** selector de fecha y lista de cursos en los formularios ([8908a0b](https://github.com/Laos19/TareaIA/commit/8908a0b02e7adfb78fa45cc874a55fd491cd8187))
* versión final de TARE·IA — arquitectura por capas, Gemini, Google Calendar y App Check ([301c656](https://github.com/Laos19/TareaIA/commit/301c65657ea13ee67838b1d8b82731eb2485e628))
* versión inicial de la aplicación Android TareIA (v4) ([fb681bc](https://github.com/Laos19/TareaIA/commit/fb681bcff27833b540026f5b69626aef031fc8c2))


### 🐛 Correcciones

* corregir errores de compilación en MainActivity y eliminar puente de Gradle ([ca1de5a](https://github.com/Laos19/TareaIA/commit/ca1de5a3a363f6ce32afdd164b85d0e802c0eba1))
* **seguridad:** resolver hallazgos ERROR de Semgrep ([a9e9dc4](https://github.com/Laos19/TareaIA/commit/a9e9dc4d1a0d76369c8c4a489c29c821da6f909c))


### 📚 Documentación

* avance de documentos FD y presentación ([e6d3a1a](https://github.com/Laos19/TareaIA/commit/e6d3a1a6d0fa7b7afbcdb724adb0368daa07ddaa))
* avance de FD03, FD04 y presentación ([4bc34e9](https://github.com/Laos19/TareaIA/commit/4bc34e9f59f82fed0b626f5f631d2cd594e09541))
* documento de entrega por criterio de la rúbrica, licencia y plantillas de GitHub ([6e0c17b](https://github.com/Laos19/TareaIA/commit/6e0c17b7bc2f80ed303c34825f69c3d886b3a8f5))
* presentación final, FD01–FD04, investigación, entrega y diccionario en Word y PDF ([ecf4d51](https://github.com/Laos19/TareaIA/commit/ecf4d516a874ba1aab498fcece774aefdea5e200))
* **presentacion:** diapositiva 15 con la sincronización del aula virtual y guion repartido entre expositores ([8eb7512](https://github.com/Laos19/TareaIA/commit/8eb75125593ac8ad1eedea6dec3a6ddd279818ba))
* problema y objetivos, FD01 Factibilidad y FD02 Visión ([7d317fa](https://github.com/Laos19/TareaIA/commit/7d317fab6f46f6a01538412e225db5288ed2d667))


### ♻️ Refactorización

* estructura por capas (android/, backend/, db/, dashboard/, acceptance/) ([41c7ef7](https://github.com/Laos19/TareaIA/commit/41c7ef75ef6e1a959a5eeb8a4189559902f49452))
* **ui:** fase 1 — dividir MainActivity en pantallas, componentes y navegación tipada ([973dc43](https://github.com/Laos19/TareaIA/commit/973dc433ae26ede5c4b849e51978c7b01d8f8c18))


### 🧪 Pruebas

* **android:** corregir imports de ImportarFlowTest (assertExists es método de SemanticsNodeInteraction) ([16986c9](https://github.com/Laos19/TareaIA/commit/16986c9c600bc18be6e765a0b5f7ef6f2115b7d1))
* **ui:** fase 3 — pruebas automáticas, auditoría de accesibilidad y heurística ([9a30940](https://github.com/Laos19/TareaIA/commit/9a309407f5d5937efd771ccaf18e0d42f417bde5))


### 🔁 Integración continua

* automatizaciones de calidad, infraestructura, release y documentación; integración MCP ([0192c37](https://github.com/Laos19/TareaIA/commit/0192c375e01f95e0b03cd04f1d2b061c20ed930e))
* corregir prueba de Terraform del módulo de base de datos y mostrar hallazgos de Semgrep en el log ([30a0bf7](https://github.com/Laos19/TareaIA/commit/30a0bf7718d8ec960e1afe10b61fe3ed9f45f3b6))
