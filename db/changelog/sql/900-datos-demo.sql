--liquibase formatted sql

--changeset adriana.laos:900-1 context:demo comment:Cuentas de demostración (contraseña tareia2026)
-- El hash bcrypt (coste 10) lo calcula PostgreSQL con pgcrypto: no hay hashes escritos en el repositorio.
INSERT INTO usuario (email, nombre, rol, password_hash) VALUES
  ('admin@demo.tareia.pe', 'Administración TARE·IA', 'admin', crypt('tareia2026', gen_salt('bf', 10))),
  ('estudiante@demo.tareia.pe', 'Estudiante Demo', 'estudiante', crypt('tareia2026', gen_salt('bf', 10)));
--rollback DELETE FROM usuario WHERE email IN ('admin@demo.tareia.pe', 'estudiante@demo.tareia.pe');

--changeset adriana.laos:900-2 context:demo comment:Cursos, actividades y horario de la estudiante demo
INSERT INTO curso (id, usuario_id, nombre, abreviatura, prioridad, dificultad)
SELECT v.id::uuid, u.id, v.nombre, v.abrev, v.prioridad, v.dificultad
  FROM usuario u,
       (VALUES ('11111111-0000-4000-8000-000000000001', 'Calidad y Pruebas de Software', 'SI-784', 5, 4),
               ('11111111-0000-4000-8000-000000000002', 'Base de Datos II', 'SI-783', 4, 4),
               ('11111111-0000-4000-8000-000000000003', 'Inteligencia de Negocios', 'SI-785', 3, 3)) AS v(id, nombre, abrev, prioridad, dificultad)
 WHERE u.email = 'estudiante@demo.tareia.pe';

INSERT INTO actividad (id, usuario_id, curso_id, titulo, tipo, origen, estado, fecha_limite, id_externo)
SELECT v.id::uuid, u.id, v.curso::uuid, v.titulo, v.tipo, v.origen, v.estado, current_date + v.dias, v.externo
  FROM usuario u,
       (VALUES ('22222222-0000-4000-8000-000000000001', '11111111-0000-4000-8000-000000000001', 'Proyecto de Unidad II', 'PROYECTO', 'INTEGRACION', 'CONFIRMADA', 6, 'moodle:784:133370'),
               ('22222222-0000-4000-8000-000000000002', '11111111-0000-4000-8000-000000000001', 'Informe de pruebas de mutación', 'ENTREGABLE', 'IA', 'PENDIENTE_VALIDAR', 3, NULL),
               ('22222222-0000-4000-8000-000000000003', '11111111-0000-4000-8000-000000000002', 'Examen parcial', 'EXAMEN', 'MANUAL', 'CONFIRMADA', 1, NULL),
               ('22222222-0000-4000-8000-000000000004', '11111111-0000-4000-8000-000000000003', 'Tarea 3: tablero de ventas', 'TAREA', 'MANUAL', 'CONFIRMADA', 10, NULL)) AS v(id, curso, titulo, tipo, origen, estado, dias, externo)
 WHERE u.email = 'estudiante@demo.tareia.pe';

INSERT INTO franja_horaria (usuario_id, dia_semana, hora_inicio, hora_fin, tipo)
SELECT u.id, v.dia, v.ini::time, v.fin::time, v.tipo
  FROM usuario u,
       (VALUES (1, '08:00', '12:00', 'CLASE'), (1, '16:00', '19:00', 'DISPONIBLE_ESTUDIO'),
               (3, '15:00', '18:00', 'DISPONIBLE_ESTUDIO'), (6, '09:00', '13:00', 'DISPONIBLE_ESTUDIO')) AS v(dia, ini, fin, tipo)
 WHERE u.email = 'estudiante@demo.tareia.pe';
--rollback DELETE FROM franja_horaria WHERE usuario_id = (SELECT id FROM usuario WHERE email = 'estudiante@demo.tareia.pe'); DELETE FROM actividad WHERE id::text LIKE '22222222-%'; DELETE FROM curso WHERE id::text LIKE '11111111-%';

--changeset adriana.laos:900-3 context:demo comment:Telemetría sintética de 30 días para el dashboard
SELECT setseed(0.42);
INSERT INTO sesion (id, usuario_id, origen, dispositivo, version_app, iniciada_en, ultima_actividad, cerrada_en, motivo_cierre)
SELECT gen_random_uuid(), x.usuario_id, 'android', 'Pixel 8 (API 35)', '1.0.0',
       x.inicio, x.inicio + make_interval(mins => x.minutos), x.inicio + make_interval(mins => x.minutos + 1), 'logout'
  FROM (SELECT u.id AS usuario_id,
               d + make_interval(hours => 8 + (random() * 12)::int) AS inicio,
               5 + (random() * 40)::int AS minutos
          FROM usuario u, generate_series(current_date - 29, current_date - 1, interval '1 day') AS d,
               generate_series(1, 3) AS n
         WHERE u.email = 'estudiante@demo.tareia.pe' AND random() < 0.7) AS x;

INSERT INTO evento (sesion_id, usuario_id, tipo, pantalla, creado_en)
SELECT s.id, s.usuario_id, t.tipo, t.pantalla, s.iniciada_en + make_interval(mins => (random() * 4)::int)
  FROM sesion s
 CROSS JOIN (VALUES ('app_abierta', NULL, 1.0), ('pantalla_vista', 'Inicio', 1.0), ('pantalla_vista', 'Tareas', 0.9),
                    ('pantalla_vista', 'Importar', 0.6), ('pantalla_vista', 'Plan', 0.5), ('pantalla_vista', 'Calendario', 0.4),
                    ('actividad_creada', 'Tareas', 0.6), ('actividad_confirmada', 'Tareas', 0.5), ('actividad_completada', 'Tareas', 0.45),
                    ('importacion_ia', 'Importar', 0.35), ('plan_generado', 'Plan', 0.3), ('plan_confirmado', 'Plan', 0.2),
                    ('calendario_sincronizado', 'Tareas', 0.25), ('moodle_importado', 'Integraciones', 0.15),
                    ('reporte_pdf', 'Reporte', 0.1)) AS t(tipo, pantalla, peso)
 WHERE random() + 0 * length(s.id::text || t.tipo) < t.peso;
REFRESH MATERIALIZED VIEW mv_uso_diario;
--rollback DELETE FROM sesion WHERE dispositivo = 'Pixel 8 (API 35)';
