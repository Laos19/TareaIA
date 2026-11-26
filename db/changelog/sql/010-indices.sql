--liquibase formatted sql

--changeset adriana.laos:010-1 comment:Índices para claves foráneas y consultas de sincronización
CREATE INDEX ix_curso_usuario_act ON curso (usuario_id, actualizado_en);
CREATE INDEX ix_actividad_usuario_act ON actividad (usuario_id, actualizado_en);
CREATE INDEX ix_actividad_curso ON actividad (curso_id);
CREATE INDEX ix_actividad_pendiente ON actividad (usuario_id, fecha_limite) WHERE eliminado_en IS NULL AND estado <> 'COMPLETADA';
CREATE INDEX ix_apunte_usuario_act ON apunte (usuario_id, actualizado_en);
CREATE INDEX ix_apunte_curso ON apunte (curso_id);
CREATE INDEX ix_franja_usuario ON franja_horaria (usuario_id);
CREATE INDEX ix_plan_usuario ON plan_estudio (usuario_id);
CREATE INDEX ix_sesion_estudio_plan ON sesion_estudio (plan_id);
CREATE INDEX ix_sesion_estudio_actividad ON sesion_estudio (actividad_id);
CREATE INDEX ix_evento_sesion ON evento (sesion_id);
CREATE INDEX ix_evento_usuario ON evento (usuario_id);
CREATE INDEX ix_historial_usuario ON historial_cambio (usuario_id);
CREATE INDEX ix_log_cliente_sesion ON log_cliente (sesion_id);
--rollback DROP INDEX ix_log_cliente_sesion, ix_historial_usuario, ix_evento_usuario, ix_evento_sesion, ix_sesion_estudio_actividad, ix_sesion_estudio_plan, ix_plan_usuario, ix_franja_usuario, ix_apunte_curso, ix_apunte_usuario_act, ix_actividad_pendiente, ix_actividad_curso, ix_actividad_usuario_act, ix_curso_usuario_act;
