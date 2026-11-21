import { Client } from '@modelcontextprotocol/sdk/client/index.js';
import { InMemoryTransport } from '@modelcontextprotocol/sdk/inMemory.js';
import { describe, expect, it } from 'vitest';
import { ClienteTareia } from '../src/api.js';
import { crearServidor } from '../src/servidor.js';

/** fetch falso que responde como la API de TARE·IA y guarda las peticiones. */
function fetchFalso() {
  const llamadas: Array<{ url: string; init?: RequestInit }> = [];
  const impl = (async (input: string | URL | Request, init?: RequestInit) => {
    const url = String(input);
    llamadas.push({ url, init });
    const json = (d: unknown, status = 200) => new Response(JSON.stringify(d), { status, headers: { 'content-type': 'application/json' } });
    const auth = new Headers(init?.headers).get('authorization');
    if (url.endsWith('/api/health')) return json({ estado: 'ok', db: 'ok', version: '2.0.0' });
    if (url.endsWith('/api/auth/login')) {
      const b = JSON.parse(String(init?.body));
      return b.password === 'correcta' ? json({ token: b.email.startsWith('admin') ? 'jwt-admin' : 'jwt-est' }) : json({ mensaje: 'Correo o contraseña incorrectos' }, 401);
    }
    if (!auth) return json({ mensaje: 'Inicia sesión para continuar' }, 401);
    if (url.endsWith('/api/actividades/pendientes')) {
      return json([{ id: '11111111-1111-4111-8111-111111111111', titulo: 'Informe', tipo: 'TAREA', estado: 'CONFIRMADA', origen: 'MANUAL', fechaLimite: '2030-01-02', curso: 'Calidad', diasRestantes: 1, urgencia: 'CRITICA' }]);
    }
    if (/\/api\/actividades\/[\w-]+\/estado$/.test(url)) {
      const { accion } = JSON.parse(String(init?.body));
      return accion === 'sincronizar' ? json({ mensaje: 'Confirma o corrige primero esta actividad (RN01)' }, 409) : json({ id: 'x', titulo: 'Informe', estado: 'COMPLETADA' });
    }
    if (/\/api\/actividades\/[\w-]+\/historial$/.test(url)) return json([{ operacion: 'I', antes: null, despues: { estado: 'CONFIRMADA' }, cambiadoEn: '2030-01-01T09:00:00Z' }]);
    if (/\/api\/actividades\/[\w-]+$/.test(url) && init?.method === 'PUT') {
      const b = JSON.parse(String(init.body));
      return json({ id: url.split('/').pop(), titulo: b.titulo, estado: 'PENDIENTE_VALIDAR', fechaLimite: b.fechaLimite, origen: b.origen });
    }
    if (url.endsWith('/api/dashboard')) {
      return auth === 'Bearer jwt-admin'
        ? json({ resumen: { usuarios: 3, usuariosActivos7d: 2, sesiones7d: 9, sesionesAbiertas: 1, duracionMediaMin: 12.5, actividades: 14, erroresCliente7d: 0 }, funciones: [{ tipo: 'actividad_creada', total: 7 }], origenActividades: [] })
        : json({ mensaje: 'Tu rol no permite esta acción' }, 403);
    }
    if (url.endsWith('/api/eventos')) return json({ guardados: 1, descartados: 0 }, 202);
    return json({ mensaje: 'no encontrado' }, 404);
  }) as typeof fetch;
  return { impl, llamadas };
}

async function conectar(cred: { email?: string; password?: string } = { email: 'est@upt.pe', password: 'correcta' }) {
  const f = fetchFalso();
  const server = crearServidor({ cliente: new ClienteTareia({ baseUrl: 'http://api.test/', ...cred, fetchImpl: f.impl }), hoy: () => '2030-01-01' });
  const [a, b] = InMemoryTransport.createLinkedPair();
  const client = new Client({ name: 'prueba', version: '1.0.0' });
  await Promise.all([server.connect(a), client.connect(b)]);
  const llamar = async (name: string, args: Record<string, unknown> = {}) => {
    const r = await client.callTool({ name, arguments: args });
    return { texto: (r.content as Array<{ text: string }>)[0]!.text, isError: Boolean(r.isError) };
  };
  return { client, llamar, llamadas: f.llamadas };
}

describe('servidor MCP de TARE·IA (protocolo en memoria)', () => {
  it('expone las 7 herramientas y la guía', async () => {
    const { client } = await conectar();
    const { tools } = await client.listTools();
    expect(tools.map((t) => t.name).sort()).toEqual(['actividades_pendientes', 'cambiar_estado_actividad', 'estado_api', 'historial_actividad', 'plan_semanal', 'registrar_actividad', 'resumen_dashboard']);
    const { resources } = await client.listResources();
    expect(resources[0]!.uri).toBe('tareia://guia');
    const guia = await client.readResource({ uri: 'tareia://guia' });
    expect((guia.contents[0] as { text: string }).text).toContain('RN09');
  });

  it('estado_api no requiere sesión', async () => {
    const { llamar } = await conectar({});
    expect((await llamar('estado_api')).texto).toContain('ok · BD ok');
  });

  it('inicia sesión con origen mcp y lista pendientes', async () => {
    const { llamar, llamadas } = await conectar();
    const r = await llamar('actividades_pendientes');
    expect(r.texto).toContain('**Informe** · Calidad');
    const login = llamadas.find((l) => l.url.endsWith('/api/auth/login'))!;
    expect(JSON.parse(String(login.init?.body))).toMatchObject({ origen: 'mcp' });
  });

  it('registrar_actividad crea con origen IA (queda por validar)', async () => {
    const { llamar, llamadas } = await conectar();
    const r = await llamar('registrar_actividad', { titulo: 'Leer capítulo 5', fechaLimite: '2030-01-10' });
    expect(r.texto).toContain('PENDIENTE_VALIDAR');
    const put = llamadas.find((l) => l.init?.method === 'PUT')!;
    expect(JSON.parse(String(put.init?.body))).toMatchObject({ origen: 'IA', titulo: 'Leer capítulo 5' });
  });

  it('devuelve el rechazo de la regla de negocio como error de herramienta', async () => {
    const { llamar } = await conectar();
    const r = await llamar('cambiar_estado_actividad', { id: '11111111-1111-4111-8111-111111111111', accion: 'sincronizar' });
    expect(r.isError).toBe(true);
    expect(r.texto).toContain('RN01');
    expect((await llamar('cambiar_estado_actividad', { id: '11111111-1111-4111-8111-111111111111', accion: 'completar' })).texto).toContain('COMPLETADA');
  });

  it('plan semanal e historial', async () => {
    const { llamar } = await conectar();
    expect((await llamar('plan_semanal')).texto).toContain('**2030-01-01**: Informe');
    expect((await llamar('historial_actividad', { id: '11111111-1111-4111-8111-111111111111' })).texto).toContain('creada');
  });

  it('el dashboard exige rol admin', async () => {
    const est = await conectar();
    expect((await est.llamar('resumen_dashboard')).isError).toBe(true);
    const admin = await conectar({ email: 'admin@upt.pe', password: 'correcta' });
    expect((await admin.llamar('resumen_dashboard')).texto).toContain('Estudiantes: 3');
  });

  it('sin credenciales explica cómo configurarlas; con clave incorrecta informa el error', async () => {
    expect((await (await conectar({})).llamar('actividades_pendientes')).texto).toContain('TAREIA_EMAIL');
    expect((await (await conectar({ email: 'x@upt.pe', password: 'mala' })).llamar('actividades_pendientes')).texto).toContain('incorrectos');
  });

  it('valida los argumentos (id debe ser UUID)', async () => {
    const { client } = await conectar();
    const r = await client.callTool({ name: 'cambiar_estado_actividad', arguments: { id: 'abc', accion: 'completar' } });
    expect(r.isError).toBe(true);
  });
});
