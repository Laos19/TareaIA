import { randomUUID } from 'node:crypto';
import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { z } from 'zod';
import { ClienteTareia, ErrorApi } from './api.js';
import { formatoHistorial, formatoPendientes, formatoPlan, planSemanal } from './herramientas.js';

export const VERSION = '2.0.0';

type Resultado = { content: Array<{ type: 'text'; text: string }>; isError?: boolean };
const texto = (t: string): Resultado => ({ content: [{ type: 'text', text: t }] });
const error = (t: string): Resultado => ({ content: [{ type: 'text', text: t }], isError: true });

/** Opciones del servidor. */
export interface OpcionesServidor {
  cliente: ClienteTareia;
  /** Registrar el uso de cada herramienta como telemetría (TAREIA_TELEMETRIA=0 lo desactiva). */
  telemetria?: boolean;
  /** Fecha de hoy inyectable (pruebas). */
  hoy?: () => string;
}

/**
 * Crea el servidor MCP de TARE·IA. No conecta transporte: `index.ts` usa stdio y las pruebas, memoria.
 * Herramientas: estado_api, actividades_pendientes, registrar_actividad, cambiar_estado_actividad,
 * historial_actividad, plan_semanal y resumen_dashboard.
 */
export function crearServidor({ cliente, telemetria = true, hoy = () => new Date().toISOString().slice(0, 10) }: OpcionesServidor): McpServer {
  const server = new McpServer(
    { name: 'tareia', title: 'TARE·IA', version: VERSION },
    {
      instructions:
        'Herramientas del asistente académico TARE·IA. Usa actividades_pendientes para ver tareas y exámenes, ' +
        'registrar_actividad para anotar una tarea (queda POR VALIDAR, igual que lo que propone la IA en la app), ' +
        'cambiar_estado_actividad para validar, enviar al calendario, completar o reabrir, plan_semanal para organizar la semana ' +
        'y resumen_dashboard (cuenta admin) para métricas de uso. Responde en español.',
    },
  );

  const seguro = <A>(nombre: string, fn: (args: A) => Promise<Resultado>) => async (args: A): Promise<Resultado> => {
    try {
      const r = await fn(args);
      if (telemetria) cliente.registrarUso(nombre).catch(() => undefined);
      return r;
    } catch (e) {
      return error(`[${nombre}] ${e instanceof ErrorApi ? e.message : `Error inesperado: ${(e as Error).message}`}`);
    }
  };

  server.registerTool('estado_api', {
    title: 'Estado de la API',
    description: 'Comprueba que la API de TARE·IA y su base de datos respondan.',
    inputSchema: {},
    annotations: { readOnlyHint: true },
  }, seguro('estado_api', async () => {
    const s = await cliente.salud();
    return texto(`API ${cliente.baseUrl}: ${s.estado} · BD ${s.db} · versión ${s.version}`);
  }));

  server.registerTool('actividades_pendientes', {
    title: 'Actividades pendientes',
    description: 'Lista las tareas, exámenes y entregables no completados, ordenados por urgencia, con su id.',
    inputSchema: { limite: z.number().int().min(1).max(100).default(20).describe('Máximo de actividades a mostrar') },
    annotations: { readOnlyHint: true },
  }, seguro('actividades_pendientes', async ({ limite }: { limite: number }) => texto(formatoPendientes(await cliente.pendientes(), limite))));

  server.registerTool('registrar_actividad', {
    title: 'Registrar actividad',
    description: 'Anota una actividad académica. Queda PENDIENTE DE VALIDAR (RN09) hasta que el estudiante la confirme en la app o con cambiar_estado_actividad.',
    inputSchema: {
      titulo: z.string().min(1).max(200).describe('Título, p. ej. "Informe de laboratorio 3"'),
      fechaLimite: z.string().regex(/^\d{4}-\d{2}-\d{2}$/).optional().describe('Fecha de entrega AAAA-MM-DD'),
      descripcion: z.string().max(2000).optional(),
    },
    annotations: { readOnlyHint: false, idempotentHint: false },
  }, seguro('registrar_actividad', async (a: { titulo: string; fechaLimite?: string; descripcion?: string }) => {
    const r = await cliente.guardarActividad(randomUUID(), { titulo: a.titulo, fechaLimite: a.fechaLimite ?? null, descripcion: a.descripcion ?? '', origen: 'IA' });
    return texto(`Registrada «${r.titulo}» (${r.fechaLimite ?? 'sin fecha'}) en estado ${r.estado}. Id: ${r.id}`);
  }));

  server.registerTool('cambiar_estado_actividad', {
    title: 'Cambiar estado de una actividad',
    description: 'Aplica una acción del ciclo de vida (patrón State): confirmar, sincronizar (enviar a Google Calendar), completar o reabrir. La API rechaza transiciones inválidas con la regla incumplida.',
    inputSchema: {
      id: z.string().uuid().describe('Id de la actividad (de actividades_pendientes)'),
      accion: z.enum(['confirmar', 'sincronizar', 'completar', 'reabrir']),
    },
    annotations: { readOnlyHint: false, idempotentHint: true },
  }, seguro('cambiar_estado_actividad', async ({ id, accion }: { id: string; accion: 'confirmar' | 'sincronizar' | 'completar' | 'reabrir' }) => {
    const r = await cliente.accion(id, accion);
    return texto(`«${r.titulo}» ahora está en estado ${r.estado}.`);
  }));

  server.registerTool('historial_actividad', {
    title: 'Historial de una actividad',
    description: 'Muestra cada cambio registrado de una actividad (antes y después).',
    inputSchema: { id: z.string().uuid() },
    annotations: { readOnlyHint: true },
  }, seguro('historial_actividad', async ({ id }: { id: string }) => texto(formatoHistorial(await cliente.historial(id)))));

  server.registerTool('plan_semanal', {
    title: 'Plan semanal',
    description: 'Propone en qué día de los próximos 7 trabajar cada actividad confirmada, empezando por las más urgentes.',
    inputSchema: { porDia: z.number().int().min(1).max(5).default(2).describe('Actividades por día como máximo') },
    annotations: { readOnlyHint: true },
  }, seguro('plan_semanal', async ({ porDia }: { porDia: number }) => {
    const lista = await cliente.pendientes();
    const plan = planSemanal(lista, hoy(), porDia);
    return texto(formatoPlan(plan, lista.filter((a) => a.urgencia === 'VENCIDA').length, lista.filter((a) => a.estado === 'PENDIENTE_VALIDAR').length));
  }));

  server.registerTool('resumen_dashboard', {
    title: 'Resumen del dashboard de uso',
    description: 'Indicadores de utilización del producto (requiere una cuenta con rol admin).',
    inputSchema: {},
    annotations: { readOnlyHint: true },
  }, seguro('resumen_dashboard', async () => {
    const d = await cliente.dashboard();
    const r = d.resumen;
    const top = d.funciones.slice(0, 5).map((f) => `${f.tipo} (${f.total})`).join(', ');
    return texto([
      `Estudiantes: ${r.usuarios} (${r.usuariosActivos7d} activos en 7 días)`,
      `Sesiones en 7 días: ${r.sesiones7d} · abiertas: ${r.sesionesAbiertas} · duración media: ${r.duracionMediaMin ?? '—'} min`,
      `Actividades: ${r.actividades} · errores de la app en 7 días: ${r.erroresCliente7d}`,
      `Funciones más usadas: ${top || '—'}`,
    ].join('\n'));
  }));

  server.registerResource('guia', 'tareia://guia', {
    title: 'Guía de TARE·IA',
    description: 'Ciclo de vida de una actividad y reglas de negocio.',
    mimeType: 'text/markdown',
  }, async (uri) => ({
    contents: [{
      uri: uri.href,
      mimeType: 'text/markdown',
      text: [
        '# Ciclo de vida de una actividad (patrón State)',
        'PENDIENTE_VALIDAR → (confirmar) → CONFIRMADA → (sincronizar) → SINCRONIZADA → (completar) → COMPLETADA → (reabrir).',
        '- RN01: no se envía a Google Calendar nada sin validar.',
        '- RN02: no se permiten duplicados (mismo título, curso y fecha).',
        '- RN05: para confirmar se necesita título y fecha límite.',
        '- RN09: lo que propone la IA o llega de Moodle nace PENDIENTE_VALIDAR.',
      ].join('\n'),
    }],
  }));

  return server;
}
