# Guion de la exposición: quién dice qué

> **TARE·IA** · SI-784 Calidad y Pruebas de Software · UPT · Ing. Patrick Cuadros Quiroga.
> Exponen **Adriana** Rafaela Laos González (2023077474) y **Antony** Piero Solorzano Zegarra (2023078696).
> Unos 20 minutos más preguntas. Cada uno habla unos 10 minutos.
> Reparto: **Adriana** presenta el producto (problema, app y aula virtual). **Antony** presenta la calidad (pruebas, análisis estático, nube y automatización).
> Lo que está entre corchetes es una acción, no se dice. La vista del orador (tecla **S**) muestra la versión larga de cada nota.

| # | Diapositiva | Expone | Tiempo |
|---|---|---|---|
| 1 | Portada | Adriana (y Antony se presenta) | 1:00 |
| 2 | Problema | Adriana | 1:15 |
| 3 | Objetivos SMART | Adriana | 1:15 |
| 4 | La app | Adriana | 1:00 |
| 5 | Arquitectura | Adriana | 1:00 |
| 6 | Patrones | Adriana | 1:15 |
| 7 | Base de datos | **Antony** | 1:00 |
| 8 | Requisitos no funcionales | Antony | 1:15 |
| 9 | Pruebas | Antony | 1:15 |
| 10 | Análisis estático | Antony | 1:00 |
| 11 | Terraform y costos | Antony | 1:00 |
| 12 | Release y despliegue | Antony | 0:50 |
| 13 | Documentación y Pages | Antony | 0:50 |
| 14 | Dashboard de uso | Antony | 1:00 |
| 15 | Reto: integraciones (aula virtual) | **Adriana** | 1:30 |
| 16 | Demo en vivo | Adriana en el teléfono, Antony en la laptop | 4:00 |
| 17 | Conclusiones | Antony y Adriana | 1:00 |
| 18 | Cierre y preguntas | Adriana abre, los dos responden | 0:45 |

---

## 1 · Portada · Adriana

**Adriana:** Buenos días, profesor Cuadros, compañeros. Soy Adriana Laos.

**Antony:** Y yo soy Antony Solorzano.

**Adriana:** Les presentamos TARE·IA, nuestro proyecto de la Unidad II. Es una app Android para no perder tareas: le das una foto de tus apuntes, un PDF o tu aula virtual, y la app las convierte en actividades con fecha, arma un plan de estudio y las manda a tu Google Calendar.
Pero en este curso lo importante es cómo comprobamos que funciona, así que la mayor parte trata de pruebas, análisis estático, infraestructura y automatización.
Cada diapositiva tiene abajo a la izquierda el criterio de la rúbrica que cubre, para que pueda seguir la evaluación punto por punto.

## 2 · Problema · Adriana

**Adriana:** Un estudiante de la UPT recibe tareas por todos lados: el aula virtual, el sílabo, sus apuntes, el grupo del curso. Nada de eso llega solo a su calendario. Copia cada fecha a mano, y ahí aparecen los olvidos y el estudio de última hora.
Tres datos con fuente. Steel, en 2007, reporta que entre el 80 y el 95 % de los universitarios procrastina. Kim y Seo, con 33 estudios y más de 38 mil participantes, encontraron que a más procrastinación, menor rendimiento. Y según el INEI, el 98,4 % de los universitarios usa internet, y el 90 % lo hace desde el celular: por eso es una app Android.
Garzón y Gil vieron que tener una agenda no basta; lo que ayuda es fijar prioridades. Por eso TARE·IA no es otra agenda: reduce el trabajo de registrar y ordena por urgencia. La IA lee, pero nada llega al calendario sin que tú lo confirmes.

## 3 · Objetivos · Adriana

**Adriana:** El objetivo general: en 16 semanas, desarrollar, probar y documentar TARE·IA, con calidad verificada por mutación y análisis estático.
Cada objetivo tiene indicador, meta e instrumento. Destaco cuatro. OS-4: la IA nunca crea eventos sin permiso; lo garantiza un trigger y lo comprueba un escenario de Cucumber. OS-5: cero duplicados. OS-8: pruebas que detecten defectos; la meta era 60 % de mutación y logramos 93,8 en la API y 68,4 en Kotlin. Y OS-11: costo cero, cumplido.
Los que están abajo, como el SUS de usabilidad, dependen de un piloto con estudiantes. La instrumentación está lista; la medición, todavía no.

## 4 · La app · Adriana

**Adriana:** Esta es la app, nativa en Kotlin con Jetpack Compose.
A la izquierda, el inicio: una cinta con los próximos 14 días y la tabla de cursos, cada curso como un elemento de la tabla periódica. Al centro lo más importante: lo que propone la IA llega con este sello verde, «IA · Por validar»; ese color está reservado solo para la IA. A la derecha, el modo oscuro; también la probamos con la letra al 180 %.
El flujo es de cuatro pasos: entra el material, la IA propone, tú validas y recién ahí se planifica y va a Calendar.

## 5 · Arquitectura · Adriana

**Adriana:** Son tres piezas. La app Android, en capas: interfaz, casos de uso, un dominio que no depende de Android, e infraestructura con base local cifrada, IA y conexión remota.
La API REST en Node con Fastify y TypeScript, con rutas, servicios y repositorios, JWT y Swagger.
Y los datos: PostgreSQL en Neon, versionado con Liquibase, de donde leen el dashboard y el servidor MCP.
Abajo, los servicios externos: Firebase, Gemini, Google Calendar, el aula virtual y el OCR de ML Kit, que corre en el teléfono.

## 6 · Patrones · Adriana

**Adriana:** Usamos nueve patrones y cada uno resuelve algo concreto.
Builder: una actividad manual con fecha nace Confirmada; una que viene de la IA o del aula virtual nace Pendiente de validar. Prototype: el plan se clona como propuesta, y el anterior queda como Reemplazado. Abstract Factory fabrica los servicios de IA; Singleton, la base local.
Facade: la clase Nube esconde la API y las colas.
State: la actividad pasa de Pendiente a Confirmada, Sincronizada y Completada, sin saltarse pasos. Strategy: si la IA falla, planifica localmente por prioridad. Observer avisa a la telemetría. Y Repository separa el dominio del origen de datos. Todos tienen pruebas.
Antony les explica cómo esas reglas también viven en la base de datos.

## 7 · Base de datos · Antony

**Antony:** Gracias, Adriana. La base es PostgreSQL 16 en Neon: 11 tablas, 6 vistas, 7 funciones y 7 triggers, construidas con 20 changesets de Liquibase, cada uno con su rollback.
La decisión clave: el patrón State que vieron en Kotlin está repetido en un trigger. Si alguien intenta pasar una actividad de Pendiente directo a Sincronizada, aunque sea con SQL a mano, la base lo rechaza. Así se cumple la regla RN01: nada va a Calendar sin validar.
Otro trigger guarda el historial, y las vistas alimentan el dashboard. El diccionario y el diagrama entidad-relación se generan desde la base real.

## 8 · Requisitos no funcionales · Antony

**Antony:** La rúbrica pide cinco.
Sesiones: el token de Firebase se cambia por un JWT de nuestra API, de 7 días, y cada inicio abre una fila en la tabla sesion; se cierra a los 30 minutos sin actividad. En el teléfono va cifrado con el Keystore.
Historial: un trigger guarda el antes y el después en JSON de cada cambio.
Caché: en memoria para el dashboard, con la cabecera x-cache HIT o MISS, además de la vista materializada.
Logs: en JSON, ocultando contraseñas, tokens y correos.
Telemetría: la app envía eventos de uso en lotes de 20, sin datos personales.

## 9 · Pruebas · Antony

**Antony:** Esta es la pirámide con números reales. 66 unitarias de la API y 35 del dominio Kotlin; 22 de integración contra un PostgreSQL real y 11 de sistema. En interfaz, Playwright en escritorio y en un Pixel 7, y pruebas de Compose en el emulador. Arriba, 18 escenarios de Cucumber en español.
La mutación: Stryker detectó el 93,8 % de los mutantes de la API y Pitest el 68,4 % del dominio.
Y lo más valioso: las pruebas encontraron tres defectos reales. El historial fallaba al borrar en cascada; un correo con mayúsculas chocaba con el mismo usuario de Google; y los duplicados con espacios repetidos no se detectaban. Los tres se corrigieron y quedaron con su prueba.

## 10 · Análisis estático · Antony

**Antony:** Usamos tres herramientas porque miran cosas distintas. SonarQube Cloud mide calidad: bugs, vulnerabilidades, hotspots, duplicación y cobertura, con un quality gate. Semgrep busca patrones inseguros, como inyección SQL o secretos, y sus reportes salen en la pestaña Security. Snyk revisa las dependencias de npm y Gradle.
Las tres corren solas en cada push. Semgrep, por ejemplo, nos marcó tres hallazgos graves que ya corregimos.
[Mostrar la pestaña Security o SonarCloud; no decir cifras de memoria.]

## 11 · Terraform y costos · Antony

**Antony:** Toda la nube se crea con Terraform: un módulo para Neon y otro para Render, con el estado en HCP Terraform.
La infraestructura también se prueba: formato, validación, tflint, terraform test con proveedores simulados y checkov.
Hoy cuesta cero dólares, con capas gratuitas. Sus límites: medio giga de base y la API se duerme tras 15 minutos. Para comparar, la misma arquitectura en AWS o Azure costaría unos 38 dólares al mes.

## 12 · Release y despliegue · Antony

**Antony:** Con commits convencionales, release-please calcula la versión y escribe el CHANGELOG. Al publicar, se generan el APK, la API, el dashboard y el servidor MCP en GitHub Releases. Luego Liquibase actualiza la base, se despliega en Render y una prueba de humo consulta el endpoint de salud. Si no responde, el workflow falla.

## 13 · Documentación · Antony

**Antony:** La documentación se genera desde el código. docs.yml produce el diccionario de datos y los diagramas de entidad-relación, clases, componentes y despliegue, y actualiza el README. pages.yml publica en GitHub Pages la referencia de cada clase, método y propiedad con Dokka y TypeDoc, más los reportes de pruebas. Nadie tiene que acordarse de actualizarla.

## 14 · Dashboard · Antony

**Antony:** El dashboard de uso está en React y solo lo ve un administrador. Arriba, estudiantes, sesiones, duración media, actividades y errores. Debajo, la evolución por día, las funciones y pantallas más usadas, y el origen de las actividades por estado. Los números de la captura son de demostración, no de un piloto.
Adriana les cuenta el reto de integraciones.

## 15 · Reto: integraciones · Adriana

**Adriana:** El reto pedía tres integraciones o un plugin con skills y agente. Hicimos cinco.
Firebase, para entrar con Google. Gemini, que lee los apuntes y propone actividades. Google Calendar, que solo recibe lo que validaste.
La cuarta es la que más nos importa: el aula virtual de la UPT. La universidad no nos da acceso a la API de Moodle, así que usamos algo que cualquier estudiante tiene: en el aula, Calendario, Exportar calendario, copias una URL. La pegas una sola vez en la app y se guarda cifrada, porque funciona como una contraseña. Desde ahí, cada tres horas la app trae tus entregas: las nuevas entran como Por validar, si el docente cambia una fecha te avisa, y no duplica nada. Lo probamos en mi teléfono, un Redmi Note 14, con nuestras entregas reales.
Y la quinta, el servidor MCP con siete herramientas, una skill y un agente para Claude Code: le preguntas «¿qué entrego esta semana?» y responde con tus pendientes por urgencia.
Ahora lo vemos funcionando.

## 16 · Demo en vivo · Adriana (teléfono) y Antony (laptop)

[Proyectar el teléfono con scrcpy o el cable. Antony tiene en la laptop el dashboard y GitHub Actions abiertos.]

**Adriana:** [Abrir la app → Continuar con Google] Entro con Google. Por detrás, la API me da su propia sesión.
[Integraciones → Aula Virtual UPT → Sincronizar ahora] Sincronizo el aula virtual: aparecen mis entregas reales con el sello «Por validar».
[Importar una foto de un apunte] Ahora una foto de un apunte: la IA propone la actividad, otra vez por validar. Si intento mandarla a Calendar, la app no me deja.
[Validar, corregir la fecha si hace falta, enviar a Calendar] La valido y ahora sí va a Google Calendar.

**Antony:** [Dashboard en la laptop, recargar] En el dashboard aparece esta sesión y los eventos que Adriana acaba de generar, como la importación con IA y el envío a Calendar.
[GitHub → Actions → abrir un workflow en verde] Y este es el pipeline: cada cambio pasa por pruebas, mutación y análisis estático antes de llegar a producción.

> **Plan B:** si Render está dormido, Antony dice «es el arranque en frío del plan gratuito» y muestra las capturas. Si falla el teléfono, usar el video grabado. Detalle en `guion-demo.md`.

## 17 · Conclusiones · Antony y Adriana

**Antony:** Aprendimos tres cosas. Las pruebas pagaron: encontraron tres defectos reales antes de producción. La mutación nos enseñó que tener pruebas no significa que sirvan. Y todo el ciclo, del análisis estático al despliegue, está automatizado y cuesta cero.

**Adriana:** Y la regla principal, «la IA propone y tú decides», se cumple en la app, en la API y en la base de datos. Lo que sigue: un piloto con estudiantes de la UPT, subir la mutación en Kotlin a 75 %, más dispositivos de prueba y recordatorios push.

## 18 · Cierre · Adriana abre, los dos responden

**Adriana:** Eso es TARE·IA. Aquí están el repositorio, la documentación en GitHub Pages, la API y el dashboard. Muchas gracias.

**Antony:** Con gusto respondemos sus preguntas.

### Quién responde qué

| Tema de la pregunta | Responde |
|---|---|
| Problema, objetivos, fuentes, la app, UX | Adriana |
| Patrones de diseño, arquitectura Android | Adriana |
| Aula virtual, Moodle, IA, Google Calendar, MCP | Adriana |
| Base de datos, Liquibase, triggers | Antony |
| Pruebas, mutación, Cucumber, Playwright | Antony |
| SonarQube, Semgrep, Snyk | Antony |
| Terraform, costos, release, Pages, dashboard | Antony |

Regla: quien responde repite la pregunta en voz alta, contesta en menos de un minuto y, si sirve, abre el archivo o el reporte. Si algo no se midió, se dice así. Respuestas preparadas en `banco-de-preguntas.md`.

### Preguntas probables sobre el aula virtual (Adriana)

- **¿Por qué no usan la API de Moodle?** La UPT no habilita el servicio web para estudiantes. El calendario exportado es una función estándar de Moodle que cualquier estudiante puede usar, sin instalar nada.
- **¿Es seguro guardar esa URL?** La URL lleva un token personal, por eso se guarda cifrada con el Android Keystore, solo se acepta por HTTPS y nunca se envía a nuestra API ni se escribe en los logs.
- **¿Qué pasa si el docente cambia la fecha?** La app lo detecta al comparar con lo ya importado y muestra el cambio para que el estudiante lo aplique; no lo cambia solo.
- **¿Cómo lo probaron?** Con 12 pruebas automatizadas (lectura del ICS, zonas horarias, duplicados y cambios de fecha) y en un teléfono real con entregas reales.
