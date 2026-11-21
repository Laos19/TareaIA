--liquibase formatted sql

--changeset adriana.laos:005-1 comment:Historial de cambios (auditoría antes/después)
CREATE TABLE historial_cambio (
  id            bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  tabla         varchar(30) NOT NULL,
  registro_id   text        NOT NULL,
  usuario_id    bigint      REFERENCES usuario (id) ON DELETE CASCADE,
  operacion     char(1)     NOT NULL CHECK (operacion IN ('I', 'U', 'D')),
  antes         jsonb,
  despues       jsonb,
  cambiado_en   timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_historial_registro ON historial_cambio (tabla, registro_id, cambiado_en DESC);
CREATE INDEX ix_historial_fecha ON historial_cambio USING brin (cambiado_en);
COMMENT ON TABLE historial_cambio IS 'Historial de cada alta, cambio o borrado de cursos, actividades y apuntes (trigger fn_registrar_historial).';
COMMENT ON COLUMN historial_cambio.antes IS 'Fila anterior en JSON (nulo en altas).';
--rollback DROP TABLE historial_cambio;
