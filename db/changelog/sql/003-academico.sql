--liquibase formatted sql

--changeset adriana.laos:003-1 comment:Cursos, actividades académicas y apuntes (copia en la nube del modelo de dominio de la app)
CREATE TABLE curso (
  id              uuid         PRIMARY KEY,
  usuario_id      bigint       NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
  nombre          varchar(120) NOT NULL CHECK (btrim(nombre) <> ''),
  abreviatura     varchar(12),
  prioridad       smallint     NOT NULL DEFAULT 3 CHECK (prioridad BETWEEN 1 AND 5),
  dificultad      smallint     NOT NULL DEFAULT 3 CHECK (dificultad BETWEEN 1 AND 5),
  eliminado_en    timestamptz,
  version         integer      NOT NULL DEFAULT 1,
  creado_en       timestamptz  NOT NULL DEFAULT now(),
  actualizado_en  timestamptz  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_curso_nombre ON curso (usuario_id, fn_normalizar(nombre)) WHERE eliminado_en IS NULL;
COMMENT ON TABLE curso IS 'Curso del estudiante. El id (UUID) lo genera la app para poder crear sin conexión.';
COMMENT ON COLUMN curso.eliminado_en IS 'Papelera (RF-21): si no es nulo, el curso está en la papelera y puede restaurarse.';
COMMENT ON COLUMN curso.version IS 'Contador de cambios para sincronización optimista (lo incrementa un trigger).';

CREATE TABLE actividad (
  id                    uuid         PRIMARY KEY,
  usuario_id            bigint       NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
  curso_id              uuid         REFERENCES curso (id) ON DELETE SET NULL,
  titulo                varchar(200) NOT NULL CHECK (btrim(titulo) <> ''),
  descripcion           text         NOT NULL DEFAULT '',
  tipo                  varchar(12)  NOT NULL DEFAULT 'TAREA' CHECK (tipo IN ('TAREA', 'EXAMEN', 'ENTREGABLE', 'PROYECTO')),
  origen                varchar(12)  NOT NULL DEFAULT 'MANUAL' CHECK (origen IN ('IA', 'MANUAL', 'INTEGRACION')),
  estado                varchar(20)  NOT NULL DEFAULT 'PENDIENTE_VALIDAR'
                        CHECK (estado IN ('PENDIENTE_VALIDAR', 'CONFIRMADA', 'SINCRONIZADA', 'COMPLETADA')),
  fecha_limite          date,
  id_externo            varchar(120),
  evento_calendario_id  varchar(200),
  completada_en         timestamptz,
  eliminado_en          timestamptz,
  version               integer      NOT NULL DEFAULT 1,
  creado_en             timestamptz  NOT NULL DEFAULT now(),
  actualizado_en        timestamptz  NOT NULL DEFAULT now(),
  CONSTRAINT ck_actividad_confirmable CHECK (estado = 'PENDIENTE_VALIDAR' OR fecha_limite IS NOT NULL)
);
CREATE UNIQUE INDEX ux_actividad_externa ON actividad (usuario_id, id_externo) WHERE id_externo IS NOT NULL AND eliminado_en IS NULL;
COMMENT ON TABLE actividad IS 'Actividad académica (tarea, examen, entregable o proyecto). Refleja ActividadAcademica del dominio Android.';
COMMENT ON COLUMN actividad.estado IS 'Patrón State: las transiciones válidas las controla el trigger tg_actividad_transicion (RN01, RN05).';
COMMENT ON COLUMN actividad.origen IS 'IA y Moodle (INTEGRACION) nacen PENDIENTE_VALIDAR (RN09).';
COMMENT ON COLUMN actividad.id_externo IS 'Identificador en el sistema de origen (p. ej. moodle:curso:tarea); evita importar dos veces.';
COMMENT ON CONSTRAINT ck_actividad_confirmable ON actividad IS 'RN05: solo se confirma una actividad con fecha límite.';

CREATE TABLE apunte (
  id              uuid         PRIMARY KEY,
  usuario_id      bigint       NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
  curso_id        uuid         REFERENCES curso (id) ON DELETE SET NULL,
  carpeta         varchar(80),
  titulo          varchar(200) NOT NULL CHECK (btrim(titulo) <> ''),
  tipo            varchar(8)   NOT NULL DEFAULT 'TEXTO' CHECK (tipo IN ('PDF', 'IMAGEN', 'TEXTO')),
  contenido       text         NOT NULL DEFAULT '',
  eliminado_en    timestamptz,
  version         integer      NOT NULL DEFAULT 1,
  creado_en       timestamptz  NOT NULL DEFAULT now(),
  actualizado_en  timestamptz  NOT NULL DEFAULT now()
);
COMMENT ON TABLE apunte IS 'Contenido académico importado (texto OCR de PDF/imagen o nota escrita). Sin curso = bandeja Sin clasificar (RF-20).';
--rollback DROP TABLE apunte; DROP TABLE actividad; DROP TABLE curso;
