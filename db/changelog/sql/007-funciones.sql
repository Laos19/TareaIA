--liquibase formatted sql

--changeset adriana.laos:007-1 splitStatements:false comment:Patrón State en la BD: transiciones válidas de una actividad
CREATE OR REPLACE FUNCTION fn_transicion_valida(desde text, hacia text) RETURNS boolean
LANGUAGE sql IMMUTABLE PARALLEL SAFE AS $$
  SELECT desde = hacia OR (desde, hacia) IN (
    ('PENDIENTE_VALIDAR', 'CONFIRMADA'),   -- confirmar()   (RN05: exige fecha, ver ck_actividad_confirmable)
    ('CONFIRMADA',        'SINCRONIZADA'), -- sincronizar() (RN01: nunca desde PENDIENTE_VALIDAR)
    ('CONFIRMADA',        'COMPLETADA'),   -- completar()
    ('SINCRONIZADA',      'COMPLETADA'),
    ('COMPLETADA',        'CONFIRMADA'),   -- reabrir()
    ('COMPLETADA',        'SINCRONIZADA')
  )
$$;
COMMENT ON FUNCTION fn_transicion_valida(text, text) IS 'Misma tabla de transiciones que las clases EstadoActividad de la app (PendienteValidar, Confirmada, Sincronizada, Completada).';
--rollback DROP FUNCTION fn_transicion_valida(text, text);

--changeset adriana.laos:007-2 splitStatements:false comment:Trigger de transición de estado
CREATE OR REPLACE FUNCTION fn_actividad_transicion() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
  IF NEW.estado IS DISTINCT FROM OLD.estado THEN
    IF NOT fn_transicion_valida(OLD.estado, NEW.estado) THEN
      RAISE EXCEPTION 'Transición no permitida: % → %', OLD.estado, NEW.estado USING ERRCODE = 'P0001';
    END IF;
    NEW.completada_en := CASE WHEN NEW.estado = 'COMPLETADA' THEN now() ELSE NULL END;
  END IF;
  RETURN NEW;
END
$$;
--rollback DROP FUNCTION fn_actividad_transicion();

--changeset adriana.laos:007-3 splitStatements:false comment:Marca de actualización y versión optimista
CREATE OR REPLACE FUNCTION fn_tocar_version() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
  NEW.version := OLD.version + 1;
  NEW.actualizado_en := now();
  RETURN NEW;
END
$$;
--rollback DROP FUNCTION fn_tocar_version();

--changeset adriana.laos:007-4 splitStatements:false comment:Historial genérico antes/después
CREATE OR REPLACE FUNCTION fn_registrar_historial() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
  fila jsonb := to_jsonb(COALESCE(NEW, OLD));
BEGIN
  -- Al borrar un usuario, sus cursos y actividades se borran en cascada: no se registra historial
  -- de un usuario que ya no existe (su historial también se borra por ON DELETE CASCADE).
  IF TG_OP = 'DELETE' AND NOT EXISTS (SELECT 1 FROM usuario WHERE id = (fila ->> 'usuario_id')::bigint) THEN
    RETURN OLD;
  END IF;
  INSERT INTO historial_cambio (tabla, registro_id, usuario_id, operacion, antes, despues)
  VALUES (TG_TABLE_NAME, fila ->> 'id', (fila ->> 'usuario_id')::bigint, left(TG_OP, 1),
          CASE WHEN TG_OP <> 'INSERT' THEN to_jsonb(OLD) END,
          CASE WHEN TG_OP <> 'DELETE' THEN to_jsonb(NEW) END);
  RETURN COALESCE(NEW, OLD);
END
$$;
--rollback DROP FUNCTION fn_registrar_historial();

--changeset adriana.laos:007-5 splitStatements:false comment:Cierre de sesiones abandonadas
CREATE OR REPLACE FUNCTION fn_cerrar_sesiones_inactivas(minutos integer DEFAULT 30) RETURNS integer
LANGUAGE plpgsql AS $$
DECLARE
  cerradas integer;
BEGIN
  UPDATE sesion
     SET cerrada_en = ultima_actividad, motivo_cierre = 'inactividad'
   WHERE cerrada_en IS NULL
     AND ultima_actividad < now() - make_interval(mins => minutos);
  GET DIAGNOSTICS cerradas = ROW_COUNT;
  RETURN cerradas;
END
$$;
COMMENT ON FUNCTION fn_cerrar_sesiones_inactivas(integer) IS 'Cierra las sesiones sin actividad en los últimos N minutos. Devuelve cuántas cerró.';
--rollback DROP FUNCTION fn_cerrar_sesiones_inactivas(integer);

--changeset adriana.laos:007-6 splitStatements:false comment:Confirmar un plan (Prototype: la propuesta reemplaza al vigente)
CREATE OR REPLACE FUNCTION fn_confirmar_plan(p_plan uuid, p_usuario bigint) RETURNS void
LANGUAGE plpgsql AS $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM plan_estudio WHERE id = p_plan AND usuario_id = p_usuario) THEN
    RAISE EXCEPTION 'Plan no encontrado' USING ERRCODE = 'P0002';
  END IF;
  UPDATE plan_estudio SET estado = 'REEMPLAZADO'
   WHERE usuario_id = p_usuario AND estado = 'VIGENTE' AND id <> p_plan;
  UPDATE plan_estudio SET estado = 'VIGENTE' WHERE id = p_plan;
END
$$;
COMMENT ON FUNCTION fn_confirmar_plan(uuid, bigint) IS 'Marca el plan como VIGENTE y el anterior como REEMPLAZADO en una sola transacción.';
--rollback DROP FUNCTION fn_confirmar_plan(uuid, bigint);
