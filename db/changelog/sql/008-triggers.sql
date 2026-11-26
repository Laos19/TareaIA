--liquibase formatted sql

--changeset adriana.laos:008-1 comment:Triggers de estado, versión e historial
CREATE TRIGGER tg_actividad_transicion BEFORE UPDATE OF estado ON actividad
  FOR EACH ROW EXECUTE FUNCTION fn_actividad_transicion();

CREATE TRIGGER tg_curso_version BEFORE UPDATE ON curso
  FOR EACH ROW EXECUTE FUNCTION fn_tocar_version();
CREATE TRIGGER tg_actividad_version BEFORE UPDATE ON actividad
  FOR EACH ROW EXECUTE FUNCTION fn_tocar_version();
CREATE TRIGGER tg_apunte_version BEFORE UPDATE ON apunte
  FOR EACH ROW EXECUTE FUNCTION fn_tocar_version();

CREATE TRIGGER tg_curso_historial AFTER INSERT OR UPDATE OR DELETE ON curso
  FOR EACH ROW EXECUTE FUNCTION fn_registrar_historial();
CREATE TRIGGER tg_actividad_historial AFTER INSERT OR UPDATE OR DELETE ON actividad
  FOR EACH ROW EXECUTE FUNCTION fn_registrar_historial();
CREATE TRIGGER tg_apunte_historial AFTER INSERT OR UPDATE OR DELETE ON apunte
  FOR EACH ROW EXECUTE FUNCTION fn_registrar_historial();
--rollback DROP TRIGGER tg_apunte_historial ON apunte; DROP TRIGGER tg_actividad_historial ON actividad; DROP TRIGGER tg_curso_historial ON curso; DROP TRIGGER tg_apunte_version ON apunte; DROP TRIGGER tg_actividad_version ON actividad; DROP TRIGGER tg_curso_version ON curso; DROP TRIGGER tg_actividad_transicion ON actividad;
