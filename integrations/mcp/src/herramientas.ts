/** Funciones puras que dan formato a las respuestas de las herramientas MCP (probadas sin red). */
import type { Pendiente } from './api.js';

const ORDEN: Record<Pendiente['urgencia'], number> = { VENCIDA: 0, CRITICA: 1, ALTA: 2, MEDIA: 3, BAJA: 4, SIN_FECHA: 5 };
const ETIQUETA: Record<Pendiente['urgencia'], string> = {
  VENCIDA: '⛔ vencida',
  CRITICA: '🔴 vence en ≤ 1 día',
  ALTA: '🟠 ≤ 3 días',
  MEDIA: '🟡 ≤ 7 días',
  BAJA: '🟢 más de 7 días',
  SIN_FECHA: '⚪ sin fecha',
};

/** Ordena por urgencia y luego por fecha. */
export function ordenarPorUrgencia(lista: Pendiente[]): Pendiente[] {
  return [...lista].sort((a, b) => ORDEN[a.urgencia] - ORDEN[b.urgencia] || (a.fechaLimite ?? '9999').localeCompare(b.fechaLimite ?? '9999'));
}

/** Lista de pendientes en Markdown. */
export function formatoPendientes(lista: Pendiente[], limite = 20): string {
  if (lista.length === 0) return 'No tienes actividades pendientes. 🎉';
  const filas = ordenarPorUrgencia(lista).slice(0, limite).map((a) =>
    `- **${a.titulo}**${a.curso ? ` · ${a.curso}` : ''} — ${a.fechaLimite ?? 'sin fecha'} (${ETIQUETA[a.urgencia]})${a.estado === 'PENDIENTE_VALIDAR' ? ' · _por validar_' : ''} · id \`${a.id}\``);
  const porValidar = lista.filter((a) => a.estado === 'PENDIENTE_VALIDAR').length;
  return [`Tienes ${lista.length} actividad(es) pendiente(s)${porValidar ? `, ${porValidar} por validar` : ''}:`, '', ...filas].join('\n');
}

/**
 * Propone en qué días trabajar cada actividad durante los próximos 7 días: las más urgentes primero,
 * como máximo `porDia` actividades por día y nunca después de su fecha límite.
 * @param lista Actividades pendientes.
 * @param hoy Fecha de inicio (ISO AAAA-MM-DD).
 */
export function planSemanal(lista: Pendiente[], hoy: string, porDia = 2): { dia: string; actividades: string[] }[] {
  const dias = Array.from({ length: 7 }, (_, i) => {
    const d = new Date(`${hoy}T00:00:00Z`);
    d.setUTCDate(d.getUTCDate() + i);
    return { dia: d.toISOString().slice(0, 10), actividades: [] as string[] };
  });
  for (const a of ordenarPorUrgencia(lista).filter((x) => x.estado !== 'PENDIENTE_VALIDAR' && x.urgencia !== 'VENCIDA')) {
    const libre = dias.find((d) => d.actividades.length < porDia && (!a.fechaLimite || d.dia <= a.fechaLimite));
    if (libre) libre.actividades.push(a.titulo);
  }
  return dias;
}

/** Plan semanal en Markdown. */
export function formatoPlan(plan: { dia: string; actividades: string[] }[], vencidas: number, porValidar: number): string {
  const lineas = plan.map((d) => `- **${d.dia}**: ${d.actividades.length ? d.actividades.join(' · ') : '_libre_'}`);
  const avisos = [
    vencidas ? `⚠️ ${vencidas} actividad(es) vencida(s): revísalas primero.` : '',
    porValidar ? `ℹ️ ${porValidar} actividad(es) por validar no se planifican hasta confirmarlas (RN09).` : '',
  ].filter(Boolean);
  return ['Plan sugerido para los próximos 7 días:', '', ...lineas, ...(avisos.length ? ['', ...avisos] : [])].join('\n');
}

/** Historial de cambios en Markdown. */
export function formatoHistorial(h: Array<{ operacion: string; antes: Record<string, unknown> | null; despues: Record<string, unknown> | null; cambiadoEn: string }>): string {
  if (h.length === 0) return 'Sin historial.';
  const op: Record<string, string> = { I: 'creada', U: 'modificada', D: 'eliminada' };
  return h.map((x) => {
    const cambios = x.antes && x.despues
      ? Object.keys(x.despues).filter((k) => !['version', 'actualizado_en'].includes(k) && JSON.stringify(x.antes![k]) !== JSON.stringify(x.despues![k])).map((k) => `${k}: ${JSON.stringify(x.antes![k])} → ${JSON.stringify(x.despues![k])}`)
      : [];
    return `- ${x.cambiadoEn.slice(0, 16).replace('T', ' ')} · ${op[x.operacion] ?? x.operacion}${cambios.length ? ` (${cambios.join('; ')})` : ''}`;
  }).join('\n');
}
