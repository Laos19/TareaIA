/**
 * Pruebas de SISTEMA: levantan el servidor real en un puerto (HTTP de verdad, con CORS, helmet,
 * límite de peticiones y Swagger) y recorren el flujo completo de una estudiante y del administrador
 * tal como lo hacen la app Android y el dashboard web.
 */
import { afterAll, beforeAll, describe, expect, it } from 'vitest';
import type { FastifyInstance } from 'fastify';
import { randomUUID } from 'node:crypto';
import { buildApp } from '../../src/app.js';
import { loadConfig } from '../../src/config.js';
import { DATABASE_URL, tokenGoogle, verificadorFalso } from '../ayudantes.js';

let app: FastifyInstance;
let base = '';
const sufijo = Date.now();

beforeAll(async () => {
  app = await buildApp(loadConfig({ DATABASE_URL, JWT_SECRET: 's'.repeat(40), NODE_ENV: 'test', CORS_ORIGIN: 'https://tareia-dashboard.onrender.com' }), { verificador: verificadorFalso });
  await app.listen({ port: 0, host: '127.0.0.1' });
  const dir = app.server.address();
  base = `http://127.0.0.1:${typeof dir === 'object' && dir ? dir.port : 0}`;
});
afterAll(async () => {
  await app.servicios.db.query("DELETE FROM usuario WHERE email LIKE 'sis-%@virtual.upt.pe'");
  await app.close();
});

const http = async (metodo: string, ruta: string, cuerpo?: unknown, token?: string, extra: Record<string, string> = {}) => {
  const r = await fetch(base + ruta, {
    method: metodo,
    headers: { ...(cuerpo ? { 'content-type': 'application/json' } : {}), ...(token ? { authorization: `Bearer ${token}` } : {}), ...extra },
    body: cuerpo ? JSON.stringify(cuerpo) : undefined,
  });
  const texto = await r.text();
  return { status: r.status, headers: r.headers, json: texto && r.headers.get('content-type')?.includes('json') ? JSON.parse(texto) : texto };
};

describe('flujo de la estudiante (app Android)', () => {
  let token = '';
  const cursoId = randomUUID();
  const idIa = randomUUID();

  it('1. inicia sesión con Google', async () => {
    const r = await http('POST', '/api/auth/google', { idToken: tokenGoogle(`sis-${sufijo}`, `sis-${sufijo}@virtual.upt.pe`), origen: 'android', dispositivo: 'Emulador API 35' });
    expect(r.status).toBe(200);
    token = r.json.token;
  });

  it('2. sincroniza su curso y una propuesta de la IA en un solo lote', async () => {
    const r = await http('POST', '/api/sync', {
      cursos: [{ id: cursoId, nombre: 'Calidad y Pruebas de Software', abreviatura: 'SI-784', prioridad: 5, dificultad: 4 }],
      actividades: [{ id: idIa, cursoId, titulo: 'Proyecto de Unidad II', origen: 'IA', fechaLimite: '2030-11-26' }],
    }, token);
    expect(r.status).toBe(200);
    expect(r.json.aplicados).toBe(2);
    expect(r.json.actividades[0]).toMatchObject({ id: idIa, estado: 'PENDIENTE_VALIDAR' });
  });

  it('3. valida la propuesta, la envía al calendario y la completa', async () => {
    for (const [accion, estado] of [['confirmar', 'CONFIRMADA'], ['sincronizar', 'SINCRONIZADA'], ['completar', 'COMPLETADA']] as const) {
      const r = await http('POST', `/api/actividades/${idIa}/estado`, { accion }, token);
      expect(r.status).toBe(200);
      expect(r.json.estado).toBe(estado);
    }
  });

  it('4. envía telemetría y logs en lote', async () => {
    const e = await http('POST', '/api/eventos', { eventos: [{ tipo: 'app_abierta' }, { tipo: 'actividad_completada', pantalla: 'Tareas' }] }, token);
    expect(e.status).toBe(202);
    const l = await http('POST', '/api/logs', { logs: [{ nivel: 'WARN', etiqueta: 'Calendario', mensaje: 'reintento' }] }, token);
    expect(l.json.guardados).toBe(1);
  });

  it('5. consulta su historial y cierra sesión', async () => {
    const h = await http('GET', `/api/actividades/${idIa}/historial`, undefined, token);
    expect(h.json.map((x: { despues: { estado: string } }) => x.despues.estado)).toEqual(['COMPLETADA', 'SINCRONIZADA', 'CONFIRMADA', 'PENDIENTE_VALIDAR']);
    expect((await http('POST', '/api/auth/logout', undefined, token)).status).toBe(204);
    expect((await http('GET', '/api/perfil', undefined, token)).status).toBe(401);
  });
});

describe('flujo del administrador (dashboard web)', () => {
  it('inicia sesión y ve el dashboard con métricas de 30 días', async () => {
    const login = await http('POST', '/api/auth/login', { email: 'admin@demo.tareia.pe', password: 'tareia2026', origen: 'web' });
    expect(login.status).toBe(200);
    const d = await http('GET', '/api/dashboard', undefined, login.json.token);
    expect(d.status).toBe(200);
    expect(d.json.usoDiario).toHaveLength(30);
    expect(d.json.resumen).toHaveProperty('duracionMediaMin');
  });
});

describe('requisitos no funcionales sobre HTTP real', () => {
  it('CORS solo permite el origen configurado', async () => {
    const ok = await http('GET', '/api/health', undefined, undefined, { origin: 'https://tareia-dashboard.onrender.com' });
    expect(ok.headers.get('access-control-allow-origin')).toBe('https://tareia-dashboard.onrender.com');
    const otro = await http('GET', '/api/health', undefined, undefined, { origin: 'https://malicioso.example' });
    expect(otro.headers.get('access-control-allow-origin')).toBeNull();
  });

  it('helmet agrega cabeceras de seguridad', async () => {
    const r = await http('GET', '/api/health');
    expect(r.headers.get('x-content-type-options')).toBe('nosniff');
    expect(r.headers.get('x-frame-options')).toBeTruthy();
  });

  it('Swagger UI está disponible', async () => {
    const r = await http('GET', '/api/docs');
    expect([200, 302]).toContain(r.status);
  });

  it('limita los intentos de login (10 por minuto)', async () => {
    const codigos: number[] = [];
    for (let i = 0; i < 12; i++) {
      codigos.push((await http('POST', '/api/auth/login', { email: `sis-x${i}@virtual.upt.pe`, password: 'mala' })).status);
    }
    expect(codigos).toContain(429);
  });

  it('responde 404 en JSON para rutas inexistentes', async () => {
    const r = await http('GET', '/api/no-existe');
    expect(r.status).toBe(404);
  });
});
