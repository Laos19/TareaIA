--liquibase formatted sql

--changeset adriana.laos:002-1 comment:Usuarios y sesiones
CREATE TABLE usuario (
  id              bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  firebase_uid    varchar(128) UNIQUE,
  email           varchar(254) NOT NULL,
  nombre          varchar(120) NOT NULL,
  rol             varchar(12)  NOT NULL DEFAULT 'estudiante' CHECK (rol IN ('estudiante', 'admin')),
  password_hash   varchar(100),
  activo          boolean      NOT NULL DEFAULT true,
  creado_en       timestamptz  NOT NULL DEFAULT now(),
  ultimo_acceso   timestamptz,
  CONSTRAINT ck_usuario_email CHECK (email ~* '^[^@\s]+@[^@\s]+\.[^@\s]+$'),
  CONSTRAINT ck_usuario_credencial CHECK (firebase_uid IS NOT NULL OR password_hash IS NOT NULL)
);
CREATE UNIQUE INDEX ux_usuario_email ON usuario (lower(email));
COMMENT ON TABLE usuario IS 'Estudiante (entra con Google mediante Firebase Auth) o administrador del dashboard (correo y contraseña).';
COMMENT ON COLUMN usuario.firebase_uid IS 'UID de Firebase Authentication; nulo para cuentas locales de administración.';
COMMENT ON COLUMN usuario.password_hash IS 'Hash bcrypt; solo para cuentas de administración y demostración.';

CREATE TABLE sesion (
  id                uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
  usuario_id        bigint      REFERENCES usuario (id) ON DELETE CASCADE,
  origen            varchar(10) NOT NULL DEFAULT 'android' CHECK (origen IN ('android', 'web', 'mcp', 'claude')),
  dispositivo       varchar(80),
  version_app       varchar(20),
  iniciada_en       timestamptz NOT NULL DEFAULT now(),
  ultima_actividad  timestamptz NOT NULL DEFAULT now(),
  cerrada_en        timestamptz,
  motivo_cierre     varchar(20) CHECK (motivo_cierre IN ('logout', 'expirada', 'inactividad')),
  CONSTRAINT ck_sesion_fechas CHECK (ultima_actividad >= iniciada_en AND (cerrada_en IS NULL OR cerrada_en >= iniciada_en))
);
CREATE INDEX ix_sesion_inicio ON sesion USING brin (iniciada_en);
CREATE INDEX ix_sesion_usuario ON sesion (usuario_id);
COMMENT ON TABLE sesion IS 'Sesión de uso: se abre al iniciar sesión, se renueva con cada petición y se cierra con logout o inactividad.';
COMMENT ON COLUMN sesion.ultima_actividad IS 'Se actualiza en cada petición autenticada; fn_cerrar_sesiones_inactivas cierra las abandonadas.';
--rollback DROP TABLE sesion; DROP TABLE usuario;
