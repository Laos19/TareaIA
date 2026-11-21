/**
 * Pruebas de INTEGRACIÓN: la API completa (rutas + servicios + repositorios) contra PostgreSQL real
 * migrado con Liquibase. Usan `app.inject` (sin abrir un puerto). Requiere DATABASE_URL.
 */
import { afterAll, beforeAll, describe, expect, it } from 'vitest';
import type { FastifyInstance } from 'fastify';
import { randomUUID } from 'node:crypto';
import { buildApp } from '../../src/app.js';
import { loadConfig } from '../../src/config.js';
import { DATABASE_URL, tokenGoogle, verificadorFalso } from '../ayudantes.js';

let app: FastifyInstance;
const sufijo = Date.now();
const correo = `test-${sufijo}@virtual.upt.pe`;
let token = '';
let tokenAdmin = '';
const cursoId = randomUUID();
const actividadId = randomUUID();

beforeAll(async () => {
  app = await buildApp(loadConfig({ DATABASE_URL, JWT_SECRET: 'p'.repeat(40), NODE_ENV: 'test', CACHE_TTL_SEGUNDOS: '60' }), { verificador: verificadorFalso });
  await app.ready();
});
afterAll(async () => {
  await app.servicios.db.query("DELETE FROM usuario WHERE email LIKE 'test-%@virtual.upt.pe'");
  await app.close();
});

const pedir = (method: 'GET' | 'POST' | 'PUT' | 'DELETE', url: string, payload?: unknown, t = token) =>
  app.inject({ method, url, payload: payload as object, headers: t ? { authorization: `Bearer ${t}` } : {} });

describe('sistema', () => {
  it('GET /api/health responde ok con la BD conectada', async () => {
    const r = await pedir('GET', '/api/health', undefined, '');
    expect(r.statusCode).toBe(200);
    expect(r.json()).toMatchObject({ estado: 'ok', db: 'ok' });
  });

  it('OpenAPI documenta las rutas principales', async () => {
    const paths = Object.keys((await pedir('GET', '/api/docs/json', undefined, '')).json().paths);
    expect(paths).toEqual(expect.arrayContaining(['/api/sync', '/api/actividades/{id}/estado', '/api/dashboard', '/api/eventos']));
  });
});

describe('autenticación y sesiones', () => {
  it('inicia sesión con Google, crea el usuario y abre una sesión', async () => {
    const r = await pedir('POST', '/api/auth/google', { idToken: tokenGoogle(`uid1-${sufijo}`, correo), dispositivo: 'Pixel', versionApp: '2.0.0' }, '');
    expect(r.statusCode).toBe(200);
    const body = r.json();
    token = body.token;
    expect(body.usuario).toMatchObject({ email: correo, rol: 'estudiante' });
    const [s] = await app.servicios.db.query('SELECT origen, dispositivo, cerrada_en FROM sesion WHERE id = $1', [body.sesionId]);
    expect(s).toMatchObject({ origen: 'android', dispositivo: 'Pixel', cerrada_en: null });
  });

  it('un segundo inicio con Google reutiliza el usuario (sin duplicar el correo)', async () => {
    const r = await pedir('POST', '/api/auth/google', { idToken: tokenGoogle(`uid1-${sufijo}`, correo.toUpperCase()) }, '');
    expect(r.statusCode).toBe(200);
    const filas = await app.servicios.db.query('SELECT id FROM usuario WHERE lower(email) = lower($1)', [correo]);
    expect(filas).toHaveLength(1);
  });

  it('rechaza un token de Google inválido', async () => {
    const r = await pedir('POST', '/api/auth/google', { idToken: 'x'.repeat(40) }, '');
    expect(r.statusCode).toBe(401);
    expect(r.json().error).toBe('TOKEN_GOOGLE');
  });

  it('login de administración con contraseña y rechazo de clave incorrecta', async () => {
    const ok = await pedir('POST', '/api/auth/login', { email: 'admin@demo.tareia.pe', password: 'tareia2026', origen: 'web' }, '');
    expect(ok.statusCode).toBe(200);
    tokenAdmin = ok.json().token;
    expect((await pedir('POST', '/api/auth/login', { email: 'admin@demo.tareia.pe', password: 'mala' }, '')).statusCode).toBe(401);
  });

  it('protege rutas privadas y de administración', async () => {
    expect((await pedir('GET', '/api/perfil', undefined, '')).statusCode).toBe(401);
    expect((await pedir('GET', '/api/perfil', undefined, 'token.falso.x')).statusCode).toBe(401);
    expect((await pedir('GET', '/api/dashboard')).statusCode).toBe(403);
    expect((await pedir('GET', '/api/perfil')).json()).toMatchObject({ email: correo });
  });

  it('valida el cuerpo de las peticiones (400)', async () => {
    const r = await pedir('POST', '/api/auth/login', { email: 'no-es-correo' }, '');
    expect(r.statusCode).toBe(400);
    expect(r.json().error).toBe('VALIDACION');
  });
});

describe('cursos y actividades', () => {
  it('crea un curso y una actividad manual que nace CONFIRMADA', async () => {
    expect((await pedir('PUT', `/api/cursos/${cursoId}`, { nombre: 'Calidad y Pruebas', abreviatura: 'SI-784', prioridad: 5 })).statusCode).toBe(200);
    const r = await pedir('PUT', `/api/actividades/${actividadId}`, { titulo: 'Informe de mutación', cursoId, fechaLimite: '2030-12-01' });
    expect(r.statusCode).toBe(200);
    expect(r.json()).toMatchObject({ estado: 'CONFIRMADA', origen: 'MANUAL', version: 1, fechaLimite: '2030-12-01' });
  });

  it('rechaza un duplicado con distinta capitalización y tildes (RN02)', async () => {
    const r = await pedir('PUT', `/api/actividades/${randomUUID()}`, { titulo: 'INFORME de mutación', cursoId, fechaLimite: '2030-12-01' });
    expect(r.statusCode).toBe(409);
  });

  it('lo que viene de la IA nace PENDIENTE_VALIDAR y no se puede sincronizar (RN01)', async () => {
    const id = randomUUID();
    const a = await pedir('PUT', `/api/actividades/${id}`, { titulo: 'Leer capítulo 4', origen: 'IA' });
    expect(a.json().estado).toBe('PENDIENTE_VALIDAR');
    const s = await pedir('POST', `/api/actividades/${id}/estado`, { accion: 'sincronizar' });
    expect(s.statusCode).toBe(409);
    expect(s.json().mensaje).toContain('RN01');
    const c = await pedir('POST', `/api/actividades/${id}/estado`, { accion: 'confirmar' });
    expect(c.statusCode).toBe(409);
    expect(c.json().mensaje).toContain('RN05');
  });

  it('recorre el ciclo confirmar → sincronizar → completar → reabrir', async () => {
    const accion = async (a: string) => (await pedir('POST', `/api/actividades/${actividadId}/estado`, { accion: a })).json().estado;
    expect(await accion('sincronizar')).toBe('SINCRONIZADA');
    expect(await accion('completar')).toBe('COMPLETADA');
    expect(await accion('reabrir')).toBe('CONFIRMADA');
  });

  it('el trigger de la BD bloquea una transición inválida aunque se salte la API', async () => {
    const r = await pedir('PUT', `/api/actividades/${actividadId}`, { titulo: 'Informe de mutación', cursoId, fechaLimite: '2030-12-01', estado: 'PENDIENTE_VALIDAR' });
    expect(r.statusCode).toBe(409);
    expect(r.json().error).toBe('REGLA_NEGOCIO');
  });

  it('guarda el historial de cambios con antes y después', async () => {
    const h = (await pedir('GET', `/api/actividades/${actividadId}/historial`)).json();
    expect(h.length).toBeGreaterThanOrEqual(4);
    expect(h.at(-1)).toMatchObject({ operacion: 'I', antes: null });
    expect(h[0].despues.estado).toBe('CONFIRMADA');
  });

  it('papelera: enviar oculta de pendientes y restaurar la recupera', async () => {
    expect((await pedir('DELETE', `/api/actividades/${actividadId}`)).statusCode).toBe(204);
    let pend = (await pedir('GET', '/api/actividades/pendientes')).json();
    expect(pend.find((p: { id: string }) => p.id === actividadId)).toBeUndefined();
    expect((await pedir('POST', `/api/actividades/${actividadId}/restaurar`)).json().eliminadoEn).toBeNull();
    pend = (await pedir('GET', '/api/actividades/pendientes')).json();
    expect(pend.find((p: { id: string }) => p.id === actividadId)).toMatchObject({ urgencia: 'BAJA' });
  });

  it('404 para recursos de otro usuario o inexistentes', async () => {
    const otro = (await pedir('POST', '/api/auth/google', { idToken: tokenGoogle(`uid2-${sufijo}`, `test-otro-${sufijo}@virtual.upt.pe`) }, '')).json().token;
    expect((await pedir('POST', `/api/actividades/${actividadId}/estado`, { accion: 'completar' }, otro)).statusCode).toBe(404);
    expect((await pedir('DELETE', `/api/cursos/${randomUUID()}`)).statusCode).toBe(404);
    expect((await pedir('GET', '/api/actividades', undefined, otro)).json()).toEqual([]);
  });
});

describe('sincronización', () => {
  it('aplica cambios del cliente y devuelve los del servidor desde una fecha', async () => {
    const desde = new Date(Date.now() - 60_000).toISOString();
    const nueva = randomUUID();
    const r = await pedir('POST', '/api/sync', {
      desde,
      cursos: [],
      actividades: [
        { id: nueva, titulo: 'Tarea desde Moodle', origen: 'INTEGRACION', idExterno: `moodle:1:${sufijo}`, fechaLimite: '2030-11-30' },
        { id: actividadId, titulo: 'Título viejo', cursoId, fechaLimite: '2030-12-01', actualizadoEn: '2020-01-01T00:00:00Z', version: 1 },
      ],
    });
    expect(r.statusCode).toBe(200);
    const body = r.json();
    expect(body).toMatchObject({ aplicados: 1, conservados: 1, rechazados: [] });
    expect(body.actividades.map((a: { id: string }) => a.id)).toEqual(expect.arrayContaining([nueva, actividadId]));
    expect(body.actividades.find((a: { id: string }) => a.id === nueva).estado).toBe('PENDIENTE_VALIDAR');
  });

  it('valida el lote (400 si un id no es UUID)', async () => {
    expect((await pedir('POST', '/api/sync', { actividades: [{ id: 'abc', titulo: 'x' }] })).statusCode).toBe(400);
  });
});

describe('telemetría, logs y dashboard', () => {
  it('guarda eventos válidos y descarta los desconocidos', async () => {
    const r = await pedir('POST', '/api/eventos', { eventos: [
      { tipo: 'app_abierta' }, { tipo: 'pantalla_vista', pantalla: 'Tareas', datos: { email: 'x@y.pe', ms: 80 } }, { tipo: 'desconocido' },
    ] });
    expect(r.statusCode).toBe(202);
    expect(r.json()).toEqual({ guardados: 2, descartados: 1 });
    const [e] = await app.servicios.db.query("SELECT datos FROM evento WHERE pantalla = 'Tareas' ORDER BY id DESC LIMIT 1");
    expect(e!.datos).toEqual({ ms: 80 });
  });

  it('guarda solo logs WARN/ERROR', async () => {
    const r = await pedir('POST', '/api/logs', { logs: [{ nivel: 'ERROR', etiqueta: 'Sync', mensaje: 'timeout' }, { nivel: 'DEBUG', etiqueta: 'x', mensaje: 'y' }] });
    expect(r.json()).toEqual({ guardados: 1, descartados: 1 });
  });

  it('el dashboard usa caché (MISS → HIT) y se invalida al refrescar', async () => {
    await pedir('POST', '/api/dashboard/refrescar', undefined, tokenAdmin);
    const a = await pedir('GET', '/api/dashboard', undefined, tokenAdmin);
    expect(a.statusCode).toBe(200);
    expect(a.headers['x-cache']).toBe('MISS');
    const d = a.json();
    expect(d.usoDiario).toHaveLength(30);
    expect(d.resumen.usuarios).toBeGreaterThanOrEqual(2);
    expect(d.funciones.length).toBeGreaterThan(0);
    expect((await pedir('GET', '/api/dashboard', undefined, tokenAdmin)).headers['x-cache']).toBe('HIT');
    const ref = await pedir('POST', '/api/dashboard/refrescar', undefined, tokenAdmin);
    expect(ref.json()).toHaveProperty('sesionesCerradas');
    expect((await pedir('GET', '/api/dashboard', undefined, tokenAdmin)).headers['x-cache']).toBe('MISS');
  });

  it('logout cierra la sesión y el mismo token deja de servir', async () => {
    expect((await pedir('POST', '/api/auth/logout')).statusCode).toBe(204);
    const r = await pedir('GET', '/api/perfil');
    expect(r.statusCode).toBe(401);
    expect(r.json().error).toBe('SESION_CERRADA');
  });
});
