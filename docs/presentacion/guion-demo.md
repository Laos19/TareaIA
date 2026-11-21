# Guion de la demo en vivo: TARE·IA

> Exponen: Adriana Rafaela Laos González (2023077474) y Antony Piero Solorzano Zegarra (2023078696) ·
> SI-784 Calidad y Pruebas de Software · UPT · Ing. Patrick Cuadros Quiroga.
> La demo dura unos 4 minutos y entra en la diapositiva 16 («Veámoslo funcionar»). Si el docente lo prefiere,
> puede hacerse después de la diapositiva 4 («La app»).
> Abran este guion en el celular o en un segundo monitor; la vista del orador de reveal.js (tecla **S**)
> muestra las notas de cada diapositiva.

## URLs y cuentas (confirmar la víspera)

| Qué | URL / dato | Nota |
|---|---|---|
| API (Render) | `https://tareia-api.onrender.com/api/health` | Confirmar con `terraform output` o las variables del repo. |
| Swagger | `https://tareia-api.onrender.com/api/docs` | OpenAPI de todos los endpoints. |
| Dashboard | `https://tareia-dashboard.onrender.com` | Solo rol admin. |
| Cuenta admin (dashboard) | `admin@demo.tareia.pe` / `tareia2026` | Ve el dashboard. |
| Cuenta estudiante | `estudiante@demo.tareia.pe` / `tareia2026` | **No** puede ver el dashboard (sirve para mostrar el control por rol). |
| Documentación | `https://laos19.github.io/TareaIA/` | Dokka, TypeDoc, diccionario, diagramas, reportes. |
| Repositorio | `https://github.com/Laos19/TareaIA` | Pestañas **Actions**, **Security** y **Releases**. |
| App Android | APK del último Release, instalada en el teléfono o en el emulador | Cuenta de Google de prueba con sesión abierta en el teléfono. |

---

## Checklist previa

**La noche anterior**
- [ ] Abrir cada URL de la tabla y confirmar que carga. Anotar las que fallen y usar su plan B.
- [ ] Confirmar que la última ejecución de `ci.yml`, `android.yml`, `mutacion.yml`, `sonarqube.yml`,
      `semgrep.yml`, `snyk.yml`, `infra.yml`, `release.yml` y `pages.yml` está en verde. Si alguna falla,
      **no la escondan**: preparen la explicación (banco de preguntas).
- [ ] Instalar el APK del último Release en el teléfono de la demo y entrar una vez con Google.
- [ ] Preparar en el teléfono una **foto de un apunte** con una tarea y fecha clara (por ejemplo,
      «Informe de laboratorio de Base de Datos — entregar el viernes 28») y un PDF corto de sílabo.
- [ ] Revisar que Google Calendar de la cuenta de prueba no tenga ya esa tarea (para que se vea el alta).
- [ ] Grabar un **video de respaldo** de 3 minutos con la misma secuencia (plan C).
- [ ] Copiar a una USB: `TAREIA-presentacion.pdf`, `TAREIA-presentacion.pptx` y el video.
- [ ] Repasar `banco-de-preguntas.md`, en especial las de pruebas, mutación y análisis estático.

**El día de la exposición (30 minutos antes)**
- [ ] Laptop cargada, cargador, adaptador HDMI/USB-C. Teléfono cargado y con **scrcpy** o cable para
      proyectar la pantalla (o el emulador abierto en la laptop).
- [ ] Modo «No molestar» en laptop y teléfono.
- [ ] Zoom del navegador al 125 %.
- [ ] Pestañas en este orden: (1) presentación, (2) dashboard, (3) GitHub Actions, (4) Pages, (5) Swagger.

**5 minutos antes (calentar Render)**
- [ ] Abrir `/api/health`. El plan gratuito se duerme tras 15 min sin tráfico y el primer arranque tarda
      cerca de 1 minuto. Esperar a ver la respuesta con la base de datos en «ok».
- [ ] Abrir el dashboard y dejar la sesión de admin iniciada.
- [ ] Recargar `/api/health` justo antes de empezar.

---

## Guion minuto a minuto

| Tiempo | Acción | Qué decir (resumen) |
|---|---|---|
| 0:00 | Diapositiva 16 en pantalla. | «Son cinco pasos; los van a ver en el teléfono proyectado.» |
| 0:15 | **Paso 1.** Abrir la app → «Continuar con Google». | «Google Sign-In con Credential Manager. La app cambia el token de Firebase por un JWT de nuestra API; eso abre una sesión que veremos en el dashboard.» |
| 0:45 | **Paso 2.** Pestaña «Importar» → elegir la foto del apunte. Mientras procesa, señalar el punto que late. | «El OCR corre en el teléfono con ML Kit; luego Gemini propone título, curso y fecha. El punto amarillo que late es el indicador de que la app está pensando.» |
| 1:15 | Mostrar la actividad propuesta con el sello **IA · Por validar**. Tocar «Calendario» **sin validar**. | «Intento mandarla a Calendar… y no me deja: regla RN01. Esa misma regla está en la API y en un trigger de la base de datos.» |
| 1:45 | **Paso 3.** «Editar» → corregir la fecha si hace falta → confirmar → «Calendario». Abrir Google Calendar. | «Ahora sí: Confirmada y luego Sincronizada. Patrón State. Aquí está el evento en mi calendario.» |
| 2:20 | (Opcional) Importar de nuevo la misma tarea. | «Si la importo otra vez, no se duplica: RN02, título, curso y fecha normalizados. Este fue uno de los defectos que encontraron las pruebas.» |
| 2:40 | **Paso 4.** Cambiar al dashboard → «Recalcular métricas». | «Solo un admin entra aquí. Ven la sesión que acabo de abrir y los eventos: importación con IA, actividad validada, enviada a Calendar. La cabecera `x-cache` dice si vino de caché.» |
| 3:20 | **Paso 5.** GitHub → Actions → último `ci.yml` o `mutacion.yml` en verde → *Summary*. | «Cada push corre 99 pruebas de la API y el reporte de mutación queda aquí como artefacto.» |
| 3:50 | Volver a la presentación (diapositiva 17). | «Eso es lo que vieron funcionando; ahora las conclusiones.» |

**Paso extra si sobra tiempo (reto, 40 s):** en Claude Code, con el servidor MCP `tareia` conectado, escribir
«¿qué entrego esta semana?». El agente `coach-academico` llama a `actividades_pendientes` y responde ordenado
por urgencia. Después, en el dashboard, se ve una sesión con origen `mcp`.

---

## Plan B por cada falla

| Falla | Señal | Qué hacer y qué decir |
|---|---|---|
| Render dormido | `/api/health` tarda más de 20 s | «Es el arranque en frío del plan gratuito; es un límite documentado en el reporte de costos.» Seguir con la app (pasos 1–3 funcionan con la base local) y volver al dashboard al final. |
| La API no responde nada | Error 5xx o timeout por más de 1 min | Mostrar el dashboard con las capturas de la diapositiva 14 y el video de respaldo. Decirlo sin rodeos. |
| Gemini no responde o tarda | El punto sigue latiendo más de 20 s | «Patrón Strategy: si la IA falla, la app usa la planificación local por prioridad.» Crear la actividad con «Nueva actividad» a mano y seguir con el paso 3. |
| Google Sign-In falla (red del aula) | Error de credenciales | Usar el acceso local con PIN (pantalla de login) y explicar que la sesión en la nube no se abrirá en ese caso. |
| Google Calendar no muestra el evento | No aparece tras 10 s | Refrescar Calendar; si no, mostrar el estado «Sincronizada» en la app y el historial de la actividad (`/actividades/{id}/historial` en Swagger). |
| No se puede proyectar el teléfono | scrcpy o cable no funcionan | Usar el emulador de la laptop o pasar directo al video de respaldo. |
| Un workflow está en rojo | Actions muestra ❌ | Abrirlo igual y explicar qué falló y por qué; es más creíble que esconderlo. |
| Se acabó el tiempo | El docente pide avanzar | Saltar el paso 5 y el paso extra; ir a la diapositiva 17. |

---

## Después de la demo

- Cerrar sesión en el dashboard si la laptop no es suya.
- Si se creó una actividad de prueba en la cuenta compartida, dejarla en la papelera (RN14) para la próxima demo.
