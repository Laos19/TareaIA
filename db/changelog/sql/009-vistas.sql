--liquibase formatted sql

--changeset adriana.laos:009-1 comment:Vista de actividades pendientes con urgencia
CREATE VIEW v_actividad_pendiente AS
SELECT a.id, a.usuario_id, a.titulo, a.tipo, a.estado, a.origen, a.fecha_limite,
       c.nombre AS curso,
       (a.fecha_limite - current_date) AS dias_restantes,
       CASE
         WHEN a.fecha_limite IS NULL THEN 'SIN_FECHA'
         WHEN a.fecha_limite < current_date THEN 'VENCIDA'
         WHEN a.fecha_limite - current_date <= 1 THEN 'CRITICA'
         WHEN a.fecha_limite - current_date <= 3 THEN 'ALTA'
         WHEN a.fecha_limite - current_date <= 7 THEN 'MEDIA'
         ELSE 'BAJA'
       END AS urgencia
  FROM actividad a
  LEFT JOIN curso c ON c.id = a.curso_id
 WHERE a.eliminado_en IS NULL AND a.estado <> 'COMPLETADA';
COMMENT ON VIEW v_actividad_pendiente IS 'Actividades abiertas con días restantes y nivel de urgencia (mismas reglas que Urgencia.kt).';
--rollback DROP VIEW v_actividad_pendiente;

--changeset adriana.laos:009-2 comment:Vistas del dashboard de utilización
CREATE MATERIALIZED VIEW mv_uso_diario AS
SELECT d::date AS dia,
       (SELECT count(*) FROM sesion s WHERE s.iniciada_en::date = d::date) AS sesiones,
       (SELECT count(DISTINCT s.usuario_id) FROM sesion s WHERE s.iniciada_en::date = d::date) AS usuarios_activos,
       (SELECT count(*) FROM evento e WHERE e.creado_en::date = d::date) AS eventos,
       (SELECT count(*) FROM actividad a WHERE a.creado_en::date = d::date) AS actividades_creadas,
       (SELECT count(*) FROM actividad a WHERE a.completada_en::date = d::date) AS actividades_completadas
  FROM generate_series(current_date - 29, current_date, interval '1 day') AS d;
CREATE UNIQUE INDEX ux_mv_uso_diario ON mv_uso_diario (dia);
COMMENT ON MATERIALIZED VIEW mv_uso_diario IS 'Caché de métricas diarias de los últimos 30 días. Se refresca con REFRESH MATERIALIZED VIEW CONCURRENTLY.';

CREATE VIEW v_uso_por_funcion AS
SELECT tipo, count(*) AS total, count(DISTINCT usuario_id) AS usuarios, max(creado_en) AS ultimo_uso
  FROM evento
 WHERE creado_en >= now() - interval '30 days'
 GROUP BY tipo;
COMMENT ON VIEW v_uso_por_funcion IS 'Uso de cada función de la app en los últimos 30 días.';

CREATE VIEW v_uso_por_pantalla AS
SELECT pantalla, count(*) AS vistas, count(DISTINCT sesion_id) AS sesiones
  FROM evento
 WHERE tipo = 'pantalla_vista' AND pantalla IS NOT NULL AND creado_en >= now() - interval '30 days'
 GROUP BY pantalla;
COMMENT ON VIEW v_uso_por_pantalla IS 'Pantallas más visitadas (30 días).';

CREATE VIEW v_origen_actividades AS
SELECT origen, estado, count(*) AS total
  FROM actividad
 WHERE eliminado_en IS NULL
 GROUP BY origen, estado;
COMMENT ON VIEW v_origen_actividades IS 'Cuántas actividades vienen de IA, registro manual o Moodle, por estado (mide la adopción de la importación).';

CREATE VIEW v_resumen_sesiones AS
SELECT count(*) FILTER (WHERE cerrada_en IS NULL) AS abiertas,
       count(*) FILTER (WHERE iniciada_en >= now() - interval '7 days') AS ultimos_7_dias,
       round(avg(extract(epoch FROM (coalesce(cerrada_en, ultima_actividad) - iniciada_en)) / 60)::numeric, 1) AS duracion_media_min
  FROM sesion;
COMMENT ON VIEW v_resumen_sesiones IS 'Sesiones abiertas, de la última semana y duración media en minutos.';
--rollback DROP VIEW v_resumen_sesiones; DROP VIEW v_origen_actividades; DROP VIEW v_uso_por_pantalla; DROP VIEW v_uso_por_funcion; DROP MATERIALIZED VIEW mv_uso_diario;
