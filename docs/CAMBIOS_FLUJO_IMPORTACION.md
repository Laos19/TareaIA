# TARE·IA — revisión del flujo de importación (12-11-2026)

Este paquete parte de `TareaIA.zip` enviado por el equipo. Se han conservado Google Sign-In, Google Calendar REST, App Check, Gemini, gestión de cursos, tareas y la estructura general del proyecto.

## Qué cambió

- `app/src/main/java/com/tareia/app/ImportWorkspaceStore.kt` **nuevo**: conserva cifrados el texto OCR, el nombre del documento, las propuestas editadas, el origen de las propuestas, el mensaje del proceso y el ID del apunte guardado. El estado se vincula al correo del perfil local. Se recupera al volver a Importar y al reiniciar la aplicación.
- `app/src/main/java/com/tareia/app/ui/TareiaApp.kt`: eleva el estado de importación al nivel de navegación; ejecuta OCR y Gemini en una corrutina que no se cancela por cambiar de pestaña. Comprueba el almacenamiento de apuntes y tareas antes de indicar éxito y ofrece navegar a Apuntes.
- `app/src/main/java/com/tareia/app/ui/screens/ImportarScreen.kt`: agrega «Descartar esta propuesta», «Descartar todas» con confirmación, «Limpiar proceso» con confirmación y «Volver a sugerencias locales (sin IA)». Distingue propuestas locales y Gemini; solicita corrección de fecha y título antes de guardar; incorpora título editable del apunte y «Ver en Más → Apuntes».
- `app/src/androidTest/java/com/tareia/app/ui/ImportarFlowTest.kt` **nuevo**: escenarios para descartar una o todas las propuestas conservando el texto.

## Verificación manual en Android Studio

1. Ejecutar `Build → Rebuild Project` y abrir TARE·IA en el emulador API 35.
2. **Sin Gemini:** Importar un PDF, imagen o TXT. Revisar las propuestas locales. Descartar una y después todas. Verificar que el texto OCR se conserve.
3. **Con Gemini:** Elegir un documento que contenga tareas con fechas. Pulsar «Analizar apunte con Gemini IA», descartar propuestas y revisar el formulario. Repetir con una propuesta sin fecha: debe exigir corrección o descarte antes de confirmar.
4. **Navegación:** Importar un documento, editar el texto y una propuesta; abrir Tareas o Más, volver a Importar y comprobar que texto, nombre y correcciones permanecen. Cerrar y abrir la app y repetir.
5. **Apuntes:** Pulsar «Guardar texto completo en Apuntes», después «Ver en Más → Apuntes». Comprobar contenido y título. Regresar a Importar, editar y pulsar «Actualizar apunte guardado»; debe modificar la misma nota y no duplicarla.
6. **Guardado de tareas:** Confirmar propuestas válidas; verificar en Tareas. Intentar confirmar datos duplicados: debe informar que se omitieron o que no se añadió ninguna.
7. **Limpiar:** Probar «Limpiar proceso» y cancelar primero. Luego confirmar: el borrador debe desaparecer, sin borrar tareas o apuntes ya guardados.
8. **Cambio de pestaña durante el trabajo:** Iniciar OCR o Gemini, pasar a otra pestaña y volver; al completarse deben verse los resultados, salvo que el usuario cierre la sesión o finalice el proceso de la app.

## Consideraciones

- El borrador conserva el **texto reconocido**, no una copia del archivo original; volver a abrir el PDF original no forma parte de esta mejora.
- El texto del apunte y las propuestas se guardan cifrados en el almacenamiento privado local con `SecureTokenStore`. No se sincronizan entre dispositivos.
- Se conserva la arquitectura actual. La base local sigue sin estar aislada por cuentas Google; el estado específico de importación se compara con el correo local.
- No subir tokens de depuración de App Check, archivos `local.properties`, carpetas `.gradle`, `build` o `.idea` al repositorio.
- Se comprobó sintaxis Kotlin de los archivos intervenidos, pero **no se ha ejecutado compilación Android ni pruebas instrumentadas en este entorno**. Debe realizarse la verificación manual y automatizada en el equipo con Android SDK.
