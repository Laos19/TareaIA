# TARE·IA — cobertura funcional RF-01 a RF-27

Esta versión amplía el prototipo Android incluido en el ZIP. La persistencia es local y protegida con Android Keystore; la interfaz usa Jetpack Compose y se adapta a distintos anchos. Los datos no se sincronizan entre dispositivos salvo la lectura de Moodle y el calendario configurado en Android.

## Matriz de requisitos

| RF | Implementación en esta versión | Alcance y condición |
|---|---|---|
| RF-01 | Perfil local y acceso con PIN protegido mediante hash PBKDF2 con sal. | No crea cuentas institucionales ni sincroniza usuarios con un servidor. |
| RF-02 | Lectura y escritura de eventos mediante Android Calendar Provider; selección de calendario y asociación de tarea/evento. | Google Calendar aparece si la cuenta está añadida y sincronizada en Android. Requiere permiso de calendario. |
| RF-03 | OCR local de fotos e imágenes y de PDF (hasta 30 páginas), además de texto plano; análisis opcional mediante proveedor compatible configurado por el usuario. | La extracción por IA envía texto al endpoint HTTPS elegido; no viene endpoint ni API key institucional preconfigurada. |
| RF-04 | Extrae candidatos por fechas y palabras académicas, identifica cursos por nombre/abreviatura y propone tareas; opcionalmente usa IA. | Los resultados pueden ser incompletos y se presentan para corrección. |
| RF-05 | Revisión y edición de título, curso y fecha antes de confirmar la importación. | Las entradas sin curso quedan en estado por validar y en Sin clasificar. |
| RF-06 | Registro y edición manual de tareas, con validación de fechas y estado. | Local al dispositivo. |
| RF-07 | Planificador determinista que usa fecha límite, prioridad, dificultad, disponibilidad semanal y sesiones existentes. | Se puede proponer menos sesiones si no hay bloques suficientes; no es un optimizador ni una garantía académica. |
| RF-08 | Sincronización explícita de tarea como evento en calendario Android, con actualización del vínculo al editar. | No realiza sincronización bidireccional continua con la nube. Requiere permisos. |
| RF-09 | Alarmas y notificaciones locales previas a la fecha de entrega; reprogramación tras reinicio. | Requiere permiso de notificaciones en Android 13 o posterior; Android puede limitar alarmas según fabricante/ahorro de batería. |
| RF-10 | Inicio con resumen de tareas y próximos eventos/planificación. | Resumen local. |
| RF-11 | Detección de duplicados por título, curso y fecha; deduplicación por lote y por ID externo de Moodle. | La comparación textual no detecta todas las equivalencias semánticas. |
| RF-12 | Centro de permisos con acceso a micrófono, calendario y notificaciones. | El sistema operativo controla la concesión efectiva. |
| RF-13 | Edición del perfil académico guardado localmente. | No verifica correo/código con UPT. |
| RF-14 | Alta, edición y eliminación de cursos con prioridad y dificultad; al eliminar, tareas/notas relacionadas pasan a papelera. | Los cursos son locales. |
| RF-15 | Recepción de texto, PDF e imágenes compartidos desde otras aplicaciones mediante Android Sharesheet/intent. | Depende de que la aplicación de origen permita compartir el contenido. |
| RF-16 | Apuntes con carpetas y asociación a curso. | Almacenamiento local. |
| RF-17 | Edición de bloques semanales de disponibilidad; validación de solapamientos; usados por el planificador. | No importa automáticamente horarios institucionales. |
| RF-18 | Prioridad y dificultad por curso usadas para ajustar el número de sesiones propuesto. | Escala discreta local del 1 al 5. |
| RF-19 | Al cambiar la fecha límite se puede recalcular el plan de sesiones de la tarea. | El usuario debe activar la opción de recalcular al editar. |
| RF-20 | Bandeja Sin clasificar para importaciones o notas sin curso confirmado. | Requiere clasificación manual posterior. |
| RF-21 | Papelera para tareas, cursos y apuntes; restauración y eliminación permanente. | La purga es irreversible dentro de la aplicación. |
| RF-22 | Selector de archivos para PDF/imágenes/texto, captura con cámara e importación compartida; OCR local y revisión de resultados. | PDF limitado a 30 páginas por procesamiento. |
| RF-23 | Crear, editar, organizar y eliminar apuntes; papelera para recuperación. | Local al dispositivo. |
| RF-24 | Dictado de actividad, curso y fecha; edición de transcripción/campos; propuesta y confirmación de sesiones. | Usa SpeechRecognizer disponible en el dispositivo; el servicio de voz puede operar en la nube según Android/proveedor. |
| RF-25 | Generación de reporte PDF con filtro/estado y selector de destino Android. | El usuario elige dónde guardar o compartir el archivo. |
| RF-26 | Consulta Moodle de identidad, cursos y tareas mediante Web Services; deduplicación e importación como pendientes de validar; token cifrado. | Requiere URL HTTPS, token autorizado y funciones Web Services habilitadas por el administrador. Solo lectura. |
| RF-27 | Acceso directo al portal indicado: https://aulavirtual.upt.edu.pe/. | Implementado como apertura del portal. La integración automática con el sistema distinto de Moodle no es posible sin conocer proveedor, API y autenticación. |

## Preparación

1. Abrir `TAREIA_v4` en Android Studio y sincronizar Gradle.
2. Compilar con Android SDK 35; `minSdk` es 28. El proyecto de origen no incluye Gradle Wrapper.
3. Conceder los permisos solicitados cuando se utilicen micrófono, calendario o notificaciones.
4. En Más > Calendario, seleccionar un calendario del dispositivo antes de sincronizar eventos.
5. En Más > Integraciones > Moodle, configurar instancia HTTPS y token de servicio autorizado. La instancia debe permitir las funciones `core_webservice_get_site_info`, `core_enrol_get_users_courses` y `mod_assign_get_assignments`.
6. Opcionalmente, en Más > IA, configurar endpoint HTTPS compatible con Chat Completions, nombre de modelo y API key. La clave queda cifrada con Android Keystore. La app solo envía texto cuando el usuario pulsa “Analizar texto con IA configurada”.

## Dependencias externas y límites

- RF-26 necesita habilitación y token del administrador Moodle; el código no puede habilitar servicios del servidor.
- RF-27 necesita documentación del proveedor del sistema adicional para implementar intercambio de datos real. Por ahora abre el portal público facilitado por la usuaria.
- La autenticación es un PIN local; no hay backend, recuperación remota de cuenta, sincronización multidispositivo ni autenticación institucional.
- Las integraciones de IA y Moodle no incluyen credenciales. La IA es opcional y el planificador local funciona sin ella.
- La instalación puede requerir descarga de dependencias Gradle/ML Kit. En el entorno de trabajo no se encontró Gradle Wrapper ni CLI de Gradle, por lo que esta entrega no se pudo compilar aquí.

## Referencias técnicas

- [ML Kit Text Recognition para Android](https://developers.google.com/ml-kit/vision/text-recognition/v2/android)
- [Android Calendar Provider](https://developer.android.com/identity/providers/calendar-provider)
- [Permiso de notificaciones Android](https://developer.android.com/develop/ui/views/notifications/notification-permission)
- [Moodle External Services API](https://moodledev.io/docs/4.5/apis/subsystems/external)
- [Android SpeechRecognizer](https://developer.android.com/reference/android/speech/SpeechRecognizer)
