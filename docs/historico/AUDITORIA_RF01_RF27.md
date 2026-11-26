# Auditoría de TARE·IA frente a RF-01 a RF-27

Revisión estática de la app Android. «Presente» significa que hay un flujo en el código. La aceptación requiere ejecutar el caso indicado. «Parcial» identifica una diferencia entre el código y el requisito suministrado.

Gradle completó la tarea de ensamblado del APK de depuración. `testDebugUnitTest` informó `NO-SOURCE`. Android Lint reportó dos errores por contenedores `BoxWithConstraints` cuyo alcance no se usa, además de cinco avisos. No se ejecutaron los flujos en un dispositivo ni con cuentas de Google o Moodle.

| RF | Estado y comportamiento actual | Criterio para cerrar el requisito |
|---|---|---|
| 01 | Presente. Alta y acceso mediante perfil y PIN local. | Reiniciar la app y comprobar que rechaza un PIN incorrecto y abre con el correcto. |
| 02 | Parcial. Android solicita acceso a calendarios y permite elegir uno editable. El selector admite proveedores distintos de Google. | Identificar el calendario Google del estudiante, confirmar el destino y comprobar los escenarios de autorización, denegación y revocación. Si el consentimiento se exige directamente ante Google, incorporar OAuth. |
| 03 | Parcial. ML Kit lee texto de imágenes y PDF. La IA configurable recibe el texto extraído. | Verificar con una captura que contenga información visual relevante y un PDF con texto. Si la interpretación requiere imagen o estructura visual, añadir entrada visual al motor y comprobar el resultado. |
| 04 | Parcial. Hay extracción heurística y una opción de extracción por IA. La detección local depende de palabras y formatos de fecha. | Probar un documento que incluya una tarea, un entregable, un examen y varias fechas. Confirmar que cada título y fecha corresponden a su actividad. |
| 05 | Presente. Se revisan y editan los borradores de importación. | Corregir curso y fecha y comprobar que el evento solo se crea tras validar y solicitar sincronización. Probar también la ruta desde Moodle. |
| 06 | Presente. Editor para alta manual con título, curso y fecha. | Crear una actividad desde el editor sin configurar IA y verificar su persistencia. |
| 07 | Parcial. La pantalla genera y permite confirmar sesiones con el planificador local. Existe `AiClient.plan`, pero la pantalla llama a `TaskPlanner.propose`. | Conectar la propuesta IA. Validar las sesiones contra disponibilidad, conflictos y fecha límite antes de mostrarlas. Guardarlas solo tras confirmar. |
| 08 | Parcial. La app escribe en el calendario Android seleccionado y conserva el ID del evento. Google depende del destino seleccionado y de la sincronización del dispositivo. | Confirmar una tarea, sincronizarla hacia un calendario Google y verificar su aparición en esa misma cuenta. Repetir la operación y comprobar que sigue existiendo un solo evento. |
| 09 | Parcial. El código programa avisos con antelación fija. Falta edición y desactivación por actividad desde el editor y coordinar los avisos del calendario cuando cambia la configuración. | Cambiar la antelación y desactivar el aviso en tareas manuales e importadas. Comprobar que se cancelan los avisos anteriores, también tras reinicio. |
| 10 | Parcial. Inicio muestra próximas actividades y Actividades tiene un filtro por estado. | Combinar filtro por curso con pendientes de validar y comprobar resultados, incluidas actividades sin curso. |
| 11 | Parcial. Detecta duplicados y los rechaza u omite. | Ofrecer Sobrescribir y Cancelar en altas e importaciones. Sobrescribir debe mantener el vínculo del evento existente y evitar avisos o sesiones duplicados. |
| 12 | Parcial. Hay permisos de calendario, micrófono y notificaciones. La app delega la selección de archivos y la captura a componentes del sistema. | Verificar los flujos de fotos, archivos y cámara y su feedback cuando se cancelan o fallan. Si se requiere acceso directo a cámara o biblioteca, solicitar el permiso correspondiente al usarlo. |
| 13 | Presente. Permite editar nombre, correo, código y semestre. | Cambiar datos de perfil y verificar que se conservan tras volver a abrir la app. |
| 14 | Presente. Se crean y editan cursos con abreviatura y se rechaza una abreviatura repetida. | Importar un apunte que mencione solo la abreviatura y confirmar que se propone el curso correcto. |
| 15 | Presente en código. Hay recepción de texto e imágenes/documentos a través de Android. | Compartir texto, una imagen y un PDF desde otras apps. Verificar qué ocurre con formatos no admitidos o varios adjuntos. |
| 16 | Parcial. Hay cursos y carpetas, con asignación desde el editor de apuntes y clasificación de tareas por curso. | Detectar el curso de un apunte y de su actividad y agruparlos bajo la carpeta del curso. Probar también un curso indeterminado. |
| 17 | Parcial. Permite ingresar bloques libres manualmente y rechaza solapamientos de bloques. | Definir formatos de horario aceptados, importar uno, revisar los intervalos detectados y comprobar que el plan evita esas horas ocupadas. |
| 18 | Presente en código. Prioridad y dificultad intervienen en el cálculo de sesiones del planificador local. | Comparar dos casos con disponibilidad equivalente y verificar cómo varía el tiempo total asignado según la dificultad y prioridad. |
| 19 | Parcial. Se puede recalcular al editar una fecha manualmente. La importación crea borradores, sin una propuesta específica de actualización de una actividad existente. | Importar un apunte que cambie una entrega del jueves al viernes. Mostrar la comparación y permitir confirmar, editar o descartar. Comprobar sesiones, aviso y evento vinculado tras confirmar. |
| 20 | Parcial. La bandeja consulta notas sin curso o carpeta y permite editarlas. Las actividades sin curso se muestran como pendientes de validar en Actividades. | Mostrar el contenido sin curso, incluidas actividades detectadas, y comprobar que sale de la bandeja al asignarlo manualmente. |
| 21 | Presente en código. Papelera con restauración y eliminación permanente de cursos y apuntes. | Eliminar y restaurar un curso y sus apuntes asociados. Comprobar restauración de relaciones y que una purga elimina el elemento elegido. |
| 22 | Presente en código. Selector de PDF, imágenes y texto, y captura desde cámara. | Elegir un PDF, una captura y una imagen reales del dispositivo y revisar errores de archivos inválidos. |
| 23 | Presente. Hay creación, edición y eliminación de apuntes. | Crear, editar y mover un apunte, cerrar la app y verificar persistencia. |
| 24 | Presente en código. Dictado, revisión de datos y confirmación del plan mediante el planificador local. | Dictar una actividad con curso y fecha y confirmar sesiones. Probar micrófono denegado y errores de transcripción. |
| 25 | Presente en código. Reporte PDF con filtros. | Guardar y abrir el PDF. Comparar tareas, fechas y estados con el filtro aplicado. |
| 26 | Implementado en código, pendiente de prueba con Moodle autorizado. Consulta cursos y tareas con URL y token e importa pendientes de validar. | Ejecutar con un token habilitado, repetir importación y confirmar que no duplica actividades. |
| 27 | Parcial si se exige intercambio de datos. El flujo actual abre la URL del portal en el navegador. | Confirmar qué significa «integración directa». Si requiere consulta de datos, identificar el proveedor, autenticación, API y datos autorizados. Si el criterio es acceso al portal, verificar el enlace. |

## Vista de Google Calendar

RF-02 pide consentimiento y RF-08 pide que el evento validado se cree en la cuenta Google del estudiante. RF-10 pide una vista de seguimiento de las actividades, con filtro por curso y pendientes de validar. Estos criterios se pueden cubrir con la vista interna de actividades y la sincronización al calendario Google elegido. Una vista mensual o una copia completa de Google Calendar sería una ampliación del alcance.

Para que el estudiante pueda comprobarlo, basta con mostrar el calendario y cuenta de destino, el resultado del envío y una acción «Abrir evento en calendario». Con Android Calendar Provider, guardar correctamente el evento confirma su escritura en el dispositivo. La aparición en la cuenta remota debe verificarse por separado. [Documentación de Android Calendar Provider](https://developer.android.com/identity/providers/calendar-provider)

## Orden de implementación

1. Establecer una compilación reproducible y probar acceso, importación y planificación de punta a punta.
2. Cerrar Google Calendar, recordatorios y duplicados juntos. Los tres comparten el vínculo entre actividad, aviso y evento externo.
3. Implementar filtrado, clasificación automática, carga de horario, propuesta IA y revisión de cambios detectados.
4. Validar Moodle con una instancia autorizada y precisar RF-27. Usar esta matriz como criterio de aceptación para la demo y los documentos.
