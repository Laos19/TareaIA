import type { FastifyInstance } from 'fastify';
import { API_VERSION } from '../config.js';
import type { Accion } from '../dominio/estado.js';
import { urgencia } from '../dominio/urgencia.js';
import { HttpError } from '../errors.js';
import type { ActividadEntrada, CursoEntrada } from '../services/ActividadService.js';
import type { DatosSesion } from '../auth/AuthService.js';
import type { LineaLog } from '../repositories/TelemetriaRepository.js';
import * as S from '../schemas.js';

type ConId = { Params: { id: string } };

/**
 * Registra todas las rutas bajo /api.
 * Las rutas privadas usan `app.autenticar` (JWT + sesión abierta) o `app.requiereRol('admin')`.
 */
export async function registrarRutas(app: FastifyInstance): Promise<void> {
  const { auth, academico, actividades, telemetria, dashboard, db } = app.servicios;
  const privado = { onRequest: [app.autenticar], schema: { security: [{ bearerAuth: [] }] } };
  const admin = { onRequest: [app.requiereRol('admin')], schema: { security: [{ bearerAuth: [] }] } };

  // ─────────────── sistema ───────────────
  app.get('/health', { schema: { tags: ['sistema'], summary: 'Estado de la API, la base de datos y la caché' } }, async () => {
    const dbOk = await db.ping().catch(() => false);
    const c = dashboard.cache;
    return { estado: dbOk ? 'ok' : 'degradado', db: dbOk ? 'ok' : 'error', version: API_VERSION, cache: { entradas: c.tamano, aciertos: c.aciertos, fallos: c.fallos } };
  });

  // ─────────────── auth y sesiones ───────────────
  const responderSesion = async (r: { usuario: { id: number; rol: 'estudiante' | 'admin' }; sid: string }) => ({
    token: await app.jwt.sign({ sub: r.usuario.id, rol: r.usuario.rol, sid: r.sid }),
    sesionId: r.sid,
    usuario: r.usuario,
  });

  app.post<{ Body: { email: string; password: string } & DatosSesion }>('/auth/login', { schema: { tags: ['auth'], summary: 'Inicio de sesión con correo y contraseña', body: S.loginBody }, config: { rateLimit: { max: app.servicios.limiteLogin, timeWindow: '1 minute' } } },
    async (req) => {
      const { email, password, ...datos } = req.body;
      return responderSesion(await auth.login(email, password, datos));
    });

  app.post<{ Body: { idToken: string } & DatosSesion }>('/auth/google', { schema: { tags: ['auth'], summary: 'Inicio de sesión con el ID token de Google (Firebase)', body: S.googleBody }, config: { rateLimit: { max: app.servicios.limiteLogin * 2, timeWindow: '1 minute' } } },
    async (req) => {
      const { idToken, ...datos } = req.body;
      return responderSesion(await auth.loginGoogle(idToken, datos));
    });

  app.post('/auth/logout', { ...privado, schema: { ...privado.schema, tags: ['auth'], summary: 'Cierra la sesión actual' } }, async (req, reply) => {
    await auth.cerrarSesion(req.user.sid, req.user.sub);
    return reply.status(204).send();
  });

  app.get('/perfil', { ...privado, schema: { ...privado.schema, tags: ['auth'], summary: 'Perfil del usuario autenticado' } }, async (req) =>
    auth.perfil(req.user.sub));

  // ─────────────── cursos ───────────────
  app.get<{ Querystring: { desde?: string } }>('/cursos', { ...privado, schema: { ...privado.schema, tags: ['cursos'], querystring: S.desdeQuery } },
    async (req) => academico.listarCursos(req.user.sub, req.query.desde));

  app.put<{ Params: { id: string }; Body: Omit<CursoEntrada, 'id'> }>('/cursos/:id', { ...privado, schema: { ...privado.schema, tags: ['cursos'], summary: 'Crea o actualiza un curso', params: S.idParam, body: S.cursoBody } },
    async (req) =>
      academico.guardarCurso(req.user.sub, { ...req.body, id: req.params.id }));

  app.delete<ConId>('/cursos/:id', { ...privado, schema: { ...privado.schema, tags: ['cursos'], summary: 'Envía el curso a la papelera', params: S.idParam } },
    async (req, reply) => {
      if (!(await academico.marcarPapelera(req.user.sub, 'curso', req.params.id, true))) throw new HttpError(404, 'Curso no encontrado', 'NO_ENCONTRADO');
      return reply.status(204).send();
    });

  // ─────────────── actividades ───────────────
  app.get<{ Querystring: { desde?: string } }>('/actividades', { ...privado, schema: { ...privado.schema, tags: ['actividades'], summary: 'Actividades (opcionalmente las cambiadas desde una fecha)', querystring: S.desdeQuery } },
    async (req) =>
      (await academico.listarActividades(req.user.sub, req.query.desde)).map((a) => ({ ...a, urgencia: urgencia(a.fechaLimite) })));

  app.get('/actividades/pendientes', { ...privado, schema: { ...privado.schema, tags: ['actividades'], summary: 'Actividades abiertas con días restantes y urgencia' } },
    async (req) => academico.pendientes(req.user.sub));

  app.put<{ Params: { id: string }; Body: Omit<ActividadEntrada, 'id'> }>('/actividades/:id', { ...privado, schema: { ...privado.schema, tags: ['actividades'], summary: 'Crea o actualiza una actividad (controla duplicados RN02)', params: S.idParam, body: S.actividadBody } },
    async (req) =>
      actividades.guardar(req.user.sub, { ...req.body, id: req.params.id }));

  app.post<{ Params: { id: string }; Body: { accion: Accion } }>('/actividades/:id/estado', { ...privado, schema: { ...privado.schema, tags: ['actividades'], summary: 'Aplica una acción del ciclo de vida (patrón State)', params: S.idParam, body: S.accionBody } },
    async (req) =>
      actividades.accion(req.user.sub, req.params.id, req.body.accion));

  app.delete<ConId>('/actividades/:id', { ...privado, schema: { ...privado.schema, tags: ['actividades'], summary: 'Envía la actividad a la papelera', params: S.idParam } },
    async (req, reply) => {
      if (!(await academico.marcarPapelera(req.user.sub, 'actividad', req.params.id, true))) throw new HttpError(404, 'Actividad no encontrada', 'NO_ENCONTRADO');
      return reply.status(204).send();
    });

  app.post<ConId>('/actividades/:id/restaurar', { ...privado, schema: { ...privado.schema, tags: ['actividades'], summary: 'Restaura una actividad de la papelera', params: S.idParam } },
    async (req) => {
      if (!(await academico.marcarPapelera(req.user.sub, 'actividad', req.params.id, false))) throw new HttpError(404, 'Actividad no encontrada', 'NO_ENCONTRADO');
      return academico.buscarActividad(req.user.sub, req.params.id);
    });

  app.get<ConId>('/actividades/:id/historial', { ...privado, schema: { ...privado.schema, tags: ['actividades'], summary: 'Historial de cambios (antes/después)', params: S.idParam } },
    async (req) => academico.historial(req.user.sub, 'actividad', req.params.id));

  // ─────────────── sincronización ───────────────
  app.post<{ Body: { desde?: string; cursos: CursoEntrada[]; actividades: ActividadEntrada[] } }>('/sync', { ...privado, schema: { ...privado.schema, tags: ['sincronización'], summary: 'Envía cambios de la app y recibe los del servidor', body: S.syncBody } },
    async (req) =>
      actividades.sincronizar(req.user.sub, req.body.cursos, req.body.actividades, req.body.desde));

  // ─────────────── telemetría y logs ───────────────
  app.post<{ Body: { eventos: { tipo: string; pantalla?: string; datos?: unknown; creadoEn?: string }[] } }>('/eventos', { ...privado, schema: { ...privado.schema, tags: ['telemetría'], summary: 'Lote de eventos de uso', body: S.eventosBody }, config: { rateLimit: { max: 60, timeWindow: '1 minute' } } },
    async (req, reply) =>
      reply.status(202).send(await telemetria.registrarEventos(req.user.sid, req.user.sub, req.body.eventos)));

  app.post<{ Body: { logs: LineaLog[] } }>('/logs', { ...privado, schema: { ...privado.schema, tags: ['telemetría'], summary: 'Lote de logs WARN/ERROR de la app', body: S.logsBody }, config: { rateLimit: { max: 30, timeWindow: '1 minute' } } },
    async (req, reply) =>
      reply.status(202).send(await telemetria.registrarLogs(req.user.sid, req.body.logs)));

  // ─────────────── dashboard (admin) ───────────────
  app.get('/dashboard', { ...admin, schema: { ...admin.schema, tags: ['dashboard'], summary: 'Dashboard de utilización del producto' } }, async (_req, reply) => {
    const datos = await dashboard.obtener();
    reply.header('x-cache', datos.desdeCache ? 'HIT' : 'MISS');
    return datos;
  });

  app.post('/dashboard/refrescar', { ...admin, schema: { ...admin.schema, tags: ['dashboard'], summary: 'Recalcula la vista materializada y cierra sesiones inactivas' } },
    async () => dashboard.refrescar());
}
