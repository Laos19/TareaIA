# Diccionario de datos — TARE·IA

> Generado automáticamente desde PostgreSQL por `scripts/docs/build-diccionario.mjs` el 2026-11-21. No editar a mano.

Motor: PostgreSQL 16 · Esquema: `public` · Migraciones: Liquibase (`db/changelog`).

## Tablas

| Tabla | Descripción |
|---|---|
| [`actividad`](#actividad) | Actividad académica (tarea, examen, entregable o proyecto). Refleja ActividadAcademica del dominio Android. |
| [`apunte`](#apunte) | Contenido académico importado (texto OCR de PDF/imagen o nota escrita). Sin curso = bandeja Sin clasificar (RF-20). |
| [`curso`](#curso) | Curso del estudiante. El id (UUID) lo genera la app para poder crear sin conexión. |
| [`evento`](#evento) | Eventos de uso enviados en lotes por la app (append-only). Alimentan el dashboard de utilización. |
| [`franja_horaria`](#franja_horaria) | Bloque semanal del horario. Solo DISPONIBLE_ESTUDIO se usa para planificar (RN16). |
| [`historial_cambio`](#historial_cambio) | Historial de cada alta, cambio o borrado de cursos, actividades y apuntes (trigger fn_registrar_historial). |
| [`log_cliente`](#log_cliente) | Logs WARN/ERROR que la app Android sube para diagnosticar fallos (sin datos personales). |
| [`plan_estudio`](#plan_estudio) | Plan de estudio. Una PROPUESTA es un clon (Prototype) del plan VIGENTE; al confirmarla el anterior queda REEMPLAZADO (RNF-14). |
| [`sesion`](#sesion) | Sesión de uso: se abre al iniciar sesión, se renueva con cada petición y se cierra con logout o inactividad. |
| [`sesion_estudio`](#sesion_estudio) | Bloque de estudio propuesto para una actividad dentro de un plan. |
| [`usuario`](#usuario) | Estudiante (entra con Google mediante Firebase Auth) o administrador del dashboard (correo y contraseña). |

## actividad

Actividad académica (tarea, examen, entregable o proyecto). Refleja ActividadAcademica del dominio Android.

| Columna | Tipo | Nulo | Por defecto | Clave | Descripción |
|---|---|:---:|---|---|---|
| `id` | uuid | No |  | PK | Identificador único (clave primaria). |
| `usuario_id` | bigint | No |  | FK | Usuario dueño del registro. |
| `curso_id` | uuid | Sí |  | FK | Curso al que pertenece (nulo = sin curso). |
| `titulo` | character varying(200) | No |  |  | Título visible. |
| `descripcion` | text | No | `''::text` |  | Detalle libre. |
| `tipo` | character varying(12) | No | `'TAREA'::character varying` |  | Clasificación del registro (ver CHECK). |
| `origen` | character varying(12) | No | `'MANUAL'::character varying` |  | IA y Moodle (INTEGRACION) nacen PENDIENTE_VALIDAR (RN09). |
| `estado` | character varying(20) | No | `'PENDIENTE_VALIDAR'::character varying` |  | Patrón State: las transiciones válidas las controla el trigger tg_actividad_transicion (RN01, RN05). |
| `fecha_limite` | date | Sí |  |  | Fecha de entrega. |
| `id_externo` | character varying(120) | Sí |  |  | Identificador en el sistema de origen (p. ej. moodle:curso:tarea); evita importar dos veces. |
| `evento_calendario_id` | character varying(200) | Sí |  |  | Id del evento creado en Google Calendar. |
| `completada_en` | timestamp with time zone | Sí |  |  | Momento en que se marcó como completada (lo fija el trigger de estado). |
| `eliminado_en` | timestamp with time zone | Sí |  |  | Papelera: fecha de envío a la papelera; nulo si está activo. |
| `version` | integer | No | `1` |  | Versión optimista para sincronización (la incrementa un trigger). |
| `creado_en` | timestamp with time zone | No | `now()` |  | Fecha y hora de creación. |
| `actualizado_en` | timestamp with time zone | No | `now()` |  | Fecha y hora del último cambio (la actualiza un trigger). |

**Restricciones**

| Nombre | Tipo | Definición |
|---|---|---|
| `actividad_estado_check` | CHECK | `CHECK (((estado)::text = ANY ((ARRAY['PENDIENTE_VALIDAR'::character varying, 'CONFIRMADA'::character varying, 'SINCRONIZADA'::character varying, 'COMPLETADA'::character varying])::text[])))` |
| `actividad_origen_check` | CHECK | `CHECK (((origen)::text = ANY ((ARRAY['IA'::character varying, 'MANUAL'::character varying, 'INTEGRACION'::character varying])::text[])))` |
| `actividad_tipo_check` | CHECK | `CHECK (((tipo)::text = ANY ((ARRAY['TAREA'::character varying, 'EXAMEN'::character varying, 'ENTREGABLE'::character varying, 'PROYECTO'::character varying])::text[])))` |
| `actividad_titulo_check` | CHECK | `CHECK ((btrim((titulo)::text) <> ''::text))` |
| `ck_actividad_confirmable` | CHECK | `CHECK ((((estado)::text = 'PENDIENTE_VALIDAR'::text) OR (fecha_limite IS NOT NULL)))` |
| `actividad_curso_id_fkey` | FOREIGN KEY | `FOREIGN KEY (curso_id) REFERENCES curso(id) ON DELETE SET NULL` |
| `actividad_usuario_id_fkey` | FOREIGN KEY | `FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE` |

**Índices**

| Índice | Definición |
|---|---|
| `actividad_pkey` | `UNIQUE INDEX actividad_pkey ON actividad USING btree (id)` |
| `ix_actividad_curso` | `INDEX ix_actividad_curso ON actividad USING btree (curso_id)` |
| `ix_actividad_pendiente` | `INDEX ix_actividad_pendiente ON actividad USING btree (usuario_id, fecha_limite) WHERE ((eliminado_en IS NULL) AND ((estado)::text <> 'COMPLETADA'::text))` |
| `ix_actividad_usuario_act` | `INDEX ix_actividad_usuario_act ON actividad USING btree (usuario_id, actualizado_en)` |
| `ux_actividad_externa` | `UNIQUE INDEX ux_actividad_externa ON actividad USING btree (usuario_id, id_externo) WHERE ((id_externo IS NOT NULL) AND (eliminado_en IS NULL))` |

## apunte

Contenido académico importado (texto OCR de PDF/imagen o nota escrita). Sin curso = bandeja Sin clasificar (RF-20).

| Columna | Tipo | Nulo | Por defecto | Clave | Descripción |
|---|---|:---:|---|---|---|
| `id` | uuid | No |  | PK | Identificador único (clave primaria). |
| `usuario_id` | bigint | No |  | FK | Usuario dueño del registro. |
| `curso_id` | uuid | Sí |  | FK | Curso al que pertenece (nulo = sin curso). |
| `carpeta` | character varying(80) | Sí |  |  | Carpeta libre del apunte. |
| `titulo` | character varying(200) | No |  |  | Título visible. |
| `tipo` | character varying(8) | No | `'TEXTO'::character varying` |  | Clasificación del registro (ver CHECK). |
| `contenido` | text | No | `''::text` |  | Texto del apunte (OCR o escrito). |
| `eliminado_en` | timestamp with time zone | Sí |  |  | Papelera: fecha de envío a la papelera; nulo si está activo. |
| `version` | integer | No | `1` |  | Versión optimista para sincronización (la incrementa un trigger). |
| `creado_en` | timestamp with time zone | No | `now()` |  | Fecha y hora de creación. |
| `actualizado_en` | timestamp with time zone | No | `now()` |  | Fecha y hora del último cambio (la actualiza un trigger). |

**Restricciones**

| Nombre | Tipo | Definición |
|---|---|---|
| `apunte_tipo_check` | CHECK | `CHECK (((tipo)::text = ANY ((ARRAY['PDF'::character varying, 'IMAGEN'::character varying, 'TEXTO'::character varying])::text[])))` |
| `apunte_titulo_check` | CHECK | `CHECK ((btrim((titulo)::text) <> ''::text))` |
| `apunte_curso_id_fkey` | FOREIGN KEY | `FOREIGN KEY (curso_id) REFERENCES curso(id) ON DELETE SET NULL` |
| `apunte_usuario_id_fkey` | FOREIGN KEY | `FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE` |

**Índices**

| Índice | Definición |
|---|---|
| `apunte_pkey` | `UNIQUE INDEX apunte_pkey ON apunte USING btree (id)` |
| `ix_apunte_curso` | `INDEX ix_apunte_curso ON apunte USING btree (curso_id)` |
| `ix_apunte_usuario_act` | `INDEX ix_apunte_usuario_act ON apunte USING btree (usuario_id, actualizado_en)` |

## curso

Curso del estudiante. El id (UUID) lo genera la app para poder crear sin conexión.

| Columna | Tipo | Nulo | Por defecto | Clave | Descripción |
|---|---|:---:|---|---|---|
| `id` | uuid | No |  | PK | Identificador único (clave primaria). |
| `usuario_id` | bigint | No |  | FK | Usuario dueño del registro. |
| `nombre` | character varying(120) | No |  |  | Nombre visible. |
| `abreviatura` | character varying(12) | Sí |  |  | Código corto del curso (p. ej. SI-784). |
| `prioridad` | smallint | No | `3` |  | Prioridad del curso de 1 a 5. |
| `dificultad` | smallint | No | `3` |  | Dificultad del curso de 1 a 5. |
| `eliminado_en` | timestamp with time zone | Sí |  |  | Papelera (RF-21): si no es nulo, el curso está en la papelera y puede restaurarse. |
| `version` | integer | No | `1` |  | Contador de cambios para sincronización optimista (lo incrementa un trigger). |
| `creado_en` | timestamp with time zone | No | `now()` |  | Fecha y hora de creación. |
| `actualizado_en` | timestamp with time zone | No | `now()` |  | Fecha y hora del último cambio (la actualiza un trigger). |

**Restricciones**

| Nombre | Tipo | Definición |
|---|---|---|
| `curso_dificultad_check` | CHECK | `CHECK (((dificultad >= 1) AND (dificultad <= 5)))` |
| `curso_nombre_check` | CHECK | `CHECK ((btrim((nombre)::text) <> ''::text))` |
| `curso_prioridad_check` | CHECK | `CHECK (((prioridad >= 1) AND (prioridad <= 5)))` |
| `curso_usuario_id_fkey` | FOREIGN KEY | `FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE` |

**Índices**

| Índice | Definición |
|---|---|
| `curso_pkey` | `UNIQUE INDEX curso_pkey ON curso USING btree (id)` |
| `ix_curso_usuario_act` | `INDEX ix_curso_usuario_act ON curso USING btree (usuario_id, actualizado_en)` |
| `ux_curso_nombre` | `UNIQUE INDEX ux_curso_nombre ON curso USING btree (usuario_id, fn_normalizar((nombre)::text)) WHERE (eliminado_en IS NULL)` |

## evento

Eventos de uso enviados en lotes por la app (append-only). Alimentan el dashboard de utilización.

| Columna | Tipo | Nulo | Por defecto | Clave | Descripción |
|---|---|:---:|---|---|---|
| `id` | bigint | No | `identity` | PK | Identificador único (clave primaria). |
| `sesion_id` | uuid | Sí |  | FK | Sesión de uso en la que ocurrió. |
| `usuario_id` | bigint | Sí |  | FK | Usuario dueño del registro. |
| `tipo` | character varying(30) | No |  |  | Clasificación del registro (ver CHECK). |
| `pantalla` | character varying(30) | Sí |  |  | Pantalla de la app donde ocurrió. |
| `datos` | jsonb | No | `'{}'::jsonb` |  | Datos libres sin información personal (p. ej. {"origen":"IA","propuestas":3}). |
| `creado_en` | timestamp with time zone | No | `now()` |  | Fecha y hora de creación. |

**Restricciones**

| Nombre | Tipo | Definición |
|---|---|---|
| `evento_tipo_check` | CHECK | `CHECK (((tipo)::text = ANY ((ARRAY['app_abierta'::character varying, 'pantalla_vista'::character varying, 'actividad_creada'::character varying, 'actividad_confirmada'::character varying, 'actividad_completada'::character varying, 'actividad_eliminada'::character varying, 'importacion_iniciada'::character varying, 'importacion_ia'::character varying, 'plan_generado'::character varying, 'plan_confirmado'::character varying, 'calendario_sincronizado'::character varying, 'moodle_importado'::character varying, 'reporte_pdf'::character varying, 'recordatorio_mostrado'::character varying, 'error_cliente'::character varying])::text[])))` |
| `evento_sesion_id_fkey` | FOREIGN KEY | `FOREIGN KEY (sesion_id) REFERENCES sesion(id) ON DELETE CASCADE` |
| `evento_usuario_id_fkey` | FOREIGN KEY | `FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE SET NULL` |

**Índices**

| Índice | Definición |
|---|---|
| `evento_pkey` | `UNIQUE INDEX evento_pkey ON evento USING btree (id)` |
| `ix_evento_creado` | `INDEX ix_evento_creado ON evento USING brin (creado_en)` |
| `ix_evento_sesion` | `INDEX ix_evento_sesion ON evento USING btree (sesion_id)` |
| `ix_evento_tipo_fecha` | `INDEX ix_evento_tipo_fecha ON evento USING btree (tipo, creado_en)` |
| `ix_evento_usuario` | `INDEX ix_evento_usuario ON evento USING btree (usuario_id)` |

## franja_horaria

Bloque semanal del horario. Solo DISPONIBLE_ESTUDIO se usa para planificar (RN16).

| Columna | Tipo | Nulo | Por defecto | Clave | Descripción |
|---|---|:---:|---|---|---|
| `id` | bigint | No | `identity` | PK | Identificador único (clave primaria). |
| `usuario_id` | bigint | No |  | FK | Usuario dueño del registro. |
| `dia_semana` | smallint | No |  |  | Día ISO (1 = lunes … 7 = domingo). |
| `hora_inicio` | time without time zone | No |  |  | Hora de inicio. |
| `hora_fin` | time without time zone | No |  |  | Hora de fin (posterior al inicio). |
| `tipo` | character varying(20) | No | `'DISPONIBLE_ESTUDIO'::character varying` |  | Clasificación del registro (ver CHECK). |

**Restricciones**

| Nombre | Tipo | Definición |
|---|---|---|
| `ck_franja_orden` | CHECK | `CHECK ((hora_fin > hora_inicio))` |
| `franja_horaria_dia_semana_check` | CHECK | `CHECK (((dia_semana >= 1) AND (dia_semana <= 7)))` |
| `franja_horaria_tipo_check` | CHECK | `CHECK (((tipo)::text = ANY ((ARRAY['CLASE'::character varying, 'DISPONIBLE_ESTUDIO'::character varying])::text[])))` |
| `franja_horaria_usuario_id_fkey` | FOREIGN KEY | `FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE` |

**Índices**

| Índice | Definición |
|---|---|
| `franja_horaria_pkey` | `UNIQUE INDEX franja_horaria_pkey ON franja_horaria USING btree (id)` |
| `ix_franja_usuario` | `INDEX ix_franja_usuario ON franja_horaria USING btree (usuario_id)` |

## historial_cambio

Historial de cada alta, cambio o borrado de cursos, actividades y apuntes (trigger fn_registrar_historial).

| Columna | Tipo | Nulo | Por defecto | Clave | Descripción |
|---|---|:---:|---|---|---|
| `id` | bigint | No | `identity` | PK | Identificador único (clave primaria). |
| `tabla` | character varying(30) | No |  |  | Tabla auditada. |
| `registro_id` | text | No |  |  | Id del registro auditado. |
| `usuario_id` | bigint | Sí |  | FK | Usuario dueño del registro. |
| `operacion` | character(1) | No |  |  | I = alta, U = cambio, D = borrado. |
| `antes` | jsonb | Sí |  |  | Fila anterior en JSON (nulo en altas). |
| `despues` | jsonb | Sí |  |  | Fila nueva en JSON (nula en borrados). |
| `cambiado_en` | timestamp with time zone | No | `now()` |  | Momento del cambio. |

**Restricciones**

| Nombre | Tipo | Definición |
|---|---|---|
| `historial_cambio_operacion_check` | CHECK | `CHECK ((operacion = ANY (ARRAY['I'::bpchar, 'U'::bpchar, 'D'::bpchar])))` |
| `historial_cambio_usuario_id_fkey` | FOREIGN KEY | `FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE` |

**Índices**

| Índice | Definición |
|---|---|
| `historial_cambio_pkey` | `UNIQUE INDEX historial_cambio_pkey ON historial_cambio USING btree (id)` |
| `ix_historial_fecha` | `INDEX ix_historial_fecha ON historial_cambio USING brin (cambiado_en)` |
| `ix_historial_registro` | `INDEX ix_historial_registro ON historial_cambio USING btree (tabla, registro_id, cambiado_en DESC)` |
| `ix_historial_usuario` | `INDEX ix_historial_usuario ON historial_cambio USING btree (usuario_id)` |

## log_cliente

Logs WARN/ERROR que la app Android sube para diagnosticar fallos (sin datos personales).

| Columna | Tipo | Nulo | Por defecto | Clave | Descripción |
|---|---|:---:|---|---|---|
| `id` | bigint | No | `identity` | PK | Identificador único (clave primaria). |
| `sesion_id` | uuid | Sí |  | FK | Sesión de uso en la que ocurrió. |
| `nivel` | character varying(5) | No |  |  | Nivel del log. |
| `etiqueta` | character varying(40) | No |  |  | Módulo que generó el log. |
| `mensaje` | character varying(500) | No |  |  | Mensaje (sin datos personales). |
| `version_app` | character varying(20) | Sí |  |  | Versión de la app que generó el dato. |
| `creado_en` | timestamp with time zone | No | `now()` |  | Fecha y hora de creación. |

**Restricciones**

| Nombre | Tipo | Definición |
|---|---|---|
| `log_cliente_nivel_check` | CHECK | `CHECK (((nivel)::text = ANY ((ARRAY['DEBUG'::character varying, 'INFO'::character varying, 'WARN'::character varying, 'ERROR'::character varying])::text[])))` |
| `log_cliente_sesion_id_fkey` | FOREIGN KEY | `FOREIGN KEY (sesion_id) REFERENCES sesion(id) ON DELETE CASCADE` |

**Índices**

| Índice | Definición |
|---|---|
| `ix_log_cliente_fecha` | `INDEX ix_log_cliente_fecha ON log_cliente USING brin (creado_en)` |
| `ix_log_cliente_sesion` | `INDEX ix_log_cliente_sesion ON log_cliente USING btree (sesion_id)` |
| `log_cliente_pkey` | `UNIQUE INDEX log_cliente_pkey ON log_cliente USING btree (id)` |

## plan_estudio

Plan de estudio. Una PROPUESTA es un clon (Prototype) del plan VIGENTE; al confirmarla el anterior queda REEMPLAZADO (RNF-14).

| Columna | Tipo | Nulo | Por defecto | Clave | Descripción |
|---|---|:---:|---|---|---|
| `id` | uuid | No |  | PK | Identificador único (clave primaria). |
| `usuario_id` | bigint | No |  | FK | Usuario dueño del registro. |
| `estado` | character varying(12) | No | `'PROPUESTA'::character varying` |  |  |
| `estrategia` | character varying(20) | No | `'LOCAL'::character varying` |  | Estrategia de planificación usada (patrón Strategy). |
| `origen_id` | uuid | Sí |  | FK | Plan del que se clonó esta propuesta (trazabilidad). |
| `creado_en` | timestamp with time zone | No | `now()` |  | Fecha y hora de creación. |

**Restricciones**

| Nombre | Tipo | Definición |
|---|---|---|
| `plan_estudio_estado_check` | CHECK | `CHECK (((estado)::text = ANY ((ARRAY['PROPUESTA'::character varying, 'VIGENTE'::character varying, 'REEMPLAZADO'::character varying])::text[])))` |
| `plan_estudio_estrategia_check` | CHECK | `CHECK (((estrategia)::text = ANY ((ARRAY['LOCAL'::character varying, 'IA'::character varying])::text[])))` |
| `plan_estudio_origen_id_fkey` | FOREIGN KEY | `FOREIGN KEY (origen_id) REFERENCES plan_estudio(id) ON DELETE SET NULL` |
| `plan_estudio_usuario_id_fkey` | FOREIGN KEY | `FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE` |

**Índices**

| Índice | Definición |
|---|---|
| `ix_plan_usuario` | `INDEX ix_plan_usuario ON plan_estudio USING btree (usuario_id)` |
| `plan_estudio_pkey` | `UNIQUE INDEX plan_estudio_pkey ON plan_estudio USING btree (id)` |
| `ux_plan_vigente` | `UNIQUE INDEX ux_plan_vigente ON plan_estudio USING btree (usuario_id) WHERE ((estado)::text = 'VIGENTE'::text)` |

## sesion

Sesión de uso: se abre al iniciar sesión, se renueva con cada petición y se cierra con logout o inactividad.

| Columna | Tipo | Nulo | Por defecto | Clave | Descripción |
|---|---|:---:|---|---|---|
| `id` | uuid | No | `gen_random_uuid()` | PK | Identificador único (clave primaria). |
| `usuario_id` | bigint | Sí |  | FK | Usuario dueño del registro. |
| `origen` | character varying(10) | No | `'android'::character varying` |  | De dónde proviene el registro. |
| `dispositivo` | character varying(80) | Sí |  |  | Modelo del dispositivo o navegador. |
| `version_app` | character varying(20) | Sí |  |  | Versión de la app que generó el dato. |
| `iniciada_en` | timestamp with time zone | No | `now()` |  | Inicio de la sesión. |
| `ultima_actividad` | timestamp with time zone | No | `now()` |  | Se actualiza en cada petición autenticada; fn_cerrar_sesiones_inactivas cierra las abandonadas. |
| `cerrada_en` | timestamp with time zone | Sí |  |  | Cierre de la sesión (nulo si está abierta). |
| `motivo_cierre` | character varying(20) | Sí |  |  | logout, expirada o inactividad. |

**Restricciones**

| Nombre | Tipo | Definición |
|---|---|---|
| `ck_sesion_fechas` | CHECK | `CHECK (((ultima_actividad >= iniciada_en) AND ((cerrada_en IS NULL) OR (cerrada_en >= iniciada_en))))` |
| `sesion_motivo_cierre_check` | CHECK | `CHECK (((motivo_cierre)::text = ANY ((ARRAY['logout'::character varying, 'expirada'::character varying, 'inactividad'::character varying])::text[])))` |
| `sesion_origen_check` | CHECK | `CHECK (((origen)::text = ANY ((ARRAY['android'::character varying, 'web'::character varying, 'mcp'::character varying, 'claude'::character varying])::text[])))` |
| `sesion_usuario_id_fkey` | FOREIGN KEY | `FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE` |

**Índices**

| Índice | Definición |
|---|---|
| `ix_sesion_inicio` | `INDEX ix_sesion_inicio ON sesion USING brin (iniciada_en)` |
| `ix_sesion_usuario` | `INDEX ix_sesion_usuario ON sesion USING btree (usuario_id)` |
| `sesion_pkey` | `UNIQUE INDEX sesion_pkey ON sesion USING btree (id)` |

## sesion_estudio

Bloque de estudio propuesto para una actividad dentro de un plan.

| Columna | Tipo | Nulo | Por defecto | Clave | Descripción |
|---|---|:---:|---|---|---|
| `id` | bigint | No | `identity` | PK | Identificador único (clave primaria). |
| `plan_id` | uuid | No |  | FK | Plan de estudio al que pertenece. |
| `actividad_id` | uuid | No |  | FK | Actividad académica relacionada. |
| `fecha` | date | No |  |  | Día de la sesión de estudio. |
| `minuto_inicio` | smallint | No |  |  | Minuto del día en que empieza (0–1439). |
| `duracion_min` | smallint | No | `60` |  | Duración en minutos (15–240). |

**Restricciones**

| Nombre | Tipo | Definición |
|---|---|---|
| `sesion_estudio_duracion_min_check` | CHECK | `CHECK (((duracion_min >= 15) AND (duracion_min <= 240)))` |
| `sesion_estudio_minuto_inicio_check` | CHECK | `CHECK (((minuto_inicio >= 0) AND (minuto_inicio <= 1439)))` |
| `sesion_estudio_actividad_id_fkey` | FOREIGN KEY | `FOREIGN KEY (actividad_id) REFERENCES actividad(id) ON DELETE CASCADE` |
| `sesion_estudio_plan_id_fkey` | FOREIGN KEY | `FOREIGN KEY (plan_id) REFERENCES plan_estudio(id) ON DELETE CASCADE` |

**Índices**

| Índice | Definición |
|---|---|
| `ix_sesion_estudio_actividad` | `INDEX ix_sesion_estudio_actividad ON sesion_estudio USING btree (actividad_id)` |
| `ix_sesion_estudio_plan` | `INDEX ix_sesion_estudio_plan ON sesion_estudio USING btree (plan_id)` |
| `sesion_estudio_pkey` | `UNIQUE INDEX sesion_estudio_pkey ON sesion_estudio USING btree (id)` |

## usuario

Estudiante (entra con Google mediante Firebase Auth) o administrador del dashboard (correo y contraseña).

| Columna | Tipo | Nulo | Por defecto | Clave | Descripción |
|---|---|:---:|---|---|---|
| `id` | bigint | No | `identity` | PK | Identificador único (clave primaria). |
| `firebase_uid` | character varying(128) | Sí |  |  | UID de Firebase Authentication; nulo para cuentas locales de administración. |
| `email` | character varying(254) | No |  |  | Correo electrónico (único sin distinguir mayúsculas). |
| `nombre` | character varying(120) | No |  |  | Nombre visible. |
| `rol` | character varying(12) | No | `'estudiante'::character varying` |  | Rol del usuario: estudiante o admin. |
| `password_hash` | character varying(100) | Sí |  |  | Hash bcrypt; solo para cuentas de administración y demostración. |
| `activo` | boolean | No | `true` |  | Si la cuenta está habilitada. |
| `creado_en` | timestamp with time zone | No | `now()` |  | Fecha y hora de creación. |
| `ultimo_acceso` | timestamp with time zone | Sí |  |  | Último inicio de sesión. |

**Restricciones**

| Nombre | Tipo | Definición |
|---|---|---|
| `ck_usuario_credencial` | CHECK | `CHECK (((firebase_uid IS NOT NULL) OR (password_hash IS NOT NULL)))` |
| `ck_usuario_email` | CHECK | `CHECK (((email)::text ~* '^[^@\s]+@[^@\s]+\.[^@\s]+$'::text))` |
| `usuario_rol_check` | CHECK | `CHECK (((rol)::text = ANY ((ARRAY['estudiante'::character varying, 'admin'::character varying])::text[])))` |
| `usuario_firebase_uid_key` | UNIQUE | `UNIQUE (firebase_uid)` |

**Índices**

| Índice | Definición |
|---|---|
| `usuario_firebase_uid_key` | `UNIQUE INDEX usuario_firebase_uid_key ON usuario USING btree (firebase_uid)` |
| `usuario_pkey` | `UNIQUE INDEX usuario_pkey ON usuario USING btree (id)` |
| `ux_usuario_email` | `UNIQUE INDEX ux_usuario_email ON usuario USING btree (lower((email)::text))` |

## Vistas

| Vista | Tipo | Descripción |
|---|---|---|
| `mv_uso_diario` | materializada | Caché de métricas diarias de los últimos 30 días. Se refresca con REFRESH MATERIALIZED VIEW CONCURRENTLY. |
| `v_actividad_pendiente` | vista | Actividades abiertas con días restantes y nivel de urgencia (mismas reglas que Urgencia.kt). |
| `v_origen_actividades` | vista | Cuántas actividades vienen de IA, registro manual o Moodle, por estado (mide la adopción de la importación). |
| `v_resumen_sesiones` | vista | Sesiones abiertas, de la última semana y duración media en minutos. |
| `v_uso_por_funcion` | vista | Uso de cada función de la app en los últimos 30 días. |
| `v_uso_por_pantalla` | vista | Pantallas más visitadas (30 días). |

## Funciones

| Función | Argumentos | Devuelve | Descripción |
|---|---|---|---|
| `fn_actividad_transicion` | `—` | trigger |  |
| `fn_cerrar_sesiones_inactivas` | `minutos integer DEFAULT 30` | integer | Cierra las sesiones sin actividad en los últimos N minutos. Devuelve cuántas cerró. |
| `fn_confirmar_plan` | `p_plan uuid, p_usuario bigint` | void | Marca el plan como VIGENTE y el anterior como REEMPLAZADO en una sola transacción. |
| `fn_normalizar` | `texto text` | text | Minúsculas, sin tildes y con espacios unificados. Inmutable para poder usarse en índices (detección de duplicados RN02). |
| `fn_registrar_historial` | `—` | trigger |  |
| `fn_tocar_version` | `—` | trigger |  |
| `fn_transicion_valida` | `desde text, hacia text` | boolean | Misma tabla de transiciones que las clases EstadoActividad de la app (PendienteValidar, Confirmada, Sincronizada, Completada). |

## Triggers

| Trigger | Tabla | Función |
|---|---|---|
| `tg_actividad_historial` | `actividad` | `fn_registrar_historial` |
| `tg_actividad_transicion` | `actividad` | `fn_actividad_transicion` |
| `tg_actividad_version` | `actividad` | `fn_tocar_version` |
| `tg_apunte_historial` | `apunte` | `fn_registrar_historial` |
| `tg_apunte_version` | `apunte` | `fn_tocar_version` |
| `tg_curso_historial` | `curso` | `fn_registrar_historial` |
| `tg_curso_version` | `curso` | `fn_tocar_version` |
