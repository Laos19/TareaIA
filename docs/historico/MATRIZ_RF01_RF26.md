# Matriz funcional vigente de TARE·IA — RF-01 a RF-26

Fuente del alcance: lista RF-01 a RF-26 aportada por el usuario (noviembre de 2026). Los estados son la **auditoría estática histórica** `AUDITORIA_RF01_RF27.md`, ajustada para distinguir expresamente diferencias frente a los textos nuevos. **No son pruebas ejecutadas en esta entrega.**

| RF | Requisito vigente | Evidencia en código según auditoría previa | Prueba / brecha para aceptación |
|---|---|---|---|
| RF-01 | Gestionar autenticación de usuario | Presente en código: perfil y PIN local | Definir si Google Sign-In sustituye o complementa al PIN; verificar reingreso y PIN inválido |
| RF-02 | Autorizar acceso a Google Calendar | Parcial: permisos Calendar Provider y selector de calendario editable | Verificar calendario/cuenta Google, autorización denegada/revocada y destino real |
| RF-03 | Procesar reconocimiento OCR multimodal | Parcial: ML Kit OCR de imagen/PDF; IA recibe texto | Probar documentos con imágenes, fechas y estructura visual; validar multimodal real |
| RF-04 | Extraer parámetros académicos | Parcial: extracción heurística/IA opcional | Probar tarea, entregable, examen y varias fechas en un documento |
| RF-05 | Formulario de validación | Presente en código; no probado en dispositivo | Editar fecha/curso y comprobar que nada se sincroniza sin confirmación |
| RF-06 | Creación manual de actividades | Presente en código | Crear tarea sin IA y comprobar persistencia tras reinicio |
| RF-07 | Generar y confirmar plan IA | Parcial: pantalla usa planificador local, no AiClient.plan | Validar propuesta con IA, conflictos, fecha límite y confirmación previa |
| RF-08 | Sincronizar Google Calendar | Parcial: Calendar Provider, depende del calendario elegido | Comprobar evento en Google remoto, actualización y ausencia de duplicados |
| RF-09 | Notificaciones y alertas | Parcial: avisos de antelación fija | Configurar/desactivar por actividad, reiniciar y verificar reprogramación |
| RF-10 | Seguimiento académico por curso | Parcial: próximos eventos y filtro por estado | Filtrar por curso y pendientes de validar, incluidas sin clasificar |
| RF-11 | Prevenir duplicados | Parcial: detecta y omite/rechaza | Mostrar Sobrescribir/Cancelar y mantener IDs/avisos/sesiones |
| RF-12 | Permisos del dispositivo | Parcial: calendario, micro, avisos; picker del sistema | Probar concedido, denegado, revocado y cancelación de picker/cámara |
| RF-13 | Personalizar perfil | Presente en código | Editar y reiniciar app; comprobar persistencia |
| RF-14 | Configurar cursos y abreviaturas | Presente en código | Comprobar abreviaturas únicas y clasificación por abreviatura |
| RF-15 | Compartir desde otras apps | Presente en código mediante intents | Compartir texto, imagen y PDF reales desde otras apps |
| RF-16 | Asignar apuntes en carpetas | Parcial: agrupación/editado de curso y carpeta | Clasificar automáticamente según curso; manejar curso indeterminado |
| RF-17 | Configurar horario | Parcial: bloques manuales de disponibilidad | Aclarar importación de horario; importar/revisar y evitar cruces |
| RF-18 | Personalizar priorización de estudio | Presente en código: prioridad/dificultad alimentan planificador | Comparar casos equivalentes y comprobar reparto de sesiones |
| RF-19 | Gestionar cambios de cronograma | Parcial: recalcular tras editar manualmente | Importar cambio externo y permitir confirmar/modificar/descartar |
| RF-20 | Contenido sin clasificar | Parcial: apuntes y tareas pendientes por validar | Mostrar todas las entradas sin curso y mover tras clasificación |
| RF-21 | Papelera | Presente en código: restaurar/eliminar | Eliminar y restaurar curso/apuntes y relaciones; purga irreversible |
| RF-22 | Importar PDF, imágenes y capturas | Presente en código | Probar PDF, captura, imagen y formatos inválidos |
| RF-23 | Gestionar anotaciones | Presente en código | Crear/editar/mover nota y comprobar persistencia |
| RF-24 | Planificación mediante audio IA | Parcial respecto al texto vigente: dictado+planificador local | Probar voz, transcripción, extracción y plan mediante IA si se exige |
| RF-25 | Generar reporte PDF | Presente en código | Exportar, abrir y cotejar tareas/fechas/estados filtrados |
| RF-26 | Integración Moodle | Presente en código, pendiente de servidor autorizado | Consultar cursos/tareas con token válido y repetir sin duplicar |

## Diferencias importantes a resolver

- **RF-27:** existe en documentación original como acceso a portal, pero no aparece en la nueva lista del usuario. No se borra código existente; queda fuera del alcance RF-01–RF-26 hasta una decisión formal.
- **RF-01:** el documento de Visión propone acceso Google; la auditoría reporta PIN local. Elegir comportamiento definitivo.
- **RF-02/08:** Calendar Provider local no acredita por sí mismo OAuth Google directo ni confirma sincronización remota.
- **RF-03/07/24:** OCR ML Kit, planificación local y dictado no equivalen necesariamente a OCR multimodal y planificación **mediante IA** descritos en los RF actuales.
- **RF-17:** bloques de disponibilidad manuales no demuestran importación de un horario desde archivo.
- **RF-26:** el Documento de Visión anterior excluía Moodle en la primera versión, mientras la lista actual lo incluye. La lista actual prevalece para este plan; actualizar el Documento de Visión si se necesita trazabilidad académica formal.
- **La app no tiene una certificación RF actual**: la auditoría previa reportó pruebas unitarias `NO-SOURCE`, avisos de Lint y ausencia de pruebas end-to-end y credenciales de integración.

## Cómo cerrar cada RF

Para cada RF guardar: identificador, caso y datos de prueba, entorno (emulador/teléfono y API), resultado real, evidencia, defectos y persona revisora. `Presente en código` no equivale a `Aceptado`.

## Referencias

- `AUDITORIA_RF01_RF27.md` (documento original inalterado).
- `PLAN_ADOPCION_SKILLS.md` (documento original inalterado).
- `PLAN_ADOPCION_ANDROID_COMPOSE.md` (adaptación móvil).
- `docs/referencias/FD02_DOCUMENTO_VISION.md` (visión histórica).
