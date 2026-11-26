--liquibase formatted sql

--changeset adriana.laos:006-1 comment:Telemetría de producto y logs de la aplicación
CREATE TABLE evento (
  id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  sesion_id   uuid        REFERENCES sesion (id) ON DELETE CASCADE,
  usuario_id  bigint      REFERENCES usuario (id) ON DELETE SET NULL,
  tipo        varchar(30) NOT NULL CHECK (tipo IN (
                'app_abierta', 'pantalla_vista', 'actividad_creada', 'actividad_confirmada', 'actividad_completada',
                'actividad_eliminada', 'importacion_iniciada', 'importacion_ia', 'plan_generado', 'plan_confirmado',
                'calendario_sincronizado', 'moodle_importado', 'reporte_pdf', 'recordatorio_mostrado', 'error_cliente')),
  pantalla    varchar(30),
  datos       jsonb       NOT NULL DEFAULT '{}'::jsonb,
  creado_en   timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_evento_creado ON evento USING brin (creado_en);
CREATE INDEX ix_evento_tipo_fecha ON evento (tipo, creado_en);
COMMENT ON TABLE evento IS 'Eventos de uso enviados en lotes por la app (append-only). Alimentan el dashboard de utilización.';
COMMENT ON COLUMN evento.datos IS 'Datos libres sin información personal (p. ej. {"origen":"IA","propuestas":3}).';

CREATE TABLE log_cliente (
  id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  sesion_id   uuid        REFERENCES sesion (id) ON DELETE CASCADE,
  nivel       varchar(5)  NOT NULL CHECK (nivel IN ('DEBUG', 'INFO', 'WARN', 'ERROR')),
  etiqueta    varchar(40) NOT NULL,
  mensaje     varchar(500) NOT NULL,
  version_app varchar(20),
  creado_en   timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_log_cliente_fecha ON log_cliente USING brin (creado_en);
COMMENT ON TABLE log_cliente IS 'Logs WARN/ERROR que la app Android sube para diagnosticar fallos (sin datos personales).';
--rollback DROP TABLE log_cliente; DROP TABLE evento;
