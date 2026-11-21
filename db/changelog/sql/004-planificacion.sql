--liquibase formatted sql

--changeset adriana.laos:004-1 comment:Horario disponible y planes de estudio (patrones Prototype y Strategy)
CREATE TABLE franja_horaria (
  id            bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  usuario_id    bigint      NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
  dia_semana    smallint    NOT NULL CHECK (dia_semana BETWEEN 1 AND 7),
  hora_inicio   time        NOT NULL,
  hora_fin      time        NOT NULL,
  tipo          varchar(20) NOT NULL DEFAULT 'DISPONIBLE_ESTUDIO' CHECK (tipo IN ('CLASE', 'DISPONIBLE_ESTUDIO')),
  CONSTRAINT ck_franja_orden CHECK (hora_fin > hora_inicio)
);
COMMENT ON TABLE franja_horaria IS 'Bloque semanal del horario. Solo DISPONIBLE_ESTUDIO se usa para planificar (RN16).';

CREATE TABLE plan_estudio (
  id            uuid        PRIMARY KEY,
  usuario_id    bigint      NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
  estado        varchar(12) NOT NULL DEFAULT 'PROPUESTA' CHECK (estado IN ('PROPUESTA', 'VIGENTE', 'REEMPLAZADO')),
  estrategia    varchar(20) NOT NULL DEFAULT 'LOCAL' CHECK (estrategia IN ('LOCAL', 'IA')),
  origen_id     uuid        REFERENCES plan_estudio (id) ON DELETE SET NULL,
  creado_en     timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_plan_vigente ON plan_estudio (usuario_id) WHERE estado = 'VIGENTE';
COMMENT ON TABLE plan_estudio IS 'Plan de estudio. Una PROPUESTA es un clon (Prototype) del plan VIGENTE; al confirmarla el anterior queda REEMPLAZADO (RNF-14).';
COMMENT ON COLUMN plan_estudio.origen_id IS 'Plan del que se clonó esta propuesta (trazabilidad).';

CREATE TABLE sesion_estudio (
  id              bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  plan_id         uuid     NOT NULL REFERENCES plan_estudio (id) ON DELETE CASCADE,
  actividad_id    uuid     NOT NULL REFERENCES actividad (id) ON DELETE CASCADE,
  fecha           date     NOT NULL,
  minuto_inicio   smallint NOT NULL CHECK (minuto_inicio BETWEEN 0 AND 1439),
  duracion_min    smallint NOT NULL DEFAULT 60 CHECK (duracion_min BETWEEN 15 AND 240)
);
COMMENT ON TABLE sesion_estudio IS 'Bloque de estudio propuesto para una actividad dentro de un plan.';
--rollback DROP TABLE sesion_estudio; DROP TABLE plan_estudio; DROP TABLE franja_horaria;
